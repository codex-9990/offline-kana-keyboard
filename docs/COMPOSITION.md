# Keyboard-local preedit — 0.1.6

## Why the host-span approach was removed

The user reported conversion failure and one-character commits in delivered 0.1.2. The affected app, field flags and device runtime trace are still unknown. A retained synthetic before-fix trace reproduces the missing-bound/delayed-callback predicate clearing the first に. Later reviews found acknowledged-position reuse, stripped-span duplication and coalesced preview/cancel stale positions. Historical traces remain in the verification packet; none proves behavior in the user's app.

Independent review rejected the 0.1.5 ambiguity fence as a usable fix. In the normal sequence にほん → explicitly choose 日本 → type ご before callbacks → flush, the accepted ご was lost and further kana could be blocked. **iteration-0.1.6-lockout-before-fix.txt** reproduces this on source c186fb47a0f4ff086444d3908994291d4dabf213. This was release-blocking, not an acceptable passing usability result.

The retired fence became sticky on non-current ambiguous callbacks after a coordinate reversal or a coordinate-bound overflow. A newly accepted kana could vanish when an earlier shorter commit's callback cleared the session. Current/idle callbacks, private-mode changes and keyboard hide/show did not guarantee clearing the latch: only a fresh adapter with trusted EditorInfo selection did. InputMethodManager.restartInput needs the served host View; an IME cannot promise a generic same-field restart. Trying to distinguish an old preview callback from real host movement using identical six integers had no reliable request identity. The tracker and all composing-region restoration calls are now deleted.

## Current operation contract

Every fresh session uses keyboard-local preedit. The ordinary reading and candidate preview stay in IME RAM and a visible scrollable draft row. The host receives no ordinary typed or preview text until the user taps a candidate or presses 確定. English and symbols follow the same rule. Password, numeric and NO_SUGGESTIONS fields commit keys directly without a draft or candidates.

InputSession.type returns true only after a key is accepted once into the local draft or a direct host write succeeds. The draft is capped at 256 code points. Capacity rejection is explicit and shown in the UI; confirm, delete or cancel permits more input. No automatic commit or hidden permanent lockout is used. Confirmation rejection retains the editable draft; it is not counted as a usable unsupported-host conversion.

- 変換 changes only the local preview. First Backspace cancels it; further Backspace removes a code point from the reading. Typing cancels the preview and appends once to the original reading. 取消 discards the whole draft without sending it.
- Space appends to the local reading; it does not confirm. Mode changes cancel the preview and keep the reading. Neither typing nor mode switching sends a candidate implicitly.
- Cursor arrows act on the host's actual caret using native soft-keyboard key events while preserving the draft. Confirmation inserts/replaces at the host's actual current selection. Callback coordinates are ignored for draft ownership; they cannot discard or fence it.
- Before an explicit host commit the adapter calls finishComposingText. This removes any host-owned composing style without replacing its text, then commitText operates at the actual selection. It never reads surrounding text, sets a composing span, restores a region or guesses a prior selection. If finish/commit is unsupported or rejects the operation, the failure is displayed and local editing/cancellation remains available.
- Empty-draft deletion uses native DEL, and cursor movement uses DPAD keys, not a cached selection. Actual native dispatch, grapheme behavior and WebView compatibility require runtime QA; the host fixture injects a key sender.
- Hiding the view, field finish or fresh onStartInput discards unconfirmed RAM. Relayout within the same active session preserves it and invalidates old candidate taps. A framework hide/restart during rotation can cancel it; real rotation is untested. Previously confirmed host text is never reclaimed.

The main UX cost is that the host shows only confirmed text; ordinary English/symbol typing also needs 確定. There is no in-host underline, segment editing or host autocomplete for the unconfirmed draft. Hide/fresh-focus cancellation can lose deliberately unconfirmed input. These tradeoffs are documented before any replacement delivery.

## Privacy and policy

NO_PERSONALIZED_LEARNING and manual private mode retain static local conversion but block learned lookup/recording. Live preferences independently gate retained candidate snapshots and queued persistence, including a return from settings to the same editor. Cancelling a learned preview is now a local operation and does not depend on host restoration. Password variants and NO_SUGGESTIONS retain direct input, no displayed draft or candidate state. Only successful explicit dictionary-exact choices can learn; raw text, generated phrases and cancelled/failed confirmations cannot.

No coordinate history, input log, additional persistence or surrounding read was introduced. The preedit bound is transient RAM, separate from the existing 128-entry/32-KiB learning bounds. Host apps and a compromised OS remain outside the keyboard's no-egress boundary.

## In-app instructions and learned-preview projection review

Independent review of the first held0.1.6 packet found that SettingsActivity still described host-field preview, implicit confirmation by the next kana/ABC/arrows/space, and delivery of unconfirmed text. Those statements are removed. The help now explains ordinary kana/English/symbol local drafts; explicit candidate/確定 confirmation; typing/mode/space/cursor behavior; local cancel;256capacity; hide/fresh-focus/restart cancellation and restricted direct entry. The obsolete host-restoration error hint is removed too.

The review also found refreshCandidates rendering preedit before invalidating an expired/cleared learned preview. The screen/accessibility could retain日本 while the session reverted toにほん. InputSession.preedit now cancels a non-current preview before returning one immutable text/description/visibility snapshot. The service computes current learned validity first and consumes that snapshot. Active preview provenance survives candidate-row clearing/relayout, so later expiry/clear checks remain available; cancelled/finished/fresh sessions clear it.

Three new feature groups test expiration after relayout, clear/Off before or after worker completion, and valid/restricted projection. The projected visible text and accessibility description must reflect the reading before explicit confirmation, and that exact text must be committed. Stale actions are rejected and cancellation cannot relearn the invalidated choice. These are production snapshot/coordinator host tests plus inspected/compiled view binding, not a native TextView/accessibility execution result.

Four help guards run on source constant text and the actual packaged DEX: input, learning, privacy and absence of retired claims. The prior source fails all four; the archived previously signed APK fails the new packaged-input-help guard.32negative mutations of required behavior/sections/retired claims are rejected. The original0.1.6 artifacts are preserved locally under dist/archive/0.1.6-before-help-review and have never been uploaded. Version0.1.6/code7 remains a held trial; only the current source/APK hashes identify this correction.

## Exact verification

Application source: **a91012fa2d5b300c87032835d16fa41b6cf9b930**, version 0.1.6/code 7. PASS: **36 core + 45 feature groups; 14 adapter + 23 local-preedit API host groups; 22 source checks; 24 signed APK checks**, v2/v3 signature, ten-entry payload equality and controlled tamper rejection.

The API fixture models the physical host text/selection separately from an independent local-draft/preview/confirmation oracle. All **59,048** depth-0–4 sequences over nine operations (type, preview, cancel, delete, arrow, confirm, ordered flush, reported-position coalescing and no-motion coalescing), across eight range-omission/host-span/flush options, require accepted type calls to succeed, exact expected text and confirmation counts, then a further accepted/confirmed key. **Ignored accepted keys or lockout fail.** This is not exhaustive Android scheduling or a hostile-host proof.

The exact 日本 → ご-before-callbacks sequence passes with ご retained locally and then confirmed once. **1,000** repeated delayed/coalesced conversion cycles account for **4,000 accepted keys and 2,000 confirmations**, with whole host-text equality. Separate groups cover preview/cancel stale 3→2 callbacks, local deletion/modifiers/modes, native caret/selected range, focus, relayout/hide, restricted direct input, private/NO_PERSON flags, failure/retry, stale choices and the capacity bound. Older tests were updated to the new explicit-local semantics; their unchanged counts do not claim unchanged inline behavior.

EXPECTED FAIL: the retained 0.1.5 normal-input loss trace and older historical reproduction traces. FAIL, corrected: the first 0.1.6 build packaged a deleted compiled class from stale generated output; the new DEX guard caught it. The builder now clears only its generated app/build/aosp-debug directory and the final clean build passes.

NOT RUN: Android InputMethodService/Binder/Looper ordering, real native key/MotionEvent dispatch, the reporting app, S24/One UI, actual rotation/focus, storage/backup/transfer and latency; conventional Gradle/lint. Passing fixture results do **not** establish that the user's report is resolved. [RUNTIME_QA.md](RUNTIME_QA.md) defines the minimal remaining gate.

## Android contracts consulted

Android documents that [finishComposingText](https://developer.android.com/reference/android/view/inputmethod/InputConnection#finishComposingText()) leaves text/cursor intact while removing composing state. The [IME lifecycle and selection callback](https://developer.android.com/reference/android/inputmethodservice/InputMethodService), [restartInput(View)](https://developer.android.com/reference/android/view/inputmethod/InputMethodManager#restartInput(android.view.View)) and [NO_PERSONALIZED_LEARNING](https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_NO_PERSONALIZED_LEARNING) were checked. The local-preedit design is our implementation decision; those contracts do not establish behavior in every host.

The 0.1.6 APK and review packet are held locally pending independent review. No Library replacement, physical-device action or public APK release occurred.
