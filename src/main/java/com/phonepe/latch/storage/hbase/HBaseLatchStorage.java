/**
 * Copyright (c) 2026 Original Author(s), PhonePe India Pvt. Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.phonepe.latch.storage.hbase;

import com.phonepe.latch.common.Constants;
import com.phonepe.latch.common.LatchLevel;
import com.phonepe.latch.common.TriFunction;
import com.phonepe.latch.exception.DistributedLatchException;
import com.phonepe.latch.storage.BaseLatchStorage;
import com.phonepe.latch.storage.StorageType;
import com.phonepe.latch.watcher.IWatcher;
import com.sematext.hbase.ds.AbstractRowKeyDistributor;
import com.sematext.hbase.ds.RowKeyDistributorByHashPrefix;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.io.compress.Compression;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Slf4j
public class HBaseLatchStorage extends BaseLatchStorage {
    private static final RowKeyDistributorByHashPrefix.Hasher ONE_BYTE_HASHER =
            new RowKeyDistributorByHashPrefix.OneByteSimpleHash(256);
    private static final AbstractRowKeyDistributor ROW_KEY_DISTRIBUTOR =
            new RowKeyDistributorByHashPrefix(ONE_BYTE_HASHER);
    private static final byte[] CF_NAME = Bytes.toBytes("C");
    private static final String DELIMITER = "_";
    private static final String COUNT_COL_PREFIX = "count";
    private final HBaseLatchStorageContext storageContext;

    private final ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> scheduledFutureForWatching;

    public HBaseLatchStorage(final HBaseLatchStorageContext storageContext) {
        super(StorageType.HBASE);
        this.storageContext = storageContext;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void initialize(String clientId, String farmId, String latchId, long count) {
        try {

            final TableName tableName = TableName.valueOf(getTableName());
            createTableIfNotExists(tableName);

            storageContext.getConnection().getTable(tableName)
                    .increment(new Increment(buildRowKey(clientId, latchId))
                            .addColumn(CF_NAME, getCountColumn(farmId), count)
                            .setTTL(storageContext.getTtl() * 1000L));
        } catch (IOException e) {
            throw DistributedLatchException.propagate(e);
        }
    }

    @Override
    public void decrementCount(String clientId, String farmId, String latchId) {
        try {
            storageContext.getConnection().getTable(TableName.valueOf(getTableName()))
                    .increment(new Increment(buildRowKey(clientId, latchId))
                            .addColumn(CF_NAME, getCountColumn(farmId), -1));
        } catch (IOException e) {
            throw DistributedLatchException.propagate(e);
        }
    }

    @Override
    public void incrementCount(String clientId, String farmId, String latchId) {
        try {
            storageContext.getConnection().getTable(TableName.valueOf(getTableName()))
                    .increment(new Increment(buildRowKey(clientId, latchId))
                            .addColumn(CF_NAME, getCountColumn(farmId), 1));
        } catch (IOException e) {
            throw DistributedLatchException.propagate(e);
        }
    }

    @Override
    public long getCount(String clientId, LatchLevel latchLevel, String farmId, String latchId) {
        return latchLevel.accept(new LatchLevel.Visitor<Long>() {
            @Override
            public Long visitDC() {
                try {
                    val result = storageContext.getConnection().getTable(TableName.valueOf(getTableName()))
                            .get(new Get(buildRowKey(clientId, latchId))
                                    .addColumn(CF_NAME, getCountColumn(farmId)));
                    return Bytes.toLong(result.getValue(CF_NAME, getCountColumn(farmId)));
                } catch (IOException e) {
                    throw DistributedLatchException.propagate(e);
                }
            }

            @Override
            public Long visitXDC() {
                try {
                    val qualifierValueMap = storageContext.getConnection().getTable(TableName.valueOf(getTableName()))
                            .get(new Get(buildRowKey(clientId, latchId))
                                    .addFamily(CF_NAME)).getFamilyMap(CF_NAME);
                    return qualifierValueMap.values()
                            .stream()
                            .mapToLong(Bytes::toLong)
                            .sum();
                } catch (IOException e) {
                    throw DistributedLatchException.propagate(e);
                }
            }
        });

    }

    @Override
    public void startWatching(String clientId,
                              LatchLevel latchLevel,
                              String farmId,
                              String latchId,
                              CountDownLatch latch,
                              TriFunction<IWatcher, CountDownLatch, Long, Void> function) {
        scheduledFutureForWatching = scheduledExecutorService.scheduleAtFixedRate(
                () -> function.apply(HBaseLatchStorage.this, latch, getCount(clientId, latchLevel, farmId, latchId)), 0, 5,
                TimeUnit.SECONDS);
    }

    @Override
    public void stopWatching() {
        scheduledFutureForWatching.cancel(false);
    }

    private void createTableIfNotExists(TableName tableName) throws IOException {
        final boolean tableExists = storageContext.getConnection().getAdmin().tableExists(tableName);
        if (!tableExists) {
            final TableDescriptor tableDescriptor = TableDescriptorBuilder.newBuilder(tableName)
                    .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(CF_NAME)
                            .setCompressionType(Compression.Algorithm.GZ)
                            .build())
                    .build();
            storageContext.getConnection().getAdmin()
                    .createTable(tableDescriptor, ONE_BYTE_HASHER.getAllPossiblePrefixes());
        }
    }

    private String getTableName() {
        return String.join(DELIMITER, Constants.D_LATCH, storageContext.getTableSuffix());
    }

    private byte[] buildRowKey(final String clientId, final String latchId) {
        return ROW_KEY_DISTRIBUTOR.getDistributedKey(Bytes.toBytes(String.join(DELIMITER, clientId, latchId)));
    }

    private byte[] getCountColumn(final String farmId) {
        return Bytes.toBytes(String.join(DELIMITER, COUNT_COL_PREFIX, farmId));
    }
}
