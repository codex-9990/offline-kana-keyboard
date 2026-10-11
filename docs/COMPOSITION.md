# Local editing and explicit field conversion — 0.1.8

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

NO_SUGGESTIONS requests omission of suggestion UI; FORCE_ASCII requests an IME capable of ASCII/Roman entry. Offering fixed conversion after a user request is our compatibility design. FORCE_ASCII is not itself an ASCII-only validation rule; a host can still reject or silently filter Unicode or unsupported commits. We preserve its English default/ASCII keys, do not encode/rewrite Unicode or bypass host validation. Unknown text variations are conservatively direct-only here; Android generally recommends assuming normal for an unknown variation, so this stricter restriction is our privacy choice.

## Explicit request state and delayed work

InputSession distinguishes conversion eligibility from an open list. 変換 calls requestConversion **before** checking candidates.isEmpty, so its first press opens the list and previews a choice without needing prior candidates. Before a request, CandidateList returns empty before dictionary/history assembly. Explicit-only fields include fixed dictionary candidates and kana/katakana fallbacks; history suppliers are never invoked and stale segmented results are excluded.

Actual reading edits, mode/private changes, confirmation, cancel, hide, finish or fresh field/restart close the request. First Backspace cancelling a preview keeps the unchanged reading's request; the next deletion closes it. Capacity rejection retains the accepted draft/preview. Arrows and same-session relayout retain the request but advance revisions, rejecting old buttons. An observable failed dispatch retains the editable draft/request; there is no ambiguity fence or permanent lockout. A true dispatch result is not proof that the host applied text, so a later filtered/dropped command can clear the draft without visible text.

Dictionary-load completion and learning notifications rebuild from the current session, never open a request. Phrase work is cancelled at field/lifecycle clearing and in explicit-only fields. Delayed phrase delivery requires current phrase eligibility and matching current/requested reading, plus the worker generation gate. Static taps do not require unchanged learning generation; learned taps retain generation/privacy/expiry checks. Manual-only fields do not trigger learned count scans or field-start maintenance. App-wide initialization/cleanup of an already enabled store is separate from field-dependent queries and receives no field text.

## Local draft workflow retained from0.1.6

- Ordinary kana/English/symbol input stays in keyboard RAM, visible up to256 code points; no host composing span or surrounding read.
- Candidate tap or確定 commits explicitly;変換 previews locally. Typing after preview appends once to the original reading. First Backspace cancels preview, later deletes a code point;取消 discards the draft.
- Space appends; modes retain reading/cancel preview. Arrows move actual host caret while retaining draft. Confirmation uses the host's actual selection.
- ConnectionEditor finishes host-owned composing style without replacing its text, then commits at actual selection. It never reclaims a span, guesses coordinates or supplies unconfirmed text. Detected finish/commit dispatch failure leaves local edit/cancel/explicit retry available. A true remote result can precede host filtering/drop; application is not acknowledged.
- Empty-draft DEL/arrows use native soft-keyboard events. Native grapheme/WebView dispatch is untested; the API fixture injects a key model.
- Hide/fresh focus/restart/finish cancels unconfirmed input. Same-session relayout preserves it. Framework hide/restart during rotation may cancel it. Host sees only confirmed text: English/symbols also need confirmation and host autocomplete cannot see the draft.

Corrected0.1.6 Preedit snapshots/provenance remain: learned expiry/clear invalidation occurs before text/accessibility rendering, including after relayout. Those regression groups pass. Learning stays defaultOFF, optional exact lexical explicit choices only, with independent field/private/worker gates.

## Host acknowledgement and protected direct kana

ConnectionEditor uses InputConnection.finishComposingText/commitText and its observable boolean. The actual API35 remote invoker returns true when it dispatches a void command without RemoteException; it has no host application result. Consequently a command can be dispatched once, the local draft cleared and eligible learning notified, while a host later filters or drops the text. Detected failure retains an ordinary draft, but the keyboard cannot reconstruct it after a reported successful dispatch. It neither reads surrounding/selected host text nor automatically resends. No post-confirmation recovery copy was added. Confirm the result in the host UI.

Protected/direct kana opens a fixed complete-character palette with26 voiced/semivoiced and12 small kana, hiragana/katakana selection and a return key. Each choice dispatches one precomposed character; base flick kana can also use katakana within that field. It never recalls or reads the prior password character. This appends a character rather than transforming the preceding one; explicitly delete a mistyped base character in the host before selecting its complete replacement. Fresh fields reset palette/script state. No password draft, lexical lookup, learning or echo is introduced.

Settings/IME roots block full and partial obscured touch gestures, cancel active child controls and suppress tails until a fresh DOWN or lifecycle reset. Holding delete/arrows cannot continue after a swallowed obscured release in the model. Accessibility click/flick remains available. Native WindowManager reachability and One UI behavior remain untested.

## Verification and remaining gate

Application **679cc413005fe15ff53b39a0ee906646d96462d8**,0.1.8/code9. PASS:42core+51feature+9hardening;14adapter+29local-preedit+4remote-dispatch API groups;18production UI-model groups.59,048 ordinary and177,776 explicit-only depth0–4 sequences require exact draft/request/preview/host/commit state, then a further accepted/confirmed key.3,000 continuous cycles account for12,000 accepted keys/6,000 confirmations with exact host text. Ignoring accepted keys or lockout fails.

Browser/S24/Binder/Looper/views/touch/rotation/storage/backup and Gradle/lint remain NOT RUN. Historical missing-bound/cursor/duplication/coalescing and0.1.5 accepted-key-loss traces are retained as EXPECTED FAIL, not current passes. The previous0.1.7 was delivered and is preserved; its exact audit is separately saved. The0.1.8 replacement is held for independent review; no APK Library replacement. [Exact report](VERIFICATION.md), [runtime gate](RUNTIME_QA.md).

Primary contracts: [NO_SUGGESTIONS](https://developer.android.com/reference/android/text/InputType#TYPE_TEXT_FLAG_NO_SUGGESTIONS), [input classes/variations](https://developer.android.com/reference/android/text/InputType), [FORCE_ASCII](https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_FORCE_ASCII), [NO_PERSONALIZED_LEARNING](https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_NO_PERSONALIZED_LEARNING), [finishComposingText](https://developer.android.com/reference/android/view/inputmethod/InputConnection#finishComposingText()), [IME lifecycle](https://developer.android.com/reference/android/inputmethodservice/InputMethodService). Also see the [AOSP API35 remote invoker](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-15.0.0_r1/core/java/android/inputmethodservice/IRemoteInputConnectionInvoker.java) for the dispatch contract. These sources do not establish universal host compatibility.
