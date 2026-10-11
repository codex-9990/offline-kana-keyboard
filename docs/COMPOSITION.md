# Local editing and explicit field conversion — 0.1.7

## Evidence and compatibility scope

The user's browser URL/search bar and a page field fail to convert, while a browser-based notepad converts. This suggests field-dependent behavior rather than establishing a browser-wide InputConnection failure. The browser, actual field flags and native API outcomes are unknown. No actual user typing was collected. The policy matrix below is an implementation choice, not a reproduction of that specific browser.

The0.1.6 FieldPolicy treated TYPE_TEXT_FLAG_NO_SUGGESTIONS as direct-only: an accepted syntheticにほん immediately reached the host and there was no editable draft or conversion. A scoped before-fix fixture compiled that exact prior source and fails the new requirement. Version 0.1.7 enables an explicit user request for immutable dictionary candidates in recognized ordinary text, while keeping prediction and learned history separate.

| Field policy | Initial mode | Unconfirmed draft | Candidate behavior | Learned query/record |
|---|---|---|---|---|
| Normal text / WEB_EDIT_TEXT | Kana | Local RAM | Automatic fixed dictionary and bounded segmented proposals | Eligible only if enabled and not private |
| Normal text / WEB_EDIT_TEXT + NO_SUGGESTIONS | Kana | Local RAM | Fixed dictionary only after変換 | Disabled independently |
| URI / email / web email | English | Local RAM | Switchかな, type reading, request変換; fixed dictionary only | Disabled |
| Recognized text + FORCE_ASCII, with/without NO_SUGGESTIONS | English | Local RAM | Same explicit kana request; ASCII keys remain accessible | Disabled |
| NO_PERSONALIZED_LEARNING / manual private mode | Underlying field | Underlying field | Static conversion remains; underlying explicit restriction retained | Disabled |
| Text/visible/web/number password;number/phone/datetime;null/unknown class or variation | English/symbols as appropriate | None | Direct input; no explicit conversion or draft echo | Disabled |

NO_SUGGESTIONS requests omission of suggestion UI; FORCE_ASCII requests an IME capable of ASCII/Roman entry. Offering fixed conversion after a user request is our compatibility design. FORCE_ASCII is not itself an ASCII-only validation rule; a host can still reject Unicode or unsupported commits. We preserve its English default/ASCII keys, do not encode/rewrite Unicode or bypass host validation. Unknown text variations are conservatively direct-only here; Android generally recommends assuming normal for an unknown variation, so this stricter restriction is our privacy choice.

## Explicit request state and delayed work

InputSession distinguishes conversion eligibility from an open list. 変換 calls requestConversion **before** checking candidates.isEmpty, so its first press opens the list and previews a choice without needing prior candidates. Before a request, CandidateList returns empty before dictionary/history assembly. Explicit-only fields include fixed dictionary candidates and kana/katakana fallbacks; history suppliers are never invoked and stale segmented results are excluded.

Actual reading edits, mode/private changes, confirmation, cancel, hide, finish or fresh field/restart close the request. First Backspace cancelling a preview keeps the unchanged reading's request; the next deletion closes it. Capacity rejection retains the accepted draft/preview. Arrows and same-session relayout retain the request but advance revisions, rejecting old buttons. Failed host confirmation retains the editable draft/request; there is no ambiguity fence or permanent lockout.

Dictionary-load completion and learning notifications rebuild from the current session, never open a request. Phrase work is cancelled at field/lifecycle clearing and in explicit-only fields. Delayed phrase delivery requires current phrase eligibility and matching current/requested reading, plus the worker generation gate. Static taps do not require unchanged learning generation; learned taps retain generation/privacy/expiry checks. Manual-only fields do not trigger learned count scans or field-start maintenance. App-wide initialization/cleanup of an already enabled store is separate from field-dependent queries and receives no field text.

## Local draft workflow retained from0.1.6

- Ordinary kana/English/symbol input stays in keyboard RAM, visible up to256 code points; no host composing span or surrounding read.
- Candidate tap or確定 commits explicitly;変換 previews locally. Typing after preview appends once to the original reading. First Backspace cancels preview, later deletes a code point;取消 discards the draft.
- Space appends; modes retain reading/cancel preview. Arrows move actual host caret while retaining draft. Confirmation uses the host's actual selection.
- ConnectionEditor finishes host-owned composing style without replacing its text, then commits at actual selection. It never reclaims a span, guesses coordinates or supplies unconfirmed text. Rejected/unsupported finish/commit leaves local edit/cancel/retry available.
- Empty-draft DEL/arrows use native soft-keyboard events. Native grapheme/WebView dispatch is untested; the API fixture injects a key model.
- Hide/fresh focus/restart/finish cancels unconfirmed input. Same-session relayout preserves it. Framework hide/restart during rotation may cancel it. Host sees only confirmed text: English/symbols also need confirmation and host autocomplete cannot see the draft.

Corrected0.1.6 Preedit snapshots/provenance remain: learned expiry/clear invalidation occurs before text/accessibility rendering, including after relayout. Those regression groups pass. Learning stays defaultOFF, optional exact lexical explicit choices only, with independent field/private/worker gates.

## Verification and remaining gate

Application **3cd0f7dd1f04f5e2e985f909d0f520695b25f074**,0.1.7/code8. PASS:42core+51feature;14adapter+29local-preedit API groups.59,048 ordinary and177,776 explicit-only depth0–4 sequences require exact draft/request/preview/host/commit state, then a further accepted/confirmed key.3,000 continuous cycles account for12,000 accepted keys/6,000 confirmations with exact host text. Ignoring accepted keys or lockout fails.

Browser/S24/Binder/Looper/views/touch/rotation/storage/backup and Gradle/lint remain NOT RUN. Historical missing-bound/cursor/duplication/coalescing and0.1.5 accepted-key-loss traces are retained as EXPECTED FAIL, not current passes. The0.1.7 packet is held for independent review; no Library replacement. [Exact report](VERIFICATION.md), [runtime gate](RUNTIME_QA.md).

Primary contracts: [NO_SUGGESTIONS](https://developer.android.com/reference/android/text/InputType#TYPE_TEXT_FLAG_NO_SUGGESTIONS), [input classes/variations](https://developer.android.com/reference/android/text/InputType), [FORCE_ASCII](https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_FORCE_ASCII), [NO_PERSONALIZED_LEARNING](https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_NO_PERSONALIZED_LEARNING), [finishComposingText](https://developer.android.com/reference/android/view/inputmethod/InputConnection#finishComposingText()), [IME lifecycle](https://developer.android.com/reference/android/inputmethodservice/InputMethodService). These do not establish universal host compatibility.
