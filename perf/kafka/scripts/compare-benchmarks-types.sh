#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
LOG_DIR="$ROOT_DIR/logs"
mkdir -p "$LOG_DIR"

TYPES=(long int string bytes)
MODES=(long bijou bijou-java)
COUNT=${1:-100000}
ITERATIONS=${2:-3}
TOPIC=${3:-bijou64-benchmark-topic}
BOOTSTRAP=${4:-localhost:9092}

results_csv="$LOG_DIR/results-types-$(date +%Y%m%dT%H%M%S).csv"
echo "type,mode,run,rate,avg_bytes,exit_code,logfile" > "$results_csv"

run_once() {
  local type=$1
  local mode=$2
  local runid=$3
  local logfile="$LOG_DIR/producer-${type}-${mode}-run${runid}-$(date +%s).log"

  echo "[bench] type=$type mode=$mode run=$runid -> $logfile"

  # Run the producer variant
  if "$ROOT_DIR/scripts/run-producer-variant.sh" --type "$type" --mode "$mode" --topic "$TOPIC" --bootstrap-server "$BOOTSTRAP" --count "$COUNT" >"$logfile" 2>&1; then
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
    rate=$(echo "$rate_line" | sed -n 's/.*Producer rate:[[:space:]]*\([^ ]*\) records\/sec.*/\1/p' | tr -d ',')
  fi
  if [[ -n "$avg_line" ]]; then
    avg_bytes=$(echo "$avg_line" | sed -n 's/.*Average payload size:[[:space:]]*\([0-9.]*\) bytes.*/\1/p')
  fi

  echo "$type,$mode,$runid,$rate,$avg_bytes,$exit_code,$logfile" >> "$results_csv"
}

for type in "${TYPES[@]}"; do
  for mode in "${MODES[@]}"; do
    # Skip bijou modes for non-numeric types
    if [[ "$type" != "long" && "$type" != "int" && ("$mode" == "bijou" || "$mode" == "bijou-java") ]]; then
      continue
    fi
    for ((i=1;i<=ITERATIONS;i++)); do
      run_once "$type" "$mode" "$i"
    done
  done
done

echo "Detailed CSV: $results_csv"
exit 0
