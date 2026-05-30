package org.bijou64.perf.kafka;

import org.apache.kafka.common.serialization.Serializer;
import org.bijou64.Bijou64;

import java.util.Map;

public class BijouNumberSerializer implements Serializer<Number> {
    private boolean useJava = false;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        if (configs == null) return;
        Object configValue = configs.get("bijou64.useJava");
        if (configValue instanceof Boolean) {
            useJava = (Boolean) configValue;
        } else if (configValue instanceof String) {
            useJava = Boolean.parseBoolean((String) configValue);
        }
    }

    @Override
    public byte[] serialize(String topic, Number data) {
        if (data == null) return null;
        long v = data.longValue();
        return useJava ? Bijou64.encodeJava(v) : Bijou64.encode(v);
    }

    @Override
    public void close() {
        // no-op
    }
}
