package org.bijou64;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Minimal Bijou64 library entry point. Supports both native JNI bindings and
 * a pure-Java implementation for performance comparison without native code.
 */
public class Bijou64 {
    Bijou64() {
        // no-op
    }

    public static final String VERSION = "0.1.0";

    private static final int TAG_THRESHOLD = 248;
    private static final int NUM_TIERS = 8;
    private static final long[] OFFSETS = buildOffsets();
    private static final boolean NATIVE_AVAILABLE = loadNativeLibrary();

    public String getVersion() {
        return VERSION;
    }

    public static boolean isNativeAvailable() {
        return NATIVE_AVAILABLE;
    }

    public static byte[] encode(long value) {
        return NATIVE_AVAILABLE ? encodeNative(value) : encodeJava(value);
    }

    public static long decode(byte[] bytes) {
        return NATIVE_AVAILABLE ? decodeNative(bytes) : decodeJava(bytes);
    }

    public static byte[] encodeJava(long value) {
        long unsignedLongValue = value;
        if (Long.compareUnsigned(unsignedLongValue, OFFSETS[1]) < 0) {
            return new byte[] { (byte) unsignedLongValue };
        }

        int tier = 1;
        while (tier < NUM_TIERS && Long.compareUnsigned(unsignedLongValue, OFFSETS[tier + 1]) >= 0) {
            tier++;
        }

        byte tag = (byte) (247 + tier);
        long payload = unsignedLongValue - OFFSETS[tier];
        long shifted = payload << ((8 - tier) * 8);

        byte[] encoded = new byte[tier + 1];
        encoded[0] = tag;
        for (int i = 0; i < tier; i++) {
            encoded[i + 1] = (byte) (shifted >>> (56 - 8 * i));
        }
        return encoded;
    }

    public static long decodeJava(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length == 0) {
            throw new IllegalArgumentException("bijou64 input is empty");
        }

        int tag = bytes[0] & 0xFF;
        if (tag < TAG_THRESHOLD) {
            return tag;
        }

        return switch (tag) {
            case 0xF8 -> OFFSETS[1] + readUnsignedLong(bytes, 1, 1);
            case 0xF9 -> OFFSETS[2] + readUnsignedLong(bytes, 1, 2);
            case 0xFA -> OFFSETS[3] + readUnsignedLong(bytes, 1, 3);
            case 0xFB -> OFFSETS[4] + readUnsignedLong(bytes, 1, 4);
            case 0xFC -> OFFSETS[5] + readUnsignedLong(bytes, 1, 5);
            case 0xFD -> OFFSETS[6] + readUnsignedLong(bytes, 1, 6);
            case 0xFE -> OFFSETS[7] + readUnsignedLong(bytes, 1, 7);
            case 0xFF -> {
                long payload = readUnsignedLong(bytes, 1, 8);
                long maxPayloadWithoutOverflow = ~OFFSETS[8];
                if (Long.compareUnsigned(payload, maxPayloadWithoutOverflow) > 0) {
                    throw new IllegalArgumentException("bijou64 decode overflow");
                }
                yield payload + OFFSETS[8];
            }
            default -> throw new IllegalArgumentException("Invalid bijou64 tag byte: " + tag);
        };
    }

    private static long readUnsignedLong(byte[] bytes, int offset, int length) {
        ensureLength(bytes, offset + length);
        long result = 0;
        for (int i = 0; i < length; i++) {
            result = (result << 8) | (bytes[offset + i] & 0xFFL);
        }
        return result;
    }

    private static void ensureLength(byte[] bytes, int requiredLength) {
        if (bytes.length < requiredLength) {
            throw new IllegalArgumentException("bijou64 input too short");
        }
    }

    private static long[] buildOffsets() {
        long[] offsets = new long[NUM_TIERS + 1];
        offsets[0] = 0;
        offsets[1] = TAG_THRESHOLD;
        for (int tier = 2; tier <= NUM_TIERS; tier++) {
            offsets[tier] = offsets[tier - 1] + (1L << ((tier - 1) * 8));
        }
        return offsets;
    }

    private static boolean loadNativeLibrary() {
        String libName = "bijou64_jni";
        try {
            System.loadLibrary(libName);
            return true;
        } catch (UnsatisfiedLinkError ignored) {
        }

        String mappedName = System.mapLibraryName(libName);
        Path fallback = Path.of("native", "target", "release", mappedName).toAbsolutePath();
        try {
            System.load(fallback.toString());
            return true;
        } catch (UnsatisfiedLinkError ignored) {
        }
        return false;
    }

    private static native byte[] encodeNative(long value);

    private static native long decodeNative(byte[] bytes);
}
