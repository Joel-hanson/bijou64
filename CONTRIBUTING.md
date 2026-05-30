# Contributing to Bijou64

We welcome contributions! This guide explains how to get started, run tests, and submit changes.

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Rust 1.70+ (for native builds)
- Git

### Development Setup

```bash
# Clone repository
git clone --recurse-submodules https://github.com/yourusername/bijou64.git
cd bijou64

# Initialize Rust submodule if needed
git submodule update --init --recursive

# Build native library
./build-native.sh

# Build Java project
mvn clean install

# Run tests
mvn test
```

## Making Changes

### Code Style

- Follow existing code style (consistent indentation, naming conventions)
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Keep methods focused and testable

### Commit Messages

Follow conventional commits format:

```
feat: add new serializer for Integer type
fix: handle null values in deserializer
docs: update benchmark results
test: add roundtrip encoding tests
perf: optimize decoding for common values
```

### Testing

All changes must pass tests:

```bash
# Run unit tests
mvn test

# Run integration tests (requires Kafka)
cd perf/kafka
docker compose up -d
./scripts/run-producer.sh --count 10000 --runs 1
docker compose down
```

### Performance Benchmarking

Before submitting performance improvements, run benchmarks:

```bash
cd perf/kafka

# Start Kafka
docker compose up -d

# Build root project
mvn -B clean install -DskipTests

# Run comparative benchmarks
mvn -B package
./scripts/compare-benchmarks.sh 200000 3

# Compare results in logs/
head logs/results-*.csv
```

## Submitting Changes

### Pull Request Process

1. **Fork** the repository
2. **Create a feature branch**: `git checkout -b feature/your-feature`
3. **Make changes** and commit with conventional commits
4. **Add tests** for new functionality
5. **Run benchmarks** if performance-related
6. **Push** to your fork
7. **Submit PR** with detailed description

### PR Description Template

```markdown
## Description

Brief description of what this PR does.

## Motivation

Why is this change needed?

## Changes

- Bullet list of changes
- Include any API modifications

## Testing

How did you test this?

## Performance Impact

- Benchmark results (if applicable)
- Before/after metrics

## Checklist

- [ ] Tests pass locally
- [ ] Documentation updated
- [ ] Commit messages follow convention
- [ ] No breaking changes (or clearly documented)
```

## Reporting Issues

### Bug Reports

Include:

- Java version and OS
- Kafka version
- Steps to reproduce
- Expected vs actual behavior
- Full stack trace

```markdown
## Environment

- Java: 17.0.x
- Kafka: 3.5.1
- OS: Linux

## Steps to Reproduce

1. ...
2. ...

## Actual Behavior

...

## Expected Behavior

...
```

### Feature Requests

Include:

- Use case
- Proposed API/behavior
- Potential performance impact

## Running Full Test Suite

```bash
# Full build with all tests
mvn clean verify

# With coverage report
mvn clean verify jacoco:report

# View coverage
open target/site/jacoco/index.html
```

## Building Documentation

```bash
# Generate JavaDoc
mvn javadoc:javadoc

# View generated docs
open target/site/apidocs/index.html
```

## Performance Guidelines

### Adding Benchmarks

1. Add test case to `perf/kafka/src/main/java/`
2. Add script to `perf/kafka/scripts/`
3. Run multiple iterations (≥3) for statistical significance
4. Document results in PR

### Optimization PRs

Must include:

- Before/after performance metrics
- Run on multiple JVM versions
- Explain optimization rationale
- Ensure no regression in other areas

## Code Review Process

### What Reviewers Look For

- **Correctness**: Does it work as intended?
- **Testing**: Adequate test coverage?
- **Performance**: Any regressions?
- **Documentation**: Clear and accurate?
- **Style**: Consistent with codebase?

### Feedback

We aim to review PRs within 3 business days. Be responsive to feedback and willing to iterate.

## Release Process

### Cutting a Release

1. Update version in `pom.xml` files
2. Update `CHANGELOG.md` with release notes
3. Create release branch: `release/v0.2.0`
4. Tag: `git tag -s v0.2.0`
5. Push tags: `git push origin v0.2.0`
6. GitHub Actions automatically:
   - Builds artifacts
   - Runs tests
   - Creates GitHub release
   - Publishes to Maven repositories

## Questions?

- **Documentation**: See [README.md](README.md) and [Kafka Guide](perf/kafka/README.md)
- **Technical Issues**: Open a GitHub issue
- **Security**: See [SECURITY.md](SECURITY.md)

## License

By contributing, you agree that your contributions will be licensed under the same terms (Apache 2.0 OR MIT) as the project.

---

Thank you for contributing to Bijou64! 🙏
