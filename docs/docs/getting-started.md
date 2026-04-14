# Getting Started

## Requirements

- **Java 17** or later.
- One of the supported storage backends:
    - An **Aerospike** cluster reachable from application nodes, or
    - An **HBase** cluster reachable from application nodes.

## Add Dependency

```xml
<dependency>
  <groupId>com.phonepe</groupId>
  <artifactId>distributed-latch</artifactId>
  <version>${distributed-latch.version}</version>
</dependency>
```

Replace `${distributed-latch.version}` with the latest version from [Maven Central](https://central.sonatype.com/artifact/com.phonepe/distributed-latch) or [GitHub Releases](https://github.com/PhonePe/DistributedLatch/releases).

## Build Locally

```bash
git clone https://github.com/PhonePe/DistributedLatch.git
cd DistributedLatch
mvn clean install
```

To run the tests:

```bash
mvn clean test
```

## Minimal Example

### CountDown Latch (Coordinator side)

```java
// 1. Create a count-down latch with initial count = 3
IDistributedCountDownLatch latch = DistributedLatchFactory.createCountDownLatch(
    "order-service",                    // clientId
    LatchLevel.DC,                      // latch level
    "dc1",                              // farmId
    "batch-job-123",                    // latchId
    AerospikeLatchStorageContext.builder()
        .aerospikeClient(aerospikeClient)
        .namespace("latches")
        .setSuffix("distributed_latch")
        .storageType(StorageType.AEROSPIKE)
        .ttl(3600)                      // TTL in seconds
        .build(),
    3                                   // initial count
);

// 2. Wait for all workers to finish
latch.await();
// execution continues after count reaches 0
```

### CountDown Latch (Worker side)

```java
// 1. Get a reference to the existing latch (no init)
IDistributedCountDownLatch latch = DistributedLatchFactory.getCountDownLatch(
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
        .build()
);

// 2. Signal completion
latch.countDown();
```

!!! tip
    The coordinator creates the latch with an initial count using `createCountDownLatch`.
    Workers obtain a reference to the same latch using `getCountDownLatch` (without re-initializing the count)
    and call `countDown()` when their work is done.

## What's Next

- [Usage](usage.md) — factory methods, CountUpDown latch, await with timeout, error handling.
- [Latch Semantics](latch-semantics.md) — API reference, latch levels, watcher behavior.
- [Storage Backends](storages/aerospike.md) — Aerospike and HBase details.

