# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-05-30

### Added

- Initial public release of Bijou64 library
- Kafka serializers for Bijou64 compression (`Bijou64Serializer`, `Bijou64Deserializer`)
- Comprehensive benchmarking suite for Kafka producer/consumer performance
- Native JNI bindings to Rust implementation with pure-Java fallback
- Docker Compose setup for local Kafka testing (KRaft mode)
- Benchmark scripts for comparative performance analysis
- Full test coverage for encoding/decoding operations

### Performance Highlights

- **54% payload size reduction**: 3.7 bytes (Bijou64) vs 8.0 bytes (Long)
- **Minimal throughput overhead**: ~8% difference in msg/s
- **Scalable space savings**: ~1.1 MB saved per 200K messages

### Features

- Variable-length integer encoding optimized for small values
- Dual implementation: JNI (native Rust) and pure Java
- Configurable serializer behavior via properties
- Full Kafka ecosystem compatibility (3.0+)
- Self-describing format (no schema registry required)

## [Unreleased]

### Planned

- Spring Boot starter for simplified integration
- Kafka Connect integration guide
- Performance analytics dashboard
- Additional data type support (Integer, generic serialization)
- gRPC protobuf support

---

## Version Numbering

- **0.1.x** - Stabilization and performance tuning
- **0.2.0** - Additional data types and Spring Boot support
- **1.0.0** - Production-ready stable release

## Upgrading

### 0.1.0 Release

No breaking changes from this initial release. Future releases may introduce:

- Additional serializer implementations
- Configuration schema changes
- Default behavior modifications
