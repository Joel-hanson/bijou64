# Production-Ready Repository Structure

Bijou64 follows best practices for open-source Java/Kafka projects. This document explains the repository structure and its purpose.

## Directory Structure

```
bijou64/
├── README.md                          # Main project overview
├── QUICKSTART.md                      # Quick integration guide (start here!)
├── CHANGELOG.md                       # Version history and release notes
├── PERFORMANCE.md                     # Detailed performance analysis and benchmarks
├── DEPLOYMENT.md                      # Release and deployment procedures
├── CONTRIBUTING.md                    # Guidelines for contributors
├── SECURITY.md                        # Security policies and vulnerability reporting
├── LICENSE-APACHE                     # Apache 2.0 license
├── LICENSE-MIT                        # MIT license
├── pom.xml                            # Root Maven configuration
├── build-native.sh                    # Native Rust compilation script
│
├── .github/
│   └── workflows/
│       ├── ci.yml                     # CI pipeline (test on push)
│       └── release.yml                # Release automation (tag-triggered)
│
├── src/main/java/org/bijou64/         # Core library source code
│   ├── Bijou64.java                   # Main encoding/decoding class
│   ├── Main.java                      # Example/test code
│   └── ...
│
├── src/test/java/org/bijou64/         # Unit tests
│   └── ...
│
├── native/
│   ├── Cargo.toml                     # Rust workspace config
│   ├── src/lib.rs                     # JNI wrapper (Rust)
│   └── bijou/                         # Git submodule (upstream)
│       └── bijou64/                   # Upstream Bijou64 Rust library
│           └── src/lib.rs
│
├── perf/kafka/                        # Kafka performance benchmarking module
│   ├── pom.xml                        # Kafka serializers Maven config
│   ├── README.md                      # Kafka serializers integration guide
│   ├── docker-compose.yml             # Local Kafka setup (KRaft mode)
│   │
│   ├── src/main/java/org/bijou64/perf/kafka/
│   │   ├── Bijou64Serializer.java     # Kafka Serializer implementation
│   │   ├── Bijou64Deserializer.java   # Kafka Deserializer implementation
│   │   ├── ProducerBenchmark.java     # Producer benchmark harness
│   │   ├── ConsumerBenchmark.java     # Consumer benchmark harness
│   │   └── ...
│   │
│   ├── scripts/
│   │   ├── run-producer.sh            # Run producer benchmarks
│   │   ├── run-consumer.sh            # Run consumer benchmarks
│   │   ├── compare-benchmarks.sh      # Compare all modes
│   │   └── ...
│   │
│   └── logs/
│       ├── results-*.csv              # Benchmark results
│       └── *.log                      # Benchmark execution logs
│
└── target/                            # Maven build output (git-ignored)
    ├── bijou64-0.1.0.jar             # Compiled library
    └── ...
```

## File Purpose Guide

### Root Level Documentation

| File                | Purpose                                 | Audience                       |
| ------------------- | --------------------------------------- | ------------------------------ |
| **README.md**       | Project overview, features, benefits    | Everyone                       |
| **QUICKSTART.md**   | 5-minute integration guide              | Developers integrating Bijou64 |
| **CHANGELOG.md**    | Release history, breaking changes       | Version-conscious users        |
| **PERFORMANCE.md**  | Benchmark results, cost analysis        | Performance-focused teams      |
| **DEPLOYMENT.md**   | Maven Central release process           | Release managers, DevOps       |
| **CONTRIBUTING.md** | Development setup, PR process           | Contributors                   |
| **SECURITY.md**     | Vulnerability reporting, best practices | Security teams                 |

### Configuration Files

| File                | Purpose                                           |
| ------------------- | ------------------------------------------------- |
| **pom.xml**         | Maven build, dependencies, Maven Central metadata |
| **build-native.sh** | Compile Rust native library                       |

### GitHub Automation

| File                              | Purpose                             | Triggers             |
| --------------------------------- | ----------------------------------- | -------------------- |
| **.github/workflows/ci.yml**      | Build, test, benchmark              | Push to main/develop |
| **.github/workflows/release.yml** | Build, test, sign, release to Maven | Tag push (v0.1.0)    |

### Source Code Organization

```
src/main/java/org/bijou64/
├── Bijou64.java              # Core API: encode(long), decode(byte[])
├── Main.java                 # Example usage
└── ...

perf/kafka/src/main/java/org/bijou64/perf/kafka/
├── Bijou64Serializer.java         # Kafka producer serializer
├── Bijou64Deserializer.java       # Kafka consumer deserializer
├── ProducerBenchmark.java         # Benchmark entry point
└── ConsumerBenchmark.java         # Benchmark entry point
```

### Benchmarking Infrastructure

```
perf/kafka/
├── docker-compose.yml         # Single-command Kafka setup (KRaft)
├── scripts/
│   ├── run-producer.sh       # Benchmark script (configurable)
│   ├── run-consumer.sh       # Benchmark script (configurable)
│   └── compare-benchmarks.sh # Run all modes, compare results
└── logs/
    └── results-*.csv        # Machine-readable benchmark results
```

## Publishing & Releases

### Artifact Coordinates

**Core Library** (Java encode/decode only):

```xml
<groupId>org.bijou64</groupId>
<artifactId>bijou64</artifactId>
<version>0.1.0</version>
```

**Kafka Serializers** (Production-ready):

```xml
<groupId>org.bijou64</groupId>
<artifactId>bijou64-kafka-serializers</artifactId>
<version>0.1.0</version>
```

### Release Process

1. **Tag** a commit: `git tag -s v0.1.0`
2. **GitHub Actions** automatically:
   - Builds on Linux, macOS, Windows
   - Runs full test suite
   - Compiles Rust native library
   - Signs JARs with GPG
   - Creates GitHub release
   - Publishes to Maven Central (optional)

### Maven Central Publishing

Artifacts are published to:

```
https://repo1.maven.org/maven2/org/bijou64/bijou64/0.1.0/
https://repo1.maven.org/maven2/org/bijou64/bijou64-kafka-serializers/0.1.0/
```

Search at: https://central.sonatype.com/search?q=bijou64

## Development Workflow

### Setting Up Locally

```bash
# Clone with submodules
git clone --recurse-submodules <repo>

# Build Rust
./build-native.sh

# Build Java
mvn clean install

# Run tests
mvn test

# Run benchmarks
cd perf/kafka && docker compose up -d
./scripts/run-producer.sh --mode bijou --count 10000 --topic bijou64-benchmark-topic --bootstrap-server localhost:9092
```

### CI/CD Pipeline

**On every push** (ci.yml):

- ✅ Compile Java on 3 OS platforms
- ✅ Compile Rust native
- ✅ Run unit tests
- ✅ Run integration tests (with Kafka in Docker)
- ✅ Run benchmarks
- ✅ Upload benchmark artifacts

**On tag push** (release.yml):

- ✅ Build all platforms
- ✅ Sign artifacts (GPG)
- ✅ Create GitHub release
- ✅ Publish to Maven Central
- ✅ Update GitHub release page

## Production Deployment Checklist

Before deploying to production:

- [ ] Review [PERFORMANCE.md](PERFORMANCE.md) for your use case
- [ ] Run [benchmarks](perf/kafka/README.md#benchmarking) on your infrastructure
- [ ] Test with your Kafka cluster topology
- [ ] Verify serializer JAR is in classpath
- [ ] Monitor deserialization error rates
- [ ] Gradually roll out (canary deployment)
- [ ] Track compression metrics
- [ ] Plan rollback strategy

## Maintenance & Support

### Issue Handling

1. **Bug Report** → Investigate → Fix → Test → Release
2. **Feature Request** → Discuss → Design → Implement → Document
3. **Performance Issue** → Benchmark → Analyze → Optimize → Verify

### Version Support

| Version | Status  | Support |
| ------- | ------- | ------- |
| 0.1.x   | Current | Active  |
| 1.0+    | Future  | TBD     |

### Security Updates

- Subscribe to [SECURITY.md](SECURITY.md) for advisories
- Check [CHANGELOG.md](CHANGELOG.md) for CVE fixes
- Report vulnerabilities privately to security@bijou64.org

## Integration Examples

### Existing Kafka Project

1. Add Maven dependency (see [QUICKSTART.md](QUICKSTART.md))
2. Update `producer.properties`: `value.serializer=org.bijou64.perf.kafka.Bijou64Serializer`
3. Update `consumer.properties`: `value.deserializer=org.bijou64.perf.kafka.Bijou64Deserializer`
4. Deploy serializer JAR to Kafka classpath
5. Restart producers/consumers
6. Monitor compression metrics

### Spring Boot Application

1. Add Maven dependencies
2. Configure in `application.properties` (see [QUICKSTART.md](QUICKSTART.md))
3. Use `@KafkaListener` and `KafkaTemplate` normally
4. Serialization is automatic

### Kafka Connect

1. Add JAR to `plugin.path`
2. Reference in connector config
3. Connector uses serializers automatically

## Governance

### Decision Making

- **Small fixes/docs**: Direct merge after review
- **Feature additions**: Discussion issue first
- **Breaking changes**: Major version bump, with migration guide
- **Security fixes**: ASAP release, security advisory

### Code Quality Standards

- Unit test coverage: >80%
- No warnings in `mvn clean compile`
- Benchmark improvements documented
- All PRs require passing CI

## References

- [Official Bijou Encoding](https://github.com/inkandswitch/bijou)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Maven Central Guide](https://central.sonatype.org/publish/publish-guide/)
- [Semantic Versioning](https://semver.org/)

---

**Last Updated**: May 30, 2026

This structure ensures Bijou64 is:

- ✅ Easy to integrate into existing Kafka pipelines
- ✅ Professional and production-ready
- ✅ Well-documented for all audiences
- ✅ Properly versioned and released
- ✅ Maintainable and extensible
