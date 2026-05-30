# Kafka Performance Report

## Goal

Compare Kafka producer/consumer performance using:

- `Bijou64` native JNI encoding/decoding
- standard Kafka `LongSerializer` / `LongDeserializer`

## Environment

- Machine:
- OS:
- Java:
- Maven:
- Kafka version:
- `Bijou64` version: 0.1.0

## Setup

- Root repo built with `mvn -B clean install -DskipTests`
- Perf module built with `mvn -B -f perf/kafka/pom.xml package -DskipTests`
- Kafka broker running at:

## Test cases

| Test              | Serializer            | Records | Topic | Notes |
| ----------------- | --------------------- | ------- | ----- | ----- |
| Producer baseline | `LongSerializer`      |         |       |       |
| Producer bijou    | `Bijou64Serializer`   |         |       |       |
| Consumer baseline | `LongDeserializer`    |         |       |       |
| Consumer bijou    | `Bijou64Deserializer` |         |       |       |

## Results

### Producer throughput

| Mode    | Messages | Elapsed (s) | Rate (msg/s) | Average payload size | Notes |
| ------- | -------- | ----------- | ------------ | -------------------- | ----- |
| Long    |          |             |              |                      |       |
| Bijou64 |          |             |              |                      |       |

### Consumer throughput

| Mode    | Messages | Elapsed (s) | Rate (msg/s) | Notes |
| ------- | -------- | ----------- | ------------ | ----- |
| Long    |          |             |              |       |
| Bijou64 |          |             |              |       |

## Observations

- Payload size comparison
- JNI overhead vs serialization cost
- Broker throughput vs client-side CPU
- Any anomalies or unexpected behavior

## Conclusions

- Summary of whether `Bijou64` improved end-to-end Kafka performance
- Recommended next experiments
