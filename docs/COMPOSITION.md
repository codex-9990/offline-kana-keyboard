# Composing-state investigation and 0.1.5 correction

## Report and historical reproductions

The user reported that conversion failed and each character committed in delivered 0.1.2. The affected app and runtime trace remain unknown. No actual user text or device access was needed.

The original service treated missing composing bounds as cursor movement and compared delayed callbacks to the newest cursor. That finished composition and cleared the reading. Moving the original predicate unchanged into the production adapter let the callback fixture reproduce `Expected に; got [empty]` before correction (iteration-0.1.3-reproduction-before-fix.txt).

Independent 0.1.3 review then found two further failures: incremental に/ほ/ん acknowledgements left historical positions eligible, so cursor 3→1 did not end the reading; and stripped spans with delayed updates produced ににほにほん. Both before-fix traces are retained as iteration-0.1.4-cursor-before-fix.txt and iteration-0.1.4-burst-before-fix.txt. Known-span restoration during unacknowledged writes fixes the latter modeled case.

## Coalesced preview/cancel reproduction

Review of the undelivered local 0.1.4 trial found another combination. Type にほん and flush in a host omitting bounds; preview 日本; cancel without flushing; replace queued callbacks with one final 3→3/-1/-1 update. The earliest-match rule acknowledged the earlier region-restoration position 3 instead of the final cancellation position 3. Positions 2/3 remained eligible. A real cursor 3→2 then matched the stale preview, leaving にほん active.

The new fixture reproduces that exact failure on 0.1.4: `Expected [empty]; got にほん`. Evidence is iteration-0.1.5-coalesced-before-fix.txt. This is a source/host-model trace, not an Android observation.

## Current correction and deliberate limit

Current-coordinate acknowledgements consume the ordered prefix through the latest matching operation. They retire old positions even when the callback coalesces a return to the original cursor. The known owned span is still restored before replacement when bounds are missing or earlier writes remain unacknowledged, provided the anchor is trusted.

There is a remaining information limit: these callback coordinates contain no request identity. An old preview 3→2 and a real cursor move 3→2 can be identical after a pending 3→2→3 reversal. The implementation does not keep those historical positions eligible to preserve every delayed preview. Instead, a non-current ambiguous update ends composition, preserves visible text, clears expectations and fences composing/recovery writes until a fresh adapter receives a trusted EditorInfo selection. Unknown resets, privacy toggles and later callbacks cannot unblock it. Overflow of the 64-coordinate bound also marks ordering uncertain. No extra text, timestamp, history file or surrounding read was introduced.

**Conversion can pause after rapid preview/cancel or a shorter commit followed quickly by a new word.** This can happen for a self-generated callback. The keyboard shows `Koto · 変換停止 · 入力欄を切り替えて再開してください`. English/symbol direct input can still write at the host's actual selection without restoring a guessed span. A fresh editor start restores normal kana conversion. This conservative behavior needs independent review and device validation; it is not seamless handling of every callback combination.

Two older fixture groups now check acknowledged commits and preview/cancel transitions. Their formerly unacknowledged reversing sequences are separately tested for text preservation and fenced conversion, rather than silently being claimed as unchanged behavior.

## Field and gesture policy

NO_PERSONALIZED_LEARNING and manual private mode disable learned reads/recording while retaining static dictionary conversion in supported composing cases. Password variants and NO_SUGGESTIONS remain direct input without a reading or learning. Numeric fields remain direct. Kana gestures emit once on release and emit nothing on cancellation. Failed composing/recovery operations do not silently commit accumulated text.

The production service delegates selection updates to ConnectionEditor. See the Android [InputConnection operations](https://developer.android.com/reference/android/view/inputmethod/InputConnection), [selection callback](https://developer.android.com/reference/android/inputmethodservice/InputMethodService#onUpdateSelection(int,%20int,%20int,%20int,%20int,%20int)) and [NO_PERSONALIZED_LEARNING flag](https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_NO_PERSONALIZED_LEARNING).

## Exact evidence and limits

PASS: 30 callback groups and 14 API adapter groups, all 78 core/feature host groups, 17 source checks, 19 actual APK checks, v2/v3 signing, ten-entry payload equality and tamper rejection. Version 0.1.5/code 6 uses the existing certificate.

PASS: 49,760 bounded sequences in one callback group. All operation strings of depth 0–4 over append/preview/cancel/ordered flush/two coalescing forms are crossed with range omission, span stripping, region callbacks, batching and cursor-drain choices. Preview lengths vary with step; cursor targets 0/1/2 are distributed by sequence index. An independent visible-text oracle verifies action semantics, preceding text, callback non-rewrites, safe insertion or rejection, and recovery after a fresh trusted adapter. Additional groups cover interleaved old callbacks, a shorter commit followed by another word, positive/missing aliases, direct-mode input, private toggles, unknown resets and overflow.

EXPECTED FAIL: the original defect and all three review reproductions fail on their respective prior source. They are retained separately from final passing checks.

NOT RUN: real InputMethodService/Looper/Binder ordering, native MotionEvent, the affected app, S24/One UI, rotation/restarts, native storage/backup/transfer and latency; conventional Gradle/lint. The bounded fixture models ordered or coalesced notifications, not arbitrary interprocess scheduling, every host mutation or a hostile editor. Unsupported metadata/operations and callbacks beyond the bound are not a seamless-conversion guarantee. Host apps and compromised OS remain outside the no-egress boundary.

The 0.1.5 artifacts are local and replacement delivery is held pending independent review. No Library write, device installation or public APK release occurred.
