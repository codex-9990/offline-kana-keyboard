# Per-character commit report and 0.1.3 correction

## Observation and concrete cause

The user reported that conversion did not work and every character committed immediately in delivered 0.1.2. The app and runtime callback trace were unavailable for this investigation. No actual user typing or device access was requested or used.

The 0.1.2 service callback treated candidatesStart < 0 as a cursor movement and also compared every incoming cursor to candidatesEnd without accounting for its own pending writes. It called InputSession.selectionMovedOutsideComposition(), which called finishComposingText() and emptied the kana reading. Thus an editor reporting (-1,-1) for its composing range caused the first に to become committed, and subsequent characters could not accumulate for word conversion. Delayed older callbacks could also clear a newer reading/preview.

This is **source-proven and reproduced in a new callback-aware host fixture**. The original callback predicate was first moved unchanged into the adapter/service delegation path; the new test then failed with `Expected に; got [empty]`. That red result is retained as iteration-0.1.3-reproduction-before-fix.txt. The same test passes after correction. This does not prove which callback/field flags the user's app actually sends. Older passing tests did not deliver these callbacks and were insufficient for this behavior.

## Correction

- The service delegates selection updates to ConnectionEditor.updateSelection, the production path exercised by the new tests.
- EditorSelection keeps at most 64 transient coordinate expectations for text writes and composing-region restoration. Expected immediate, delayed and coalesced callbacks do not end the current reading. Negative/absent selection metadata alone is not evidence of a user cursor move.
- Genuine cursor/selection moves and conflicting positive spans finish the composing state while preserving text already visible in the host.
- If the editor omits/strips composing spans, a known owned region is re-established before setComposingText or commitText. This prevents repeated accumulated readings from being appended. Region restoration and replacement use balanced batch calls and also track the restoration callback when batching is unsupported.
- Unknown initial selection can be established from a valid reported span or one acknowledged write with consistent supplied old/new selection. Without a trustworthy anchor, or if the editor rejects recovery/text writes, the operation stops and a static Japanese error is shown. No surrounding-text read, guessed substring replacement or silent immediate-commit fallback is used.
- Pending coordinates contain no strings, are never saved, and are dropped at field/adapter replacement or real external selection changes. Clear/Off/private learning controls and their storage bounds are unchanged.

The InputConnection contract describes composing replacement/region operations and batch-update behavior; see [InputConnection](https://developer.android.com/reference/android/view/inputmethod/InputConnection) and the [selection callback](https://developer.android.com/reference/android/inputmethodservice/InputMethodService#onUpdateSelection(int,%20int,%20int,%20int,%20int,%20int)).

## Field policy and gesture inspection

NO_PERSONALIZED_LEARNING disables learning, not static dictionary conversion. That existing separation was retained and tested through sequential flicks with callback delivery. Manual private mode behaves the same way. Android defines this flag for personalized-data updates; see [EditorInfo](https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_NO_PERSONALIZED_LEARNING).

Password variants and NO_SUGGESTIONS remain direct-input/no-buffer/no-learning under this prototype's existing policy. Numeric fields remain direct. An app requesting those policies or rejecting composing APIs remains outside the tested normal-text conversion cases; the app name/field behavior is needed if the replacement still fails.

Kana gestures emit once on release; cancellation emits nothing. Candidate scheduling does not itself commit text. setComposingText failures retain the previous buffer rather than substitute commitText. finishComposingText remains explicit on commit/hide/finish/rotation or a real external cursor/selection change; it is no longer triggered merely by missing composing-range metadata.

## Evidence and limits

PASS: 16 new callback-aware groups use actual Android API signatures, physical composing-range replacement in a synthetic visible-text buffer, queued/coalesced callbacks, production flick/session/adapter code and the real bundled dictionary. They cover sequential kana → 日本, omitted/stripped spans, preserved prefix text, previews/deletion, cursor selections, private/restricted fields, rejected APIs, unknown anchors, stale commit/finish acknowledgements, recovery callbacks and bounded metadata.

PASS: all 78 prior host groups, 14 prior Android API adapter groups, 17 source policy checks and 19 packaged APK checks; signing/payload/tamper verification passes. Version 0.1.3 uses versionCode 4 and the existing certificate.

NOT RUN: actual InputMethodService/Looper/Binder callbacks, native MotionEvent, user's app, Galaxy S24/One UI, Android rotation/restarts/host filtering and on-device latency. Extremely old callbacks beyond the 64-expectation bound may conservatively end composition. Editors without usable selection/composition metadata or recovery APIs may remain incompatible. The fix is ready for a user-controlled retest, not a device-verified resolution claim.
