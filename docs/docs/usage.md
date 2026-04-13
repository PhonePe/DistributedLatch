# Usage

## Factory Methods

All latch instances are created via `DistributedLatchFactory`. The factory is a utility class with static methods — no instantiation required.

### Common Parameters

| Parameter        | Type                  | Description |
|------------------|-----------------------|-------------|
| `clientId`       | `String`              | Unique identifier for the calling service. Latch keys are scoped as `D_LTCH#<clientId>#<latchId>`. |
| `latchLevel`     | `LatchLevel`          | `DC` (single data center) or `XDC` (cross data center). |
| `farmId`         | `String`              | Data center / farm identifier. Used in key construction for `DC`-level latches. |
| `latchId`        | `String`              | Logical identifier for the latch (e.g. `"batch-job-123"`). |
| `storageContext` | `LatchStorageContext` | Configuration for the storage backend (Aerospike or HBase). |
| `count`          | `long`                | Initial count for the latch (only for `create*` methods). |

## CountDown Latch

A countdown latch supports only **decrement** operations. Use it when the total number of tasks is known upfront.

### Create and Initialize

=== "Aerospike"

    ```java
    IDistributedCountDownLatch latch = DistributedLatchFactory.createCountDownLatch(
        "order-service",
        LatchLevel.DC,
        "dc1",
        "batch-job-123",
        AerospikeLatchStorageContext.builder()
            .aerospikeClient(aerospikeClient)
            .namespace("latches")
            .setSuffix("distributed_latch")
            .storageType(StorageType.AEROSPIKE)
            .ttl(3600)
            .build(),
        5  // initial count
    );
    ```

=== "HBase"

    ```java
    IDistributedCountDownLatch latch = DistributedLatchFactory.createCountDownLatch(
        "order-service",
        LatchLevel.DC,
        "dc1",
        "batch-job-123",
        HBaseLatchStorageContext.builder()
            .connection(hbaseConnection)
            .tableSuffix("distributed_latch")
            .storageType(StorageType.HBASE)
            .ttl(3600)
            .build(),
        5  // initial count
    );
    ```

### Get Existing Latch (No Init)

Use `getCountDownLatch` to obtain a reference to an already-initialized latch without resetting the count:

```java
IDistributedCountDownLatch latch = DistributedLatchFactory.getCountDownLatch(
    "order-service", LatchLevel.DC, "dc1", "batch-job-123", storageContext
);
```

### Count Down

```java
latch.countDown();
```

Each call decrements the count by 1 in the distributed store.

### Get Current Count

```java
long remaining = latch.getCount();
```

### Await

```java
// Block indefinitely until count reaches 0
latch.await();

// Block with a timeout
boolean completed = latch.await(30, TimeUnit.SECONDS);
```

- `await()` starts a background watcher that polls the store every **5 seconds**. When the count reaches zero or below, the local `CountDownLatch` is released.
- `await(time, unit)` returns `true` if the count reached zero within the timeout, `false` otherwise.

## CountUpDown Latch

A count-up-down latch supports both **increment** and **decrement** operations. Use it when the total number of tasks is not known upfront, or tasks can dynamically spawn sub-tasks.

### Create and Initialize

```java
IDistributedCountUpDownLatch latch = DistributedLatchFactory.createCountUpDownLatch(
    "order-service", LatchLevel.DC, "dc1", "dynamic-job-456", storageContext, 0
);
```

### Get or Create

`getOrCreateCountUpDownLatch` is a convenience method that creates the latch if it doesn't exist, or returns a reference if it already does (swallows `KEY_EXISTS_ERROR` from Aerospike):

```java
IDistributedCountUpDownLatch latch = DistributedLatchFactory.getOrCreateCountUpDownLatch(
    "order-service", LatchLevel.DC, "dc1", "dynamic-job-456", storageContext
);
```

### Count Up and Down

```java
latch.countUp();    // increment by 1
latch.countDown();  // decrement by 1
```

### Await

Same as CountDown Latch — `await()` and `await(time, unit)` block until the count reaches zero.

## Error Handling

All latch operations throw `DistributedLatchException` on failure. Use `getErrorCode()` to distinguish:

```java
try {
    latch.countDown();
} catch (DistributedLatchException e) {
    switch (e.getErrorCode()) {
        case INTERNAL_ERROR -> log.error("Unexpected error", e);
        case NOT_IMPLEMENTED -> log.error("Operation not supported", e);
    }
}
```

## Complete Lifecycle Example

```java
// ── Setup (application startup) ──
AerospikeLatchStorageContext storageContext = AerospikeLatchStorageContext.builder()
    .aerospikeClient(aerospikeClient)
    .namespace("latches")
    .setSuffix("distributed_latch")
    .storageType(StorageType.AEROSPIKE)
    .ttl(3600)
    .build();

// ── Coordinator ──
IDistributedCountDownLatch latch = DistributedLatchFactory.createCountDownLatch(
    "payment-service", LatchLevel.DC, "dc1", "settlement-batch-789",
    storageContext, 3
);

// Dispatch 3 tasks to workers...

// Block until all workers signal completion
boolean done = latch.await(60, TimeUnit.SECONDS);
if (!done) {
    log.warn("Timed out waiting for workers");
}

// ── Worker (in a different JVM / instance) ──
IDistributedCountDownLatch workerLatch = DistributedLatchFactory.getCountDownLatch(
    "payment-service", LatchLevel.DC, "dc1", "settlement-batch-789",
    storageContext
);

try {
    processSettlement();
} finally {
    workerLatch.countDown();
}
```

