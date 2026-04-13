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

package com.phonepe.latch.exception;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DistributedLatchException extends RuntimeException {

    private static final long serialVersionUID = 3941797721266293207L;
    private final ErrorCode errorCode;

    @Builder
    public DistributedLatchException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public static DistributedLatchException propagate(final Throwable throwable) {
        return propagate("Error occurred", throwable);
    }

    public static DistributedLatchException propagate(final String message, final Throwable throwable) {
        if (throwable instanceof DistributedLatchException) {
            return (DistributedLatchException) throwable;
        } else if (throwable.getCause() instanceof DistributedLatchException) {
            return (DistributedLatchException) throwable.getCause();
        }
        return new DistributedLatchException(ErrorCode.INTERNAL_ERROR, message, throwable);
    }
}
