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

import com.aerospike.client.IAerospikeClient;
import com.phonepe.latch.storage.StorageType;
import com.phonepe.latch.storage.context.ILatchStorageContextVisitor;
import com.phonepe.latch.storage.context.LatchStorageContext;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AerospikeLatchStorageContext extends LatchStorageContext {
    private final IAerospikeClient aerospikeClient;
    private final String namespace;
    private final String setSuffix;

    @Builder
    public AerospikeLatchStorageContext(final IAerospikeClient aerospikeClient,
                                        final String namespace,
                                        final String setSuffix,
                                        final StorageType storageType,
                                        final int ttl) {
        super(storageType, ttl);
        this.aerospikeClient = aerospikeClient;
        this.namespace = namespace;
        this.setSuffix = setSuffix;
    }

    @Override
    public <T> T accept(ILatchStorageContextVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
