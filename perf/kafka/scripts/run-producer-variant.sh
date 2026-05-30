#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

# Ensure root artifact installed
mvn -B -DskipTests -Dgpg.skip=true -f ../.. install

mvn -B -DskipTests -Dgpg.skip=true package exec:java \
  -Dexec.mainClass=org.bijou64.perf.kafka.ProducerBenchmarkVariant \
  -Dexec.args="$*"
