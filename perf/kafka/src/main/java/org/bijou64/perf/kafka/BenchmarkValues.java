package org.bijou64.perf.kafka;

import java.util.Random;
import java.util.function.LongConsumer;

final class BenchmarkValues {
    private static final long UNIFORM_SEED = 0xBEEF_CAFE;

    private static final long[] BOUNDARY_VALUES = {
            0L,
            247L,
            248L,
            503L,
            504L,
            66_039L,
            66_040L,
            16_843_255L,
            16_843_256L,
            4_311_810_551L,
            4_311_810_552L,
            1_103_823_438_327L,
            1_103_823_438_328L,
            282_578_800_148_983L,
            282_578_800_148_984L,
            72_340_172_838_076_919L,
            72_340_172_838_076_920L,
            Long.MAX_VALUE
    };

    private BenchmarkValues() {
    }

    static String parseDistribution(String distribution) {
        if (distribution == null || distribution.isBlank()) {
            return "sequential";
        }
        String normalized = distribution.toLowerCase();
        if ("sequential".equals(normalized) || "uniform".equals(normalized) || "boundary".equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Invalid distribution: " + distribution);
    }

    static void forEach(String distribution, long count, LongConsumer consumer) {
        switch (parseDistribution(distribution)) {
            case "sequential" -> {
                for (long i = 1; i <= count; i++) {
                    consumer.accept(i);
                }
            }
            case "uniform" -> {
                Random random = new Random(UNIFORM_SEED);
                for (long i = 0; i < count; i++) {
                    consumer.accept(random.nextLong() >>> 1);
                }
            }
            case "boundary" -> {
                for (long i = 0; i < count; i++) {
                    consumer.accept(BOUNDARY_VALUES[(int) (i % BOUNDARY_VALUES.length)]);
                }
            }
            default -> throw new IllegalArgumentException("Invalid distribution: " + distribution);
        }
    }

    static double averagePayloadBytes(String mode, String distribution, long count) {
        long[] totalBytes = {0L};
        forEach(distribution, count, value -> {
            if ("long".equalsIgnoreCase(mode)) {
                totalBytes[0] += Long.BYTES;
            } else {
                totalBytes[0] += org.bijou64.Bijou64.encodedLen(value);
            }
        });
        return totalBytes[0] / (double) count;
    }
}
