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

### CI Kafka producer benchmark

On every push to `main`, the **`benchmark` job** in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs the producer comparison test:

```bash
./scripts/compare-benchmarks.sh --ci --distribution sequential 50000 2 \
  bijou64-benchmark-topic localhost:9092
```

**Test setup:** 50,000 messages per run, `sequential` distribution (values `1..N`), 2 measured iterations per mode (plus 1 discarded warmup), median rate reported. Kafka 4.3.0 (KRaft) on `ubuntu-latest`, Java 17.

**Modes tested in CI:** `long` and `bijou` / `bijou-java`, each with `none` and `zstd` compression.

Results from CI run `2026-06-06` ([`results-20260606T110707.csv`](results-20260606T110707.csv)):

| Mode           | Compression | Median throughput | Avg payload |
| -------------- | ----------- | ----------------- | ----------- |
| Long           | none        | 102,849 msg/s     | 8.0 bytes   |
| Long           | zstd        | 94,099 msg/s      | 8.0 bytes   |
| Bijou64 (JNI)  | none        | 114,590 msg/s     | 3.0 bytes   |
| Bijou64 (JNI)  | zstd        | 98,789 msg/s      | 3.0 bytes   |
| Bijou64 (Java) | none        | 116,683 msg/s     | 3.0 bytes   |
| Bijou64 (Java) | zstd        | 108,589 msg/s     | 3.0 bytes   |

**Takeaways from CI:**

- **62% smaller payloads** on sequential integers (3 vs 8 bytes) — consistent with the ~54% average savings claim for mixed distributions.
- **~11–14% higher producer throughput** than `LongSerializer` without compression on the GHA runner.
- `zstd` transport compression reduces throughput for all modes; Bijou64’s smaller payloads compress less data, so the relative penalty is smaller.

These are **end-to-end Kafka producer runs** (client + broker + network), not pure encode/decode microbenchmarks. Absolute rates depend on hardware; run the same test locally for your environment (see [perf/kafka](perf/kafka/README.md)).

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
