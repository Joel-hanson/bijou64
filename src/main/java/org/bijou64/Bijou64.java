package org.bijou64;

import java.nio.file.Path;

/**
 * Minimal Bijou64 library entry point with a native Rust JNI implementation.
 */
public class Bijou64 {
    public static final String VERSION = "0.1.0";

    static {
        loadNativeLibrary();
    }

    public String getVersion() {
        return VERSION;
    }

    public static byte[] encode(long value) {
        return encodeNative(value);
    }

    public static long decode(byte[] bytes) {
        return decodeNative(bytes);
    }

    private static native byte[] encodeNative(long value);

    private static native long decodeNative(byte[] bytes);

    private static void loadNativeLibrary() {
        String libName = "bijou64_jni";
        try {
            System.loadLibrary(libName);
            return;
        } catch (UnsatisfiedLinkError ignored) {
        }

        String mappedName = System.mapLibraryName(libName);
        Path fallback = Path.of("native", "target", "release", mappedName).toAbsolutePath();
        try {
            System.load(fallback.toString());
        } catch (UnsatisfiedLinkError error) {
            throw new ExceptionInInitializerError(
                    "Unable to load native library '" + libName + "'. "
                            + "Build the Rust library with './build-native.sh' and run from the project root. "
                            + error.getMessage());
        }
    }
}
