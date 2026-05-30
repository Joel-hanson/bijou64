#!/usr/bin/env bash
set -euo pipefail

# cd "$(dirname \"$0\")/.."

# Ensure the root `bijou64` artifact is installed so the perf module
# compiles against the local sources (includes recent Bijou64 methods).
mvn -B -DskipTests -f ../.. install

mvn -B -DskipTests package exec:java \
  -Dexec.mainClass=org.bijou64.perf.kafka.ConsumerBenchmark \
  -Dexec.args="$*"
