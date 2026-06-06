#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT_DIR"

./build-native.sh

JMH_ARGS=("$@")
if [[ ${#JMH_ARGS[@]} -eq 0 ]]; then
  JMH_ARGS=(-wi 5 -i 5 -f 1)
fi
mvn -B -Pjmh -DskipTests -Dgpg.skip=true clean install package
java -jar target/benchmarks.jar "${JMH_ARGS[@]}"
