# Quick Start Guide

Get Bijou64 Kafka serializers integrated into your project in 5 minutes.

## Installation

### Maven

Add to `pom.xml`:

```xml
<dependencies>
    <!-- Core Bijou64 library -->
    <dependency>
        <groupId>org.bijou64</groupId>
        <artifactId>bijou64</artifactId>
        <version>0.1.0</version>
    </dependency>

    <!-- Kafka serializers -->
    <dependency>
        <groupId>org.bijou64</groupId>
        <artifactId>bijou64-kafka-serializers</artifactId>
        <version>0.1.0</version>
    </dependency>

    <!-- Your existing Kafka dependency -->
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-clients</artifactId>
        <version>3.5.1</version>
    </dependency>
</dependencies>
```


## Basic Usage

### Kafka Producer (Java)

```java
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.bijou64.perf.kafka.Bijou64Serializer;
import java.util.Properties;

// Configure producer with Bijou64 serializer
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.bijou64.perf.kafka.Bijou64Serializer");

KafkaProducer<String, Long> producer = new KafkaProducer<>(props);

// Send messages with Long values (automatically compressed)
for (long i = 0; i < 1000; i++) {
    producer.send(new ProducerRecord<>(
        "my-topic",           // topic
        "key-" + i,          // key
        System.currentTimeMillis()  // value (Long)
    ));
}

producer.close();
```

### Kafka Consumer (Java)

```java
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.bijou64.perf.kafka.Bijou64Deserializer;
import java.util.Arrays;
import java.util.Properties;

// Configure consumer with Bijou64 deserializer
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("group.id", "my-consumer-group");
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("value.deserializer", "org.bijou64.perf.kafka.Bijou64Deserializer");

KafkaConsumer<String, Long> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("my-topic"));

while (true) {
    ConsumerRecords<String, Long> records = consumer.poll(100);
    records.forEach(record -> {
        System.out.printf("Key: %s, Value: %d%n", record.key(), record.value());
    });
}
```

### Properties File Configuration

**producer.properties**:

```properties
bootstrap.servers=kafka-broker:9092
acks=all
retries=3
key.serializer=org.apache.kafka.common.serialization.StringSerializer
value.serializer=org.bijou64.perf.kafka.Bijou64Serializer

# Optional: use pure Java implementation
bijou64.useJava=true
```

**consumer.properties**:

```properties
bootstrap.servers=kafka-broker:9092
group.id=my-group
key.deserializer=org.apache.kafka.common.serialization.StringDeserializer
value.deserializer=org.bijou64.perf.kafka.Bijou64Deserializer
```

### Spring Boot Integration

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class MyKafkaApp {
    public static void main(String[] args) {
        SpringApplication.run(MyKafkaApp.class, args);
    }
}

@Service
public class KafkaService {
    private final KafkaTemplate<String, Long> kafkaTemplate;

    public KafkaService(KafkaTemplate<String, Long> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String topic, Long value) {
        kafkaTemplate.send(topic, value);
    }

    @KafkaListener(topics = "my-topic")
    public void consume(Long value) {
        System.out.println("Received: " + value);
    }
}
```

**application.properties**:

```properties
spring.kafka.bootstrap-servers=localhost:9092

# Producer
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.bijou64.perf.kafka.Bijou64Serializer

# Consumer
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.bijou64.perf.kafka.Bijou64Deserializer
spring.kafka.consumer.group-id=my-consumer-group
```

## Verifying Compression

Check that Bijou64 is actually compressing messages:

```bash
# Start Kafka console consumer with hex output
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic my-topic \
  --from-beginning \
  --formatter kafka.tools.DefaultMessageFormatter \
  --property print.key=true \
  --property print.value=true \
  --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
  --property value.deserializer=org.bijou64.perf.kafka.Bijou64Deserializer

# Look for compact byte sequences (3.7 bytes average)
```

## Performance Comparison

To compare performance with standard Long serialization:

```bash
cd perf/kafka

# Build benchmarks
mvn clean package

# Run Long serializer (baseline)
./scripts/run-producer.sh --mode long --count 100000 --topic bijou64-benchmark-topic --bootstrap-server localhost:9092

# Run Bijou64 serializer
./scripts/run-producer.sh --mode bijou --count 100000 --topic bijou64-benchmark-topic --bootstrap-server localhost:9092

# View results
cat logs/results-*.csv | column -t -s','
```

## Troubleshooting

### "ClassNotFoundException: org.bijou64.perf.kafka.Bijou64Serializer"

**Solution**: Ensure JAR is in classpath:

```bash
# For Kafka broker
cp bijou64-kafka-serializers-0.1.0.jar $KAFKA_HOME/libs/

# For Java client
export CLASSPATH=".:bijou64-kafka-serializers-0.1.0.jar"
```

### "Native library not found, falling back to Java"

**Solution**: This is normal and expected. The library will use pure Java implementation:

```properties
# Optionally, force Java implementation
bijou64.useJava=true
```

### Deserialization fails with "Invalid bijou64 tag byte"

**Solution**: Data wasn't serialized with Bijou64. Check consumer is reading from correct topic and both producer/consumer are configured properly.

### Performance not improving

**Solutions**:

1. Check average payload sizes (should be 3.7 bytes vs 8.0 bytes)
2. Verify serializer is actually being used (add logging)
3. Ensure batch sizes are large enough for compression to matter
4. Run benchmarks to get baseline metrics

## Next Steps

- Read [Kafka Serializers Guide](perf/kafka/README.md) for advanced configuration
- Check [Performance Report](PERFORMANCE.md) for detailed metrics
- Explore [DEPLOYMENT Guide](DEPLOYMENT.md) for production setup
- Review [Contributing Guide](CONTRIBUTING.md) if extending functionality

## Common Patterns

### Measuring Compression

```java
// Rough measurement in your application
long start = System.nanoTime();
byte[] encoded = Bijou64.encode(myLongValue);
long elapsed = System.nanoTime() - start;

System.out.printf("Encoded %d bytes in %.2f µs%n",
    encoded.length, elapsed / 1000.0);
```

### Graceful Fallback

```java
// Use Bijou64 if available, fall back to Long otherwise
String serializer = NATIVE_AVAILABLE ?
    "org.bijou64.perf.kafka.Bijou64Serializer" :
    "org.apache.kafka.common.serialization.LongSerializer";
props.put("value.serializer", serializer);
```

### Monitoring Topic Size

```bash
# Estimate space saved
kafka-log-dirs.sh --bootstrap-server localhost:9092 --describe | grep size
```

## Getting Help

- **Documentation**: See [README.md](README.md)
- **Issues**: Open GitHub issue with Kafka version and environment
- **Performance**: Include benchmark results from your environment
- **Security**: See [SECURITY.md](SECURITY.md)

---

That's it! You're now using Bijou64 to compress Long values in Kafka. Enjoy the reduced payload sizes! 🚀
