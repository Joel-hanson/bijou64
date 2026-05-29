# Bijou64

Minimal Java scaffold for Bijou64. This repository contains both Maven and Gradle build files.

Build with Maven:

```bash
mvn -B clean package
```

Run tests with Maven:

```bash
mvn test
```

Build with Gradle (requires Gradle installed):

```bash
gradle build
```

Run the sample main:

```bash
java -cp target/bijou64-0.1.0.jar org.bijou64.Main
```

## Native Rust integration

This project now uses a git submodule checkout of the upstream `inkandswitch/bijou` repository under `native/bijou`.
The Rust JNI wrapper in `native/` depends on `bijou64` via `path = "bijou/bijou64"`.

After cloning this repository, initialize the native submodule:

```bash
git submodule update --init --recursive
```

Build the native library:

```bash
./build-native.sh
```

Then run the Java sample:

```bash
java -cp target/bijou64-0.1.0.jar org.bijou64.Main
```

If you use Gradle, the Rust native library is built automatically as part of `gradle build`.
If you use Maven, running `mvn -B clean package` will also invoke the native build before packaging.
