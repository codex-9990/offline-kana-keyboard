# Verification report — 2026-10-11

## Delivered state

The local first version compiles to an **unsigned debug APK** with the complete implemented UI and bundled SKK-ML word dictionary. Following explicit user approval, a separate **development-signed trial APK** was produced, its signatures verified and its Library delivery confirmed. The development signing identity is retained privately for compatible later trial updates. No private key, password or private storage detail is included in any deliverable. The app has not run in an Android emulator or on a Galaxy S24. No physical device was installed on or enabled. No SDK agreement was accepted.

The final compiled package requests zero permissions. The host app remains able to transmit text supplied to its editor; the keyboard's boundary does not cover that app or a compromised OS.

## Results

| Check | Exact result | Evidence/method |
|---|---|---|
| Java 17 toolchain | PASS | Temurin 17.0.20.1+1 javac/java version checks |
| Gradle 8.9 startup | PASS after recovery | Project-owned cache resolves default native-service initialization failure |
| Android SDK/NDK/adb/emulator/KVM | NOT AVAILABLE | Files/commands/device inspection; none found |
| SDK agreement consent | PENDING, no agreement accepted | Saved setup records and user instruction; build does not depend on agreement acceptance |
| Conventional Gradle build/lint | NOT RUN | `build_debug.sh` preflight exits 78 before Gradle due missing SDK |
| SKK-ML original bytes | PASS | Pinned Git blob and SHA-256 verified |
| Dictionary assets/notices | PASS — 5 deterministic artifacts | `prepare_dictionary.py --check`; 48,750 keys / 67,342 candidates |
| Host input/conversion tests | PASS — 19 groups | `test_host.sh`, actual bundled dictionary plus synthetic fixtures |
| Static privacy policy checks | PASS — 14 checks | Source manifest, storage writes, outward calls, dependencies and backup rules |
| Java Android API compilation | PASS | All app and generated resource classes compile against AOSP API 35 reference |
| Resources and APK linking | PASS | Pinned Google Maven AAPT2 |
| DEX generation | PASS | Pinned R8/D8; only project classes supplied as inputs |
| Packaged APK privacy/content checks | PASS — 17 checks | Actual binary XML, ZIP assets, DEX strings/class definitions and resource alignment |
| Android Gradle merged manifest | NOT RUN | SDK build unavailable; successful alternative build's packaged manifest inspected directly |
| Development signing identity | PASS, explicitly approved | Identity retained privately for compatible trial updates; private material excluded from publication and archives |
| Trial APK signature validation | PASS | Google apksig 8.7.3; API 26–35; v2/v3; one expected signer; zero errors/warnings |
| Signed/unsigned application payload equality | PASS — all 10 ZIP entries | Manifest, DEX, resources and assets byte-identical; unchanged zero-permission and backup policy |
| Tamper detection | PASS | Deliberately altered DEX payload rejected by cryptographic signature verification |
| Signed trial Library delivery | PASS | Successful Library create confirmed; public-only receipt in `TRIAL_DELIVERY.json` |
| Installation and IME registration | NOT RUN | No emulator; no actual-device installation approval |
| Android flick gesture/multitouch/cancel | NOT RUN | Mapping algorithm tested on host; MotionEvent/UI not run |
| Android composing, deletion, candidate selection, modes | NOT RUN | Host port/state models pass; Android editor behavior unverified |
| Android password fields and restricted learning flags | NOT RUN | Host policy/session tests pass; actual editor fields unverified |
| Android rotation and service lifecycle | NOT RUN | Host lifecycle model passes; framework callbacks unverified |
| Runtime logs, filesystem, OEM backup/device transfer | NOT RUN | Source/compiled-resource checks pass; no runtime assertions claimed |
| Airplane-mode functionality | NOT RUN | No emulator/device; not treated as proof of privacy |
| Galaxy S24 haptics/insets/latency | NOT RUN | Native implementation compiled; actual One UI/device verification required |
| Existing unrelated project | PASS | All 175 tracked files byte-identical to initial baseline and git status clean |
| Initial GitHub destination preflight | PASS | User-created public repository `codex-9990/offline-kana-keyboard`, ID 1413877097; initial empty-repository and push-permission checks passed |

There are **no unresolved failures in executed final host/build/artifact checks**. Unrun Android/device checks remain blockers to claiming a validated daily-use keyboard.

## Host coverage detail

The 19 passing groups cover all twelve keys/five directions, gesture thresholds/axes/diagonals, modifier cycles, composing/candidate replacement, Unicode codepoint deletion, mode changes and one-shot Shift, four Android password variations, NO_PERSONALIZED_LEARNING, numeric/phone/date/no-suggestions fields, email/URI defaults, cursor behavior, direct space insertion, outside selections/hide, editor changes, a rotation lifecycle model, failed host writes, bounded composition, dictionary expression handling/immutability and real bundled word/okurigana candidates.

These tests exercise the exact framework-free production core through a synthetic editor port. They do not establish InputConnection, InputMethodService, MotionEvent, Samsung editor or window/inset behavior.

## APK inspection detail

Binary XML decoding confirms package `dev.local.koto`, minimum API 26, target API 35, debuggable build, allowBackup=false and usesCleartextTraffic=false. Only the launcher and BIND_INPUT_METHOD-protected IME are exported; settings are internal; no provider/receiver is present. All nine backup domains are excluded in legacy, cloud and device-transfer resources. All 17 checks also passed on the signed trial APK. Signing did not modify any application payload bytes.

The packaged dictionary, NOTICE and GPL text match the reviewed source bytes. DEX references contain no network, WebView, speech, clipboard, logging, telemetry or host-test markers. Every defined DEX class belongs to the project namespace. No native library, signing key or build-tool/framework jar is packaged. The resources table is stored and four-byte aligned. This is static inspection, not a comprehensive security audit.

## Recovery record

The first default-cache Gradle version check failed to load native services. It passed using the new project's writable cache and existing JDK. The standard SDK build preflight remained blocked as designed.

The first alternative APK compiled successfully but its audit stopped because the checker expected an older AAPT2 text format (`sdkVersion` and typed hexadecimal attributes). The produced manifest already had correct SDK values. The audit was changed to decode structured binary XML, then all 17 packaged checks passed. Subsequent source changes were rebuilt and rechecked successfully.

## Host performance sample

One synthetic JVM run on this environment: dictionary load **220.411 ms**; 10,000 warm lookups had median **0.004 ms**, p95 **0.013 ms**, after 1,000 warm-up lookups. This is not an S24 benchmark or an Android touch-to-text/cold-start measurement. The Android dictionary load is deliberately off the UI thread.

## Repository access result

The user created [codex-9990/offline-kana-keyboard](https://github.com/codex-9990/offline-kana-keyboard) and authorized source publication. The initial destination check confirmed repository ID **1413877097**, public visibility, an empty default branch and push permission. Initial source publication succeeded at commit `ac0e954e20086fd7bf101e1087e70fb420ff2bcc`. Signing and delivery documentation is updated separately without application-code changes. No scopes, credentials or network permissions were added.

The publication export contains only the reviewed project source, dictionary, license/notices, build utilities and public documentation. It excludes APKs, signing files, build caches, private storage details, private environment records and the unrelated project. The publication receipt records the remote commit and comparison of its complete tree with the reviewed local files. No APK, private signing material or public APK release is included.

## Evidence locations and delivery

The selected verification archive contains final `host-tests.txt`, `host-benchmark.txt`, `aosp-build.txt`, decoded `apk-manifest.xml`, AAPT manifest/badging text, compiled backup XML dumps and an unchanged-project receipt. The signed trial adds public `trial-signature.json`, `trial-apk-policy.txt` and `trial-payload-check.txt`. Public certificate and artifact SHA-256 values are documented in [SIGNING.md](SIGNING.md) and [TRIAL_DELIVERY.json](TRIAL_DELIVERY.json). Private baseline records and private delivery/storage metadata are excluded.

`dist/` contains the unsigned debug APK, development-signed trial APK, an allowlisted source ZIP, a selected verification ZIP, a verification/report ZIP and SHA-256 receipts. The source ZIP excludes `.git`, build caches, APKs, signing files, baseline records and private environment files. Source and report archives are saved in the user's Library; updates replace those same Library files. The signed trial APK is delivered through the user's Library. No public APK release or device operation was performed. Signature verification establishes package integrity and signing identity; Android installation and runtime behavior remain **NOT RUN**.
