# Verification report — 0.1.6 development trial

Application source: **0aa62da1c7428341556a035864fd715be2212d61**. APK SHA-256 **2ba3e39ca74d7da11015be808a1014ed4fcde6e67ff77b4cab332bfe3505d9ff**, **512,678 bytes**, version code **7**. Signed with the existing approved certificate, debuggable=false. **Android emulator, Galaxy S24, the reporting app and conventional Gradle/lint remain NOT RUN.** No device action, new SDK agreement/key or public APK release occurred. Replacement delivery remains held pending independent review.

## Exact results

| Check | Result | Scope |
|---|---|---|
| Production core | PASS — 36 groups | Flick mappings, local preedit/preview, Unicode deletion, explicit confirmation, modes, capacity, restricted/password policies and actions |
| Feature regressions | PASS — 42 groups | Retention/quotas/queue bounds, live private-mode gates, failures/races, real Linux host files, layouts/sensitivity and heuristic phrase proposals |
| ConnectionEditor API adapter | PASS — 14 groups | Actual Android API signatures, synthetic InputConnection, injected native-key model; no native event loop |
| Local-preedit editor | PASS — 23 groups | Separate host text/caret and local oracle; callbacks, flick/dictionary choice, delete/cancel/modes, focus/relayout, privacy, failure/retry |
| Bounded sequences | PASS — 59,048 cases | Every length-0–4 string over nine operations, eight host/range/flush options; exact draft/preview/text/commit counts and accepted continuing key; no lockout accepted |
| Continuous conversion | PASS — 1,000 cycles | 4,000 accepted keys, 2,000 expected confirmations; delayed/coalesced updates; complete expected host text |
| 0.1.5 loss/lockout reproduction | EXPECTED FAIL — retained | にほん → 日本 → accepted ご before callbacks → flush loses ご on c186fb4; separately captured and not counted as final success |
| Older historical reproductions | EXPECTED FAIL — retained | Original missing-bound first-kana loss; 0.1.3 stale cursor and stripped-span duplication; 0.1.4 coalesced preview/cancel |
| Source policy | PASS — 18 checks | Zero permissions, no logs/network/surrounding reads, local preedit/no span reclamation, bounded no-backup learning, exports/intents/dependencies |
| Initial 0.1.6 build | FAIL — corrected | New DEX guard caught deleted EditorSelection.class left in generated output; builder cleanup added; failed log retained |
| Clean API compilation/resources/DEX | PASS | Existing pinned AOSP API 35 jar, AAPT2/D8; no SDK install/agreements |
| Signed APK policy/content | PASS — 20 checks | Actual binary manifest version7/0.1.6, zero permissions, exports, debuggable=false, backup XML, dictionary/assets, DEX namespace/no composing machinery and alignment |
| Signature/certificate | PASS | apksig 8.7.3; v2/v3 API 26–35; one expected signer; zero errors/warnings |
| Signed/unsigned payload | PASS — all 10 ZIP entries | Byte-identical application payloads and ZIP CRC checks |
| Tamper rejection | PASS | Same four-argument verifier accepts valid control and rejects altered DEX with one signature error; temporary copies removed |
| Earlier APK preservation | PASS | Six prior signed APKs 0.1.0–0.1.5 unchanged |
| Unrelated project preservation | PASS | 175 tracked hashes unchanged; clean Git status |
| Conventional Gradle build/lint/merged manifest | NOT RUN | SDK absent; alternative build's actual packaged manifest inspected |
| Android installation/IME registration/Binder/Looper | NOT RUN | No SDK/adb/emulator/KVM; no physical-device authorization |
| Native DEL/cursor, graphemes/WebView, actions | NOT RUN | Actual API signatures compiled; injected host key model only |
| Android views/touch/layout/rotation/lifecycle | NOT RUN | Host session/gesture models pass; native framework and One UI not observed |
| Android storage/backup/device transfer/logs | NOT RUN | Source/artifact controls and real Linux file tests; Android/OEM behavior not observed |
| S24 latency/insets/haptics/airplane mode | NOT RUN | No native runtime claims or measurements |
| Reporting app / user's failure | NOT RUN | App/field/EditorInfo flags unknown; user-visible resolution not established |

## Interpreting the evidence

Review found that the 0.1.5 safety fence could lose an accepted next key during ordinary Japanese typing and halt further kana. That is release-blocking. The current version deletes span/cursor ownership tracking and holds ordinary reading/preview locally until explicit confirmation. No callback can discard, send or permanently fence the draft. Unsupported confirmation operations retain an editable draft; failure handling is not counted as successful conversion in those hosts.

The updated tests require each accepted key exactly once according to explicit edits, exact host text and commit counts, continuous repeated conversion and subsequent accepted input. The 59,048 sequence cases are one test group, not 59,048 separate test groups. The 1,000 continuous cycles are likewise one group. Core/feature groups were updated for local-preedit semantics rather than being claimed unchanged. The model is bounded and synthetic, not arbitrary Binder scheduling, all host mutations or a security audit against a hostile editor.

Normal English/symbol text also stays local until 確定. Space appends rather than confirms. Hide/fresh focus discards unconfirmed input; relayout preserves it only within the same active session, and framework hide/restart on rotation can discard it. These deliberate UX limits and the lack of native deletion/rotation/S24 evidence remain material. The keyboard uses a licensed real dictionary and bounded heuristic phrases, not full Mozc conversion.

## Evidence files

- iteration-0.1.6-host-tests.txt: 36 core +42 feature groups and18 source checks.
- iteration-0.1.6-adapter-tests.txt: 14 adapter +23 local-preedit groups,59,048 cases and1,000 conversion cycles.
- iteration-0.1.6-lockout-before-fix.txt: expected failure on0.1.5.
- iteration-0.1.6-initial-build.txt: failed stale-output guard; iteration-0.1.6-aosp-build.txt: final clean build.
- iteration-0.1.6-apk-policy.txt, signature.json and payload-check.json: actual signed artifact evidence.
- iteration-0.1.6-publication.json and publication-manifest.json: complete approved remote tree comparison; no APK release.
- iteration-0.1.6-protected-project.json: baseline and older-APK preservation proof without unrelated source inventory.
- apk-manifest.xml/badging.txt and backup resource dumps: actual final packaged manifest/resources.

The review archive allowlists public docs and these synthetic/artifact receipts. Source export contains only tracked reviewed project files, no build caches, private signing material, user data or Library identifiers. Delivery guards/identities remain in a separate private local receipt. Native rows stay NOT RUN until results from [RUNTIME_QA.md](RUNTIME_QA.md) and [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md) are recorded. The affected app/S24 retest is required before calling the user's report resolved.
