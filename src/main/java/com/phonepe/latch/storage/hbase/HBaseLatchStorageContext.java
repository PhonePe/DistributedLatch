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

import com.phonepe.latch.storage.StorageType;
import com.phonepe.latch.storage.context.ILatchStorageContextVisitor;
import com.phonepe.latch.storage.context.LatchStorageContext;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.hadoop.hbase.client.Connection;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HBaseLatchStorageContext extends LatchStorageContext {

    private final Connection connection;
    private final String tableSuffix;

    @Builder
    public HBaseLatchStorageContext(final Connection connection,
                                    final String tableSuffix,
                                    final StorageType storageType,
                                    final int ttl) {
        super(storageType, ttl);
        this.connection = connection;
        this.tableSuffix = tableSuffix;
    }

    @Override
    public <T> T accept(ILatchStorageContextVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
