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

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import com.github.rholder.retry.RetryException;
import com.phonepe.latch.common.LatchLevel;
import com.phonepe.latch.exception.DistributedLatchException;
import com.phonepe.latch.impl.DistributedCountDownLatch;
import com.phonepe.latch.impl.DistributedCountUpDownLatch;
import com.phonepe.latch.storage.BaseLatchStorage;
import com.phonepe.latch.storage.aerospike.AerospikeLatchStorage;
import com.phonepe.latch.storage.aerospike.AerospikeLatchStorageContext;
import com.phonepe.latch.storage.context.ILatchStorageContextVisitor;
import com.phonepe.latch.storage.context.LatchStorageContext;
import com.phonepe.latch.storage.hbase.HBaseLatchStorage;
import com.phonepe.latch.storage.hbase.HBaseLatchStorageContext;
import com.phonepe.latch.storage.zookeeper.ZookeeperLatchStorage;
import com.phonepe.latch.storage.zookeeper.ZookeeperLatchStorageContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DistributedLatchFactory {

    public static IDistributedCountDownLatch createCountDownLatch(
            final String clientId,
            final LatchLevel latchLevel,
            final String farmId,
            final String latchId,
            final LatchStorageContext storageContext,
            final long count) {
        final IDistributedCountDownLatch latch = new DistributedCountDownLatch(
                clientId, latchLevel, farmId, latchId, storageContext.accept(new LatchStorageContextVisitorImpl()));
        latch.init(count);
        return latch;
    }

    public static IDistributedCountUpDownLatch createCountUpDownLatch(
            final String clientId,
            final LatchLevel latchLevel,
            final String farmId,
            final String latchId,
            final LatchStorageContext storageContext,
            final long count) {
        final IDistributedCountUpDownLatch latch = new DistributedCountUpDownLatch(
                clientId, latchLevel, farmId, latchId, storageContext.accept(new LatchStorageContextVisitorImpl()));
        latch.init(count);
        return latch;
    }

    public static IDistributedCountUpDownLatch getOrCreateCountUpDownLatch(
            final String clientId,
            final LatchLevel latchLevel,
            final String farmId,
            final String latchId,
            final LatchStorageContext storageContext) {
        final IDistributedCountUpDownLatch latch = new DistributedCountUpDownLatch(
                clientId, latchLevel, farmId, latchId, storageContext.accept(new LatchStorageContextVisitorImpl()));
        try {
            latch.init(0);
        } catch (DistributedLatchException e) {
            handleException(e);
        }
        return latch;
    }

    public static IDistributedCountDownLatch getCountDownLatch(
            final String clientId,
            final LatchLevel latchLevel,
            final String farmId,
            final String latchId,
            final LatchStorageContext storageContext) {
        return new DistributedCountDownLatch(
                clientId, latchLevel, farmId, latchId, storageContext.accept(new LatchStorageContextVisitorImpl()));
    }

    public static IDistributedCountUpDownLatch getCountUpDownLatch(
            final String clientId,
            final LatchLevel latchLevel,
            final String farmId,
            final String latchId,
            final LatchStorageContext storageContext) {
        return new DistributedCountUpDownLatch(
                clientId, latchLevel, farmId, latchId, storageContext.accept(new LatchStorageContextVisitorImpl()));
    }

    private static final class LatchStorageContextVisitorImpl implements ILatchStorageContextVisitor<BaseLatchStorage> {
        @Override
        public AerospikeLatchStorage visit(AerospikeLatchStorageContext context) {
            return new AerospikeLatchStorage(context);
        }

        @Override
        public ZookeeperLatchStorage visit(ZookeeperLatchStorageContext context) {
            return new ZookeeperLatchStorage(context);
        }

        @Override
        public HBaseLatchStorage visit(HBaseLatchStorageContext context) {
            return new HBaseLatchStorage(context);
        }
    }

    private static void handleException(DistributedLatchException e) {
        // handling KEY_EXISTS_ERROR error
        if (e.getCause() instanceof RetryException &&
                e.getCause().getCause() instanceof AerospikeException &&
                ((AerospikeException) e.getCause().getCause()).getResultCode() == ResultCode.KEY_EXISTS_ERROR) {
            return;
        }
        throw e;
    }

}
