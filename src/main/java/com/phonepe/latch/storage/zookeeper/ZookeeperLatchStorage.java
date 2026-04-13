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

package com.phonepe.latch.storage.zookeeper;

import com.phonepe.latch.common.LatchLevel;
import com.phonepe.latch.common.TriFunction;
import com.phonepe.latch.exception.DistributedLatchException;
import com.phonepe.latch.exception.ErrorCode;
import com.phonepe.latch.storage.BaseLatchStorage;
import com.phonepe.latch.storage.StorageType;
import com.phonepe.latch.watcher.IWatcher;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.CountDownLatch;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ZookeeperLatchStorage extends BaseLatchStorage {

    @Builder
    public ZookeeperLatchStorage(final ZookeeperLatchStorageContext storageContext) {
        super(StorageType.ZOOKEEPER);
        throw DistributedLatchException.builder().errorCode(ErrorCode.NOT_IMPLEMENTED).build();
    }

    @Override
    public void initialize(String clientId, String farmId, String latchId, long count) {
        throw DistributedLatchException.builder().errorCode(ErrorCode.NOT_IMPLEMENTED).build();
    }

    @Override
    public void decrementCount(String clientId, String farmId, String latchId) {
        throw DistributedLatchException.builder().errorCode(ErrorCode.NOT_IMPLEMENTED).build();
    }

    @Override
    public void incrementCount(String clientId, String farmId, String latchId) {
        throw DistributedLatchException.builder().errorCode(ErrorCode.NOT_IMPLEMENTED).build();
    }

    @Override
    public long getCount(String clientId, LatchLevel latchLevel, String farmId, String latchId) {
        throw DistributedLatchException.builder().errorCode(ErrorCode.NOT_IMPLEMENTED).build();
    }

    @Override
    public void startWatching(String clientId, LatchLevel latchLevel, String farmId, String latchId,
                              CountDownLatch latch, TriFunction<IWatcher, CountDownLatch, Long, Void> function) {
        throw DistributedLatchException.builder().errorCode(ErrorCode.NOT_IMPLEMENTED).build();
    }

    @Override
    public void stopWatching() {
        throw DistributedLatchException.builder().errorCode(ErrorCode.NOT_IMPLEMENTED).build();
    }


}
