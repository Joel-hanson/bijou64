package org.bijou64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class Bijou64Test {
    @Test
    void versionMatches() {
        Bijou64 b = new Bijou64();
        assertEquals("0.1.0", b.getVersion());
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
}
