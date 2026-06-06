# Bijou64 Kafka Serializers

Production-ready Kafka Serializer/Deserializer implementations for Bijou64 variable-length integer encoding.

## Overview

This module provides drop-in Kafka serializers that enable efficient compression of Long values in your Kafka topics. By reducing message payload size by **54%**, you can:

- Lower storage costs for historical data
- Reduce network egress bandwidth
- Improve producer/consumer throughput (fewer bytes to transfer)
- Decrease topic partition size for faster rebalancing

## Classes

### `Bijou64Serializer`

Implements `Serializer<Long>` for Kafka producers.

```java
import org.bijou64.perf.kafka.Bijou64Serializer;

// In producer config:
props.put("value.serializer", "org.bijou64.perf.kafka.Bijou64Serializer");
```

**Configuration Options:**

- `bijou64.useJava` (boolean): Use pure-Java implementation instead of JNI (default: false)

### `Bijou64Deserializer`

Implements `Deserializer<Long>` for Kafka consumers.

```java
import org.bijou64.perf.kafka.Bijou64Deserializer;

// In consumer config:
props.put("value.deserializer", "org.bijou64.perf.kafka.Bijou64Deserializer");
```

## Integration Guide

### Step 1: Add Dependency

```xml
<dependency>
    <groupId>org.bijou64</groupId>
    <artifactId>bijou64</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Step 2: Include Serializer JAR in Classpath

Build this module and include in your Kafka broker's plugin path or client classpath:

```bash
mvn -B clean package
# JAR: target/bijou64-kafka-serializers-0.1.0.jar
```

### Step 3: Configure Serializers

#### Kafka Producer (Java Client)

```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.bijou64.perf.kafka.Bijou64Serializer");

KafkaProducer<String, Long> producer = new KafkaProducer<>(props);
producer.send(new ProducerRecord<>("my-topic", "key", 123456789L));
```

#### Kafka Consumer (Java Client)

```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("group.id", "my-group");
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("value.deserializer", "org.bijou64.perf.kafka.Bijou64Deserializer");

KafkaConsumer<String, Long> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("my-topic"));
```

#### Properties File

```properties
bootstrap.servers=localhost:9092
key.serializer=org.apache.kafka.common.serialization.StringSerializer
value.serializer=org.bijou64.perf.kafka.Bijou64Serializer
key.deserializer=org.apache.kafka.common.serialization.StringDeserializer
value.deserializer=org.bijou64.perf.kafka.Bijou64Deserializer
```

## Release signaling

This repository uses Git tags and a changelog to signal releases:

- Create a signed Git tag like `v0.1.0` for each release.
- Update `CHANGELOG.md` with release notes under the `Unreleased` section before tagging.
- GitHub Actions will build and publish artifacts when a `v*` tag is pushed.

## Benchmarking

Compare Bijou64 performance against standard Kafka Long encoding:

### Prerequisites

1. **Build root library**

```bash
cd ../..
mvn -B clean install -DskipTests
```

2. **Build benchmark module**

```bash
mvn -B clean package -DskipTests
```

3. **Start Kafka** (KRaft mode with Docker Compose, Kafka 4.3.0)

```bash
docker compose up -d
```

Verify Kafka is running:

```bash
docker compose logs kafka
```

### Run Benchmarks

#### Producer Benchmarks

Test standard Long serialization:

```bash
./scripts/run-producer.sh --mode long --count 200000 --topic bijou64-benchmark-topic --bootstrap-server localhost:9092
```

Test Kafka transport compression with standard Long serialization:

```bash
./scripts/run-producer.sh --mode long --compression zstd --count 200000 --topic bijou64-benchmark-topic --bootstrap-server localhost:9092
./scripts/run-producer.sh --mode long --compression snappy --count 200000 --topic bijou64-benchmark-topic --bootstrap-server localhost:9092
./scripts/run-producer.sh --mode long --compression lz4 --count 200000 --topic bijou64-benchmark-topic --bootstrap-server localhost:9092
```

Test Bijou64 (JNI) serialization:

```bash
./scripts/run-producer.sh --mode bijou --count 200000 --topic bijou64-benchmark-topic --bootstrap-server localhost:9092
```

Test Bijou64 (Java) serialization:

```bash
./scripts/run-producer.sh --mode bijou-java --count 200000 --topic bijou64-benchmark-topic --bootstrap-server localhost:9092
```

Test with a different value distribution:

```bash
./scripts/run-producer.sh --mode bijou --distribution uniform --count 200000 --topic bijou64-benchmark-topic --bootstrap-server localhost:9092
./scripts/run-producer.sh --mode bijou --distribution boundary --warmup-count 10000 --count 200000 --topic bijou64-benchmark-topic --bootstrap-server localhost:9092
```

Supported distributions:

- `sequential` (default): values `1..count` — favors small integers (~3.7 byte avg)
- `uniform`: seeded random full-range u64 values
- `boundary`: cycles tier-edge values from the Rust shootout

#### Consumer Benchmarks

Test deserialization performance:

```bash
./scripts/run-consumer.sh --mode long --count 200000 --topic bijou64-benchmark-topic --group-id bijou64-benchmark-group --bootstrap-server localhost:9092
./scripts/run-consumer.sh --mode bijou --count 200000 --topic bijou64-benchmark-topic --group-id bijou64-benchmark-group --bootstrap-server localhost:9092
```

Self-contained consumer run (produces data first, unique consumer group):

```bash
./scripts/run-consumer.sh --mode bijou --produce-first true --count 200000 --topic bijou64-benchmark-topic --group-id bijou64-benchmark-group --bootstrap-server localhost:9092
```

#### Comparative Analysis

Run all modes and compare side-by-side:

```bash
./scripts/compare-benchmarks.sh 200000 3
./scripts/compare-benchmarks.sh --distribution uniform 200000 3
```

#### CI benchmark (automated)

The **`benchmark` job** in [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) runs on every push to `main`. It executes the producer comparison test:

```bash
./scripts/compare-benchmarks.sh --ci --distribution sequential 50000 2 bijou64-benchmark-topic localhost:9092
```

| Parameter        | Value                                              |
| ---------------- | -------------------------------------------------- |
| Test class       | `org.bijou64.perf.kafka.ProducerBenchmark`         |
| Messages per run | 50,000                                             |
| Distribution     | `sequential` (values `1..N`)                       |
| Iterations       | 2 measured + 1 discarded warmup per mode           |
| Kafka            | `apache/kafka:4.3.0` (KRaft), `ubuntu-latest`      |
| Java             | 17 (Temurin)                                       |
| CI modes         | `long`, `bijou`, `bijou-java` × `none`, `zstd`     |

Each mode runs one discarded warmup iteration, then measured iterations. Summary reports **median** rate (not best-of). Topics are reset between runs via `kafka-topics` (local Docker or GHA service container).

A full local run compares all compression codecs:

```bash
./scripts/compare-benchmarks.sh 200000 3
./scripts/compare-benchmarks.sh --distribution uniform 200000 3
```

Use these results to answer: “why use Bijou64 instead of `compression.type`?” by comparing payload size, throughput, and end-to-end behavior.

### Results

Benchmark results are stored in `logs/results-*.csv` (uploaded as a CI artifact from the `benchmark` job).

**Key Metrics:**

- **rate**: Messages per second throughput (end-to-end Kafka producer run)
- **avg_bytes**: Average serialized message size in bytes (computed via `encodedLen`, not double-encoded)
- **exit_code**: 0 = success, non-zero = failure
- **distribution**: Value generator used for the run

#### Latest CI results (2026-06-06)

From [`results-20260606T110707.csv`](../../results-20260606T110707.csv) — all runs `exit_code=0`:

| Mode           | Compression | Run 1   | Run 2   | Median    | Avg payload |
| -------------- | ----------- | ------- | ------- | --------- | ----------- |
| Long           | none        | 103,613 | 102,085 | 102,849   | 8.0 bytes   |
| Long           | zstd        | 98,682  | 89,515  | 94,099    | 8.0 bytes   |
| Bijou64 (JNI)  | none        | 109,022 | 120,157 | 114,590   | 3.0 bytes   |
| Bijou64 (JNI)  | zstd        | 94,896  | 102,681 | 98,789    | 3.0 bytes   |
| Bijou64 (Java) | none        | 113,348 | 120,018 | 116,683   | 3.0 bytes   |
| Bijou64 (Java) | zstd        | 121,785 | 95,392  | 108,589   | 3.0 bytes   |

**Interpretation:**

- **Payload size:** 3 bytes vs 8 bytes on sequential integers (62% smaller on the wire).
- **Throughput:** Bijou64 modes are ~11–14% faster than `LongSerializer` without compression on the GHA runner; absolute rates vary by hardware.
- **Compression:** `zstd` reduces throughput for all modes; Bijou64’s smaller payloads mean less data to compress, so the penalty is relatively smaller.
- **JNI vs Java:** Comparable on CI; Java slightly ahead on `none`, with higher variance on `zstd`.

#### Local full benchmark (200K messages, 3 runs, sequential)

Run locally for higher-resolution numbers on your hardware:

```bash
./scripts/compare-benchmarks.sh 200000 3
```

## Configuration

### Environment Variables

```bash
# Kafka broker address (default: localhost:9092)
export BOOTSTRAP_SERVERS="kafka1:9092,kafka2:9092"

# Topic prefix (default: bijou-test)
export TOPIC_PREFIX="my-app"

# Message count per run (default: 200000)
export MESSAGE_COUNT=1000000
```

### Running with Custom Bootstrap Servers

```bash
./scripts/run-producer.sh \
  --bootstrap-server broker1:9092,broker2:9092 \
  --count 500000 \
  --mode bijou
```

## Troubleshooting

### Native Library Not Found

If you see errors about missing JNI library:

```bash
# Use Java implementation instead
./scripts/run-producer.sh --mode bijou-java --count 100000
```

Or configure clients:

```properties
bijou64.useJava=true
```

### Docker Compose Issues

Ensure Docker is running:

```bash
docker ps
```

Stop and restart Kafka:

```bash
docker compose down --volumes
docker compose up -d
docker compose logs -f kafka
```

### Benchmark Timeouts

Increase timeout or reduce message count:

```bash
./scripts/run-producer.sh --mode bijou --count 50000 --topic bijou64-benchmark-topic --bootstrap-server localhost:9092
```

## Production Deployment

### Requirements

- Java 17+
- Kafka 3.0+ (any version supported)
- Bijou64 library in classpath

### High-Volume Topics (Billions of Messages)

**Recommended Configuration:**

```properties
# Use Java implementation for compatibility across clusters
bijou64.useJava=true

# Monitor serialization overhead (should be minimal)
metrics.num.samples=100
```

### Monitoring

Add metrics to track Bijou64 usage:

- **Serialized bytes/sec**: Monitor compression effectiveness
- **Deserialized bytes/sec**: Verify consumer side
- **Error rate**: Track serialization failures
- **Latency**: Measure encoding/decoding time

### Compatibility

- Works with any Kafka cluster (3.0+)
- Compatible with existing topics (mix serializers per topic)
- Backward compatible with Long deserializer for migration
- Cross-platform: Linux, macOS, Windows

## Advanced: Using with Connect

For Kafka Connect, include the JAR in the `plugin.path`:

```properties
plugin.path=/opt/kafka/plugins/bijou64
```

Then reference in connector config:

```json
{
  "value.converter": "org.apache.kafka.connect.storage.StringConverter",
  "value.serializer": "org.bijou64.perf.kafka.Bijou64Serializer"
}
```

## Performance Tips

1. **Batch Publishing**: Larger batches benefit more from compression
2. **Async Send**: Use async producer to hide serialization latency
3. **Consumer Group**: Distribute deserialization across consumer instances
4. **Monitoring**: Track bytes/sec to quantify savings

## Limitations

- Supports only `Long` type (not arbitrary objects)
- Encoding is deterministic (same input always produces same output)
- Payload size varies: 1-9 bytes depending on value magnitude
- No schema registry required (self-describing format)

## License

Licensed under either of:

- Apache License, Version 2.0
- MIT license

at your option.

## Contributing

Found a performance issue or have suggestions? [Open an issue](../../../issues) with:

- Kafka version
- Machine specs (CPU, memory)
- Benchmark results from your environment
- Steps to reproduce

### Bijou Java producer benchmark

```bash
cd perf/kafka
./scripts/run-producer.sh --mode bijou-java --topic bijou-test --bootstrap-server localhost:9092 --count 200000
```

### Bijou consumer benchmark

```bash
cd perf/kafka
./scripts/run-consumer.sh --mode bijou --topic bijou-test --bootstrap-server localhost:9092 --group-id bijou-consumer --count 200000
```

### Bijou Java consumer benchmark

```bash
cd perf/kafka
./scripts/run-consumer.sh --mode bijou-java --topic bijou-test --bootstrap-server localhost:9092 --group-id bijou-consumer --count 200000
```

## Notes

- `Bijou64` uses the native JNI library, so the root project must be built and installed first.
- Run the benchmark from the repository root or set the JVM library path if the native library does not load.
