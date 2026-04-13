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

import com.google.common.primitives.Longs;
import com.phonepe.latch.DistributedLatchFactory;
import com.phonepe.latch.IDistributedCountDownLatch;
import com.phonepe.latch.common.LatchLevel;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class HBaseLatchStorageTest {
    private final static String CLIENT_ID = "testClientId";
    private final static String FARM_ID = "FA1";
    private final static String LATCH_ID = "testLatchId";
    @Mock
    private final Configuration config = HBaseConfiguration.create();
    @Mock
    private final Connection connection;
    @Mock
    private Admin admin;
    private HBaseLatchStorage hBaseLatchStorage;
    @Mock
    private Table table;

    public HBaseLatchStorageTest() throws IOException {
        this.connection = ConnectionFactory.createConnection(config);
    }

    @Before
    public void before() throws IOException {
        MockitoAnnotations.initMocks(this);
        HBaseLatchStorageContext storageContext = HBaseLatchStorageContext.builder()
                .ttl(100)
                .connection(connection)
                .tableSuffix("test")
                .build();

        hBaseLatchStorage = new HBaseLatchStorage(storageContext);
        when(connection.getAdmin()).thenReturn(admin);
        when(connection.getTable(TableName.valueOf("D_LTCH_test"))).thenReturn(table);
    }

    @Test
    public void testInitialize() throws IOException {
        doReturn(false).when(admin).tableExists(any());
        final ArgumentCaptor<Increment> incrementArgumentCaptor = ArgumentCaptor.forClass(Increment.class);
        hBaseLatchStorage.initialize(CLIENT_ID, FARM_ID, LATCH_ID, 10);
        verify(table, times(1)).increment(incrementArgumentCaptor.capture());
        final Increment increment = incrementArgumentCaptor.getValue();
        assert (increment.has(Bytes.toBytes("C"), Bytes.toBytes("count_FA1"), Longs.toByteArray(10)));
    }

    @Test
    public void testGetCount() throws IOException {
        when(table.get(any(Get.class))).thenReturn(Result.create(Collections.singletonList(
                new KeyValue(Bytes.toBytes(""), Bytes.toBytes("C"), Bytes.toBytes("count_FA1"), Bytes.toBytes(10L))
        )));
        final long count = hBaseLatchStorage.getCount(CLIENT_ID, LatchLevel.DC, FARM_ID, LATCH_ID);
        assertEquals(10L, count);
    }

    @Test
    public void testXDCGetCount() throws IOException {
        when(table.get(any(Get.class))).thenReturn(Result.create(Arrays.asList(
                new KeyValue(Bytes.toBytes(""), Bytes.toBytes("C"), Bytes.toBytes("count_FA1"), Bytes.toBytes(10L)),
                new KeyValue(Bytes.toBytes(""), Bytes.toBytes("C"), Bytes.toBytes("count_FA2"), Bytes.toBytes(10L))
        )));
        final long count = hBaseLatchStorage.getCount(CLIENT_ID, LatchLevel.XDC, FARM_ID, LATCH_ID);
        assertEquals(20L, count);
    }

    @Test
    public void testIncrementCount() throws IOException {
        final ArgumentCaptor<Increment> incrementArgumentCaptor = ArgumentCaptor.forClass(Increment.class);
        hBaseLatchStorage.incrementCount(CLIENT_ID, FARM_ID, LATCH_ID);
        verify(table, times(1)).increment(incrementArgumentCaptor.capture());
        final Increment increment = incrementArgumentCaptor.getValue();
        assert (increment.has(Bytes.toBytes("C"), Bytes.toBytes("count_FA1"), Longs.toByteArray(1)));
    }

    @Test
    public void testDecrementCount() throws IOException {
        final ArgumentCaptor<Increment> incrementArgumentCaptor = ArgumentCaptor.forClass(Increment.class);
        hBaseLatchStorage.decrementCount(CLIENT_ID, FARM_ID, LATCH_ID);
        verify(table, times(1)).increment(incrementArgumentCaptor.capture());
        final Increment increment = incrementArgumentCaptor.getValue();
        assert (increment.has(Bytes.toBytes("C"), Bytes.toBytes("count_FA1"), Longs.toByteArray(-1)));
    }

    @Test
    public void testHBaseAwait() throws IOException {
        IDistributedCountDownLatch countDownLatch = DistributedLatchFactory.createCountDownLatch(CLIENT_ID, LatchLevel.DC, FARM_ID, LATCH_ID,
                hBaseLatchStorage.getStorageContext(), 0L);
        when(table.get(any(Get.class))).thenReturn(Result.create(Collections.singletonList(
                new KeyValue(Bytes.toBytes(""), Bytes.toBytes("C"), Bytes.toBytes("count_FA1"), Bytes.toBytes(0L))
        )));

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Assert.fail();
        }
        Assert.assertEquals(0, countDownLatch.getCount());
    }
}