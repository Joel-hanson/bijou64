#!/usr/bin/env bash
set -euo pipefail

# compare-benchmarks.sh
# Builds the project, runs the producer benchmark across modes, and summarizes results.
# Cleans up Kafka topic between runs for accurate benchmarking.

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
LOG_DIR="$ROOT_DIR/logs"
mkdir -p "$LOG_DIR"

MODES=(
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
COUNT=${1:-100000}
ITERATIONS=${2:-3}
TOPIC=${3:-bijou-benchmark-topic}
BOOTSTRAP=${4:-localhost:9092}

results_csv="$LOG_DIR/results-$(date +%Y%m%dT%H%M%S).csv"
echo "mode,compression,run,rate,avg_bytes,exit_code,logfile" > "$results_csv"

# Function to delete and recreate the Kafka topic for clean benchmarking
reset_topic() {
  local topic=$1
  local bootstrap=$2
  
  echo "[bench] Resetting topic '$topic' for clean benchmark..."
  
  # Check if kafka-topics command is available (from Kafka installation or Docker)
  if command -v kafka-topics.sh &> /dev/null; then
    KAFKA_CMD="kafka-topics.sh"
  elif command -v docker &> /dev/null && docker ps | grep -q bijou64-kafka; then
    # Use docker exec if Kafka is running in the bijou64-kafka container
    KAFKA_CMD="docker exec bijou64-kafka /opt/kafka/bin/kafka-topics.sh"
  else
    echo "[bench] Warning: Cannot find kafka-topics command. Skipping topic reset."
    echo "[bench] Install Kafka CLI tools or ensure Docker container 'bijou64-kafka' is running."
    return 0
  fi
  
  # Delete the topic if it exists (ignore errors if it doesn't exist)
  $KAFKA_CMD --bootstrap-server "$bootstrap" --delete --topic "$topic" 2>/dev/null || true
  
  # Wait a moment for deletion to complete
  sleep 2
  
  # Create the topic with appropriate settings for benchmarking
  # Using 1 partition for consistent comparison, replication factor 1 for single-broker setup
  $KAFKA_CMD --bootstrap-server "$bootstrap" --create --topic "$topic" \
    --partitions 1 --replication-factor 1 \
    --config retention.ms=3600000 \
    --config segment.bytes=1073741824 2>/dev/null || {
    echo "[bench] Warning: Failed to create topic. It may already exist."
    sleep 5 # wait for 2 seconds
  }
  
  echo "[bench] Topic reset complete."
}

run_once() {
  local mode=$1
  local runid=$2
  IFS=':' read -r base_mode compression <<< "$mode"
  local logfile="$LOG_DIR/producer-${base_mode}-${compression}-run${runid}-$(date +%s).log"

  echo "[bench] Mode=$base_mode compression=$compression run=$runid -> logfile=$logfile"

  # Reset topic before each run for clean benchmarking
  reset_topic "$TOPIC" "$BOOTSTRAP"

  # Ensure the root artifact is installed so perf module compiles against local sources
  mvn -B -DskipTests -Dgpg.skip=true -f "$ROOT_DIR/../.." install >/dev/null 2>&1

  local compression_args=()
  if [[ "$compression" != "none" ]]; then
    compression_args=(--compression "$compression")
  fi

  # Run the producer (script will package and exec)
  if "$ROOT_DIR/scripts/run-producer.sh" --mode "$base_mode" --topic "$TOPIC" --bootstrap-server "$BOOTSTRAP" --count "$COUNT" "${compression_args[@]}" >"$logfile" 2>&1; then
    exit_code=0
  else
    exit_code=$?
  fi

  # Parse metrics
  rate_line=$(grep -E "Producer rate:" "$logfile" || true)
  avg_line=$(grep -E "Average payload size:" "$logfile" || true)

  rate=0
  avg_bytes=0
  if [[ -n "$rate_line" ]]; then
    # Extract the number between colon and 'records/sec', remove commas/spaces
    rate=$(echo "$rate_line" | sed -n 's/.*Producer rate:[[:space:]]*\([^ ]*\) records\/sec.*/\1/p' | tr -d ',')
  fi
  if [[ -n "$avg_line" ]]; then
    avg_bytes=$(echo "$avg_line" | sed -n 's/.*Average payload size:[[:space:]]*\([0-9.]*\) bytes.*/\1/p')
  fi

  echo "$base_mode,$compression,$runid,$rate,$avg_bytes,$exit_code,$logfile" >> "$results_csv"
}

# Run iterations
for mode in "${MODES[@]}"; do
  for ((i=1;i<=ITERATIONS;i++)); do
    run_once "$mode" "$i"
  done
done

# Summarize
echo
printf "%-10s %-12s %-6s %-12s %-12s\n" "Mode" "Compression" "Runs" "BestRate" "AvgBytes"
for mode in "${MODES[@]}"; do
  IFS=':' read -r base_mode compression <<< "$mode"
  rates=($(awk -F, -v m="$base_mode" -v c="$compression" '$1==m && $2==c { if($4=="") next; print $4 }' "$results_csv" | tr '\n' ' '))
  avgbytes=($(awk -F, -v m="$base_mode" -v c="$compression" '$1==m && $2==c { if($5=="") next; print $5 }' "$results_csv" | tr '\n' ' '))
  best=0
  for r in "${rates[@]}"; do
    if [[ -z "$r" ]]; then continue; fi
    if (( $(echo "$r > $best" | bc -l) )); then best=$r; fi
  done
  # Use first avg_bytes as representative (they should be stable)
  rep_avg=0
  if [[ ${#avgbytes[@]} -gt 0 ]]; then rep_avg=${avgbytes[0]}; fi
  printf "%-10s %-12s %-6d %-12s %-12s\n" "$base_mode" "$compression" "$ITERATIONS" "${best:-0}" "${rep_avg:-0}"
done

echo
echo "Detailed CSV: $results_csv"

exit 0
