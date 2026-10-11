# Koto — Offline Japanese Keyboard 0.1.3

日本語 12 キーのフリック、英語 QWERTY、端末内の漢字候補を備えた Android キーボードの開発用試作版です。**学習は初期 OFF**。保存期間、フリック感度、キー高さ、振動を調整できます。ネットワーク権限・広告・解析・音声・クリップボード機能はありません。

The signed 0.1.3 trial is not debuggable, uses the existing approved development certificate, and targets API 35 with minimum API 26. **Android emulator and Galaxy S24 runtime validation remain NOT RUN.** It is not yet validated for daily use.

Version 0.1.3 addresses the reported 0.1.2 failure where each kana committed immediately. A new callback-aware host test reproduces that defect before the fix. Expected editor selection updates now preserve the reading; a missing span can be restored using known cursor metadata. Sequential に・ほ・ん → 日本 passes the new tests. **The user's app/device still needs a retest**; older host-test success did not establish that behavior. See [composition diagnosis](docs/COMPOSITION.md).

## Implemented controls

- Japanese keypad with conventional kana positions, mode controls on the left and delete/conversion/space/Enter on the right. English uses staggered QWERTY, wider Shift/delete, a wide spacebar and bottom mode controls. This is an original familiar arrangement, without copied Gboard branding or an exact-parity claim.
- Flick distance **8–40 dp**, default/reset **22 dp**. Smaller distance means higher sensitivity. The settings try-it pad displays only its last kana, sends nothing to a host editor, never learns or persists its input, and resets when hidden.
- Offline word candidates from licensed **SKK-JISYO.ML**: 48,750 keys / 67,342 words. Raw kana/katakana remain reachable within the 64-candidate limit.
- Bounded heuristic phrase proposals, labelled **分割**, from the same dictionary with particles and limited polite-ending rules. Verified examples include にほんにいきます → 日本に行きます and きょうはとうきょうにいきます → 今日は東京に行きます. These work with learning off; generated phrases are never saved.
- Tap a candidate to commit, or cycle with 変換 to preview. Enter/space explicitly confirms a preview. Typing/mode/cursor commits once; first Backspace restores the reading. Those implicit commits do not learn.
- Held cursor/delete keys cancel on release, hide, field changes and lifecycle transitions. Custom editor actions display their action names.
- All 95 printable ASCII characters, two symbol pages and one-shot Shift. Password fields suppress candidates/composition. Height 44–76 dp, default 58; landscape 44 dp without fullscreen extraction. Haptics default off.

## Optional local learning and storage

Settings offer **Off / 1 / 7 / 30 / 90 days**, clear-all and private mode. Only successfully and explicitly confirmed dictionary-exact Japanese lexical choices are eligible. Records contain **reading, word and last explicit selection time**; matching readings/prefixes can show up to eight remembered candidates. There is no whole-sentence history, surrounding-text capture, contextual prediction or frequency log.

Passwords, NO_PERSONALIZED_LEARNING, NO_SUGGESTIONS, numeric, forced-ASCII, email/URI, name/address and manual private mode prevent learning and remembered-candidate lookup. Ordinary fields may contain sensitive content without flags; use private mode or leave learning off.

At most **128 entries / 32 KiB serialized data** are kept. Oldest entries are deterministically evicted. Expired/future-dated entries are unusable immediately on lookup; disk pruning runs in the background on start/use and period changes. Reads/previews do not extend expiry, and extending retention cannot revive expired entries. This does not schedule physical deletion while the app is stopped.

One bounded worker coalesces at most 32 pending choices. Files use fixed names in app-private no-backup storage: one data file, one temporary file, each capped at 32 KiB. Interrupted temporary files are discarded; there is no app-created journal, duplicate backup or disk dictionary cache. Clear/disable invalidates old suggestions and queued choices immediately; completion waits for the storage operation. Failures stop learning and are displayed. Clear-all retains the period setting for future explicit choices; Off deletes records and stops learning.

The shipped dictionary remains fixed at **1,209,080 bytes uncompressed**; the signed APK is **512,678 bytes**. Clearing learning leaves that dictionary and display preferences intact. Android-managed installation files, filesystem metadata and preference bookkeeping are additional; actual installed size is unmeasured. No physical secure-erasure guarantee is made. See [local learning/storage details](docs/LOCAL_LEARNING.md).

## Privacy boundary

The actual signed manifest requests **zero permissions**, disables debugging/backup/cleartext traffic, protects the exported IME with BIND_INPUT_METHOD and keeps settings internal. All nine backup domains are excluded in legacy/cloud/device-transfer XML. Optional learning and six non-text controls stay local. No input-text log or upload path is present in source/DEX checks.

The host receives composed/committed text and may transmit it. A compromised OS/root, privileged observers and secure erasure of JVM/Binder strings are outside this boundary. Clear-all cannot remove text already supplied to the host or words in the static dictionary. See [threat model](docs/THREAT_MODEL.md) and [exact verification results](docs/VERIFICATION.md).

## Build and test

Use a complete JDK 17+, Python 3 and curl on Linux x86_64:

    export JAVA_HOME=/path/to/jdk-17
    python3 scripts/prepare_dictionary.py --check
    bash scripts/test_host.sh
    python3 scripts/build_aosp.py --fetch
    bash scripts/test_android_adapter.sh

Fetch explicitly obtains three pinned recognized open-source compiler artifacts; they are not packaged in the app. Later builds omit --fetch. This route installs no Android SDK, accepts no SDK agreement, creates no key and operates no device. See [toolchain provenance](docs/TOOLCHAIN.md).

Unsigned non-debuggable output: dist/offline-kana-keyboard-0.1.3-trial-unsigned.apk. Signed private-delivery trial: dist/offline-kana-keyboard-0.1.3-trial.apk. Version code **4** uses the same certificate as earlier trials. Check [artifact/signature receipt](docs/SIGNING.md) before a separately approved device installation.

A conventional approved-SDK build uses ANDROID_SDK_ROOT and scripts/build_debug.sh. SDK auto-download is disabled. Gradle build/lint remain NOT RUN here because the SDK is absent. [RUNTIME_QA.md](docs/RUNTIME_QA.md) describes setup approval and KVM requirements; [DEVICE_TEST_PLAN.md](docs/DEVICE_TEST_PLAN.md) remains the daily-use validation gate.

## Conversion, source and licensing

Phrase costs/segmentation are heuristic, not Mozc, a linguistic grammar engine or Gboard-quality conversion. Unknown/ambiguous readings and unsupported conjugations can be wrong; choose kana or words individually. No surrounding context is read. [Conversion scope and mature-engine prerequisites](docs/CONVERSION.md) document the limits.

Application code, tests, documentation and SKK dictionary are **GPL-2.0-or-later**. Original dictionary source/header, notices and GPL text are retained. Gradle wrapper utilities are separately Apache-2.0. No Sumire/Gboard/Mozc executable or code was adopted.

Source: [codex-9990/offline-kana-keyboard](https://github.com/codex-9990/offline-kana-keyboard). Application commit: **479f18ccbf50a92d7e41fa907281392198c4fbc8**. Delivery uses the user's Library; no public APK release, device installation or new signing identity was created.
