# Threat model

## Goal and scope

Koto performs kana input, dictionary lookup and bounded heuristic phrase proposals locally. Data flows from user gesture → IME RAM → Android InputConnection → host editor. With opt-in learning, eligible explicit dictionary choices also create bounded app-private reading/word/timestamp records. No conversion download or cloud request exists. The normal host-editor exchange is necessary for a keyboard.

This is a development prototype. The [baseline artifact audit and corrected-build verification](SECURITY_AUDIT.md) document their exact scope; they do not establish an absolute guarantee against a compromised device. Airplane-mode operation would be useful functional evidence, but would not establish the privacy boundary by itself.

## Assets and possible observers

Sensitive assets are composition, candidates, supplied characters and, if enabled, local learned lexical records. Composition/phrase state remains transient; learned pairs are intentionally retained for the selected period. Preferences contain six non-text controls/policy fields: height, haptics, flick distance, private mode, retention days and cleanup-required flag. Learning defaults off.

Potential observers include an untrusted network SDK, a malicious app binding the IME, a backup/transfer agent, a log collector, an external speech recognizer, a clipboard collector, a host app, accessibility services, screen capture, root/ADB and a compromised OS. The first six are addressed within the application. The host and privileged OS-level observers remain outside its guarantee.

## Implemented boundaries

| Area | Implemented control | Verification scope |
|---|---|---|
| Direct network | No permissions, socket/HTTP SDK, WebView, telemetry, ads or crash upload | Source plus actual packaged manifest and DEX checks |
| Indirect outward calls | Only internal settings, system IME picker, and a user-tapped settings intent resolved to a system activity and made explicit; no typed extras | Source inspection; runtime routing not run |
| IME access | Required exported service is guarded by BIND_INPUT_METHOD; internal settings are not exported | Source and packaged manifest |
| Persistence | Opt-in lexical records only, max 128/32 KiB; six non-text preferences; no whole-input history | Production core plus real host file tests; Android filesystem test not run |
| Backup and transfer | allowBackup=false and separate explicit exclusions for nine domains in legacy/cloud/device-transfer rules | Source and compiled APK resource XML; actual OEM transfer not run |
| Password input | Text, visible, web and number password variants detected; direct commits, no candidates or composition string | Host policy/session tests; Android editors not run |
| Learning restrictions | Password/restricted/sensitive-type flags and manual private mode block recording and remembered lookup; static conversion remains available | Host tests; ordinary-field semantic sensitivity cannot be inferred |
| Clipboard/speech | No clipboard or speech feature, listeners, service or corresponding permission | Source/DEX/manifest checks |
| Logs | No runtime log sites or text-bearing error uploads | Source and DEX checks; runtime logcat not run |
| Dictionary data | Pinned upstream bytes; no Lisp evaluation, plugins, dynamic code or update downloader | Hash, deterministic derivation and lookup tests |
| Input lifecycle | Clear transient state on hidden view, editor finish, focus changes and configuration changes | Host model tests; actual service lifecycle/rotation not run |
| Retention/clear | Lookup excludes expired records; background pruning and clear invalidate stale generations; failures stop use and are reported | Host failure/race tests and real file adapter; Android I/O/runtime not run |
| Storage growth | Fixed no-backup files, max 32 KiB each; no app journal/dictionary cache; bounded pending worker/phrase queues | Host quota/interruption tests; Android-managed bookkeeping size unmeasured |
| Debug access | Versions 0.1.1/0.1.2 explicitly disable debugging in both build routes | Source plus actual signed APK binary manifest; authorized privileged OS access remains out of scope |

## Limits

The receiving app can record and transmit text; Android delivers ordinary composed text to it before confirmation. This cannot be prevented by an IME. Password characters must also be delivered to the receiving app.

A compromised OS can disregard manifests, inspect memory, capture input or transfer data. OEM-specific transfer behavior has not been tested. Full input/composition is not persisted, but opt-in learned lexical pairs contain deliberately retained reading/word data.

Java String objects and Binder/editor copies cannot be reliably wiped. Clearing session/candidate references reduces retention but is not secure erasure. Accessibility and screen capture can observe normal on-screen candidates; password candidates are suppressed. The public launcher intentionally ignores inbound extras and stores no supplied input data.

Original 0.1.0, 0.1.1, 0.1.2, 0.1.3 and 0.1.4 APKs are retained unchanged. The 0.1.5 trial keeps debugging disabled and uses the same approved development certificate. Android installation, runtime behavior and OEM transfer are untested. A public APK release needs separate approval. Signing files stay outside source/export archives.

Composition selection tracking stores only at most 64 transient coordinate records, with no strings or persistence. Known owned spans may be re-established through the host InputConnection; surrounding text is never read. Unknown anchors/rejected operations stop safely instead of guessing a host substring. Actual OEM/editor callback behavior is untested; see [COMPOSITION.md](COMPOSITION.md).

Learned records are plaintext inside the app UID boundary. A stopped app does not delete at an exact expiry clock tick. File removal/reference clearing is not physical secure erasure. Failed policy writes cannot establish durable settings guarantees; the UI reports failure. Clear does not remove host text, static dictionary entries or OS-level copies. [LOCAL_LEARNING.md](LOCAL_LEARNING.md) describes these limits.

## Before adding features

Any expansion of learning must retain field/private-mode exclusions, bounded app-private storage, backup/transfer controls and truthful deletion. Clipboard history must remain opt-in. Mozc needs an explicit persistence/native-dependency review and new artifact/outward-call audits. No network client or recognizer may arrive implicitly with a dependency.
