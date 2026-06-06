# Kafka Counter Example

A minimal Kafka app that pulls **Bijou64 from [GitHub Packages](https://github.com/Joel-hanson/bijou64/packages)** — the same way you would in your own project. It sends sequential `Long` values with `Bijou64Serializer` and reads them back with `Bijou64Deserializer`.

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker (for local Kafka)
- A GitHub account with a PAT that has **`read:packages`** ([create one here](https://github.com/settings/tokens))

## 1. Configure Maven for GitHub Packages

GitHub Packages requires authentication, even for public artifacts.

Copy `settings.xml.example` into your Maven settings file and fill in your credentials:

```bash
mkdir -p ~/.m2
cp settings.xml.example ~/.m2/settings.xml
# edit ~/.m2/settings.xml — set your GitHub username and PAT
```

The server `<id>github</id>` must match the repository `<id>` in `pom.xml`.

**CI / environment variable:**

```xml
<password>${env.GITHUB_TOKEN}</password>
```

## 2. Build the example

This resolves `org.bijou64:bijou64-kafka-serializers:0.2.0` from GitHub Packages (Kafka clients come from Maven Central):

```bash
cd examples/kafka-counter-app
mvn -B package
```

Your `pom.xml` should look like any customer application — a GitHub Packages repository plus the dependency:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/Joel-hanson/bijou64</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>org.bijou64</groupId>
    <artifactId>bijou64-kafka-serializers</artifactId>
    <version>0.2.0</version>
  </dependency>
</dependencies>
```

## 3. Start Kafka

```bash
docker compose up -d
```

## 4. Run the producer and consumer

**Terminal 1 — produce 10 counter values:**

```bash
mvn exec:java -Dexec.mainClass=org.bijou64.examples.kafka.CounterProducer
```

**Terminal 2 — consume them:**

```bash
mvn exec:java -Dexec.mainClass=org.bijou64.examples.kafka.CounterConsumer
```

Optional arguments: `bootstrap-server`, `topic`, `count` / `max-messages`.

```bash
mvn exec:java -Dexec.mainClass=org.bijou64.examples.kafka.CounterProducer \
  -Dexec.args="localhost:9092 my-topic 100"
```

## What to look for

Producer output shows each value encoded in **1 byte** for small integers (`1`–`247`), compared with **8 bytes** for standard Kafka `LongSerializer`:

```
sent value=1 encoded=1 byte(s)
sent value=2 encoded=1 byte(s)
...
sent value=300 encoded=2 byte(s)
```

Both sides must use Bijou64 serializers on the same topic. Mixing `LongSerializer` on one side will produce unreadable values on the other.

## Serializer configuration in your app

The example sets serializers in Java. In a properties-driven app you would use:

```properties
# producer
value.serializer=org.bijou64.perf.kafka.Bijou64Serializer

# consumer
value.deserializer=org.bijou64.perf.kafka.Bijou64Deserializer
```

## Developing inside the bijou64 repository

If you are working on the library itself, install snapshots locally instead of pulling from GitHub Packages:

```bash
# from the repository root
git submodule update --init --recursive
mvn -B install -DskipTests -Dgpg.skip=true
mvn -B install -DskipTests -Dgpg.skip=true -f perf/kafka/pom.xml
```

Maven resolves from `~/.m2` first, so the example builds without GitHub Packages auth once the artifacts are installed locally.
