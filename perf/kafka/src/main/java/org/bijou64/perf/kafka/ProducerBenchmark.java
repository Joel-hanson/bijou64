package org.bijou64.perf.kafka;

import org.bijou64.Bijou64;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
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

        System.out.println("Producer benchmark starting with mode=" + mode + ", compression=" + compression + ", topic="
                + topic + ", count=" + count);
        try (KafkaProducer<String, Long> producer = new KafkaProducer<>(props)) {
            long start = System.nanoTime();
            long totalBytes = 0;
            boolean usingBijou = "bijou".equalsIgnoreCase(mode) || useJavaBijou;

            for (long i = 1; i <= count; i++) {
                Long value = i;
                if (usingBijou) {
                    byte[] encoded = useJavaBijou ? Bijou64.encodeJava(value) : Bijou64.encode(value);
                    totalBytes += encoded.length;
                } else {
                    totalBytes += Long.BYTES;
                }
                producer.send(new ProducerRecord<>(topic, String.valueOf(i), value));
            }

            producer.flush();
            long elapsedNanos = System.nanoTime() - start;
            double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
            double rate = count / elapsedSeconds;
            double avgBytes = totalBytes / (double) count;

            System.out.printf("Finished sending %,d records in %.3f seconds.%n", count, elapsedSeconds);
            System.out.printf("Producer rate: %,.0f records/sec%n", rate);
            System.out.printf("Average payload size: %.1f bytes%n", avgBytes);
        }
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
                "Usage: java org.bijou64.perf.kafka.ProducerBenchmark --mode [bijou|bijou-java|long] --topic <topic> --bootstrap-server <host:port> --count <n> [--compression none|zstd|snappy|lz4]");
        System.exit(1);
    }
}
