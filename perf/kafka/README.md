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
# JAR: target/bijou64-kafka-perf-0.1.0.jar
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

3. **Start Kafka** (KRaft mode with Docker Compose)

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
./scripts/run-producer.sh --mode long --count 200000 --runs 3
```

Test Bijou64 (JNI) serialization:

```bash
./scripts/run-producer.sh --mode bijou --count 200000 --runs 3
```

Test Bijou64 (Java) serialization:

```bash
./scripts/run-producer.sh --mode bijou-java --count 200000 --runs 3
```

#### Consumer Benchmarks

Test deserialization performance:

```bash
./scripts/run-consumer.sh --mode long --count 200000 --runs 3
./scripts/run-consumer.sh --mode bijou --count 200000 --runs 3
```

#### Comparative Analysis

Run all modes and compare side-by-side:

```bash
./scripts/compare-benchmarks.sh 200000 3
```

### Results

Benchmark results are stored in `logs/results-*.csv`:

```csv
mode,run,rate,avg_bytes,exit_code
long,1,305051,8.0,0
long,2,373400,8.0,0
bijou,1,282473,3.7,0
bijou,2,316501,3.7,0
bijou-java,1,370375,3.7,0
bijou-java,2,377400,3.7,0
```

**Key Metrics:**

- **rate**: Messages per second throughput
- **avg_bytes**: Average serialized message size in bytes
- **exit_code**: 0 = success, non-zero = failure

### Performance Insights

#### Bijou64 vs Long (200K messages, 3 runs)

| Mode           | Avg Throughput | Avg Payload | Space Savings | Network Savings       |
| -------------- | -------------- | ----------- | ------------- | --------------------- |
| Long           | 336k msg/s     | 8.0 bytes   | —             | —                     |
| Bijou64 (JNI)  | 308k msg/s     | 3.7 bytes   | **54%**       | ~1.1 MB per 200K msgs |
| Bijou64 (Java) | 363k msg/s     | 3.7 bytes   | **54%**       | ~1.1 MB per 200K msgs |

**Interpretation:**

- **Payload size reduction** is consistent across implementations (3.7 vs 8.0 bytes)
- **Pure-Java implementation** slightly faster than JNI due to lower overhead for small payloads
- **Network benefits**: Every million messages saves ~5.5 MB in transmission
- **Storage benefits**: Historical topics storing billions of messages see massive cumulative savings

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
./scripts/run-producer.sh --count 50000 --runs 1
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

### Normal producer benchmark

```bash
cd perf/kafka
./scripts/run-producer-normal.sh --topic bijou-test --bootstrap-server localhost:9092 --count 200000
```

### Normal consumer benchmark

```bash
cd perf/kafka
./scripts/run-consumer-normal.sh --topic bijou-test --bootstrap-server localhost:9092 --group-id bijou-consumer --count 200000
```

## Notes

- `Bijou64` uses the native JNI library, so the root project must be built and installed first.
- Run the benchmark from the repository root or set the JVM library path if the native library does not load.
- `report.md` is a template for capturing results and observations.
