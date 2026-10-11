# Koto — Offline Japanese Keyboard 0.1.7

日本語12キーのフリック、英語QWERTY、端末内の漢字候補を備えたAndroidキーボードの開発用試作版です。**学習は初期OFF**。ネットワーク権限・広告・解析・音声・クリップボード機能はありません。

**0.1.7では、通常の入力はキーボード内の「未確定の読み」に表示されます。候補タップ、または「確定」で入力欄へ送ります。英字・記号も同じ操作です。** 入力欄には確定まで表示されません。空白は未確定入力に追加され、確定しません。パスワード・数値・電話・日時・種類不明の欄では直接入力します。

**0.1.7では、NO_SUGGESTIONSの文章欄でも、読みを入力して「変換」を押すと固定辞書で手動変換できます。保存済み学習の参照・保存、自動の分割候補は行いません。** 読みを変更すると候補を閉じます。メール・URL・FORCE_ASCII欄はABCから始まり、日本語は「かな」→読み→「変換」で入力します。入力先が日本語を拒否する場合は読みを保持して編集・取消できます。

The user reports failures in both a browser URL/search bar and a page field, while a browser-based notepad converts successfully. This suggests field-dependent behavior; the browser name, EditorInfo flags and native return values remain unknown. Version 0.1.6 treated NO_SUGGESTIONS as direct-only, preventing dictionary conversion in those text fields. This update separates explicit static conversion from unsolicited prediction and learning. **The affected browser and Galaxy S24 have not been retested; the reported failure is not claimed resolved.** [Compatibility and editing tradeoffs](docs/COMPOSITION.md).

The local draft, explicit confirmation, lifecycle cancellation and corrected immutable preedit/accessibility projection from0.1.6 are retained. Its corrected packet was previously delivered as a limited trial and remains unchanged. The first pre-help-review0.1.6 variant was never delivered.

The signed trial is not debuggable, reuses the approved development certificate, and targets API 35 with minimum API 26. **Android emulator and Galaxy S24 validation remain NOT RUN.** The source and review packet are prepared; private replacement delivery is held pending independent review. It is not validated for daily use.

## Controls and editing

- Japanese keypad: conventional twelve kana positions and five flick directions; left mode controls and right delete/conversion/space/Enter controls. English: staggered QWERTY, wide Shift/delete and space. Familiar original arrangement; no copied branding or exact Gboard-parity claim.
- Visible two-line scrollable local draft above candidates; at most **256 code points**. At the limit, confirm, delete or cancel before continuing; text is never automatically committed.
- Tap a candidate to confirm it, or cycle with 変換 to preview locally and press 確定. The first Backspace cancels a preview; subsequent Backspace deletes the last draft code point. Typing after preview restores the original reading and appends the key once. 取消 discards the entire unconfirmed draft.
- Switching kana/English/symbol modes preserves the reading and cancels a preview. Space appends locally. Cursor arrows move the host caret while retaining the draft; later confirmation uses the host's actual selection. Empty-draft delete uses the native host delete key.
- **Hiding the keyboard or starting a fresh field/session cancels unconfirmed input.** Relayout within the same active session preserves it, but a framework hide/restart during rotation may cancel it. Actual rotation behavior is untested.
- All 95 printable ASCII characters, two symbol pages, one-shot Shift and labelled editor actions. Password variants show no draft/candidates and commit directly.
- Flick distance **8–40 dp**, default/reset **22 dp**. The settings try-it pad shows only its last kana, sends nothing to a host, never learns/persists and resets when hidden.
- Height **44–76 dp**, default **58**; landscape keys **44 dp**, no fullscreen extraction. Haptics default off. Held delete/arrows cancel on release, hide, field change and lifecycle transitions. Real S24 touch/insets/haptic behavior remains unmeasured.

## Conversion and local learning

The licensed **SKK-JISYO.ML** dictionary has **48,750 keys / 67,342 candidates**. Kana/katakana fallbacks remain reachable within a 64-candidate limit. In ordinary text, bounded heuristic phrase proposals, labelled 分割, use that dictionary plus particles and limited polite-ending rules. Tested examples include 日本に行きます and 今日は東京に行きます. This is not Mozc, a contextual grammar engine or full SKK interaction; homophones, segmentation, vocabulary and conjugations have limits. Explicit-only fields show fixed dictionary candidates and kana fallbacks after the request, without generated phrases. No surrounding host text is read. See [conversion scope and Mozc assessment](docs/CONVERSION.md).

Settings offer **Off / 1 / 7 / 30 / 90 days**, clear-all and private mode. Only successful explicit dictionary-exact lexical choices may save reading/word/last-selection time. Raw kana, generated phrases, cancelled/failed choices and whole sentences are not saved. Private mode and password/NO_PERSONALIZED_LEARNING/NO_SUGGESTIONS/sensitive field policies block remembered lookup and recording. Ordinary fields may be sensitive; leave learning off or use private mode.

Limits: **128 records / 32 KiB**, one fixed no-backup data file plus one temporary file of at most 32 KiB, one worker and at most 32 pending choices. Clear/Off invalidates old suggestions and queued choices immediately; the UI reports completion only after file operations succeed. Expiry is enforced on lookup; stopped-app exact-time deletion and physical secure erasure are not promised. The static dictionary is **1,209,080 bytes uncompressed**; the signed APK is **512,678 bytes**. Android installation/pref/filesystem overhead is unmeasured. See [learning details](docs/LOCAL_LEARNING.md).

## Privacy and verification

The actual signed manifest requests **zero permissions**, disables debugging/backup/cleartext, protects the exported IME with BIND_INPUT_METHOD and keeps settings internal. Nine backup domains are excluded in legacy/cloud/device-transfer XML. Source/DEX checks found no network, logging, analytics, clipboard, speech, upload or surrounding-text read path. Ordinary unconfirmed text stays in keyboard RAM; confirmed and restricted direct text goes to the host app, which may transmit it. OS/root/privileged observers and memory erasure are outside the no-egress boundary. [Threat model](docs/THREAT_MODEL.md).

PASS: **93 core/feature groups**, **43 Android API host groups**, **236,824 bounded sequences (59,048 ordinary +177,776 explicit-only)**, **3,000 continuous conversion cycles accounting for 12,000 accepted keys and 6,000 confirmations**, **29 source checks**, **26 signed APK checks**, 45 help +6 compatibility negative mutations, v2/v3 signatures, payload equality and tamper rejection. These are synthetic/host/artifact checks. Real Binder/Looper, touch, native deletion, rotation, app-private storage/backup, the reporting app and S24 remain **NOT RUN**. Exact results and retained failures: [VERIFICATION.md](docs/VERIFICATION.md).

## Build, delivery and licensing

Use a complete JDK 17+, Python 3 and curl on Linux x86_64:

    export JAVA_HOME=/path/to/jdk-17
    python3 scripts/prepare_dictionary.py --check
    bash scripts/test_host.sh
    python3 scripts/build_aosp.py
    bash scripts/test_android_adapter.sh

A first build requires explicit --fetch to obtain three pinned recognized open-source compiler artifacts; the verified existing cache was reused here. They are not app dependencies. Later builds omit --fetch. The builder clears only its own generated output. This route installs no SDK, accepts no SDK agreement, creates no signing key and operates no device. [Toolchain provenance](docs/TOOLCHAIN.md).

Outputs: dist/offline-kana-keyboard-0.1.7-trial-unsigned.apk and the separately development-signed dist/offline-kana-keyboard-0.1.7-trial.apk; version code **8**. [Signature/artifact receipt](docs/SIGNING.md). Conventional approved-SDK build/lint and real Android QA remain unavailable here; [runtime setup and minimal gate](docs/RUNTIME_QA.md) and [device test plan](docs/DEVICE_TEST_PLAN.md) describe what is still required.

Application code, tests, docs and dictionary are **GPL-2.0-or-later**; original dictionary source/header, notices and GPL text are retained. Gradle wrapper utilities are separately Apache-2.0. No Sumire/Gboard/Mozc code or binary was adopted.

Source: [codex-9990/offline-kana-keyboard](https://github.com/codex-9990/offline-kana-keyboard). Application commit: **3cd0f7dd1f04f5e2e985f909d0f520695b25f074**. APK SHA-256: **df40f0691e3d975f80b603e457fdb7cceecd43b12983f0737ca7d31ac1824e03**. Delivery route: the user's Library after the existing independent-review hold is cleared; **0.1.7 has not been uploaded**. No public APK release, device installation or new signing identity was created. Earlier APKs remain unchanged; 0.1.3–0.1.5 contain known composition regressions and should not be used for daily typing.
