package org.bijou64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class Bijou64Test {
    @Test
    void versionMatches() {
        Bijou64 b = new Bijou64();
        assertEquals("0.2.0", b.getVersion());
    }

    @Test
    void javaEncodeDecodeWorks() {
        byte[] encoded = Bijou64.encodeJava(300);
        assertArrayEquals(new byte[] { (byte) 0xF8, 0x34 }, encoded);
        assertEquals(300, Bijou64.decodeJava(encoded));

        assertArrayEquals(new byte[] { 0x00 }, Bijou64.encodeJava(0));
        assertEquals(247, Bijou64.decodeJava(new byte[] { (byte) 0xF7 }));
    }

    @Test
    void nativeEncodeDecodeWorks() {
        String libraryName = System.mapLibraryName("bijou64_jni");
        Path nativePath = Path.of("native", "target", "release", libraryName);
        assumeTrue(nativePath.toFile().exists(), "Native library must be built before running native tests.");

        byte[] encoded = Bijou64.encode(300);
        assertArrayEquals(new byte[] { (byte) 0xF8, 0x34 }, encoded);
        assertEquals(300, Bijou64.decode(encoded));
    }

    @Test
    void javaAndNativeMatchWhenNativeAvailable() {
        String libraryName = System.mapLibraryName("bijou64_jni");
        Path nativePath = Path.of("native", "target", "release", libraryName);
        assumeTrue(nativePath.toFile().exists(), "Native library must be built before running native tests.");

        byte[] javaBytes = Bijou64.encodeJava(300);
        byte[] nativeBytes = Bijou64.encode(300);
        assertArrayEquals(javaBytes, nativeBytes);
        assertEquals(Bijou64.decodeJava(nativeBytes), Bijou64.decode(nativeBytes));
    }

    @Test
    void javaRoundTripsTierBoundaries() {
        long[] values = new long[] { 0L, 247L, 248L, 503L, 504L, 66039L, Long.MAX_VALUE };
        for (long value : values) {
            assertEquals(value, Bijou64.decodeJava(Bijou64.encodeJava(value)));
        }
    }

    @Test
    void encodedLenJavaMatchesEncodeLength() {
        long[] values = new long[] {
                0L, 247L, 248L, 503L, 504L, 66039L, 16843256L, 4311810551L, Long.MAX_VALUE
        };
        for (long value : values) {
            assertEquals(Bijou64.encodeJava(value).length, Bijou64.encodedLenJava(value));
            assertEquals(Bijou64.encodedLenJava(value), Bijou64.encodedLen(value));
        }
    }

    @Test
    void decodeJavaRejectsOverflowForFfTag() {
        byte[] overflowing = new byte[] {
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF
        };
        assertThrows(IllegalArgumentException.class, () -> Bijou64.decodeJava(overflowing));
    }
}
