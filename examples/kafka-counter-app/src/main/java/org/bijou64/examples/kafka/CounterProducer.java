package org.bijou64.examples.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bijou64.Bijou64;
import org.bijou64.perf.kafka.Bijou64Serializer;

import java.util.Properties;

/**
 * Sends sequential counter values to Kafka using {@link Bijou64Serializer}.
 *
 * <p>Usage:
 * <pre>{@code
 * mvn exec:java -Dexec.mainClass=org.bijou64.examples.kafka.CounterProducer
 * }</pre>
 */
public final class CounterProducer {
    private static final String DEFAULT_BOOTSTRAP = "localhost:9092";
    private static final String DEFAULT_TOPIC = "bijou64-example-counters";

    private CounterProducer() {
    }

    public static void main(String[] args) throws Exception {
        String bootstrap = arg(args, 0, DEFAULT_BOOTSTRAP);
        String topic = arg(args, 1, DEFAULT_TOPIC);
        long count = Long.parseLong(arg(args, 2, "10"));

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Bijou64Serializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        System.out.printf(
                "Producing %,d values to topic '%s' at %s (native=%s)%n",
                count,
                topic,
                bootstrap,
                Bijou64.isNativeAvailable());

        try (KafkaProducer<String, Long> producer = new KafkaProducer<>(props)) {
            for (long value = 1; value <= count; value++) {
                byte[] encoded = Bijou64.encode(value);
                producer.send(new ProducerRecord<>(topic, "counter", value)).get();
                System.out.printf(
                        "sent value=%d encoded=%d byte(s)%n",
                        value,
                        encoded.length);
            }
            producer.flush();
        }

        System.out.println("Done.");
    }

    private static String arg(String[] args, int index, String defaultValue) {
        return args.length > index ? args[index] : defaultValue;
    }
}
