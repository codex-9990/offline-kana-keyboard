# Koto — Offline Kana Keyboard

日本語 12 キー・フリック入力、端末内の SKK 単語候補、英語 QWERTY を備える Android キーボードの試作版です。ネットワーク権限、入力の学習・保存、広告、解析、音声入力、クリップボード機能はありません。

This first version is aimed at portrait use on a Galaxy S24. It is a native Java `InputMethodService`, with no runtime dependencies or native libraries. The packaged APK targets Android 15 / API 35 and supports API 26+. Physical Galaxy S24 and Android emulator verification are still pending. **A development-signed trial APK is available privately in the user's Library. Its v2/v3 signatures verify for API 26–35; this is not Android runtime testing.**

## What is implemented

- A standard 12-key kana layout; center/left/up/right/down flicks; visible direction hints; dakuten, handakuten and small kana during composition.
- Local candidate selection using **SKK-JISYO.ML**, with 48,750 dictionary keys and 67,342 candidate words. Tap a candidate, or cycle with `変換` and confirm with Enter.
- Hiragana/katakana fallback and simple SKK okurigana, such as `たべる` → `食べる`.
- English QWERTY, one-shot Shift, numbers, punctuation, paired quotes and a basic emoji key.
- Cursor arrows, repeat on held cursor/delete keys, editor actions, and mode switching.
- Key height from 44 to 76 dp; 58 dp default. Landscape uses 44 dp and avoids fullscreen extract mode. Haptics default off and respect Android's haptic setting.
- Password fields default to English (numeric passwords use numbers). Candidates and kana composition are disabled in password fields. Email/URI fields default to English.
- `IME_FLAG_NO_PERSONALIZED_LEARNING` is honored: **learning is absent in every field**, and the field is marked private. Static dictionary lookup remains available. `TYPE_TEXT_FLAG_NO_SUGGESTIONS` disables candidates and composition.

This is **word conversion, not contextual sentence conversion or Mozc**. Select and commit words individually; an entire sentence may remain kana. Candidate order follows the dictionary and can be poor for ambiguous readings. There is no phrase segmentation, adaptive ranking, prediction, romaji-to-kana input, tap cycling, autocorrect or user dictionary. `゛小` modifies only active composing kana; it does not change already committed text in direct-input/password/no-suggestion fields. Kana base characters can still be entered directly there.

## Privacy boundary

The application requests **zero Android permissions**, including no `INTERNET`, microphone or clipboard access. The exported IME service requires the system's `BIND_INPUT_METHOD` permission. The only other exported component is the launcher. Keyboard settings are internal.

No typed text is written to files, preferences, databases, logs, crash upload services or a personal dictionary. Only `row_height` and `haptics` are saved locally. The manifest disables backup, and both cloud backup and device-transfer rules explicitly exclude all supported data domains. Clipboard history is absent, so it is off by construction. There is no learning to disable with a manual incognito switch.

The host application receives composed/committed text through Android's `InputConnection`, and may transmit it. A compromised OS, root/debug access, accessibility services, malicious overlays, screen capture and the host application's behavior are outside this keyboard's no-egress boundary. JVM strings cannot be securely erased. See [the threat model](docs/THREAT_MODEL.md) and [verification report](docs/VERIFICATION.md).

## Build without an installed Android SDK

A complete JDK 17+, Python 3 and curl are required. On Linux x86_64:

```sh
python3 scripts/prepare_dictionary.py --check
bash scripts/test_host.sh
python3 scripts/build_aosp.py --fetch
```

`--fetch` explicitly downloads three recognized open-source build artifacts into `.build-cache/`: AOSP APIs distributed by Robolectric (Apache 2.0), Google's Maven AAPT2 (Apache 2.0), and R8/D8 (BSD/Apache and notices in its distribution). Each artifact is pinned by SHA-256. Subsequent builds can omit `--fetch`. The artifacts are compiler inputs/tools and **are not packaged in the application**. This route does not install the Android SDK, accept the Android SDK License Agreement, create a signing identity, or operate a device. See [toolchain provenance](docs/TOOLCHAIN.md).

Output:

```text
dist/offline-kana-keyboard-0.1.0-debug-unsigned.apk
```

The build runs source and packaged APK policy checks. Its output is unsigned. A separate development signing identity was created with user approval for the signed trial; signing credentials are excluded from the source and deliverables. The signed trial contains debuggable code and is for private review/testing. See [signing and certificate verification](docs/SIGNING.md).

## Conventional Android Gradle build

If SDK Platform 35 and Build Tools 35.0.1 are already installed under terms approved by their owner:

```sh
export ANDROID_SDK_ROOT=/path/to/approved/android-sdk
bash scripts/build_debug.sh
```

The wrapper pins Gradle 8.9, and the Android Gradle plugin is 8.7.3. SDK auto-download is disabled. The script stops when SDK components are missing and never accepts agreements. Debug signing defaults to none. To use an existing approved test keystore, explicitly provide `KOTO_DEBUG_KEYSTORE`, `KOTO_DEBUG_STORE_PASSWORD`, `KOTO_DEBUG_KEY_ALIAS` and `KOTO_DEBUG_KEY_PASSWORD`; do not commit those values or keys.

The SDK/Gradle build and Android lint were **not run in the saved environment** because the SDK and license approval were absent. The Maven-tooling compilation and APK audit succeeded separately.

## Trial installation

Obtain `offline-kana-keyboard-0.1.0-debug-trial.apk` from the user's Library or a private workspace download, verify its SHA-256 against [the signing receipt](docs/SIGNING.md), and install it only after approving that device operation. This work did not install or enable the keyboard on a user's phone.

On a Galaxy S24, use Settings → General management → Keyboard list and default to enable Koto, then select it as the current keyboard. Alternatively open Koto and tap the system settings/picker buttons; these actions happen only on a user's tap. Android's general keyboard warning is expected, because an IME receives typed characters. Follow [the device test plan](docs/DEVICE_TEST_PLAN.md) before relying on this build.

## Source and licensing

Application code, scripts, documentation and bundled SKK dictionary: **GPL-2.0-or-later**. See [LICENSE](LICENSE) and [NOTICE](NOTICE). The complete pinned upstream JSON dictionary is retained in `third_party/skk/`; `scripts/prepare_dictionary.py` creates the read-only TSV and embedded notices deterministically and does not execute dictionary expressions. The original dictionary's copyright and license are also displayed in the settings screen.

The Gradle wrapper/scripts are separate Apache-2.0 build utilities, with their license in `third_party/gradle-wrapper/`. No Sumire/KazumaProject code or binaries were adopted. No Mozc library is shipped. See [conversion decisions and future integration](docs/CONVERSION.md).

Source repository: [codex-9990/offline-kana-keyboard](https://github.com/codex-9990/offline-kana-keyboard). The source and verification documentation are public. The development-signed trial is delivered privately through the user's Library; its public certificate fingerprint, artifact hash and verification status are recorded in [TRIAL_DELIVERY.json](docs/TRIAL_DELIVERY.json). No public APK release has been created.
