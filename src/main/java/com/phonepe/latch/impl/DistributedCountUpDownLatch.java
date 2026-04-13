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

import com.phonepe.latch.IDistributedCountUpDownLatch;
import com.phonepe.latch.common.LatchLevel;
import com.phonepe.latch.storage.BaseLatchStorage;

public final class DistributedCountUpDownLatch extends DistributedCountDownLatch
        implements IDistributedCountUpDownLatch {

    public DistributedCountUpDownLatch(final String clientId,
                                       final LatchLevel latchLevel,
                                       final String farmId,
                                       final String latchId,
                                       final BaseLatchStorage latchStorage) {
        super(clientId, latchLevel, farmId, latchId, latchStorage);
    }

    @Override
    public void countUp() {
        getLatchStorage().incrementCount(getClientId(), getFarmId(), getLatchId());
    }

}