# Bijou64 - Efficient Variable-Length Integer Encoding for Kafka

[![Maven Central](https://img.shields.io/maven-central/v/org.bijou64/bijou64)](https://central.sonatype.com/search?q=bijou64)
[![CI](https://github.com/yourusername/bijou64/actions/workflows/ci.yml/badge.svg)](https://github.com/yourusername/bijou64/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0%20%7C%20MIT-blue)](LICENSE-APACHE)

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
- **Production-Ready**: Thoroughly benchmarked with comprehensive performance metrics

## Performance Metrics

Based on comprehensive benchmarking with 200,000+ messages:

| Metric              | Long (Baseline) | Bijou64     | Improvement                |
| ------------------- | --------------- | ----------- | -------------------------- |
| Avg Payload Size    | 8.0 bytes       | 3.7 bytes   | **54% smaller**            |
| Producer Throughput | ~336k msg/s     | ~308k msg/s | Minimal overhead\*         |
| Consumer Throughput | ~285k msg/s     | ~261k msg/s | Network savings compensate |

\*The reduced payload size means fewer bytes transmitted and stored per message, making Bijou64 ideal for high-volume, space-sensitive scenarios.

## Quick Start

### For Existing Kafka Producers/Consumers

Replace standard Kafka serializers with Bijou64:

```properties
# Producer Configuration
key.serializer=org.bijou64.perf.kafka.Bijou64Serializer
value.serializer=org.bijou64.perf.kafka.Bijou64Serializer

# Consumer Configuration
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

## Documentation

- [Kafka Serializers Guide](perf/kafka/README.md) - Integration guide, configuration, and benchmarking
- [Encoding Specification](native/bijou/bijou64/SPEC.md) - Technical details on the Bijou64 encoding format
- [Performance Analysis](native/bijou/bijou64/SHOOTOUT_ANALYSIS.md) - Detailed performance comparisons

## Project Structure

```
bijou64/
├── src/main/java/org/bijou64/       # Core library (encode/decode logic)
├── src/test/java/                    # Unit tests
├── perf/kafka/                        # Kafka serializers & benchmarks
│   ├── src/main/java/org/bijou64/perf/kafka/
│   │   ├── Bijou64Serializer.java    # Producer serializer
│   │   └── Bijou64Deserializer.java  # Consumer deserializer
│   ├── scripts/                       # Benchmark runner scripts
│   ├── docker-compose.yml            # KRaft Kafka for testing
│   └── logs/                          # Performance test results
└── native/                            # Rust JNI bindings
    └── bijou/bijou64/                 # Upstream Bijou64 Rust library
```

## Running Benchmarks

Benchmark Bijou64 performance against standard Kafka Long encoding:

### 1. Start Kafka with Docker

```bash
cd perf/kafka
docker compose up -d
```

### 2. Build and Install Root Library

```bash
mvn -B clean install -DskipTests
```

### 3. Run Producer Benchmark

```bash
cd perf/kafka
./scripts/run-producer.sh --mode bijou --count 200000 --topic bijou64-benchmark-topic --bootstrap-server localhost:9092
```

### 4. Run Consumer Benchmark

```bash
cd perf/kafka
./scripts/run-consumer.sh --mode bijou --count 200000 --topic bijou64-benchmark-topic --group-id bijou64-benchmark-group --bootstrap-server localhost:9092
```

### 5. View Results

Results are saved to `perf/kafka/logs/results-*.csv` with:

- Message rates (msg/s)
- Average payload sizes
- Throughput comparisons

## Building

### Using Maven (Recommended)

```bash
mvn -B clean package
```

### Building with Native Library

Requires Rust 1.70+ installed:

```bash
# Clone and initialize submodules
git submodule update --init --recursive

# Build native Rust library
./build-native.sh

# Build Java project
mvn -B clean package
```


## Testing

```bash
mvn test
```

## Use Cases

- **High-volume time-series data**: Financial ticks, sensor data, metrics
- **Cost-sensitive deployments**: Reduce storage and egress costs
- **Edge computing**: Minimize bandwidth for resource-constrained environments
- **Long-running Kafka topics**: Significant cumulative space savings
- **Compliance & Archival**: Reduce storage footprint for historical data

## Configuration

### Java Implementation Fallback

Use pure-Java implementation instead of JNI (if native library unavailable):

```properties
bijou64.useJava=true
```

### Benchmarking Options

```bash
./scripts/run-producer.sh --mode [long|bijou|bijou-java] --count <n> --topic <topic> --bootstrap-server <host:port>
./scripts/run-consumer.sh --mode [long|bijou|bijou-java] --count <n> --topic <topic> --group-id <group> --bootstrap-server <host:port>
```

## License

Licensed under either of:

- Apache License, Version 2.0 ([LICENSE-APACHE](LICENSE-APACHE) or http://www.apache.org/licenses/LICENSE-2.0)
- MIT license ([LICENSE-MIT](LICENSE-MIT) or http://opensource.org/licenses/MIT)

at your option.

## Acknowledgments

- Based on the [Bijou](https://github.com/inkandswitch/bijou) variable-length encoding by Ink & Switch
- Kafka integration and benchmarking by the community

## Contributing

Contributions are welcome! Please ensure benchmarks pass and add tests for new features.

For issues or questions, open a GitHub issue with performance results from your environment.

If you use Maven, running `mvn -B clean package` will also invoke the native build before packaging.
