# Threat model

## Goal and scope

Koto performs kana input and dictionary lookup locally and adds no external destination for typing data. Its data flow is user gesture → IME RAM → Android InputConnection → the foreground host editor. A licensed dictionary is read from APK assets; no conversion download or cloud request exists. The normal Android host-editor exchange is necessary for a keyboard to work.

This is a reviewed prototype, not an independent security audit or an absolute guarantee against a compromised device. Airplane-mode operation would be useful functional evidence, but would not establish the privacy boundary by itself.

## Assets and possible observers

Sensitive assets are the current composing string, candidate list and characters supplied to the host. They exist transiently in application/Android memory. There is no personal dictionary or learning data. Preferences contain only an integer height and a haptic boolean.

Potential observers include an untrusted network SDK, a malicious app binding the IME, a backup/transfer agent, a log collector, an external speech recognizer, a clipboard collector, a host app, accessibility services, screen capture, root/ADB and a compromised OS. The first six are addressed within the application. The host and privileged OS-level observers remain outside its guarantee.

## Implemented boundaries

| Area | Implemented control | Verification scope |
|---|---|---|
| Direct network | No permissions, socket/HTTP SDK, WebView, telemetry, ads or crash upload | Source plus actual packaged manifest and DEX checks |
| Indirect outward calls | Only internal settings, system IME picker, and a user-tapped settings intent resolved to a system activity and made explicit; no typed extras | Source inspection; runtime routing not run |
| IME access | Required exported service is guarded by BIND_INPUT_METHOD; internal settings are not exported | Source and packaged manifest |
| Persistence | Only two non-text preferences; no history, user dictionary or learning | Source write-site checks; post-device filesystem test not run |
| Backup and transfer | allowBackup=false and separate explicit exclusions for nine domains in legacy/cloud/device-transfer rules | Source and compiled APK resource XML; actual OEM transfer not run |
| Password input | Text, visible, web and number password variants detected; direct commits, no candidates or composition string | Host policy/session tests; Android editors not run |
| Learning restrictions | No learning anywhere; flag marks private input; static candidates may still be shown | Host tests and source inspection |
| Clipboard/speech | No clipboard or speech feature, listeners, service or corresponding permission | Source/DEX/manifest checks |
| Logs | No runtime log sites or text-bearing error uploads | Source and DEX checks; runtime logcat not run |
| Dictionary data | Pinned upstream bytes; no Lisp evaluation, plugins, dynamic code or update downloader | Hash, deterministic derivation and lookup tests |
| Input lifecycle | Clear transient state on hidden view, editor finish, focus changes and configuration changes | Host model tests; actual service lifecycle/rotation not run |

## Limits

The receiving app can record and transmit text; Android delivers ordinary composed text to it before confirmation. This cannot be prevented by an IME. Password characters must also be delivered to the receiving app.

A compromised OS can disregard manifests, inspect memory, capture input or transfer data. OEM-specific transfer behavior has not been tested. Input text is never persisted by this implementation, which reduces that risk independently of backup switches.

Java String objects and Binder/editor copies cannot be reliably wiped. Clearing session/candidate references reduces retention but is not secure erasure. Accessibility and screen capture can observe normal on-screen candidates; password candidates are suppressed. The public launcher intentionally ignores inbound extras and stores no supplied input data.

The produced unsigned APK is a debug build. A signed debug build would permit authorized debugging access. Production signing, hardening, distribution and a public APK release require separate decisions. Private signing files must remain outside source/export archives.

## Before adding features

Any future learning or personal dictionary must use local storage with all backup/transfer exclusions retained, honor password and NO_PERSONALIZED_LEARNING policies, support manual incognito, and provide deletion. Clipboard history must remain opt-in. A Mozc integration needs an explicit learning/persistence review and new DEX/native/outward-call audits. There must be no native network client or recognizer path added implicitly with a dependency.
