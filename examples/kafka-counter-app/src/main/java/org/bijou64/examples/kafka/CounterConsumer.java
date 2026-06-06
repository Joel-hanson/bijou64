package org.bijou64.examples.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.bijou64.perf.kafka.Bijou64Deserializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

/**
 * Reads counter values from Kafka using {@link Bijou64Deserializer}.
 *
 * <p>Usage:
 * <pre>{@code
 * mvn exec:java -Dexec.mainClass=org.bijou64.examples.kafka.CounterConsumer
 * }</pre>
 */
public final class CounterConsumer {
    private static final String DEFAULT_BOOTSTRAP = "localhost:9092";
    private static final String DEFAULT_TOPIC = "bijou64-example-counters";
    private static final String DEFAULT_GROUP = "bijou64-example-counter-group";

    private CounterConsumer() {
    }

    public static void main(String[] args) {
        String bootstrap = arg(args, 0, DEFAULT_BOOTSTRAP);
        String topic = arg(args, 1, DEFAULT_TOPIC);
        int maxMessages = Integer.parseInt(arg(args, 2, "10"));

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, DEFAULT_GROUP);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, Bijou64Deserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        System.out.printf(
                "Consuming up to %,d messages from topic '%s' at %s%n",
                maxMessages,
                topic,
                bootstrap);

        int received = 0;
        try (KafkaConsumer<String, Long> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));

            while (received < maxMessages) {
                ConsumerRecords<String, Long> records = consumer.poll(Duration.ofSeconds(5));
                if (records.isEmpty()) {
                    System.out.println("No more records.");
                    break;
                }

                for (ConsumerRecord<String, Long> record : records) {
                    received++;
                    System.out.printf(
                            "partition=%d offset=%d key=%s value=%d%n",
                            record.partition(),
                            record.offset(),
                            record.key(),
                            record.value());

                    if (received >= maxMessages) {
                        break;
                    }
                }
            }
        }

        System.out.printf("Received %,d message(s).%n", received);
    }

    private static String arg(String[] args, int index, String defaultValue) {
        return args.length > index ? args[index] : defaultValue;
    }
}
