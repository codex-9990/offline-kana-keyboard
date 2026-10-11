# Android emulator and Galaxy S24 test plan — 0.1.7

**Every row below is NOT RUN on Android.** Host-model and artifact checks are in [VERIFICATION.md](VERIFICATION.md). Use synthetic text only; no actual user typing data is needed. The browser URL/search bar and a page field fail while a browser notepad works;browser name, actual flags and Android/One UI versions remain unknown.

## Prerequisites and device boundary

There is no SDK, adb, emulator or KVM in this saved environment. Use an explicitly approved KVM-capable API 35 emulator and an explicitly selected emulator serial. Do not install/enable the keyboard on a physical phone without separate approval. Do not accept SDK agreements, install software, create keys or alter permissions as an implicit test step. [RUNTIME_QA.md](RUNTIME_QA.md) gives the bounded setup proposal and minimum real-Binder fixture requirements.

For UI QA derive gesture/tap bounds from a step-specific UI tree using adb -s "$serial" exec-out uiautomator dump /dev/tty. Screenshots support visual inspection, not coordinate guessing. Keep all captures synthetic/private; restore the emulator's original IME afterward.

## Runtime matrix

| Flow | Synthetic steps and expected result | Status |
|---|---|---|
| Registration | Approved install; service resolves with BIND_INPUT_METHOD; enable/select only on selected emulator | NOT RUN |
| Local draft | に・ほ・ん appears only in keyboard row; host/prefix unchanged; 日本 tap confirms once; no in-host composing span | NOT RUN |
| Repeated conversion blocker | 日本 confirmation → accepted ご before callbacks → ordered/coalesced flush → ご confirmation; repeat continuously with exact text/key/commit counts and no lockout | NOT RUN |
| Preview/cancel | はし/橋 preview only locally; first Backspace restores reading; next deletes; late3→2/final3→3 callbacks cannot lose draft or block next kana | NOT RUN |
| Explicit editing | Typing cancels preview and appends to original reading; spaces/mode changes stay local; 取消 sends nothing; each accepted key once | NOT RUN |
| English/symbols | Ordinary QWERTY/symbols also local until 確定; all95 ASCII, both symbol pages, one-shot Shift, labels/exit controls | NOT RUN |
| Capacity | At256 code points next key is visibly rejected; no implicit commit; delete/confirm/cancel permits more accepted typing | NOT RUN |
| Native caret/selection | Host taps/arrows preserve draft; explicit confirmation uses actual caret/range, retains prefix; host-owned span finished without replacing its text | NOT RUN |
| Host rejection | Unsupported/rejected finish/commit displays failure and keeps editable/cancellable draft; explicit retry works when host permits; do not count rejection as usable conversion | NOT RUN |
| Native delete | Local code point deletion/preview cancel; empty-draft DEL on actual range/caret; emoji, combining/ZWJ clusters, native EditText and local WebView | NOT RUN |
| Flick mapping | All12keys/fivedirections includingや/わ;8/22/40dp thresholds/densities/edge origins; one emission on release | NOT RUN |
| Touch/repeat cancellation | Drag/cancel/multitouch/release/hide/field change/rotation cancels held delete/arrows and stale callbacks | NOT RUN |
| Layout/insets | Local row visible/scrollable, candidate rail, Japanese controls, staggered English; height44–76/default58/landscape44; font scaling/One UI/nav insets/accessibility | NOT RUN |
| Sensitivity try pad | Last kana only, no host sends/history/learning; reset on hide; default/reset22dp and changes midgesture | NOT RUN |
| Dictionary/fallback | Real 日本/東京/食べる/書く;こう/しょうkana+katakana remain within64candidates; unknown input can confirm raw | NOT RUN |
| Phrase proposals | LearningOFF/private:にほんにいきます/きょうはとうきょうにいきます/すしをたべます;分割labels, raw fallback, long/unknown/stale/cancelled work | NOT RUN |
| Passwords | Text/visible/web/number password; direct syntheticA$1, no local draft/candidates/accessibility echo/learned lookup/records; symbolpages usable | NOT RUN |
| Manual text/body/WebView | Body1 andWEB_EDIT_TEXT0xA1 with/withoutNO_SUGGESTIONS0x80000 andNO_PERSON0x01000000:local draft, first変換opens fixed list, edits close it, no history/records;normal notepad control keeps automatic candidates | NOT RUN |
| URI/email/FORCE_ASCII | URI0x11,email0x21/web0xD1;FORCE_ASCII0x80000000 combinations:ABC default, ASCII usable, switchかな→reading→変換;Unicode rejection leaves draft, no host-validation bypass | NOT RUN |
| Delayed candidates | Dictionary load/learning notification/old segmented completion before request and after field changes cannot reopen candidates;include repeated reading and normal→manual switches | NOT RUN |
| Private/restricted | NO_PERSONAL flags/private keep static conversion without learned reads/writes; NO_SUGG recognized text:explicit fixed conversion/no history or segmented proposals;numeric/phone/date/unknown direct;ASCII defaults preserved; sensitive types prevent learning | NOT RUN |
| Same-editor private settings | Return from settings without fresh onStartInput; current preference invalidates old learned preview/tap/queue; rapidON/OFF never revives old actions | NOT RUN |
| Learning eligibility | DefaultOFF; exact lexical explicit candidate/preview-confirm only; raw/cancelled/failed/phrase choices do not save | NOT RUN |
| Retention/clear | Synthetic clock:1/7/30/90days, expiry/shorten/extend/rollback; clear/Off during writes; old results cannot return; clear preserves dictionary/controls | NOT RUN |
| Storage/failures |128entries/32KiB+32KiBtemp,32pending; corrupt/oversized/interrupted data; failure message and stopped learning; no input-thread file I/O | NOT RUN |
| Host actions | Search/Next/Done/Send/Previous/customlabel/no-enter/multiline; draft Enter confirms first, empty draft executes action | NOT RUN |
| Rotation/lifecycle | Same-session relayout preserves draft; actual framework hide/freshstart cancels unconfirmed draft; confirmed text once; no field/process cross-leak or stale repeat/taps | NOT RUN |
| Host-field defaults | Email/URI/FORCE_ASCII start English local preedit; numeric/phone/date direct; app field switches and fresh sessions obey policy | NOT RUN |
| Logs/persistence | Process logcat/crash buffer has no synthetic text markers; no lexical files withOFF/raw input; only bounded no-backup eligible records with opt-in learning | NOT RUN |
| Backup/transfer | Actual app UID/files/backup/transfer eligibility on approved Android/OEM test; match packaged exclusion audit | NOT RUN |
| Offline/latency | Airplane-mode conversion plus manifest/dependency/outward audits; cold dictionary, touch-to-draft/confirmed text, candidate latency, memory/haptics onS24 | NOT RUN |
| Reported app gate | App/field+OS/OneUI/EditorInfoflags recorded; controlled synthetic normal/repeated conversion and all privacy/cursor/lifecycle cases pass before claiming user report resolved | NOT RUN |

The Android Emulator QA skill informed selected-serial/UI-tree checks. No emulator was available to execute them. Passing JVM/API fixtures or airplane mode alone does not establish native usability or the no-egress boundary.
