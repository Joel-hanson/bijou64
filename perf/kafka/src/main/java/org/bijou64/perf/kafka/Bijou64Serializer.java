package org.bijou64.perf.kafka;

import org.apache.kafka.common.serialization.Serializer;
import org.bijou64.Bijou64;

import java.util.Map;

public class Bijou64Serializer implements Serializer<Long> {
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
    public byte[] serialize(String topic, Long data) {
        if (data == null) {
            return null;
        }
        return useJava ? Bijou64.encodeJava(data) : Bijou64.encode(data);
    }

    @Override
    public void close() {
        // No resources to close.
    }
}
