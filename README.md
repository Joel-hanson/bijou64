# Bijou64 - Efficient Variable-Length Integer Encoding for Kafka

[![Maven Central](https://img.shields.io/maven-central/v/org.bijou64/bijou64)](https://central.sonatype.com/search?q=bijou64)
[![CI](https://github.com/Joel-hanson/bijou64/actions/workflows/ci.yml/badge.svg)](https://github.com/Joel-hanson/bijou64/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

Bijou64 is a variable-length integer encoding library for Kafka `Long`/numeric payloads. By compressing integer values into fewer bytes, it enables smaller network transfers and lower storage overhead for integer-heavy topics.

## When to Use / Not Use

**Use Bijou64 when:**
- Your Kafka values are mostly numeric (`Long`/`Integer`) and high-volume.
- Topic storage and network egress costs matter.
- You can deploy matching serializer/deserializer on both producer and consumer.

**Do not use Bijou64 when:**
- Payloads are arbitrary JSON/Avro/Protobuf objects (Kafka compression is usually the right first step).
- You cannot control both producer and consumer serialization.
- Your topics are short-lived and storage/network savings are negligible.

## Key Benefits

- **54% Payload Size Reduction**: Long values encoded at 3.7 bytes average vs 8.0 bytes for standard Long serialization
- **Native Performance**: JNI bindings to optimized Rust implementation
- **Drop-in Kafka Integration**: Use as a Kafka Serializer/Deserializer with minimal configuration changes
- **Dual Implementation**: Both native (JNI) and pure-Java fallback for maximum compatibility

## Performance

Benchmarked with 200,000 messages per run (Kafka 4.3.0, KRaft mode, `sequential` distribution — values `1..N`, which favors small integers):

| Metric              | Long (baseline) | Bijou64     | Improvement        |
| ------------------- | --------------- | ----------- | ------------------ |
| Avg payload size    | 8.0 bytes       | 3.7 bytes   | **54% smaller**    |
| Producer throughput | ~336k msg/s     | ~308k msg/s | ~8% overhead       |
| Consumer throughput | ~285k msg/s     | ~261k msg/s | Network savings help |

These throughput numbers are **end-to-end Kafka runs** (producer/consumer + broker + network), not pure encode/decode microbenchmarks. Re-measure on your hardware after the benchmark methodology fix (no double encoding on bijou modes).

### Microbenchmarks (JMH)

Measure encode/decode throughput in isolation:

```bash
./scripts/run-jmh.sh
```

This runs JMH benchmarks across `sequential`, `uniform`, and `boundary` value distributions for Java and JNI implementations.

### Kafka integration benchmarks

Run your own end-to-end benchmarks with the scripts in [perf/kafka](perf/kafka/README.md).

## Quick Start

### Kafka Configuration

```properties
# Producer
key.serializer=org.bijou64.perf.kafka.Bijou64Serializer
value.serializer=org.bijou64.perf.kafka.Bijou64Serializer

# Consumer
key.deserializer=org.bijou64.perf.kafka.Bijou64Deserializer
value.deserializer=org.bijou64.perf.kafka.Bijou64Deserializer
```

### Maven Dependency

```xml
<dependency>
  <groupId>org.bijou64</groupId>
  <artifactId>bijou64</artifactId>
  <version>0.1.0</version>
</dependency>

<dependency>
  <groupId>org.bijou64</groupId>
  <artifactId>bijou64-kafka-serializers</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Building

```bash
git submodule update --init --recursive
mvn -B clean package
```

`mvn package` builds the native library via Cargo before packaging the JAR.

### Pure-Java Fallback

```properties
bijou64.useJava=true
```

## Testing

```bash
mvn test
```

## License

MIT — see [LICENSE](LICENSE).

Based on the [Bijou](https://github.com/inkandswitch/bijou) encoding by Ink & Switch.
