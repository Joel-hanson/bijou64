#!/usr/bin/env bash
set -euo pipefail

# compare-benchmarks.sh
# Builds the project, runs the producer benchmark across modes, and summarizes results.

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
LOG_DIR="$ROOT_DIR/logs"
mkdir -p "$LOG_DIR"

MODES=(
#   "long:none"
#   "long:zstd"
#   "long:snappy"
#   "long:lz4"
#   "bijou:none"
  "bijou:zstd"
  "bijou:snappy"
  "bijou:lz4"
#   "bijou-java:none"
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

run_once() {
  local mode=$1
  local runid=$2
  IFS=':' read -r base_mode compression <<< "$mode"
  local logfile="$LOG_DIR/producer-${base_mode}-${compression}-run${runid}-$(date +%s).log"

  echo "[bench] Mode=$base_mode compression=$compression run=$runid -> logfile=$logfile"

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
