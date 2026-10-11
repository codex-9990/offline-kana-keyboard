# Verification report — 0.1.3 development trial

Application source: **479f18ccbf50a92d7e41fa907281392198c4fbc8**. The APK is signed with the existing approved certificate and is not debuggable. **Android emulator, Galaxy S24 and conventional Gradle/lint checks remain NOT RUN.** No device was changed, SDK agreement accepted, key created or public APK release published.

## Exact results

| Check | Result | Scope |
|---|---|---|
| Existing production-core regressions | PASS — 36 groups | Flick mappings, input/composition, password/restricted fields, actions, candidate/Unicode safety |
| New feature regressions | PASS — 42 groups | Retention, quotas, queue bounds, stale choices, failure recovery, real host files, layouts, sensitivity, phrase proposals |
| Production ConnectionEditor API adapter | PASS — 14 groups | Actual Android API signatures; synthetic InputConnection and injected key sender |
| Callback-aware composing editor | PASS — 16 new groups | Visible text/span replacement, actual API signatures, queued/coalesced selection callbacks, flick release and dictionary selection |
| Before-fix reproduction | EXPECTED FAIL — reproduced | 0.1.2's unchanged callback predicate clears the first に; captured separately, not counted as a final passing check |
| Source policy | PASS — 17 checks | No network/logging/surrounding reads; six non-text controls; file I/O confined to fixed-name no-backup adapter on worker |
| Java API compilation/resources/DEX | PASS | Existing pinned AOSP API 35 jar, Maven AAPT2 and D8 |
| Signed APK policy/content | PASS — 19 checks | Version code 4/name 0.1.3, zero permissions, debuggable=false, exports, backup XML, dictionary/assets, DEX/alignment |
| Signature/certificate | PASS | apksig 8.7.3; v2/v3, API 26–35, one expected signer, zero errors/warnings |
| Signed/unsigned payload equality | PASS — all 10 ZIP entries | Application payload bytes identical |
| Tamper rejection | PASS | DEX byte changed with signature block retained; verifier rejected it; temporary copy removed |
| Earlier APK preservation | PASS | Original 0.1.0, 0.1.1 and delivered 0.1.2 hashes unchanged |
| Unrelated project preservation | PASS | 175 tracked files unchanged; clean Git status |
| Gradle build/lint/merged manifest | NOT RUN | SDK absent; alternative build's actual packaged manifest inspected |
| Android install/IME registration/native events | NOT RUN | No SDK/emulator/KVM; no physical-device approval |
| Android controls/layout/sensitivity try pad | NOT RUN | Layout/gesture core and API compilation pass; actual View/One UI behavior unobserved |
| Android learning storage/backup/device transfer | NOT RUN | Real Linux host file adapter tested; app UID/filesystem/OEM behavior unobserved |
| Android rotation/focus/repeat/composition/editor actions | NOT RUN | Host state/adapter models pass; framework timing and native dispatch unobserved |
| Runtime logs/files, airplane mode, S24 latency/insets/haptics | NOT RUN | No Android runtime assertions claimed |

No unresolved failure remains in final executed checks. The user's 0.1.2 report exposed a composition callback case that the earlier host tests did not cover. The new regression fails before the fix and passes with metadata tracking/span restoration. This establishes the modeled editor cases, not successful behavior in the user's actual app. The earlier phrase correction also does not establish broad natural-language accuracy.

## New test evidence

The production service delegates selection callbacks to the adapter path under test. Missing candidate-range metadata and delayed self-generated callbacks previously called finishComposingText and emptied the reading. New transient coordinate tracking distinguishes expected writes from actual cursor/selection moves, including region-restoration callbacks. A known owned span is restored before replacing it, avoiding duplicated accumulated kana when an editor strips spans. Unsupported recovery or missing trustworthy anchors reject the operation and show a static error; there is no silent per-character commit fallback or surrounding-text read. At most 64 coordinate records are kept, without strings or persistence. See [COMPOSITION.md](COMPOSITION.md).

Sixteen new groups verify normal/omitted/dropped spans, prefix preservation, immediate/delayed/coalesced callbacks, shorter candidate previews, cancellation/deletion, actual cursor/selection moves, NO_PERSONALIZED_LEARNING/manual private conversion without learning, password/NO_SUGGESTIONS direct input, rejected APIs, initial unknown selection, missing anchors, old commit/finish callbacks, restoration with unsupported batching, conflicting positive ranges and bounded metadata. Native Android/One UI callback timing remains NOT RUN.

Tests verify Off/1/7/30/90 policies, expiry at the boundary, no renewal on lookup, shortening, no revival on extension/clock rollback, private/restricted fields, explicit successful choices only, prefix metadata retaining the complete reading, 10,000 repeated choices coalesced to one write, 32 pending choices/one worker/one notification, 128-entry and 32-KiB deterministic eviction, reload, malformed/oversized/duplicate/truncated storage, stale generations, clear during a pending or active write, Off/startup cleanup, simulated write/delete/policy failures, and cleanup guards.

Independent source review found that settings could enable private mode without updating an editor that remained active. The corrected implementation listens for the setting, resynchronizes input-view start/render/edit paths, and gates learned reads/recording/worker choices on the live preference. Six added host regressions cover same-editor synchronization, stale taps/previews, unavailable preferences, rejected restoration, delayed callbacks, cancelled queues, rapid toggles and changes before persistence. A pre-toggle file operation past its final check can finish; earlier records are not erased by private mode. The Android lifecycle reproduction and framework callback ordering remain NOT RUN.

Real host-file tests exercise the production Java NIO adapter: atomic replacement, interrupted temporary-file removal, oversized-file rejection, and deletion that leaves synthetic dictionary/control files unchanged. These are Linux host tests, not Android private-storage tests or secure-erasure proofs.

Flick tests cover 8/22/40 dp at multiple densities, tap/flick boundary, all axes/diagonal ties, edge origins outside keys, threshold changes cancelling an active gesture and held-key cancellation without duplicate release. Layout tests exercise the production keypad/QWERTY specifications. Native MotionEvent, density metrics, accessibility and settings click dispatch remain unrun.

Real SKK data verifies 日本に行きます, 今日は東京に行きます, and sushi + を食べます proposals. Oversized/cancelled conversion returns safely, generated phrases are ineligible for learning, kana fallbacks survive the cap, and the phrase worker discards old/cancelled fields while coalescing bursts. Phrase conversion is independent of learning storage.

## Privacy and proportional limits

Learning now exists **only by opt-in**; historical absence-of-persistence statements apply to 0.1.0/0.1.1. Eligibility checks protect field flags and manual private mode, but cannot infer sensitivity in an ordinary unflagged field. Only dictionary-exact lexical choices can be saved; generated phrases and arbitrary typing cannot. App-private no-backup records are plaintext, not cryptographically protected against OS/root compromise.

Expired records are rejected from memory reads; file pruning is queued on start/use. An app that is stopped does not physically delete on the expiry clock tick. Clear success means the fixed files were removed through the file API; media-level secure erasure and copies already held by the host/OS are not promised. If storage/policy operations fail, the current process stops learning, reports failure and attempts cleanup. A successfully persisted cleanup guard makes the next process clear before use; durable changes cannot be guaranteed when all policy writes fail.

Managed learning data is capped at 32 KiB, plus one 32-KiB temporary file during replacement. Preferences, filesystem metadata and Android-managed installation/runtime files are additional. No app-created disk dictionary cache or input log exists. The fixed dictionary is 1,209,080 uncompressed bytes; APK 512,678 bytes. Installed disk use and S24 latency are NOT MEASURED.

The host receives text and can transmit it. Compromised OS/root and privileged observers remain outside scope. Whole combining/ZWJ grapheme deletion, selection callback timing and sustained typing remain device-test gates.

## Deliverables and runtime gate

Selected evidence includes iteration-0.1.3 host/adapter/build/APK-policy logs, signature/payload receipts, actual manifest/backup dumps, source publication receipt and unchanged-project proof. Source and report exports exclude keys, credentials, private baseline inventories, Library identifiers and build caches.

See [LOCAL_LEARNING.md](LOCAL_LEARNING.md), [CONVERSION.md](CONVERSION.md), [SIGNING.md](SIGNING.md) and [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md). The earlier [0.1.0/0.1.1 audit](SECURITY_AUDIT.md) is historical. This update has no independent Android runtime audit or daily-use readiness claim.
