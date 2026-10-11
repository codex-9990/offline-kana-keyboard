# Threat model — 0.1.8

## Goal and scope

Koto performs kana input, dictionary lookup and bounded heuristic phrase proposals locally. Ordinary input flows gesture → keyboard RAM/local draft → explicit confirmation → Android InputConnection → host editor. Password, number/phone/datetime and null/unknown types use direct key commits. Recognized text with NO_SUGGESTIONS allows local editing and fixed-dictionary conversion only after an explicit request;unsolicited/segmented candidates and learned lookup/recording stay disabled. URI/email/FORCE_ASCII retain English default/ASCII access and the same manual conversion policy. Optional learning retains only eligible explicit dictionary-exact reading/word/last-selection records. There is no conversion download or cloud request.

This development trial's source/artifact checks establish a bounded application privacy design, not an absolute guarantee on a compromised device. Airplane-mode functionality alone would not establish no-egress. Native Android/S24 validation remains NOT RUN.

## Assets and possible observers

Sensitive assets are the transient reading/preview/candidates and supplied text; opt-in learned lexical pairs are deliberately retained. Local preedit has at most 256 code points and is not saved. Static-dictionary/candidate/phrase state remains transient. Six non-text preferences control height, haptics, flick distance, private mode, retention days and cleanup-required state. Learning defaults off.

Possible observers include network SDKs, malicious IME binders, backup/transfer agents, log collectors, speech/clipboard collectors, host apps, accessibility/screen capture, privileged ADB/root and a compromised OS. The application addresses its own egress, service access, persistence, backup, logging and optional-feature paths. Host and privileged observers remain outside its guarantee.

## Implemented boundaries

| Area | Control | Evidence/limit |
|---|---|---|
| Network | Zero permissions; no HTTP/socket/WebView/telemetry/advertising/crash upload SDK | Source plus actual manifest/DEX; no runtime network capture available |
| Outward calls | Internal settings, system IME picker and explicit user-tapped system keyboard settings; no typed extras | Source audited; routing runtime NOT RUN |
| Service access | Exported IME protected by BIND_INPUT_METHOD; settings internal; only launcher otherwise exported | Source and packaged manifest |
| Preedit ownership | Local bounded RAM; no setComposingText/Region, inferred selection or surrounding reads; explicit commit first finishes any host-owned style | Source/API fixture/DEX; native host compatibility untested |
| Touch/control integrity | Settings and IME roots cancel children before rejecting full/partial obscured gestures; blocked tails require fresh DOWN or lifecycle reset; repeats stop on hide/disable/detach | Production UI/core model PASS; native overlays and touch reachability NOT RUN; accessibility actions retained |
| Live privacy display | Settings synchronizes private state and controls on resume/callback without writes; private-read failure shows ON | Actual production Settings host model; native UI NOT RUN |
| Dispatch contract | Sends once; local success is command dispatch, not host acknowledgement; no post-confirmation retry copy or automatic resend | Actual API35 Java/synthetic endpoint; native Binder NOT RUN; host may filter/drop after dispatch |
| Password fields | All supported password variations: direct commits, complete precomposed voiced/small kana selector, no previous-character read/reconstruction, draft/preview/candidate echo or learning | Policy/core/API fixture; native editors untested |
| Local learning | Default off; exact lexical explicit choices dispatched without detected error;host display is unacknowledged;128 entries/32KiB; one fixed data/temp pair under no-backup storage | Host core/race/failure and real Linux file tests; Android app UID/storage untested |
| Explicit conversion | CandidateList gates before history/dictionary queries;NO_SUGGESTIONS no-learning gate is independent;delayed work cannot open a list | Production core/API/negative-source checks;actual browser flags unknown |
| Field/private policy | Password/restricted/NO_PERSON flags/sensitive types/manual private mode block remembered lookup and learning; independent live gates on worker/snapshots | Host tests; sensitivity of ordinary editor text cannot be inferred |
| Backup/transfer | allowBackup=false; all nine legacy/cloud/transfer domains excluded; learning uses getNoBackupFilesDir | Actual compiled XML/manifest; OEM transfer NOT RUN |
| Logs | No runtime log sites or text-bearing error/upload path | Source/DEX; runtime logcat NOT RUN |
| Speech/clipboard | No feature, recognizer/listener/service/history or permission | Source/DEX/manifest |
| Dictionary | Pinned licensed bytes; no SKK expression evaluation, dynamic executable plugins or updater | Hash/deterministic derivation/lookup tests |
| Lifecycle | Hide/fresh field/finish clears unconfirmed draft; same-session relayout preserves and invalidates old actions | Host session tests; real framework ordering/rotation untested |
| Retention/clear | Expired/future-dated records excluded on lookup; queued/stale records invalidated; truthful failure/cleanup gating | Host races/files; exact stopped-app deletion/secure erasure not promised |
| Growth | Bounded learning/phrase queues; no app journal, sentence history or disk dictionary cache | Host bounds; Android-managed overhead unmeasured |
| Debugging/signing | Current manifest debuggable=false; approved development certificate reused; signing files outside deliverables | Actual signed APK and apksig receipt; integrity does not establish usability |

## Limits

A successful dispatch is not evidence that the host applied text: filtering/drop can clear an ordinary local draft without display, and eligible opt-in learning may record that dispatched choice. No automatic retry, surrounding read or post-confirmation text buffer is introduced. The full/partial obscuration guard can block legitimate floating windows; it displays a generic close-floating-windows hint without input text. Privileged accessibility is not treated as a hostile touch gesture.

The host receives explicitly confirmed ordinary text and direct restricted/password characters and may record/transmit them. Ordinary unconfirmed text stays in the keyboard rather than being supplied as in-host composition. Accessibility and screen capture can observe the normal local draft/candidates; password echo is suppressed. Keyboard-private UI does not isolate text from privileged observers or make sensitive ordinary fields identifiable.

A compromised OS can disregard permissions/backup flags, inspect memory or transfer data. Java/Binder strings and old references cannot be reliably wiped. Clearing files/references is not physical secure erasure. Plaintext learned pairs stay inside the app UID boundary; root/OS compromise can access them. Stopped-app expiry is enforced at the next lookup/use, not exact-time disk deletion. Failed policy persistence cannot establish durable controls and is reported. Clear does not remove static words or text already delivered to the host.

All earlier APKs, including delivered0.1.7, are retained unchanged. Known composition regressions make0.1.3–0.1.5 unsuitable for daily typing. The0.1.7 audit identified five bounded privacy/control/usability/contract issues;0.1.8 addresses them in source/models and remains held for independent review and native QA. Signature integrity does not establish editing correctness or daily usability. No physical device, new SDK agreement/key or public APK release occurred.

Any future Mozc/native engine or learning expansion requires complete dependency/license/persistence/outward-path review and fresh packaged audits. Clipboard history must remain opt-in if ever introduced. No cloud recognizer or client may arrive implicitly. [Local learning](LOCAL_LEARNING.md), [composition tradeoffs](COMPOSITION.md) and [verification](VERIFICATION.md) describe current scope.
