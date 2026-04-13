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

package com.phonepe.latch.impl;

import com.phonepe.latch.IDistributedCountDownLatch;
import com.phonepe.latch.common.LatchLevel;
import com.phonepe.latch.common.TriFunction;
import com.phonepe.latch.storage.BaseLatchStorage;
import com.phonepe.latch.watcher.IWatcher;
import lombok.Getter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Getter
public class DistributedCountDownLatch implements IDistributedCountDownLatch {
    private final String clientId;
    private final LatchLevel latchLevel;
    private final String farmId;
    private final String latchId;
    private final BaseLatchStorage latchStorage;
    private final CountDownLatch localLatch;

    public DistributedCountDownLatch(final String clientId,
                                     final LatchLevel latchLevel,
                                     final String farmId,
                                     final String latchId,
                                     final BaseLatchStorage latchStorage) {
        this.clientId = clientId;
        this.latchLevel = latchLevel;
        this.farmId = farmId;
        this.latchId = latchId;
        this.latchStorage = latchStorage;
        this.localLatch = new CountDownLatch(1);
    }

    @Override
    public void init(long count) {
        latchStorage.initialize(clientId, farmId, latchId, count);
    }

    @Override
    public long getCount() {
        return latchStorage.getCount(clientId, latchLevel, farmId, latchId);
    }

    public void countDown() {
        latchStorage.decrementCount(clientId, farmId, latchId);
    }

    @Override
    public void await() throws InterruptedException {
        activateWatcherDaemon();
        localLatch.await();
    }

    @Override
    public boolean await(final long timeout, final TimeUnit unit) throws InterruptedException {
        activateWatcherDaemon();
        return localLatch.await(timeout, unit);
    }

    private void activateWatcherDaemon() {
        latchStorage.startWatching(clientId, latchLevel, farmId, latchId, localLatch, new TriFunctionImpl());
    }

    private static class TriFunctionImpl implements TriFunction<IWatcher, CountDownLatch, Long, Void> {
        @Override
        public Void apply(IWatcher watcher, CountDownLatch localLatch, Long count) {
            if (count <= 0) {
                localLatch.countDown();
                watcher.stopWatching();
            }
            return null;
        }
    }

}
