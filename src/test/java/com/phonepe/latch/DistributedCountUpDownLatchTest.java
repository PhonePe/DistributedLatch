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

package com.phonepe.latch;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.phonepe.latch.common.Constants;
import com.phonepe.latch.common.LatchLevel;
import com.phonepe.latch.storage.aerospike.AerospikeLatchStorage;
import com.phonepe.latch.storage.aerospike.AerospikeLatchStorageContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class DistributedCountUpDownLatchTest {
    private final static String CLIENT_ID = "testClientId";
    private final static String FARM_ID = "FA1";
    private final static String LATCH_ID = "testLatchId";
    private final static String AEROSPIKE_COUNT_BIN =
            String.join(Constants.DELIMITER, AerospikeLatchStorage.COUNT_BIN_PREFIX, FARM_ID);
    private final static long count = 5;
    private final AerospikeLatchStorageContext storageContext = AerospikeLatchStorageContext.builder()
            .ttl(100)
            .aerospikeClient(Mockito.mock(AerospikeClient.class, Mockito.RETURNS_DEEP_STUBS))
            .namespace("test")
            .setSuffix("test").build();
    private IDistributedCountUpDownLatch countUpDownLatch;

    @Before
    public void setUp() {
        Mockito.when(storageContext.getAerospikeClient().getWritePolicyDefault()).thenReturn(new WritePolicy());
        countUpDownLatch = DistributedLatchFactory.createCountUpDownLatch(
                CLIENT_ID, LatchLevel.DC, FARM_ID, LATCH_ID, storageContext, count);
    }

    @Test
    public void testGetCount() {
        Map<String, Object> values = new HashMap<>();
        values.put(AEROSPIKE_COUNT_BIN, count);
        Record mockedRecord = new Record(values, 0, 0);
        Mockito.when(storageContext.getAerospikeClient().get(Mockito.any(Policy.class), Mockito.any(Key.class)))
                .thenReturn(mockedRecord);
        Assert.assertEquals(5, countUpDownLatch.getCount());
    }

    @Test
    public void testCountDown() {
        final Map<String, Object> values = new HashMap<>();
        values.put(AEROSPIKE_COUNT_BIN, count);
        Record mockedRecord = new Record(values, 0, 0);
        Mockito.when(storageContext.getAerospikeClient().get(Mockito.any(Policy.class), Mockito.any(Key.class)))
                .thenReturn(mockedRecord);
        Assert.assertEquals(5, countUpDownLatch.getCount());

        values.put(AEROSPIKE_COUNT_BIN, count - 1);
        mockedRecord = new Record(values, 0, 0);
        Mockito.when(storageContext.getAerospikeClient().operate(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(null);
        Mockito.when(storageContext.getAerospikeClient().get(Mockito.any(Policy.class), Mockito.any(Key.class)))
                .thenReturn(mockedRecord);
        countUpDownLatch.countDown();
        Assert.assertEquals(4, countUpDownLatch.getCount());
    }

    @Test
    public void testCountUp() {
        final Map<String, Object> values = new HashMap<>();
        values.put(AEROSPIKE_COUNT_BIN, count);
        Record mockedRecord = new Record(values, 0, 0);
        Mockito.when(storageContext.getAerospikeClient().get(Mockito.any(Policy.class), Mockito.any(Key.class)))
                .thenReturn(mockedRecord);
        Assert.assertEquals(5, countUpDownLatch.getCount());

        values.put(AEROSPIKE_COUNT_BIN, count + 1);
        mockedRecord = new Record(values, 0, 0);
        Mockito.when(storageContext.getAerospikeClient().operate(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(null);
        Mockito.when(storageContext.getAerospikeClient().get(Mockito.any(Policy.class), Mockito.any(Key.class)))
                .thenReturn(mockedRecord);
        countUpDownLatch.countUp();
        Assert.assertEquals(6, countUpDownLatch.getCount());
    }

    @Test
    public void testAwait() {
        final Map<String, Object> values = new HashMap<>();
        values.put(AEROSPIKE_COUNT_BIN, 0L);
        Record mockedRecord = new Record(values, 0, 0);
        Mockito.when(storageContext.getAerospikeClient().get(Mockito.any(Policy.class), Mockito.any(Key.class)))
                .thenReturn(mockedRecord);
        try {
            countUpDownLatch.await();
        } catch (InterruptedException e) {
            Assert.fail();
        }
        Assert.assertEquals(0, countUpDownLatch.getCount());
    }
}
