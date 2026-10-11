# Verification report — corrected 0.1.1 trial

The corrected APK builds, is not debuggable, and is signed with the same approved development certificate as 0.1.0. **Android installation, emulator behavior and Galaxy S24 usability remain NOT RUN.** No phone was installed on or enabled, and no SDK agreement was accepted. This is a tested development trial, not a validated daily-use keyboard or a guarantee of zero bugs.

The exact delivered 0.1.0 APK was preserved and audited separately. [SECURITY_AUDIT.md](SECURITY_AUDIT.md) separates baseline findings from fixes. Application source is commit 9adf477030a17468feca1761f28791dbd7d8776c; later documentation commits do not alter that compiled application.

## Exact results

| Check | Result | Method |
|---|---|---|
| Existing JDK 17 / Gradle 8.9 startup | PASS | Existing toolchain; isolated writable cache |
| Android SDK/NDK/adb/emulator/KVM | NOT AVAILABLE | No SDK, image, commands or /dev/kvm |
| Conventional Gradle build / Android lint | NOT RUN | SDK preflight exits 78; no installation or terms accepted |
| Dictionary provenance / deterministic assets | PASS | Pinned SHA-256; five deterministic outputs; 48,750 keys / 67,342 words |
| Framework-free production core | PASS — 36 groups | test_host.sh; real dictionary and synthetic editor fixtures |
| Actual ConnectionEditor API adapter | PASS — 14 groups | test_android_adapter.sh; Android API classes, synthetic InputConnection, injected key sender |
| Source policy | PASS — 15 checks | Permissions, exports, storage, outward calls, controls-only preferences, debugging disabled |
| Java API compilation / resource linking / DEX | PASS | Pinned API 35 jar, AAPT2, D8; only project classes packaged |
| Actual signed APK policy / content | PASS — 19 checks | Binary manifest, version code 2 / 0.1.1, debuggable=false, backup, assets, DEX, alignment |
| Gradle merged manifest | NOT RUN | Alternative build's actual packaged manifest inspected instead |
| Signature / certificate | PASS | apksig 8.7.3; v2/v3, API 26–35, one expected signer, zero errors/warnings |
| Signed/unsigned payload equality | PASS — all 10 ZIP entries | Every uncompressed application payload byte identical |
| Tamper detection | PASS | DEX byte changed while retaining signature block; verifier rejected it; temporary copy removed |
| Original 0.1.0 preservation / reverification | PASS | Original SHA-256 unchanged; signature and 17 historical APK checks pass |
| Android installation / IME registration / MotionEvent | NOT RUN | No runtime available |
| Android composing / preview / deletion / cursor / actions | NOT RUN | Host/API-port checks pass; actual editor/event-loop behavior unverified |
| Android password / restricted-field behavior | NOT RUN | Host fixtures pass; actual fields unverified |
| Android hide / repeat / rapid focus / rotation / restart | NOT RUN | State/gesture model passes; framework callbacks unverified |
| Runtime logs / storage / OEM backup / transfer | NOT RUN | Source and compiled resources checked; runtime assertions not claimed |
| Airplane-mode behavior | NOT RUN | Never treated as proof of privacy |
| Galaxy S24 insets / haptics / accessibility / latency | NOT RUN | Physical device testing required |
| Existing unrelated project | PASS | 175 tracked files unchanged; clean Git status |

No unresolved failures remain in executed final checks. A historical 0.1.0 regression intentionally failed because the 64-candidate cap dropped both kana fallbacks for こう; the corrected implementation passes. Baseline failures were not relabelled as corrected-build passes.

## Scope and limits

Core tests cover flick mappings/thresholds, modifiers, code-point composition deletion, modes, password variations, restricted flags, FORCE_ASCII, real word/okurigana lookup, bounded fallbacks, preview/cancellation/exactly-once commit, stale candidate rejection, lifecycle state clearing, failed writes, Shift, repeat/cancel decisions, all 95 printable ASCII characters and bounded custom-action labels.

Adapter tests exercise the production ConnectionEditor against actual Android API signatures using synthetic InputConnection proxies. They cover selected/reversed-range deletion, code-point deletion, legacy fallback, failed writes, actions, preview integration and software-key flags. The injected key sender avoids native KeyEvent construction on the host JVM. They **do not run Binder, native KeyEvent dispatch, MotionEvent, the main event loop, InputMethodService callbacks or Samsung editors**.

Code-point deletion prevents splitting surrogate pairs when the editor honors the API. Whole combining-character and emoji ZWJ grapheme deletion is not guaranteed. No surrounding-text or clipboard reads were added.

The signed manifest requests zero permissions, disables debugging/backup/cleartext traffic, exports only the launcher and BIND_INPUT_METHOD-protected IME, and keeps settings internal. No provider/receiver is present. Nine domains are excluded in legacy, cloud-backup and device-transfer XML. Dictionary/notices match reviewed source. Defined DEX classes are project-owned; no runtime/native library, key, framework jar or test marker is packaged. Source/DEX checks find no direct network, WebView, speech, clipboard, logging/telemetry or input-persistence path. Static absence checks are bounded evidence.

The host receives text and can transmit it. Compromised OS/root and privileged observers remain outside the keyboard's boundary. No actual user typing data was used.

## Evidence and remaining gate

The verification archive contains iteration-0.1.1 host/adapter/build/APK-policy logs, signature/payload receipts, decoded signed manifest/backup resources, and separate audit-0.1.0 baseline evidence. Keys, credentials, private baseline inventories and Library identifiers are excluded. Public hashes are in [SIGNING.md](SIGNING.md) and [TRIAL_DELIVERY.json](TRIAL_DELIVERY.json).

Source is published to [codex-9990/offline-kana-keyboard](https://github.com/codex-9990/offline-kana-keyboard). Delivery uses the user's Library; no public APK release was created. Source/report updates preserve their existing Library identities.

The next required gate is [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md). [RUNTIME_QA.md](RUNTIME_QA.md) lists exact official packages and approval prerequisites. Missing KVM may make software emulation slow; an emulator cannot establish Galaxy S24-specific behavior.
