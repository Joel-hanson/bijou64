#!/usr/bin/env bash
set -euo pipefail

# compare-benchmarks.sh
# Builds the project, runs the producer benchmark across modes, and summarizes results.
# Cleans up Kafka topic between runs for accurate benchmarking.

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
LOG_DIR="$ROOT_DIR/logs"
mkdir -p "$LOG_DIR"

ALL_MODES=(
  "long:none"
  "long:zstd"
  "long:snappy"
  "long:lz4"
  "bijou:none"
  "bijou:zstd"
  "bijou:snappy"
  "bijou:lz4"
  "bijou-java:none"
  "bijou-java:zstd"
  "bijou-java:snappy"
  "bijou-java:lz4"
)

CI_MODES=(
  "long:none"
  "long:zstd"
  "bijou:none"
  "bijou:zstd"
  "bijou-java:none"
  "bijou-java:zstd"
)

CI_MODE=false
COUNT=100000
ITERATIONS=3
TOPIC="bijou-benchmark-topic"
BOOTSTRAP="localhost:9092"
DISTRIBUTION="sequential"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --ci)
      CI_MODE=true
      shift
      ;;
    --distribution)
      DISTRIBUTION="$2"
      shift 2
      ;;
    --topic)
      TOPIC="$2"
      shift 2
      ;;
    --bootstrap-server)
      BOOTSTRAP="$2"
      shift 2
      ;;
    -*)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
    *)
      if [[ -z "${COUNT_SET:-}" ]]; then
        COUNT="$1"
        COUNT_SET=1
      elif [[ -z "${ITERATIONS_SET:-}" ]]; then
        ITERATIONS="$1"
        ITERATIONS_SET=1
      elif [[ -z "${TOPIC_SET:-}" ]]; then
        TOPIC="$1"
        TOPIC_SET=1
      else
        BOOTSTRAP="$1"
      fi
      shift
      ;;
  esac
done

if [[ "$CI_MODE" == true ]]; then
  MODES=("${CI_MODES[@]}")
else
  MODES=("${ALL_MODES[@]}")
fi

results_csv="$LOG_DIR/results-$(date +%Y%m%dT%H%M%S).csv"
echo "mode,compression,run,rate,avg_bytes,exit_code,logfile,distribution" > "$results_csv"

resolve_kafka_topics_cmd() {
  if [[ -n "${KAFKA_TOPICS_CMD:-}" ]]; then
    echo "$KAFKA_TOPICS_CMD"
    return 0
  fi

  if command -v kafka-topics.sh &> /dev/null; then
    echo "kafka-topics.sh"
    return 0
  fi

  if ! command -v docker &> /dev/null; then
    return 1
  fi

  local kafka_bin="/opt/kafka/bin/kafka-topics.sh"
  if docker ps --format '{{.Names}}' | grep -qx bijou64-kafka; then
    echo "docker exec bijou64-kafka $kafka_bin"
    return 0
  fi

  local kafka_container
  kafka_container=$(docker ps --filter "publish=9092" --format "{{.Names}}" | head -1)
  if [[ -n "$kafka_container" ]]; then
    echo "docker exec $kafka_container $kafka_bin"
    return 0
  fi

  local kafka_image="${KAFKA_IMAGE:-apache/kafka:4.3.0}"
  echo "docker run --rm --network host $kafka_image $kafka_bin"
}

reset_topic() {
  local topic=$1
  local bootstrap=$2

  echo "[bench] Resetting topic '$topic' for clean benchmark..."

  local KAFKA_CMD
  if ! KAFKA_CMD=$(resolve_kafka_topics_cmd); then
    echo "[bench] Warning: Cannot find kafka-topics command. Skipping topic reset."
    echo "[bench] Install Kafka CLI tools or ensure a Kafka broker is reachable on $bootstrap."
    return 0
  fi

  $KAFKA_CMD --bootstrap-server "$bootstrap" --delete --topic "$topic" 2>/dev/null || true
  sleep 2

  $KAFKA_CMD --bootstrap-server "$bootstrap" --create --topic "$topic" \
    --partitions 1 --replication-factor 1 \
    --config retention.ms=3600000 \
    --config segment.bytes=1073741824 2>/dev/null || {
    echo "[bench] Warning: Failed to create topic. It may already exist."
    sleep 5
  }

  echo "[bench] Topic reset complete."
}

run_once() {
  local mode=$1
  local runid=$2
  local is_warmup=$3
  IFS=':' read -r base_mode compression <<< "$mode"
  local logfile="$LOG_DIR/producer-${base_mode}-${compression}-run${runid}-$(date +%s).log"

  if [[ "$is_warmup" == "true" ]]; then
    echo "[bench] Warmup mode=$base_mode compression=$compression -> logfile=$logfile"
  else
    echo "[bench] Mode=$base_mode compression=$compression run=$runid -> logfile=$logfile"
  fi

  reset_topic "$TOPIC" "$BOOTSTRAP"

  mvn -B -DskipTests -Dgpg.skip=true -f "$ROOT_DIR/../.." install >/dev/null 2>&1

  local compression_args=()
  if [[ "$compression" != "none" ]]; then
    compression_args=(--compression "$compression")
  fi

  if "$ROOT_DIR/scripts/run-producer.sh" \
      --mode "$base_mode" \
      --topic "$TOPIC" \
      --bootstrap-server "$BOOTSTRAP" \
      --count "$COUNT" \
      --distribution "$DISTRIBUTION" \
      "${compression_args[@]}" >"$logfile" 2>&1; then
    exit_code=0
  else
    exit_code=$?
  fi

  if [[ "$is_warmup" == "true" ]]; then
    return 0
  fi

  rate_line=$(grep -E "Producer rate:" "$logfile" || true)
  avg_line=$(grep -E "Average payload size:" "$logfile" || true)

  rate=0
  avg_bytes=0
  if [[ -n "$rate_line" ]]; then
    rate=$(echo "$rate_line" | sed -n 's/.*Producer rate:[[:space:]]*\([^ ]*\) records\/sec.*/\1/p' | tr -d ',')
  fi
  if [[ -n "$avg_line" ]]; then
    avg_bytes=$(echo "$avg_line" | sed -n 's/.*Average payload size:[[:space:]]*\([0-9.]*\) bytes.*/\1/p')
  fi

  echo "$base_mode,$compression,$runid,$rate,$avg_bytes,$exit_code,$logfile,$DISTRIBUTION" >> "$results_csv"
}

for mode in "${MODES[@]}"; do
  run_once "$mode" 0 true
  for ((i=1; i<=ITERATIONS; i++)); do
    run_once "$mode" "$i" false
  done
done

median_of() {
  awk -v mode="$1" -v compression="$2" -F, '
    $1 == mode && $2 == compression && $4 != "" {
      rates[++n] = $4 + 0
    }
    END {
      if (n == 0) {
        print 0
        exit
      }
      asort(rates)
      if (n % 2 == 1) {
        print rates[(n + 1) / 2]
      } else {
        print (rates[n / 2] + rates[n / 2 + 1]) / 2
      }
    }
  ' "$results_csv"
}

echo
printf "%-10s %-12s %-6s %-12s %-12s\n" "Mode" "Compression" "Runs" "MedianRate" "AvgBytes"
for mode in "${MODES[@]}"; do
  IFS=':' read -r base_mode compression <<< "$mode"
  median=$(median_of "$base_mode" "$compression")
  rep_avg=$(awk -F, -v m="$base_mode" -v c="$compression" '$1==m && $2==c && $5 != "" { print $5; exit }' "$results_csv")
  printf "%-10s %-12s %-6d %-12s %-12s\n" "$base_mode" "$compression" "$ITERATIONS" "${median:-0}" "${rep_avg:-0}"
done

echo
echo "Detailed CSV: $results_csv"
echo "Distribution: $DISTRIBUTION"

exit 0
