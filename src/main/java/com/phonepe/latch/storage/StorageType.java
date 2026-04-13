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

package com.phonepe.latch.storage;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum StorageType {
    AEROSPIKE(StorageType.AEROSPIKE_TEXT),
    ZOOKEEPER(StorageType.ZOOKEEPER_TEXT),
    HBASE(StorageType.HBASE_TEXT);

    static final String AEROSPIKE_TEXT = "AEROSPIKE";
    static final String ZOOKEEPER_TEXT = "ZOOKEEPER";
    static final String HBASE_TEXT = "HBASE";

    @Getter
    private final String value;
}
