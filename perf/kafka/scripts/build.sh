#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "Building root Bijou64 library and installing to local Maven repository..."
pushd ../.. >/dev/null
mvn -B clean install -DskipTests
popd >/dev/null

echo "Building perf benchmark module..."
mvn -B package -DskipTests

echo "Perf benchmark build complete."
