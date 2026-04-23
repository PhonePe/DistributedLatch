# Distributed Latch

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=PhonePe_DistributedLatch&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=PhonePe_DistributedLatch)

Distributed synchronization is a common requirement in service-oriented architectures, where a set of distributed
workers need to signal completion to a coordinator. Java's built-in `CountDownLatch` only works within a single JVM.

Distributed Latch extends this concept across process and machine boundaries, providing count-based coordination
backed by a pluggable distributed storage layer.

**Supported latch flavours:**

- **Count Down Latch** — uni-directional permit movements (decrement only). Ideal for fan-out / fan-in patterns.
- **Count Up Down Latch** — bi-directional permit movements (increment and decrement). Useful when total work is not known upfront.

## Add Maven Dependency

```xml
<dependency>
  <groupId>com.phonepe</groupId>
  <artifactId>distributed-latch</artifactId>
  <version>${distributed-latch.version}</version>
</dependency>
```

> Replace `${distributed-latch.version}` with the latest version from [Maven Central](https://central.sonatype.com/artifact/com.phonepe/distributed-latch) or [GitHub Releases](https://github.com/PhonePe/DistributedLatch/releases).

### Usage

#### Creating a Distributed CountDown Latch

##### With Aerospike as storage backend

```java
IDistributedCountDownLatch latch = DistributedLatchFactory.createCountDownLatch(
        "CLIENT_ID",
        LatchLevel.DC,
        "FA1",
        "LATCH_ID",
        AerospikeLatchStorageContext.builder()
                .aerospikeClient(aerospikeClient)
                .namespace("NAMESPACE")
                .setSuffix("distributed_latch")
                .storageType(StorageType.AEROSPIKE)
                .ttl(3600)
                .build(),
        5  // initial count
);
```

##### With HBase as storage backend

```java
IDistributedCountDownLatch latch = DistributedLatchFactory.createCountDownLatch(
        "CLIENT_ID",
        LatchLevel.DC,
        "FA1",
        "LATCH_ID",
        HBaseLatchStorageContext.builder()
                .connection(connection) // HBase connection reference
                .tableSuffix("distributed_latch")
                .storageType(StorageType.HBASE)
                .ttl(3600)
                .build(),
        5  // initial count
);
```

PS: For optimum performance, DO NOT pre-create the HBase table. The library will create it for you with the correct schema and pre-split configuration.

## Latch Operations

### CountDown Latch

1. **`countDown()`**
    - Decrements the latch count by 1 in the distributed store. The operation is atomic.

2. **`getCount()`**
    - Returns the current count from the distributed store.

3. **`await()`**
    - Blocks the calling thread until the latch count reaches zero. A background watcher polls the store every 5 seconds.

4. **`await(timeout, unit)`**
    - Blocks until the count reaches zero or the timeout expires. Returns `true` if count reached zero, `false` otherwise.

### CountUpDown Latch

Extends CountDown Latch with an additional operation:

5. **`countUp()`**
    - Increments the latch count by 1 in the distributed store.

## Example Usage

### Coordinator (creates the latch and waits)

```java
IDistributedCountDownLatch latch = DistributedLatchFactory.createCountDownLatch(
        "order-service", LatchLevel.DC, "dc1", "batch-job-123", storageContext, 3);

// Dispatch 3 tasks to workers, then wait
boolean completed = latch.await(60, TimeUnit.SECONDS);
if (!completed) {
    // handle timeout
}
```

### Worker (signals completion)

```java
IDistributedCountDownLatch latch = DistributedLatchFactory.getCountDownLatch(
        "order-service", LatchLevel.DC, "dc1", "batch-job-123", storageContext);

try {
    processTask();
} finally {
    latch.countDown();
}
```

### CountUpDown Latch (dynamic task spawning)

```java
IDistributedCountUpDownLatch latch = DistributedLatchFactory.getOrCreateCountUpDownLatch(
        "order-service", LatchLevel.DC, "dc1", "dynamic-job-456", storageContext);

// Dynamically add work
latch.countUp();
// ... do work ...
latch.countDown();
```

#### Latch Levels
* **DC** — Latch scoped within a single data center. Count is stored per `farmId`.
* **XDC** — Latch scoped across data centers. Count is aggregated across all farms.

**Caution**: Reading an XDC count aggregates across all farms. Due to storage replication lag, the count may be temporarily inconsistent across data centers. For strong consistency with XDC, use a multi-site Aerospike cluster or synchronous HBase replication.

#### Notes

A latch exists only within the scope of a client represented by `CLIENT_ID`. The internal key is `D_LTCH#<clientId>#<latchId>`, so different clients can use the same latch ID without conflict.
