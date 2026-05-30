# Deployment & Release Guide

This guide covers how to release Bijou64 to Maven Central and manage production deployments.

## Prerequisites

- Maven 3.8+
- GPG with a registered key
- JIRA account with Sonatype OSSRH
- GitHub repository admin access

## Setting Up Maven Central

### 1. Sonatype OSSRH Registration

Register with Sonatype OSSRH:

- https://issues.sonatype.org/
- Create JIRA account
- Create issue requesting namespace: `org.bijou64`
- Verify domain ownership (GitHub repository)

### 2. GPG Key Setup

Create GPG key for signing artifacts:

```bash
# Generate key (select RSA, 4096 bits, no expiration)
gpg --gen-key

# List keys
gpg --list-keys

# Publish key to keyserver
gpg --keyserver hkps://keys.openpgp.org --send-keys YOUR_KEY_ID

# Export private key (for CI/CD)
gpg --export-secret-keys YOUR_KEY_ID > private-key.gpg
```

### 3. Maven Configuration

Create/update `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>your-sonatype-username</username>
      <password>your-sonatype-password</password>
    </server>
    <server>
      <id>gpg.passphrase</id>
      <passphrase>your-gpg-passphrase</passphrase>
    </server>
  </servers>
</settings>
```

**Security Note**: Never commit credentials to git. Use environment variables:

```bash
export SONATYPE_USERNAME="your-username"
export SONATYPE_PASSWORD="your-password"
export GPG_PASSPHRASE="your-passphrase"
```

## Releasing to Maven Central

### Manual Release Process

```bash
# 1. Ensure all changes are committed
git status
git add .
git commit -m "chore: prepare for release"

# 2. Update version in pom.xml files
# Change from 0.1.0-SNAPSHOT to 0.1.0
mvn versions:set -DnewVersion=0.1.0

# 3. Commit version update
git add pom.xml perf/kafka/pom.xml
git commit -m "chore: bump version to 0.1.0"

# 4. Build and sign artifacts
mvn clean verify
mvn -Dgpg.passphrase=$GPG_PASSPHRASE clean package gpg:sign

# 5. Deploy to staging repository
mvn -Dgpg.passphrase=$GPG_PASSPHRASE clean deploy

# 6. Release from staging
mvn nexus-staging:release -Ddescription="Release 0.1.0"

# 7. Tag release
git tag -s v0.1.0 -m "Release v0.1.0"
git push origin v0.1.0

# 8. Update to next snapshot version
mvn versions:set -DnewVersion=0.2.0-SNAPSHOT
git add pom.xml perf/kafka/pom.xml
git commit -m "chore: bump version to 0.2.0-SNAPSHOT"
git push
```

### Automated Release (GitHub Actions)

Tag creation triggers automatic release:

```bash
git tag -s v0.1.0 -m "Release v0.1.0"
git push origin v0.1.0
```

GitHub Actions workflow will:

1. Build and test
2. Sign artifacts
3. Deploy to Maven Central
4. Create GitHub release
5. Upload JARs to release

## GitHub Actions Secrets

Add these to GitHub repository settings:

```
SONATYPE_USERNAME      - Sonatype OSSRH username
SONATYPE_PASSWORD      - Sonatype OSSRH password
GPG_SECRET_KEYS        - Base64-encoded GPG private key
GPG_OWNERTRUST         - Base64-encoded GPG ownertrust file
GPG_PASSPHRASE         - GPG key passphrase
```

Setup scripts:

```bash
# Export GPG secrets
gpg --export-secret-keys --armor YOUR_KEY_ID | base64 -w0 > gpg_private_key.txt
gpg --export-ownertrust | base64 -w0 > gpg_ownertrust.txt

# Add to GitHub Secrets
# Copy contents of gpg_private_key.txt -> GPG_SECRET_KEYS
# Copy contents of gpg_ownertrust.txt -> GPG_OWNERTRUST
```

## Version Management

### Semantic Versioning

- **0.1.x** - Beta/experimental
- **1.0.0** - First stable release
- **1.1.x** - Minor features (backwards compatible)
- **2.0.0** - Major breaking changes

### Snapshot Versions

Development versions use `-SNAPSHOT` suffix:

- `0.1.0-SNAPSHOT` = development towards 0.1.0
- Deployed to snapshot repository only
- Users can depend on for testing, not recommended for production

## Release Checklist

- [ ] Update CHANGELOG.md with release notes
- [ ] Update README.md with new version
- [ ] Update PERFORMANCE.md with latest benchmarks
- [ ] Run full test suite
- [ ] Run benchmarks on multiple platforms
- [ ] Update version in all pom.xml files
- [ ] Build and sign artifacts
- [ ] Deploy to staging repository
- [ ] Test staged artifacts from maven.org
- [ ] Release from staging to Maven Central
- [ ] Create GitHub release with release notes
- [ ] Update version to next SNAPSHOT
- [ ] Announce release (GitHub, email, etc.)

## Monitoring Release

### Maven Central Sync

After release:

```bash
# Check Maven Central (takes ~10-30 minutes to sync)
curl -s https://repo1.maven.org/maven2/org/bijou64/bijou64/0.1.0/bijou64-0.1.0.jar
```

### Verify Published Artifacts

```bash
# Using Maven
mvn dependency:tree | grep bijou64

# Using Gradle
gradle dependencies | grep bijou64
```

## Deployment to Production

### Kafka Broker Deployment

1. **Add JAR to Kafka classpath**:

```bash
cp bijou64-kafka-serializers-0.1.0.jar $KAFKA_HOME/libs/
```

2. **Configure producer** (producer.properties):

```properties
bootstrap.servers=kafka-broker:9092
key.serializer=org.apache.kafka.common.serialization.StringSerializer
value.serializer=org.bijou64.perf.kafka.Bijou64Serializer
```

3. **Configure consumer** (consumer.properties):

```properties
bootstrap.servers=kafka-broker:9092
group.id=my-consumer-group
key.deserializer=org.apache.kafka.common.serialization.StringDeserializer
value.deserializer=org.bijou64.perf.kafka.Bijou64Deserializer
```

### Spring Boot Integration

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

application.properties:

```properties
spring.kafka.producer.value-serializer=org.bijou64.perf.kafka.Bijou64Serializer
spring.kafka.consumer.value-deserializer=org.bijou64.perf.kafka.Bijou64Deserializer
```

### Docker Deployment

Dockerfile example:

```dockerfile
FROM openjdk:17-slim

COPY bijou64-kafka-serializers-0.1.0.jar /opt/kafka/libs/
COPY my-app.jar /opt/my-app.jar

ENTRYPOINT ["java", "-jar", "/opt/my-app.jar"]
```

## Rollback Procedures

If issues are found after release:

1. **Stop using new version**: Direct clients to previous version
2. **Investigate issue**: Add tests to prevent regression
3. **Fix issue**: Commit fix to main branch
4. **Release patch**: Tag v0.1.1 with fix
5. **Mark problematic version**: Add note to CHANGELOG

## Support & Troubleshooting

### Build Fails on CI

```bash
# Run locally with same settings
mvn -Dgpg.passphrase=$GPG_PASSPHRASE clean deploy
```

### GPG Key Not Found

```bash
# Import private key to CI environment
echo "$GPG_PRIVATE_KEY" | base64 -d | gpg --import
gpg --import-ownertrust <(echo "$GPG_OWNERTRUST" | base64 -d)
```

### Maven Central Sync Delayed

Wait 10-30 minutes and check:

```bash
https://repo1.maven.org/maven2/org/bijou64/bijou64/0.1.0/bijou64-0.1.0.pom
```

### Artifact Already Exists

Cannot re-upload same version. Use:

```bash
# Staged release URL during testing
https://s01.oss.sonatype.org/service/local/repositories/staging/content/
```

## References

- [Sonatype OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [Maven GPG Plugin](https://maven.apache.org/plugins/maven-gpg-plugin/)
- [Nexus Staging](https://github.com/sonatype/nexus-maven-plugins/wiki)
- [Semantic Versioning](https://semver.org/)

---

Last Updated: May 30, 2026
