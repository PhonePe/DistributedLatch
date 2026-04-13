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

package com.phonepe.latch.storage.aerospike;

import com.aerospike.client.*;
import com.aerospike.client.Record;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.github.rholder.retry.*;
import com.phonepe.latch.common.Constants;
import com.phonepe.latch.common.LatchLevel;
import com.phonepe.latch.common.TriFunction;
import com.phonepe.latch.exception.DistributedLatchException;
import com.phonepe.latch.storage.BaseLatchStorage;
import com.phonepe.latch.storage.StorageType;
import com.phonepe.latch.watcher.IWatcher;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.*;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class AerospikeLatchStorage extends BaseLatchStorage {
    public static final String COUNT_BIN_PREFIX = "count";
    private static final String KEY_FORMAT = String.join(Constants.DELIMITER, Constants.D_LATCH, "%s#%s");

    private final AerospikeLatchStorageContext storageContext;
    private final Retryer<Long> readRetryer;
    private final Retryer<Void> writeRetryer;
    private final ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> scheduledFutureForWatching;

    public AerospikeLatchStorage(final AerospikeLatchStorageContext storageContext) {
        super(StorageType.AEROSPIKE);
        this.storageContext = storageContext;
        this.readRetryer = RetryerBuilder.<Long>newBuilder().retryIfExceptionOfType(AerospikeException.class)
                .withStopStrategy(StopStrategies.stopAfterAttempt(5))
                .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.MILLISECONDS))
                .withBlockStrategy(BlockStrategies.threadSleepStrategy()).build();

        this.writeRetryer = RetryerBuilder.<Void>newBuilder().retryIfExceptionOfType(AerospikeException.class)
                .withStopStrategy(StopStrategies.stopAfterAttempt(5))
                .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.MILLISECONDS))
                .withBlockStrategy(BlockStrategies.threadSleepStrategy()).build();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void initialize(final String clientId, final String farmId,
                           final String latchId, final long count) {
        try {
            final WritePolicy initializationWritePolicy = new WritePolicy(
                    storageContext.getAerospikeClient().getWritePolicyDefault());
            initializationWritePolicy.recordExistsAction = RecordExistsAction.CREATE_ONLY;
            initializationWritePolicy.expiration = storageContext.getTtl();
            writeRetryer.call(() -> {
                storageContext.getAerospikeClient()
                        .put(initializationWritePolicy, new Key(storageContext.getNamespace(),
                                        getSetName(), buildKey(clientId, latchId)),
                                new Bin(getCountBin(farmId), count));
                return null;
            });
        } catch (ExecutionException | RetryException e) {
            throw DistributedLatchException.propagate(e);
        }
    }

    @Override
    public void decrementCount(final String clientId, final String farmId, final String latchId) {
        updateCount(clientId, farmId, latchId, -1);
    }

    @Override
    public void incrementCount(final String clientId, final String farmId, final String latchId) {
        updateCount(clientId, farmId, latchId, 1);
    }

    @Override
    public long getCount(final String clientId, final LatchLevel latchLevel,
                         final String farmId, final String latchId) {
        try {
            return readRetryer.call(() -> {
                final Record record = storageContext.getAerospikeClient().get(
                        storageContext.getAerospikeClient().getReadPolicyDefault(),
                        new Key(storageContext.getNamespace(), getSetName(),
                                buildKey(clientId, latchId)));

                return latchLevel.accept(new LatchLevel.Visitor<>() {
                    @Override
                    public Long visitDC() {
                        return record.getLong(getCountBin(farmId));
                    }

                    @Override
                    public Long visitXDC() {
                        return record.bins.keySet().stream()
                                .filter(bin -> bin.contains(COUNT_BIN_PREFIX.concat(Constants.DELIMITER)))
                                .map(record::getLong)
                                .reduce(Long::sum)
                                .orElse(0L);
                    }
                });
            });
        } catch (ExecutionException | RetryException e) {
            throw DistributedLatchException.propagate(e);
        }
    }

    @Override
    public void startWatching(final String clientId,
                              final LatchLevel latchLevel,
                              final String farmId,
                              final String latchId,
                              final CountDownLatch latch,
                              final TriFunction<IWatcher, CountDownLatch, Long, Void> function) {
        scheduledFutureForWatching = scheduledExecutorService.scheduleAtFixedRate(
                () -> function.apply(AerospikeLatchStorage.this, latch, getCount(clientId, latchLevel, farmId, latchId)), 0, 5,
                TimeUnit.SECONDS);
    }

    @Override
    public void stopWatching() {
        scheduledFutureForWatching.cancel(false);
    }

    private void updateCount(final String clientId, final String farmId,
                             final String latchId, long count) {
        try {
            final WritePolicy updateWritePolicy = new WritePolicy(
                    storageContext.getAerospikeClient().getWritePolicyDefault());
            updateWritePolicy.recordExistsAction = RecordExistsAction.UPDATE_ONLY;
            updateWritePolicy.expiration = storageContext.getTtl();

            writeRetryer.call(() -> {
                storageContext.getAerospikeClient().operate(updateWritePolicy,
                        new Key(storageContext.getNamespace(), getSetName(), buildKey(clientId, latchId)),
                        Operation.add(new Bin(getCountBin(farmId), count)));
                return null;
            });
        } catch (ExecutionException | RetryException e) {
            throw DistributedLatchException.propagate(e);
        }
    }

    private String getSetName() {
        return String.join(Constants.DELIMITER, Constants.D_LATCH, storageContext.getSetSuffix());
    }

    private String buildKey(final String clientId, final String latchId) {
        return String.format(KEY_FORMAT, clientId, latchId);
    }

    private String getCountBin(final String farmId) {
        return String.join(Constants.DELIMITER, COUNT_BIN_PREFIX, farmId);
    }

}
