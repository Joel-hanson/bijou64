#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

mvn -B exec:java \
  -Dexec.mainClass=org.bijou64.perf.kafka.NormalProducerBenchmark \
  -Dexec.args="$*"
