MVN       := mvn -B
MVN_FLAGS :=

# Build output directories and generated files (see .gitignore)
CLEAN_DIRS := target \
	perf/kafka/target perf/kafka/logs perf/kafka/data \
	examples/kafka-counter-app/target \
	native/target native/bijou/target \
	bin build .gradle \
	coverage .nyc_output htmlcov

CLEAN_FILES := .flattened-pom.xml pom.xml.tag pom.xml.releaseBackup release.properties \
	perf-*.json flamegraph.svg *.so *.dylib *.dll

.PHONY: help init native build test verify clean install jmh perf check

.DEFAULT_GOAL := help

help: ## Show available targets
	@awk 'BEGIN {FS = ":.*##"; printf "Usage: make <target>\n\nTargets:\n"} \
		/^[a-zA-Z0-9_-]+:.*##/ {printf "  %-10s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

init: ## Initialize git submodules
	git submodule update --init --recursive

native: ## Build Rust native library
	./scripts/build-native.sh

build: init native ## Full build (submodules + native + JAR)
	$(MVN) clean package $(MVN_FLAGS)

test: native ## Run unit tests
	$(MVN) clean test $(MVN_FLAGS)

verify: native ## Run tests and package (matches CI)
	$(MVN) clean verify $(MVN_FLAGS)

check: test ## Alias for test

install: native ## Install to local Maven repository
	$(MVN) clean install $(MVN_FLAGS)

clean: ## Remove all build artifacts
	-$(MVN) clean $(MVN_FLAGS)
	-$(MVN) -f perf/kafka/pom.xml clean $(MVN_FLAGS)
	-$(MVN) -f examples/kafka-counter-app/pom.xml clean $(MVN_FLAGS)
	-cargo clean --manifest-path native/Cargo.toml
	-cargo clean --manifest-path native/bijou/Cargo.toml
	rm -rf $(CLEAN_DIRS)
	rm -f $(CLEAN_FILES)

jmh: ## Run JMH microbenchmarks (pass args: make jmh JMH_ARGS='-wi 3 -i 3')
	./scripts/run-jmh.sh $(JMH_ARGS)

perf: ## Build Kafka perf benchmark module
	./perf/kafka/scripts/build.sh
