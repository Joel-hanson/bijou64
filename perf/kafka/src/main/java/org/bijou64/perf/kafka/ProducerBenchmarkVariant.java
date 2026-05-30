package org.bijou64.perf.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.*;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ProducerBenchmarkVariant {
    public static void main(String[] args) throws Exception {
        Map<String, String> config = parseArgs(args);

        String bootstrapServers = config.getOrDefault("bootstrap-server", "localhost:9092");
        String topic = config.getOrDefault("topic", "bijou64-benchmark-topic");
        String mode = config.getOrDefault("mode", "bijou-java");
        String type = config.getOrDefault("type", "long");
        long count = Long.parseLong(config.getOrDefault("count", "100000"));

        String serializerClass;
        boolean usingBijou = false;

        switch (type) {
            case "long", "int" -> {
                if ("bijou".equalsIgnoreCase(mode) || "bijou-java".equalsIgnoreCase(mode)) {
                    serializerClass = BijouNumberSerializer.class.getName();
                    usingBijou = true;
                } else {
                    serializerClass = LongSerializer.class.getName();
                }
            }
            case "string" -> serializerClass = StringSerializer.class.getName();
            case "bytes" -> serializerClass = ByteArraySerializer.class.getName();
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        }

        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", serializerClass);
        if ("bijou-java".equalsIgnoreCase(mode)) props.put("bijou64.useJava", "true");
        if ("bijou".equalsIgnoreCase(mode)) props.put("bijou64.useJava", "false");

        System.out.println("Producer variant starting type=" + type + " mode=" + mode + " count=" + count);
        try (KafkaProducer<String, Object> producer = new KafkaProducer<>(props)) {
            long start = System.nanoTime();
            long totalBytes = 0;

            // local serializer for accurate byte counts
            org.apache.kafka.common.serialization.Serializer<Object> localSerializer = (org.apache.kafka.common.serialization.Serializer<Object>) Class.forName(serializerClass).getDeclaredConstructor().newInstance();
            Map<String, Object> cfg = new HashMap<>();
            if (props.containsKey("bijou64.useJava")) cfg.put("bijou64.useJava", props.get("bijou64.useJava"));
            localSerializer.configure(cfg, false);

            for (long i = 1; i <= count; i++) {
                Object value;
                switch (type) {
                    case "long" -> value = Long.valueOf(i);
                    case "int" -> value = Integer.valueOf((int) i);
                    case "string" -> value = String.valueOf(i);
                    case "bytes" -> value = ByteBuffer.allocate(8).putLong(i).array();
                    default -> throw new IllegalArgumentException("Unsupported type: " + type);
                }

                if (usingBijou) {
                    byte[] encoded = localSerializer.serialize(topic, value);
                    totalBytes += (encoded == null ? 0 : encoded.length);
                } else if (value instanceof byte[]) {
                    totalBytes += ((byte[]) value).length;
                } else if (value instanceof String) {
                    totalBytes += ((String) value).getBytes().length;
                } else if (value instanceof Integer) {
                    totalBytes += Integer.BYTES;
                } else if (value instanceof Long) {
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
                case "--type" -> config.put("type", requireArg(args, ++i, arg));
                case "--count" -> config.put("count", requireArg(args, ++i, arg));
                default -> printUsageAndExit("Unknown argument: " + arg);
            }
        }
        return config;
    }

    private static String requireArg(String[] args, int index, String option) {
        if (index >= args.length) printUsageAndExit("Missing value for " + option);
        return args[index];
    }

    private static void printUsageAndExit(String message) {
        System.err.println(message);
        System.err.println("Usage: java org.bijou64.perf.kafka.ProducerBenchmarkVariant --type [long|int|string|bytes] --mode [long|bijou|bijou-java] --topic <topic> --bootstrap-server <host:port> --count <n>");
        System.exit(1);
    }
}
