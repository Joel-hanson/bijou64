# Security Policy

## Reporting Security Vulnerabilities

**Please do not open public issues for security vulnerabilities.**

If you discover a security vulnerability in Bijou64, please email: [security@bijou64.org](mailto:security@bijou64.org)

Include:

- Description of the vulnerability
- Steps to reproduce
- Affected version(s)
- Potential impact

We will investigate and provide updates as quickly as possible.

## Security Guidelines

### For Developers

- Keep dependencies up to date
- Run security scans: `mvn dependency-check:check`
- Review native code changes carefully
- Validate input data (particularly in deserializer)

### For Users

- Update to the latest version promptly
- Monitor [CHANGELOG](CHANGELOG.md) for security fixes
- Review [CVE notifications](https://nvd.nist.gov/)

## Supported Versions

| Version | Status | End of Support |
| ------- | ------ | -------------- |
| 0.1.x   | Beta   | 2026-12-31     |
| 1.0.x   | Active | 2029-12-31     |

## Known Issues

None currently reported.

## Dependencies Security

Bijou64 depends on:

- **kafka-clients** - Monitor for security updates
- **junit-jupiter** - Test only, no production impact
- **Rust standard library** - Pinned in Cargo.toml

We monitor CVE databases for all dependencies.

## Best Practices

1. **Input Validation**: Verify Long values are within expected ranges
2. **Error Handling**: Catch `IllegalArgumentException` from deserializer
3. **Monitoring**: Track deserialization errors
4. **Testing**: Test with malformed data

## Compliance

- No hardcoded credentials
- No personally identifiable information
- Memory-safe core algorithms
- Deterministic encoding (reproducible)

## Vulnerability Disclosure

We follow responsible disclosure practices:

1. Acknowledge receipt within 24 hours
2. Investigate and determine severity
3. Develop patch if needed
4. Coordinate release timing
5. Credit researcher (if desired)

---

Last Updated: May 30, 2026
