package org.bijou64.perf.kafka;

import org.bijou64.Bijou64;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConsumerBenchmark {
    public static void main(String[] args) {
        Map<String, String> config = parseArgs(args);

        String bootstrapServers = config.getOrDefault("bootstrap-server", "localhost:9092");
        String topic = config.getOrDefault("topic", "bijou64-benchmark-topic");
        String groupId = config.getOrDefault("group-id", "bijou64-benchmark-group");
        String mode = config.getOrDefault("mode", "bijou");
        long count = Long.parseLong(config.getOrDefault("count", "100000"));

        String deserializerClass;
        boolean useJavaBijou = false;
        if ("bijou".equalsIgnoreCase(mode)) {
            deserializerClass = Bijou64Deserializer.class.getName();
        } else if ("bijou-java".equalsIgnoreCase(mode)) {
            deserializerClass = Bijou64Deserializer.class.getName();
            useJavaBijou = true;
        } else if ("long".equalsIgnoreCase(mode)) {
            deserializerClass = LongDeserializer.class.getName();
        } else {
            printUsageAndExit("Invalid mode: " + mode);
            return;
        }

        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", groupId);
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", deserializerClass);
        if (useJavaBijou) {
            props.put("bijou64.useJava", "true");
        }
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "false");
        props.put("max.poll.records", "500");

        System.out.println("Consumer benchmark starting with mode=" + mode + ", topic=" + topic + ", count=" + count);
        try (KafkaConsumer<String, Long> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));
            long start = System.nanoTime();
            long received = 0;

            while (received < count) {
                ConsumerRecords<String, Long> records = consumer.poll(Duration.ofSeconds(1));
                received += records.count();
                if (records.isEmpty()) {
                    System.out.printf("No records in this poll, consumed %d/%d so far...%n", received, count);
                }
            }

            long elapsedNanos = System.nanoTime() - start;
            double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
            double rate = received / elapsedSeconds;

            System.out.printf("Finished consuming %,d records in %.3f seconds.%n", received, elapsedSeconds);
            System.out.printf("Consumer rate: %,.0f records/sec%n", rate);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> config = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--bootstrap-server" -> config.put("bootstrap-server", requireArg(args, ++i, arg));
                case "--topic" -> config.put("topic", requireArg(args, ++i, arg));
                case "--group-id" -> config.put("group-id", requireArg(args, ++i, arg));
                case "--mode" -> config.put("mode", requireArg(args, ++i, arg));
                case "--count" -> config.put("count", requireArg(args, ++i, arg));
                default -> printUsageAndExit("Unknown argument: " + arg);
            }
        }
        return config;
    }

    private static String requireArg(String[] args, int index, String option) {
        if (index >= args.length) {
            printUsageAndExit("Missing value for " + option);
        }
        return args[index];
    }

    private static void printUsageAndExit(String message) {
        System.err.println(message);
        System.err.println(
                "Usage: java org.bijou64.perf.kafka.ConsumerBenchmark --mode [bijou|bijou-java|long] --topic <topic> --bootstrap-server <host:port> --group-id <group> --count <n>");
        System.exit(1);
    }
}
