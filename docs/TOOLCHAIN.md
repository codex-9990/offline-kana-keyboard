# Build toolchain and provenance

## Inspected environment

The saved environment contains Temurin JDK 17.0.20.1+1 and Gradle 8.9; both version checks pass with a project-owned cache. The system Java runtime is OpenJDK 21, but a complete system JDK was not found. Python is 3.12.14; GCC exists.

No Android SDK, NDK, adb, emulator, Bazel/Bazelisk, CMake, Ninja, Android system image or KVM device was found in the inspected locations. Earlier setup records explicitly left SDK agreement consent pending. No Android SDK package was installed or its agreement accepted for this task. A development signing identity was later created with explicit approval, retained privately and reused for 0.1.1. No other credential or permission change was performed.

Initial Gradle startup failed to initialize native services with the default home/cache. Using the already available JDK and a cache inside the isolated project resolved the failure. The conventional SDK build preflight exits 78, because SDK Platform 35/Build Tools 35.0.1 are missing. This is not a successful Gradle build or lint result.

## Successful alternative build

These recognized open-source Maven artifacts are build inputs, not runtime dependencies:

| Artifact | Publisher/source | Declared license | SHA-256 |
|---|---|---|---|
| android-all 15-robolectric-12650502 | Robolectric / AOSP, Maven Central | Apache-2.0 | `6c0670454f6fcee9f1b17aa393e81d71bf26e7e6ae7c5777ed3924b062191d5b` |
| AAPT2 8.7.3-12006047 linux | Google Maven / AOSP | Apache-2.0 | `028916a040b58f34c9c89984e1bc48f8da6581662014f2b66391860def8b9c36` |
| R8/D8 8.7.18 | Google Maven / AOSP | BSD and bundled notices | `58366f77067207c39a17d469de7b05701d2877212a9c55201bcb0af43e59e903` |

Their primary POMs were inspected before downloading binaries: [android-all POM](https://repo.maven.apache.org/maven2/org/robolectric/android-all/15-robolectric-12650502/android-all-15-robolectric-12650502.pom), [AAPT2 POM](https://dl.google.com/android/maven2/com/android/tools/build/aapt2/8.7.3-12006047/aapt2-8.7.3-12006047.pom), [R8 POM](https://dl.google.com/android/maven2/com/android/tools/r8/8.7.18/r8-8.7.18.pom). The android-all POM specifically describes source-built Apache-licensed API binaries. The downloads matched the publishers' HTTPS SHA-1 checksums; their full SHA-256 and sizes are additionally pinned in `aosp-toolchain.lock.json` and the builder script. Detached signatures were not verified.

The tools live only in the ignored project build cache. AAPT2 compiles/links application resources using the AOSP API jar as a framework reference. Javac compiles only project classes and generated resource IDs. D8 translates those classes, treating the AOSP jar strictly as a library. The final APK's defined DEX classes all belong to `dev.local.koto`; no framework/build-tool jar or native library enters the APK.

The APK uses the app manifest, with package/version/SDK attributes injected by the builder. Version 0.1.1 explicitly sets debuggable=false, and the builder no longer supplies AAPT2's --debug-mode. There are no runtime library manifests to merge. Its actual compiled manifest and backup XML were decoded and checked. A future Android Gradle build's merged manifest should be checked independently rather than assumed identical.

The build output is unsigned. A separate signed development trial was verified using pinned Google apksig 8.7.3; its v2/v3 signatures pass for API 26–35 with the same certificate as 0.1.0. Android lint, installation, IME registration and on-device performance remain NOT RUN. The application API compilation, resource linking, DEX generation and packaged APK inspection passed. See [RUNTIME_QA.md](RUNTIME_QA.md) for the exact official SDK/emulator packages proposed for approval; nothing was installed.
