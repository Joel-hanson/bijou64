# Changelog

## Unreleased

- Added `Bijou64.encodedLen()` / `encodedLenJava()` for allocation-free size queries
- Added JMH microbenchmarks (`./scripts/run-jmh.sh`) for Java and JNI encode/decode
- Fixed Kafka producer benchmark double-encoding that inflated bijou mode CPU cost
- Added benchmark value distributions (`sequential`, `uniform`, `boundary`) and warmup support
- Consumer benchmark supports `--produce-first` for self-contained runs
- `compare-benchmarks.sh` now supports `--ci`, warmup discard, and median rate reporting
- Pinned local Docker Kafka to 4.3.0 (matches CI); expanded CI with comparative Kafka benchmarks and JMH job

## [0.1.0] - 2026-05-30

- Core Bijou64 Java library with JNI and pure-Java fallback
- Kafka serializers/deserializers in `perf/kafka`
- Benchmark scripts and Docker Compose for local testing
