#!/usr/bin/env bash
set -euo pipefail

# compare-benchmarks.sh
# Builds the project, runs the producer benchmark across modes, and summarizes results.

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
LOG_DIR="$ROOT_DIR/logs"
mkdir -p "$LOG_DIR"

MODES=(long bijou bijou-java)
COUNT=${1:-100000}
ITERATIONS=${2:-3}
TOPIC=${3:-bijou-benchmark-topic}
BOOTSTRAP=${4:-localhost:9092}

results_csv="$LOG_DIR/results-$(date +%Y%m%dT%H%M%S).csv"
echo "mode,run,rate,avg_bytes,exit_code,logfile" > "$results_csv"

run_once() {
  local mode=$1
  local runid=$2
  local logfile="$LOG_DIR/producer-${mode}-run${runid}-$(date +%s).log"

  echo "[bench] Mode=$mode run=$runid -> logfile=$logfile"

  # Ensure the root artifact is installed so perf module compiles against local sources
  mvn -B -DskipTests -Dgpg.skip=true -f "$ROOT_DIR/../.." install >/dev/null 2>&1 || true

  # Run the producer (script will package and exec)
  if "$ROOT_DIR/scripts/run-producer.sh" --mode "$mode" --topic "$TOPIC" --bootstrap-server "$BOOTSTRAP" --count "$COUNT" >"$logfile" 2>&1; then
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

  echo "$mode,$runid,$rate,$avg_bytes,$exit_code,$logfile" >> "$results_csv"
}

# Run iterations
for mode in "${MODES[@]}"; do
  for ((i=1;i<=ITERATIONS;i++)); do
    run_once "$mode" "$i"
  done
done

# Summarize
echo
printf "%-10s %-6s %-12s %-12s\n" Mode Runs BestRate AvgBytes
for mode in "${MODES[@]}"; do
  rates=($(awk -F, -v m="$mode" '$1==m { if($3=="") next; print $3 }' "$results_csv" | tr '\n' ' '))
  avgbytes=($(awk -F, -v m="$mode" '$1==m { if($4=="") next; print $4 }' "$results_csv" | tr '\n' ' '))
  best=0
  for r in "${rates[@]}"; do
    if [[ -z "$r" ]]; then continue; fi
    if (( $(echo "$r > $best" | bc -l) )); then best=$r; fi
  done
  # Use first avg_bytes as representative (they should be stable)
  rep_avg=0
  if [[ ${#avgbytes[@]} -gt 0 ]]; then rep_avg=${avgbytes[0]}; fi
  printf "%-10s %-6d %-12s %-12s\n" "$mode" "$ITERATIONS" "${best:-0}" "${rep_avg:-0}"
done

echo
echo "Detailed CSV: $results_csv"

exit 0
