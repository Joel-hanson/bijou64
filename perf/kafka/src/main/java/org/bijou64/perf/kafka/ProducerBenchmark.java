package org.bijou64.perf.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ProducerBenchmark {
    public static void main(String[] args) throws Exception {
        Map<String, String> config = parseArgs(args);

        String bootstrapServers = config.getOrDefault("bootstrap-server", "localhost:9092");
        String topic = config.getOrDefault("topic", "bijou64-benchmark-topic");
        String mode = config.getOrDefault("mode", "bijou");
        long count = Long.parseLong(config.getOrDefault("count", "100000"));
        String compression = config.getOrDefault("compression", "none");
        String distribution = BenchmarkValues.parseDistribution(config.getOrDefault("distribution", "sequential"));
        long warmupCount = Long.parseLong(config.getOrDefault("warmup-count", "0"));

        String serializerClass;
        boolean useJavaBijou = false;
        if ("bijou".equalsIgnoreCase(mode)) {
            serializerClass = Bijou64Serializer.class.getName();
        } else if ("bijou-java".equalsIgnoreCase(mode)) {
            serializerClass = Bijou64Serializer.class.getName();
            useJavaBijou = true;
        } else if ("long".equalsIgnoreCase(mode)) {
            serializerClass = LongSerializer.class.getName();
        } else {
            printUsageAndExit("Invalid mode: " + mode);
            return;
        }

        Properties props = buildProducerProperties(
                bootstrapServers, serializerClass, useJavaBijou, compression);

        double avgBytes = BenchmarkValues.averagePayloadBytes(mode, distribution, count);

        System.out.println("Producer benchmark starting with mode=" + mode + ", compression=" + compression
                + ", distribution=" + distribution + ", topic=" + topic + ", count=" + count
                + ", warmupCount=" + warmupCount);
        try (KafkaProducer<String, Long> producer = new KafkaProducer<>(props)) {
            if (warmupCount > 0) {
                produceRecords(producer, topic, distribution, warmupCount);
                producer.flush();
                System.out.printf("Warmup complete: %,d records.%n", warmupCount);
            }

            long start = System.nanoTime();
            produceRecords(producer, topic, distribution, count);
            producer.flush();
            long elapsedNanos = System.nanoTime() - start;
            double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
            double rate = count / elapsedSeconds;

            System.out.printf("Finished sending %,d records in %.3f seconds.%n", count, elapsedSeconds);
            System.out.printf("Producer rate: %,.0f records/sec%n", rate);
            System.out.printf("Average payload size: %.1f bytes%n", avgBytes);
        }
    }

    static Properties buildProducerProperties(
            String bootstrapServers, String serializerClass, boolean useJavaBijou, String compression) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", serializerClass);
        if (useJavaBijou) {
            props.put("bijou64.useJava", "true");
        }
        if (!"none".equalsIgnoreCase(compression)) {
            props.put("compression.type", compression);
        }
        props.put("acks", "1");
        props.put("linger.ms", "5");
        props.put("batch.size", "16384");
        props.put("max.in.flight.requests.per.connection", "5");
        return props;
    }

    static void produceRecords(KafkaProducer<String, Long> producer, String topic, String distribution, long count) {
        BenchmarkValues.forEach(distribution, count, value ->
                producer.send(new ProducerRecord<>(topic, String.valueOf(value), value)));
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> config = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--bootstrap-server" -> config.put("bootstrap-server", requireArg(args, ++i, arg));
                case "--topic" -> config.put("topic", requireArg(args, ++i, arg));
                case "--mode" -> config.put("mode", requireArg(args, ++i, arg));
                case "--count" -> config.put("count", requireArg(args, ++i, arg));
                case "--compression" -> config.put("compression", requireArg(args, ++i, arg));
                case "--distribution" -> config.put("distribution", requireArg(args, ++i, arg));
                case "--warmup-count" -> config.put("warmup-count", requireArg(args, ++i, arg));
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
                "Usage: java org.bijou64.perf.kafka.ProducerBenchmark --mode [bijou|bijou-java|long] "
                        + "--topic <topic> --bootstrap-server <host:port> --count <n> "
                        + "[--compression none|zstd|snappy|lz4] "
                        + "[--distribution sequential|uniform|boundary] [--warmup-count <n>]");
        System.exit(1);
    }
}
