# Verification report — 2026-10-11

## Delivered state

The local first version compiles to an **unsigned debug APK** with the complete implemented UI and bundled SKK-ML word dictionary. It is not installable until signed with an approved test identity. It has not run in an Android emulator or on a Galaxy S24. No physical device was installed on or enabled. No SDK agreement or signing-credential creation was performed.

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
| APK signature validation | NOT RUN | APK deliberately unsigned; no key exists in delivery |
| Installation and IME registration | NOT RUN | No emulator; no actual-device installation approval |
| Android flick gesture/multitouch/cancel | NOT RUN | Mapping algorithm tested on host; MotionEvent/UI not run |
| Android composing, deletion, candidate selection, modes | NOT RUN | Host port/state models pass; Android editor behavior unverified |
| Android password fields and restricted learning flags | NOT RUN | Host policy/session tests pass; actual editor fields unverified |
| Android rotation and service lifecycle | NOT RUN | Host lifecycle model passes; framework callbacks unverified |
| Runtime logs, filesystem, OEM backup/device transfer | NOT RUN | Source/compiled-resource checks pass; no runtime assertions claimed |
| Airplane-mode functionality | NOT RUN | No emulator/device; not treated as proof of privacy |
| Galaxy S24 haptics/insets/latency | NOT RUN | Native implementation compiled; actual One UI/device verification required |
| Existing unrelated project | PASS | All 175 tracked files byte-identical to initial baseline and git status clean |
| GitHub repository creation | NOT ATTEMPTED, PAUSED | Requested destination checked; no remote mutation or push |

There are **no unresolved failures in executed final host/build/artifact checks**. Unrun Android/device checks remain blockers to claiming a validated daily-use keyboard.

## Host coverage detail

The 19 passing groups cover all twelve keys/five directions, gesture thresholds/axes/diagonals, modifier cycles, composing/candidate replacement, Unicode codepoint deletion, mode changes and one-shot Shift, four Android password variations, NO_PERSONALIZED_LEARNING, numeric/phone/date/no-suggestions fields, email/URI defaults, cursor behavior, direct space insertion, outside selections/hide, editor changes, a rotation lifecycle model, failed host writes, bounded composition, dictionary expression handling/immutability and real bundled word/okurigana candidates.

These tests exercise the exact framework-free production core through a synthetic editor port. They do not establish InputConnection, InputMethodService, MotionEvent, Samsung editor or window/inset behavior.

## APK inspection detail

Binary XML decoding confirms package `dev.local.koto`, minimum API 26, target API 35, debuggable build, allowBackup=false and usesCleartextTraffic=false. Only the launcher and BIND_INPUT_METHOD-protected IME are exported; settings are internal; no provider/receiver is present. All nine backup domains are excluded in legacy, cloud and device-transfer resources.

The packaged dictionary, NOTICE and GPL text match the reviewed source bytes. DEX references contain no network, WebView, speech, clipboard, logging, telemetry or host-test markers. Every defined DEX class belongs to the project namespace. No native library, signing key or build-tool/framework jar is packaged. The resources table is stored and four-byte aligned. This is static inspection, not a comprehensive security audit.

## Recovery record

The first default-cache Gradle version check failed to load native services. It passed using the new project's writable cache and existing JDK. The standard SDK build preflight remained blocked as designed.

The first alternative APK compiled successfully but its audit stopped because the checker expected an older AAPT2 text format (`sdkVersion` and typed hexadecimal attributes). The produced manifest already had correct SDK values. The audit was changed to decode structured binary XML, then all 17 packaged checks passed. Subsequent source changes were rebuilt and rechecked successfully.

## Host performance sample

One synthetic JVM run on this environment: dictionary load **220.411 ms**; 10,000 warm lookups had median **0.004 ms**, p95 **0.013 ms**, after 1,000 warm-up lookups. This is not an S24 benchmark or an Android touch-to-text/cold-start measurement. The Android dictionary load is deliberately off the UI thread.

## Repository access result

The connected GitHub reader's GET for `codex-9990/offline-kana-keyboard` returned `404 Not Found`. A 404 can also represent inaccessible private content, so availability is not assumed. The connector exposes no repository creation action. Existing GitHub CLI read calls to the authenticated user and organization membership endpoints returned `Forbidden` from this environment before a GitHub permission result could be obtained. No scopes, credentials or network permissions were added.

The parent then requested a pause on repository creation/push while checking another route. **No create request, remote repository, remote URL verification, push, merge, or public APK release was performed here.** Creation rights are unverified; this is not a claim that the connected account lacks organization membership.

## Evidence locations and delivery

The private workspace `verification/` contains final `host-tests.txt`, `host-benchmark.txt`, `aosp-build.txt`, decoded `apk-manifest.xml`, AAPT manifest/badging text, compiled backup XML dumps, DEX string inspection and an unchanged-project receipt. It also contains a local-only baseline hash map of the unrelated project; that map must not be included in public/source exports.

`dist/` contains the unsigned debug APK, an allowlisted source ZIP, a selected verification ZIP, and SHA-256 receipts once packaging is complete. The source ZIP excludes `.git`, build caches, APKs, signing files, baseline records and private environment files. Use private workspace downloads for APK delivery. An installable signed APK is pending explicit approval to use/create a test identity, and must not be published as a public release without a separate decision.
