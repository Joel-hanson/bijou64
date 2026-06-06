package org.bijou64;

import java.nio.file.Path;
import java.util.Objects;

/**
 * bijou64 (BIJective Offset U64) - A variable-length encoding for unsigned 64-bit integers.
 *
 * <p>bijou64 encodes u64 values into 1–9 bytes using tag-byte framing with per-tier offsets
 * to achieve <strong>structural canonicality</strong> — each value has exactly one encoding,
 * and each encoding has exactly one value. This is bijective numeration applied to VARU64's
 * tag-byte framing.
 *
 * <h2>Encoding Format</h2>
 * <p>The first byte determines the encoding:
 * <ul>
 *   <li><strong>0x00–0xF7 (0–247):</strong> The byte <em>is</em> the value. One byte total.</li>
 *   <li><strong>0xF8–0xFF (248–255):</strong> Length tag. Additional bytes = tag - 247.
 *       Payload is big-endian {@code value - OFFSET[tier]}.</li>
 * </ul>
 *
 * <h2>Key Properties</h2>
 * <ul>
 *   <li><strong>Canonical by construction:</strong> No runtime check needed to reject overlong encodings</li>
 *   <li><strong>Big-endian byte order:</strong> Lexicographic byte comparison equals numeric comparison</li>
 *   <li><strong>Length from first byte:</strong> O(1) skipping and buffer pre-allocation</li>
 *   <li><strong>Compact for small values:</strong> Values 0–247 encode as a single byte</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Encode the value 300
 * byte[] encoded = Bijou64.encode(300);
 * // Result: [0xF8, 0x34] (tag 248, payload 300 - 248 = 52)
 *
 * // Decode back to 300
 * long value = Bijou64.decode(encoded);
 * }</pre>
 *
 * <p>This implementation supports both native JNI bindings (for performance) and a pure-Java
 * fallback implementation. The native library is optional; if not available, the pure-Java
 * implementation is used automatically.
 *
 * @see <a href="https://github.com/inkandswitch/bijou64/blob/main/SPEC.md">bijou64 Specification</a>
 */
public class Bijou64 {
    Bijou64() {
        // no-op
    }

    public static final String VERSION = "0.2.0";

    /**
     * Tag byte threshold: values below this (0–247) are encoded as a single byte.
     * Values 248 and above use multi-byte encoding with a tag byte.
     */
    private static final int TAG_THRESHOLD = 248;
    
    /**
     * Number of multi-byte tiers (tags 248–255 = tiers 1–8).
     */
    private static final int NUM_TIERS = 8;
    
    /**
     * Per-tier offsets. OFFSETS[t] is the first value that requires tier t.
     * Each tier's offset is the first value not representable by the previous tier.
     *
     * <p>Recurrence: {@code offset(n) = offset(n-1) + 256^(n-1)} for n >= 2,
     * with {@code offset(1) = 248} and {@code offset(0) = 0}.
     */
    private static final long[] OFFSETS = buildOffsets();
    
    /**
     * Whether the native JNI library is available.
     */
    private static final boolean NATIVE_AVAILABLE = loadNativeLibrary();

    public String getVersion() {
        return VERSION;
    }

    /**
     * Returns whether the native JNI library is available.
     * If false, all encode/decode operations use the pure-Java implementation.
     */
    public static boolean isNativeAvailable() {
        return NATIVE_AVAILABLE;
    }

    /**
     * Encodes a u64 value as bijou64. Uses native implementation if available,
     * otherwise falls back to pure-Java.
     *
     * @param value the unsigned 64-bit value to encode (treated as unsigned)
     * @return the encoded bytes (1–9 bytes)
     */
    public static byte[] encode(long value) {
        return NATIVE_AVAILABLE ? encodeNative(value) : encodeJava(value);
    }

    /**
     * Decodes a bijou64-encoded value. Uses native implementation if available,
     * otherwise falls back to pure-Java.
     *
     * @param bytes the encoded bytes
     * @return the decoded unsigned 64-bit value
     * @throws IllegalArgumentException if the input is empty, too short, or overflows u64
     */
    public static long decode(byte[] bytes) {
        return NATIVE_AVAILABLE ? decodeNative(bytes) : decodeJava(bytes);
    }

    /**
     * Returns the encoded length in bytes for a u64 value without allocating.
     *
     * @param value the unsigned 64-bit value
     * @return encoded length (1–9 bytes)
     */
    public static int encodedLen(long value) {
        return encodedLenJava(value);
    }

    /**
     * Pure-Java implementation of encoded length calculation.
     *
     * @param value the unsigned 64-bit value
     * @return encoded length (1–9 bytes)
     */
    public static int encodedLenJava(long value) {
        if (Long.compareUnsigned(value, OFFSETS[1]) < 0) {
            return 1;
        }

        int tier = 1;
        while (tier < NUM_TIERS && Long.compareUnsigned(value, OFFSETS[tier + 1]) >= 0) {
            tier++;
        }
        return tier + 1;
    }

    /**
     * Pure-Java implementation of bijou64 encoding.
     *
     * <p>Encodes a u64 value into 1–9 bytes:
     * <ul>
     *   <li>Values 0–247: single byte equal to the value</li>
     *   <li>Values 248+: tag byte (247 + tier) followed by big-endian payload of (value - OFFSET[tier])</li>
     * </ul>
     *
     * @param value the unsigned 64-bit value to encode (treated as unsigned)
     * @return the encoded bytes (1–9 bytes)
     */
    public static byte[] encodeJava(long value) {
        long unsignedLongValue = value;
        
        // Tier 0: values 0–247 encode as a single byte
        if (Long.compareUnsigned(unsignedLongValue, OFFSETS[1]) < 0) {
            return new byte[] { (byte) unsignedLongValue };
        }

        // Find the tier: the smallest tier where value < OFFSETS[tier + 1]
        int tier = 1;
        while (tier < NUM_TIERS && Long.compareUnsigned(unsignedLongValue, OFFSETS[tier + 1]) >= 0) {
            tier++;
        }

        // Tag byte: 247 + tier (maps tier 1–8 to tags 0xF8–0xFF)
        byte tag = (byte) (247 + tier);
        
        // Payload: value - OFFSET[tier], shifted to occupy the high 'tier' bytes
        long payload = unsignedLongValue - OFFSETS[tier];
        long shifted = payload << ((8 - tier) * 8);

        // Build the encoded result: [tag, payload_bytes...]
        byte[] encoded = new byte[tier + 1];
        encoded[0] = tag;
        for (int i = 0; i < tier; i++) {
            encoded[i + 1] = (byte) (shifted >>> (56 - 8 * i));
        }
        return encoded;
    }

    /**
     * Pure-Java implementation of bijou64 decoding.
     *
     * <p>Decodes a bijou64-encoded value from the front of the byte array.
     * The encoding is canonical by construction: each value has exactly one encoding,
     * so no runtime check for overlong encodings is needed (except for tier 8 overflow).
     *
     * @param bytes the encoded bytes
     * @return the decoded unsigned 64-bit value
     * @throws IllegalArgumentException if the input is empty, too short, or overflows u64
     */
    public static long decodeJava(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length == 0) {
            throw new IllegalArgumentException("bijou64 input is empty");
        }

        int tag = bytes[0] & 0xFF;
        
        // Tier 0: tag < 248 means the byte is the value itself
        if (tag < TAG_THRESHOLD) {
            return tag;
        }

        // Multi-byte tiers: read big-endian payload and add tier offset
        return switch (tag) {
            case 0xF8 -> OFFSETS[1] + readUnsignedLong(bytes, 1, 1);
            case 0xF9 -> OFFSETS[2] + readUnsignedLong(bytes, 1, 2);
            case 0xFA -> OFFSETS[3] + readUnsignedLong(bytes, 1, 3);
            case 0xFB -> OFFSETS[4] + readUnsignedLong(bytes, 1, 4);
            case 0xFC -> OFFSETS[5] + readUnsignedLong(bytes, 1, 5);
            case 0xFD -> OFFSETS[6] + readUnsignedLong(bytes, 1, 6);
            case 0xFE -> OFFSETS[7] + readUnsignedLong(bytes, 1, 7);
            case 0xFF -> {
                // Tier 8: check for overflow (only tier where OFFSET + payload can exceed u64::MAX)
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

    /**
     * Reads a big-endian unsigned long from the byte array.
     *
     * @param bytes the byte array
     * @param offset the starting offset
     * @param length the number of bytes to read (1–8)
     * @return the unsigned long value
     * @throws IllegalArgumentException if the array is too short
     */
    private static long readUnsignedLong(byte[] bytes, int offset, int length) {
        ensureLength(bytes, offset + length);
        long result = 0;
        for (int i = 0; i < length; i++) {
            result = (result << 8) | (bytes[offset + i] & 0xFFL);
        }
        return result;
    }

    /**
     * Ensures the byte array has at least the required length.
     *
     * @param bytes the byte array
     * @param requiredLength the required length
     * @throws IllegalArgumentException if the array is too short
     */
    private static void ensureLength(byte[] bytes, int requiredLength) {
        if (bytes.length < requiredLength) {
            throw new IllegalArgumentException("bijou64 input too short");
        }
    }

    /**
     * Builds the per-tier offset table.
     *
     * <p>Each tier's offset is the first value not representable by the previous tier.
     * Recurrence: {@code offset(n) = offset(n-1) + 256^(n-1)} for n >= 2,
     * with {@code offset(1) = 248} and {@code offset(0) = 0}.
     *
     * @return the offset table (9 elements for tiers 0–8)
     */
    private static long[] buildOffsets() {
        long[] offsets = new long[NUM_TIERS + 1];
        offsets[0] = 0;
        offsets[1] = TAG_THRESHOLD;
        for (int tier = 2; tier <= NUM_TIERS; tier++) {
            offsets[tier] = offsets[tier - 1] + (1L << ((tier - 1) * 8));
        }
        return offsets;
    }

    /**
     * Attempts to load the native JNI library.
     *
     * <p>First tries {@code System.loadLibrary("bijou64_jni")} (standard library path),
     * then falls back to loading from {@code native/target/release/} (development build).
     *
     * @return true if the native library was loaded successfully, false otherwise
     */
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

    /**
     * Native JNI implementation of bijou64 encoding (if available).
     */
    private static native byte[] encodeNative(long value);

    /**
     * Native JNI implementation of bijou64 decoding (if available).
     */
    private static native long decodeNative(byte[] bytes);
}
