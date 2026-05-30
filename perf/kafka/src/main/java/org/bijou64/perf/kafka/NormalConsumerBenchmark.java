package org.bijou64.perf.kafka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NormalConsumerBenchmark {
    public static void main(String[] args) {
        String[] effectiveArgs = ensureMode(args, "long");
        ConsumerBenchmark.main(effectiveArgs);
    }

    private static String[] ensureMode(String[] args, String mode) {
        for (String arg : args) {
            if ("--mode".equals(arg)) {
                return args;
            }
        }
        List<String> result = new ArrayList<>(Arrays.asList(args));
        result.add("--mode");
        result.add(mode);
        return result.toArray(new String[0]);
    }
}
