package org.bijou64;

import java.util.Arrays;

public class Main {
    Main() {
        // no-op
    }

    public static void main(String[] args) {
        Bijou64 b = new Bijou64();
        System.out.println("Bijou64 version: " + b.getVersion());

        byte[] encoded = Bijou64.encode(300);
        System.out.println("Encoded 300 -> " + Arrays.toString(encoded));

        long decoded = Bijou64.decode(encoded);
        System.out.println("Decoded bytes -> " + decoded);
    }
}
