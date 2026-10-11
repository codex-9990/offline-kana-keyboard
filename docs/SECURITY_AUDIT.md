# Security audit and corrected trial — 2026-10-11

**0.1.1 addresses the confirmed 0.1.0 findings and passes final host/build/artifact checks. Android runtime and Galaxy S24 validation remain NOT RUN.** This is a bounded source/artifact review with synthetic regressions, not a guarantee of absolute safety or zero bugs.

## Exact historical target

- Signed APK: `offline-kana-keyboard-0.1.0-debug-trial.apk`, 484,006 bytes.
- SHA-256: `1c966eb5f24993d214c0b3a3ef48b09c181a25fa8067ad3804091a1c25367cd3`.
- Application source: [ac0e954e20086fd7bf101e1087e70fb420ff2bcc](https://github.com/codex-9990/offline-kana-keyboard/tree/ac0e954e20086fd7bf101e1087e70fb420ff2bcc).

The original artifact was copied into an isolated audit snapshot and its exact hash rechecked. Historical tests pass 19 core groups, 14 source checks and 17 packaged checks. Independent apksig verification passes v2/v3, API 26–35, one expected certificate and zero errors/warnings. Those historical checkers did not require debugging to be disabled or complete ASCII coverage. Findings below refer to the delivered baseline; corrected-source behavior was not substituted for it.

## Findings and fixes

| ID/severity | Baseline condition and evidence | 0.1.1 correction and scope |
|---|---|---|
| F1 — Medium, conditional privacy | Packaged debuggable=true. Authorized local debugger/privileged access could inspect sensitive memory; remote exploitation was not demonstrated. [Builder line 82](https://github.com/codex-9990/offline-kana-keyboard/blob/ac0e954e20086fd7bf101e1087e70fb420ff2bcc/scripts/build_aosp.py#L82). | Both build routes disable debugging; actual signed binary XML confirms false. |
| F2 — Medium, action integrity | Custom action could execute while the key showed ↵. Synthetic ID 42/name 保存 executed 42; UI used only imeOptions. [Adapter line 16](https://github.com/codex-9990/offline-kana-keyboard/blob/ac0e954e20086fd7bf101e1087e70fb420ff2bcc/app/src/main/java/dev/local/koto/ConnectionEditor.java#L16), [label line 207](https://github.com/codex-9990/offline-kana-keyboard/blob/ac0e954e20086fd7bf101e1087e70fb420ff2bcc/app/src/main/java/dev/local/koto/KotoImeService.java#L207). | One EditorAction plan supplies label/command. Named custom actions show 実行/name; unlabelled custom IDs do not silently execute. Standard/custom/no-enter host regressions pass; actual display/dispatch NOT RUN. |
| F3 — Medium, password usability | Eleven ASCII characters unavailable, including $. Synthetic A$1 could not be entered entirely with this IME. [Layout line 179](https://github.com/codex-9990/offline-kana-keyboard/blob/ac0e954e20086fd7bf101e1087e70fb420ff2bcc/app/src/main/java/dev/local/koto/KotoImeService.java#L179). No disclosure demonstrated. | Two symbol pages, first-page $, all 95 printable ASCII characters and synthetic password tests pass. Actual touches/password fields NOT RUN. |
| F4 — Low, editor compatibility | Delete/cursor sent generic hardware-style KeyEvents; no native editor deletion. Editors may ignore/mishandle that path. [Adapter line 21](https://github.com/codex-9990/offline-kana-keyboard/blob/ac0e954e20086fd7bf101e1087e70fb420ff2bcc/app/src/main/java/dev/local/koto/ConnectionEditor.java#L21). Android failure not reproduced. | Selection metadata and code-point deletion API; legacy fallback/cursor use SOFT_KEYBOARD/KEEP_TOUCH_MODE flags. Adapter regressions pass; real dispatch NOT RUN. |
| F5 — Low, conversion usability | Truncating to 64 after adding fallbacks removes kana options for こう/しょう. Reproduced with real dictionary. [Lookup line 58](https://github.com/codex-9990/offline-kana-keyboard/blob/ac0e954e20086fd7bf101e1087e70fb420ff2bcc/app/src/main/java/dev/local/koto/core/SkkDictionary.java#L58). | Reserved fallback slots; real ambiguous-reading regressions pass. |
| F6 — Low, composition integrity | Cycling changed UI selection only; typing reset it and mode/cursor commit used original kana. [Cycle line 224](https://github.com/codex-9990/offline-kana-keyboard/blob/ac0e954e20086fd7bf101e1087e70fb420ff2bcc/app/src/main/java/dev/local/koto/KotoImeService.java#L224). | Core owns preview; host composition previews it. Typing/mode/cursor commits once; Backspace restores kana first; revision guards reject stale taps. Host passes; framework behavior NOT RUN. |
| F7 — Low, conditional lifecycle risk | Hide did not explicitly cancel held-repeat; depends on touch cancellation/detachment. Static condition only; Android reproduction NOT RUN. [Hide line 88](https://github.com/codex-9990/offline-kana-keyboard/blob/ac0e954e20086fd7bf101e1087e70fb420ff2bcc/app/src/main/java/dev/local/koto/KotoImeService.java#L88). | Explicit cancellation on hide/field/configuration/destruction and view visibility/detachment. Gesture model passes; Android timing NOT RUN. |

The four independent review concerns F1–F4 were reconciled with pinned baseline evidence and corrected code. This report does not claim an independent reviewer completed runtime validation of 0.1.1.

## Privacy inspection and limits

The baseline requests zero permissions, protects the IME with BIND_INPUT_METHOD, disables backup and excludes nine domains in cloud/device-transfer rules. Its 46 DEX class definitions are project-owned, with no native methods/libraries or third-party runtime classes. Extended reference checks found no network, WebView, speech, clipboard, logging, dynamic-loading or direct outbound sharing path. Only height/haptic preferences persist; no input, learning or history storage exists. The corrected signed APK retains these boundaries and passes all 19 current packaged checks.

No confirmed direct exfiltration or high/critical finding was identified within this static scope. This is not proof every bug or indirect system path is absent. The host receives text and may transmit it; compromised OS/root, privileged observers and secure erasure of JVM/Binder strings are outside scope. Runtime logs, storage, backup/transfer and editor behavior were not observed. No actual user typing data was collected.

## Corrected artifact and readiness

- Application source: [9adf477030a17468feca1761f28791dbd7d8776c](https://github.com/codex-9990/offline-kana-keyboard/tree/9adf477030a17468feca1761f28791dbd7d8776c).
- Signed 0.1.1: **488,102 bytes**, SHA-256 `8d890c246f16fd6cf74152c803e2cbedeba7482d1e723173f4376f68514cc60a`.
- **PASS:** 36 core groups, 14 API-adapter groups, 15 source checks, 19 signed-APK checks, signature/payload/tamper checks.
- **NOT RUN:** Android runtime, physical Galaxy S24, conventional Gradle build/lint.

Whole combining/ZWJ grapheme deletion, callback timing, rapid input/focus changes, S24 insets/haptics and editor action display must pass [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md) before daily-use readiness can be claimed. Conversion remains SKK word-level lookup; full sentence conversion and learning are absent. [RUNTIME_QA.md](RUNTIME_QA.md) identifies setup approval needed. No optional feature expansion was included.
