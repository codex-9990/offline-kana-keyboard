# Composing-state reports and 0.1.4 correction

## Observation and concrete cause

The user reported that conversion did not work and every character committed immediately in delivered 0.1.2. The app and runtime callback trace were unavailable for this investigation. No actual user typing or device access was requested or used.

The 0.1.2 service callback treated candidatesStart < 0 as a cursor movement and also compared every incoming cursor to candidatesEnd without accounting for its own pending writes. It called InputSession.selectionMovedOutsideComposition(), which called finishComposingText() and emptied the kana reading. Thus an editor reporting (-1,-1) for its composing range caused the first に to become committed, and subsequent characters could not accumulate for word conversion. Delayed older callbacks could also clear a newer reading/preview.

This is **source-proven and reproduced in a new callback-aware host fixture**. The original callback predicate was first moved unchanged into the adapter/service delegation path; the new test then failed with `Expected に; got [empty]`. That red result is retained as iteration-0.1.3-reproduction-before-fix.txt. The same test passes after correction. This does not prove which callback/field flags the user's app actually sends. Older passing tests did not deliver these callbacks and were insufficient for this behavior.

## Independent 0.1.3 review regressions

1. Separate に/ほ/ん writes with callbacks flushed after each left acknowledged positions 1, 2 and 3 eligible forever. A real cursor move 3→1 with missing composing bounds matched position 1 through the last-reported-cursor rule. The old reading remained active; a later key could restore/replace the old span instead of insert at the actual cursor. The previous cursor test used one にほん write and missed this case.
2. With stripSpan=true and omitRange=true, sending に/ほ/ん before callbacks were flushed appended ににほにほん. Recovery waited for a missing-range acknowledgement, so it never restored the lost span during the burst. The previous delay test did not strip spans, and the span-loss test flushed after every key.

Both are deterministic source/host-fixture traces, not observations from Android. Both fail before correction; their logs are retained as iteration-0.1.4-cursor-before-fix.txt and iteration-0.1.4-burst-before-fix.txt. Version 0.1.3 should not be used for daily typing. The 0.1.4 trial still needs runtime validation.

## Current correction

- The service delegates selection updates to ConnectionEditor.updateSelection, the production path exercised by the new tests.
- EditorSelection keeps at most 64 pending transient coordinate expectations for text writes and composing-region restoration. An acknowledgement consumes the earliest matching ordered prefix, including operations coalesced into that update. Acknowledged historical positions no longer remain eligible. A delayed earlier update cannot acknowledge a later preview/cancel operation that happens to share its position. Tested immediate, delayed and coalesced callbacks preserve the reading; absent metadata alone is not evidence of movement.
- Genuine cursor/selection moves and conflicting positive spans finish the composing state while preserving text already visible in the host.
- A known owned region is re-established before replacement when the range is missing **or prior writes remain unacknowledged**. A successful write request does not prove the host retained its span. This prevents repeated accumulated readings from being appended during the tested delayed stripped-span bursts. Restoration/replacement uses balanced batch calls and tracks restoration callbacks when batching is unsupported.
- Unknown initial selection can be established from a valid reported span or one acknowledged write with consistent supplied old/new selection. Further unacknowledged writes with an unknown anchor stop rather than duplicate/guess. Rejected recovery/text writes also stop with a static Japanese error. No surrounding-text read, guessed substring replacement or silent immediate-commit fallback is used.
- Pending coordinates contain no strings, are never saved, and are dropped at field/adapter replacement or real external selection changes. Clear/Off/private learning controls and their storage bounds are unchanged.

The InputConnection contract describes composing replacement/region operations and batch-update behavior; see [InputConnection](https://developer.android.com/reference/android/view/inputmethod/InputConnection) and the [selection callback](https://developer.android.com/reference/android/inputmethodservice/InputMethodService#onUpdateSelection(int,%20int,%20int,%20int,%20int,%20int)).

## Field policy and gesture inspection

NO_PERSONALIZED_LEARNING disables learning, not static dictionary conversion. That existing separation was retained and tested through sequential flicks with callback delivery. Manual private mode behaves the same way. Android defines this flag for personalized-data updates; see [EditorInfo](https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_NO_PERSONALIZED_LEARNING).

Password variants and NO_SUGGESTIONS remain direct-input/no-buffer/no-learning under this prototype's existing policy. Numeric fields remain direct. An app requesting those policies or rejecting composing APIs remains outside the tested normal-text conversion cases; the app name/field behavior is needed if the replacement still fails.

Kana gestures emit once on release; cancellation emits nothing. Candidate scheduling does not itself commit text. setComposingText failures retain the previous buffer rather than substitute commitText. finishComposingText remains explicit on commit/hide/finish/rotation or a real external cursor/selection change; it is no longer triggered merely by missing composing-range metadata.

## Evidence and limits

PASS: 24 callback-aware groups use actual Android API signatures, physical composing-range replacement in a synthetic visible-text buffer, queued/coalesced callbacks, production flick/session/adapter code and the real bundled dictionary. Eight added groups combine incremental cursor moves, missing/positive ranges, prefix preservation, dropped spans, delayed/coalesced bursts, restoration callbacks, preview cancellation and acknowledgement consumption. The original 16 groups also pass, including private/restricted fields and unsupported recovery.

PASS: all 78 prior host groups, 14 prior Android API adapter groups, 17 source policy checks and 19 packaged APK checks; signing/payload/tamper verification passes. Version 0.1.4 uses versionCode 5 and the existing certificate.

NOT RUN: actual InputMethodService/Looper/Binder callbacks, native MotionEvent, user's app, Galaxy S24/One UI, Android rotation/restarts/host filtering and on-device latency. Extremely old callbacks beyond the 64-expectation bound may conservatively end composition. Editors without usable selection/composition metadata or recovery APIs may remain incompatible. The fix is ready for a user-controlled retest, not a device-verified resolution claim.
