package org.bijou64;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class Bijou64JmhBenchmark {
    private static final int BATCH = 4096;
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

    @Param({"sequential", "uniform", "boundary"})
    private String distribution;

    private long[] values;
    private byte[][] encoded;

    @Setup
    public void setup() {
        values = generateValues(distribution);
        encoded = new byte[values.length][];
        for (int i = 0; i < values.length; i++) {
            encoded[i] = Bijou64.encodeJava(values[i]);
        }
    }

    @Benchmark
    public byte[] encodeJava() {
        byte[] last = null;
        for (long value : values) {
            last = Bijou64.encodeJava(value);
        }
        return last;
    }

    @Benchmark
    public byte[] encodeNative() {
        byte[] last = null;
        for (long value : values) {
            last = Bijou64.encode(value);
        }
        return last;
    }

    @Benchmark
    public void decodeJava(Blackhole blackhole) {
        for (byte[] bytes : encoded) {
            blackhole.consume(Bijou64.decodeJava(bytes));
        }
    }

    @Benchmark
    public void decodeNative(Blackhole blackhole) {
        for (byte[] bytes : encoded) {
            blackhole.consume(Bijou64.decode(bytes));
        }
    }

    @Benchmark
    public long roundTripJava(Blackhole blackhole) {
        long last = 0;
        for (long value : values) {
            last = Bijou64.decodeJava(Bijou64.encodeJava(value));
            blackhole.consume(last);
        }
        return last;
    }

    @Benchmark
    public long roundTripNative(Blackhole blackhole) {
        long last = 0;
        for (long value : values) {
            last = Bijou64.decode(Bijou64.encode(value));
            blackhole.consume(last);
        }
        return last;
    }

    private static long[] generateValues(String distributionName) {
        long[] batch = new long[BATCH];
        switch (distributionName) {
            case "sequential" -> {
                for (int i = 0; i < BATCH; i++) {
                    batch[i] = i + 1L;
                }
            }
            case "uniform" -> {
                Random random = new Random(UNIFORM_SEED);
                for (int i = 0; i < BATCH; i++) {
                    batch[i] = random.nextLong() >>> 1;
                }
            }
            case "boundary" -> {
                for (int i = 0; i < BATCH; i++) {
                    batch[i] = BOUNDARY_VALUES[i % BOUNDARY_VALUES.length];
                }
            }
            default -> throw new IllegalArgumentException("Unknown distribution: " + distributionName);
        }
        return batch;
    }
}
