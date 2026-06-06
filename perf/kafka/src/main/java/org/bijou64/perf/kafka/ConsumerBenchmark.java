package org.bijou64.perf.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class ConsumerBenchmark {
    public static void main(String[] args) throws Exception {
        Map<String, String> config = parseArgs(args);

        String bootstrapServers = config.getOrDefault("bootstrap-server", "localhost:9092");
        String topic = config.getOrDefault("topic", "bijou64-benchmark-topic");
        String groupId = config.getOrDefault("group-id", "bijou64-benchmark-group");
        String mode = config.getOrDefault("mode", "bijou");
        long count = Long.parseLong(config.getOrDefault("count", "100000"));
        String distribution = BenchmarkValues.parseDistribution(config.getOrDefault("distribution", "sequential"));
        long warmupCount = Long.parseLong(config.getOrDefault("warmup-count", "0"));
        boolean produceFirst = Boolean.parseBoolean(config.getOrDefault("produce-first", "false"));

        String deserializerClass;
        String serializerClass;
        boolean useJavaBijou = false;
        if ("bijou".equalsIgnoreCase(mode)) {
            deserializerClass = Bijou64Deserializer.class.getName();
            serializerClass = Bijou64Serializer.class.getName();
        } else if ("bijou-java".equalsIgnoreCase(mode)) {
            deserializerClass = Bijou64Deserializer.class.getName();
            serializerClass = Bijou64Serializer.class.getName();
            useJavaBijou = true;
        } else if ("long".equalsIgnoreCase(mode)) {
            deserializerClass = LongDeserializer.class.getName();
            serializerClass = org.apache.kafka.common.serialization.LongSerializer.class.getName();
        } else {
            printUsageAndExit("Invalid mode: " + mode);
            return;
        }

        if (produceFirst) {
            groupId = groupId + "-" + UUID.randomUUID();
        }

        Properties consumerProps = buildConsumerProperties(
                bootstrapServers, groupId, deserializerClass, useJavaBijou);

        System.out.println("Consumer benchmark starting with mode=" + mode + ", distribution=" + distribution
                + ", topic=" + topic + ", count=" + count + ", warmupCount=" + warmupCount
                + ", produceFirst=" + produceFirst + ", groupId=" + groupId);

        if (produceFirst) {
            Properties producerProps = ProducerBenchmark.buildProducerProperties(
                    bootstrapServers, serializerClass, useJavaBijou, "none");
            try (KafkaProducer<String, Long> producer = new KafkaProducer<>(producerProps)) {
                long produceTotal = count + warmupCount;
                ProducerBenchmark.produceRecords(producer, topic, distribution, produceTotal);
                producer.flush();
                System.out.printf("Produced %,d records before consuming.%n", produceTotal);
            }
        }

        try (KafkaConsumer<String, Long> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(topic));

            if (warmupCount > 0) {
                consumeRecords(consumer, warmupCount);
                System.out.printf("Warmup complete: %,d records.%n", warmupCount);
            }

            long start = System.nanoTime();
            long received = consumeRecords(consumer, count);
            long elapsedNanos = System.nanoTime() - start;
            double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
            double rate = received / elapsedSeconds;

            System.out.printf("Finished consuming %,d records in %.3f seconds.%n", received, elapsedSeconds);
            System.out.printf("Consumer rate: %,.0f records/sec%n", rate);
        }
    }

    private static long consumeRecords(KafkaConsumer<String, Long> consumer, long count) {
        long received = 0;
        while (received < count) {
            ConsumerRecords<String, Long> records = consumer.poll(Duration.ofSeconds(1));
            received += records.count();
            if (records.isEmpty()) {
                System.out.printf("No records in this poll, consumed %d/%d so far...%n", received, count);
            }
        }
        return received;
    }

    static Properties buildConsumerProperties(
            String bootstrapServers, String groupId, String deserializerClass, boolean useJavaBijou) {
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
        return props;
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
                case "--distribution" -> config.put("distribution", requireArg(args, ++i, arg));
                case "--warmup-count" -> config.put("warmup-count", requireArg(args, ++i, arg));
                case "--produce-first" -> config.put("produce-first", requireArg(args, ++i, arg));
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
                "Usage: java org.bijou64.perf.kafka.ConsumerBenchmark --mode [bijou|bijou-java|long] "
                        + "--topic <topic> --bootstrap-server <host:port> --group-id <group> --count <n> "
                        + "[--distribution sequential|uniform|boundary] [--warmup-count <n>] "
                        + "[--produce-first true|false]");
        System.exit(1);
    }
}
