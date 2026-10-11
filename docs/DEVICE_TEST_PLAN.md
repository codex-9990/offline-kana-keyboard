# Android emulator and Galaxy S24 test plan

All rows below are currently **NOT RUN on Android**. Host-model and packaged-artifact results are in VERIFICATION.md. Use synthetic text only; no actual user typing data is needed.

## Prerequisites and device boundary

Use an already available emulator if one is present; there was none in this saved environment. Only run `adb` commands against an explicitly selected `emulator-...` serial. Do not install or enable an IME on a physical phone without separate device approval. A signed test APK is needed. Do not accept SDK agreements, install emulator software or create signing keys as an implicit test step.

For emulator UI QA, dump a step-specific UI tree with `adb -s "$serial" exec-out uiautomator dump /dev/tty`; derive tap centers and gesture start bounds from that tree. Screenshots are for visual checks, not coordinate guessing. Keep screenshots and logcat synthetic and private. Restore the emulator's original selected IME after testing.

## Test matrix

| Flow | Synthetic steps and expected result | Status |
|---|---|---|
| Registration | Install approved signed APK; confirm IME resolves with BIND_INPUT_METHOD; enable/select on emulator | NOT RUN |
| New layouts | Japanese left mode controls/central kana/right edit keys; English staggered rows/wide space; both symbol-page exits; small screen/font scaling | NOT RUN |
| Flick sensitivity | 8/22/40 dp, multiple densities, edge origins, default/reset; change during gesture; settings try pad never writes or sends text and resets on hide | NOT RUN |
| Flicks | Exercise center/left/up/right/down for all 12 keys; check や and わ special directions | NOT RUN |
| Cancel/multitouch | Cancel a drag and add a second pointer; no character should commit | NOT RUN |
| Compose/convert | Enter にほん; host shows composing kana; choose 日本; composing span clears with no duplicate | NOT RUN |
| Candidate cycle | Enter はし; 変換 previews/cycles in the host; 確定 commits once; typing/mode/cursor commits the preview; Backspace first restores kana | NOT RUN |
| Bounded fallback | Enter こう and しょう; both kana fallbacks remain reachable within 64 candidates | NOT RUN |
| Okurigana | Enter たべる and かく; candidates include 食べる and 書く | NOT RUN |
| Phrase proposals | With learning OFF/private mode: にほんにいきます, きょうはとうきょうにいきます, すしをたべます; check 分割 labels, raw kana fallback, long/unknown/cancelled input | NOT RUN |
| Learning eligibility | Default OFF; enable 1/7/30/90 days; explicit dictionary choice tap/preview-confirm only; implicit/raw/phrase choices do not save | NOT RUN |
| Private/restricted learning | Manual private mode and password/NO_PERSONALIZED_LEARNING/NO_SUGGESTIONS/email/URI/name/address/forced ASCII suppress recorded and remembered candidates | NOT RUN |
| Same-editor settings privacy | Enable private mode in settings and return to the same editor without onStartInput; old learned preview/tap/queued choices invalidated; rapid ON/OFF never revives them; delayed preference callbacks and host restoration failure stay safe | NOT RUN |
| Retention/clear | Use synthetic test clock/fixture: expiry boundary, shorten/extend/rollback; clear/Off during queued writes; stale taps/previews cannot restore records; clear retains dictionary/preferences | NOT RUN |
| Storage bounds/failure | Repeated choices, 128 entries/32 KiB, bounded queue/temp, corrupt/oversized/interrupted stores; failure message and stopped learning; no synchronous learning-file I/O on input thread | NOT RUN |
| Deletion | Delete within composition, committed text, selected text and emoji; verify surrogate safety and combining/ZWJ clusters; hold delete; no repeat after release/cancel/hide/field change | NOT RUN |
| Mode/Shift | Kana → English commits once; one-shot Shift resets; switches to symbols and back behave | NOT RUN |
| Cursor | Left/right within surrounding host text; composition commits first; hold and release arrows | NOT RUN |
| Host actions | Search, Next, Done, Send, Previous and multiline/no-enter-action; custom action ID 42/name 保存 shows 実行/保存 and executes 42; composing Enter confirms first | NOT RUN |
| Password | Text, visible, web and number passwords: no candidate or on-keyboard text echo; enter synthetic A$1 and all printable ASCII; switch both symbol pages; distinguish ~ and 〜 | NOT RUN |
| Restricted fields | NO_PERSONALIZED_LEARNING leaves no history; NO_SUGGESTIONS shows no composition/candidates | NOT RUN |
| Email/URI/ASCII | English appears by default; FORCE_ASCII respected; punctuation, @ and digits usable | NOT RUN |
| Rotation | Portrait/landscape during composition: text retained once in host, transient state cleared, no fullscreen extract | NOT RUN |
| Lifecycle | Switch fields/apps rapidly, including the same kana reading twice; hide while holding delete; stop/restart IME process; stale candidate taps and repeat callbacks do not affect the next field | NOT RUN |
| Reported composition failure | In the reporting app and a standard editor, flick に・ほ・ん sequentially, retain the underline/reading until explicit 日本 selection; test omitted/late callbacks, prefix preservation, NO_PERSONALIZED_LEARNING and private mode; check app-rejected recovery error | NOT RUN |
| 0.1.3 combined regressions | Type に/ほ/ん with callbacks after each, then move cursor 3→1 and insert あ at the actual position; also test dropped spans plus delayed/coalesced bursts and preview/cancel; preceding host text must stay intact | NOT RUN |
| Controls | Height extremes, default 58 dp, landscape 44 dp; haptic off/on and global Android setting | NOT RUN |
| 0.1.4 coalesced preview/cancel | Type にほん, preview 日本, cancel before callback; test final 3→3 and subsequent 3→2. Confirm visible text survives, ambiguous conversion pauses with restart message, no stale region is restored, direct English input works and fresh editor start resumes kana | NOT RUN |
| Insets/S24 | Gesture/button navigation, status of IME padding, cutout, accessibility scaling and One UI layouts | NOT RUN |
| Local persistence | With learning OFF or raw marker typing, no lexical files; with explicit eligible choices and learning ON, only bounded no-backup lexical data and controls preferences; inspect backup/transfer eligibility | NOT RUN |
| Logs | Capture process-scoped logcat and crash buffer while typing synthetic markers; no markers should appear | NOT RUN |
| Offline behavior | Repeat conversion with airplane mode; pair result with manifest/dependency/outward-call audits | NOT RUN |
| Performance | Measure cold dictionary load, touch-to-text and candidate latency on S24; sustained fast flicks and memory | NOT RUN |

The Android Emulator QA skill informed these UI-tree and selected-serial checks. No emulator runtime was available to apply them in this task.
