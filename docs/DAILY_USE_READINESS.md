# Personal daily-use readiness — 0.1.8

**Status: review candidate; daily use not yet validated.** The user asked for a keyboard suitable for their personal daily use. The five0.1.7 audit findings have bounded source fixes or an explicit platform-contract limitation, and the latest host/model tests pass. The candidate must still pass independent review and approved native validation.

| Gate | Current result | What remains |
|---|---|---|
| Offline conversion foundation | PASS, licensed SKK dictionary; no runtime download | Conversion quality remains word/dictionary plus bounded heuristic segmentation, not contextual Mozc grammar |
| Privacy/source/artifact controls | PASS, zero permissions, no outward text/log/cloud path, no-backup rules | Native app UID, backup/OEM transfer and logcat verification NOT RUN |
| Five audit responses | PASS in source, API and production UI models | Independent review PENDING; native obscured-touch/overlay behavior NOT RUN |
| Repeated input and lifecycle models | PASS,236,824 sequences and3,000 cycles | Real Binder/Looper, focus, rotation, hide/show and host filtering NOT RUN |
| Password usability | PASS in production UI model, full precomposed kana with no echo/read/learning | Native password widgets and dense palette touch targets on S24 NOT RUN |
| Browser compatibility | Policy/API/model PASS for manual conversion | Actual browser, flags and URL/search/page/notepad retests NOT RUN |
| S24 interaction | Compile/model PASS for controls/rotation | Portrait/landscape, font scaling, insets, accessibility, haptics, latency, stability NOT RUN |
| Signed candidate integrity | Artifact receipt in SIGNING.md | Integrity alone does not establish runtime behavior |
| Delivery | Previous0.1.7 preserved;0.1.8 held | Independent review must finish before replacing the user's APK |

## Daily workflow tradeoffs to assess

Normal English, kana and symbols stay in keyboard RAM until explicit confirmation. Hiding the keyboard or restarting a field discards the unconfirmed draft; same-session relayout preserves it. This deliberate boundary prevents unconfirmed text being exposed as host composition, but adds confirmation steps and reduces host autocomplete integration.

Android can report successful command dispatch even when a host subsequently filters or drops the text. The keyboard cannot acknowledge host application without extra observations; it sends once and retains no post-confirmation recovery buffer. Confirm in the host UI. With eligible opt-in learning, a dispatched choice can be recorded even if the host never displays it. Learning remains off by default. No new retention or auto-retry behavior was introduced to conceal this limitation.

Direct kana selection inserts a complete voiced/small character. It does not transform preceding password text. Choose the complete character before typing it, or explicitly delete/reselect in the host. Private-mode state is refreshed on Settings resume and live notifications. A full/partial obscured gesture cancels active controls and blocks its tail; a legitimate floating window may need closing. Accessibility actions remain available.

These are implemented behavior choices, not claims that the keyboard is comfortable enough for daily typing. The approved S24 test must assess them with synthetic inputs before the user relies on this keyboard.

## Required native gate

Use DEVICE_TEST_PLAN.md and RUNTIME_QA.md on an approved, explicitly selected API35 emulator, then a separately approved S24. Include ordinary/manual/browser/password input, repeated conversion, native delete/cursor/graphemes, field changes, rotation, background/restart, Settings live privacy state, overlay cancellation with mixed flags, repeat shutdown, accessibility, layout/haptics and bounded local storage. Record PASS/FAIL/NOT RUN with Android/One UI/app versions. Do not collect actual user typing, introduce production input logging, install on a phone or enable an IME without approval.

SDK installation/agreement, host virtualization changes, physical-device access and public APK publication have not been performed. The current environment has no adb/emulator/KVM, so host/model passes cannot close these gates.
