package org.bijou64.perf.kafka;

import org.apache.kafka.common.serialization.Deserializer;
import org.bijou64.Bijou64;

import java.util.Map;

public class Bijou64Deserializer implements Deserializer<Long> {
    private boolean useJava = false;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        if (configs == null) {
            return;
        }
        Object configValue = configs.get("bijou64.useJava");
        if (configValue instanceof Boolean) {
            useJava = (Boolean) configValue;
        } else if (configValue instanceof String) {
            useJava = Boolean.parseBoolean((String) configValue);
        }
    }

    @Override
    public Long deserialize(String topic, byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        return useJava ? Bijou64.decodeJava(data) : Bijou64.decode(data);
    }

    @Override
    public void close() {
        // No resources to close.
    }
}
