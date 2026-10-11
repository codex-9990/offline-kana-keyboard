# Verification — 0.1.8 hardening candidate

Application commit **679cc413005fe15ff53b39a0ee906646d96462d8**; version0.1.8/code9. The final signed, non-debuggable candidate is **516,774 bytes**, SHA-256 **c1fc2b00570e27e0ddb6c501beae2559946c31f4d4277632376151e29d19e8d5**. The approved existing certificate is reused. Independent review is **PENDING**; replacement APK delivery is **NOT PERFORMED**. No SDK installation/agreement, new key, phone action or public APK release occurred.

## Exact executed results

| Check | Result | Scope |
|---|---|---|
| Core | PASS —42 groups | Flick/input/field/action/capacity/lifecycle/manual-conversion policy |
| Features | PASS —51 groups | Learning/races/failure/storage/layout/phrase/projection and explicit-only restrictions |
| Hardening core | PASS —9 groups | Obscured full/partial/tail transitions, complete immutable kana tables, direct policy/no echo, observable failure and one-shot dispatch |
| ConnectionEditor API adapter | PASS —14 groups | Actual API signatures, synthetic InputConnection and native-key injection model |
| Local preedit API fixture | PASS —29 groups | Independent synthetic host text/caret/draft/request/preview oracle |
| Remote dispatch contract | PASS —4 groups | Actual cached API35 RemoteInputConnection/invoker Java with synthetic endpoint; true dispatch without application, RemoteException failure, no auto-retry/retained confirmed text, learning policy |
| Production UI integration model | PASS —18 groups | Actual Settings/Setup/KeyView/SecureTouchFrame/IME/ConnectionEditor/LearningStore with declared host shims; live privacy, mixed obscuration cancellation, repeat lifecycle, accessibility, direct kana, rotation and explicit-only first request |
| Ordinary bounded sequences | PASS —59,048 cases | Depth0–4; exact synthetic host/draft/preview/counts and continued input |
| Explicit-only bounded sequences | PASS —177,776 cases | Depth0–4; explicit request and NO_PERSONALIZED_LEARNING both states |
| Repeated ordinary sessions | PASS —1,000 cycles |4,000 accepted keys /2,000 exact confirmations |
| Repeated explicit-only sessions | PASS —2,000 cycles |8,000 accepted keys /4,000 exact confirmations; no learned records |
| Help negative controls | PASS —56 mutations rejected | Missing/duplicate sections, retired claims and acknowledgement/direct-kana limits |
| Compatibility negative controls | PASS —6 mutations rejected | No-learning gate, restricted fields, first-request order, lazy history, phrase/tap generation |
| Source policy | PASS —30 checks | Permissions, logs/network/read paths, exports/intents/dependencies, preedit, backup, manual gates and help |
| Cached AOSP build | PASS | Pinned API35/AAPT2/D8 clean build; existing JDK/cache, no fetch or SDK installation |
| Signed APK policy | PASS —27 checks | Binary manifest, version/debug/exports/permissions/backup, DEX/help/assets/namespace/alignment |
| Signature | PASS —v2/v3;zero errors/warnings | apksig8.7.3, API26–35, one expected certificate |
| Payload equality/CRC | PASS —10 entries byte-identical;both ZIP CRC valid | Signed/unsigned entry bytes and ZIP CRC |
| DEX tamper rejection | PASS —altered compressed DEX rejected with1 signature error;temporary copy removed | Valid control and byte-altered DEX with retained signature block |
| Source/review packet | Prepared after the final documentation commit; exact hashes in the external review receipt | Allowlisted tracked source, per-entry manifest and evidence checks |
| Protected project/prior artifacts | PASS —175 gpd hashes/git clean,8 prior APKs and both audit hashes unchanged | Original checkout,175 gpd tracked hashes, retained APKs and audit files |
| First UI-model fixture compile | FAIL — corrected | Fixture-only missing LENGTH_LONG/self-reference; retained trace; latest full model pass |
| Gradle/lint/conventional merged manifest | NOT RUN | Full SDK absent; actual packaged binary manifest inspected instead |
| Native Android/registration/Binder/Looper/events | NOT RUN | No adb/emulator/KVM or physical-device operation |
| Native touch/WindowManager/overlay/accessibility/insets | NOT RUN | Host models do not implement real layout/hit-testing, overlays or accessibility transport |
| Native rotation/lifecycle/grapheme deletion/actions | NOT RUN | Compile/model evidence only; OEM order/host behavior unobserved |
| Android UID/storage/backup/transfer/logcat/capture | NOT RUN | Source/packaged controls and synthetic Linux file tests only |
| S24/browser/latency/haptics/airplane mode | NOT RUN | Actual reported browser flags and native resolution remain unknown |
|0.1.8 independent review | PENDING | Exact source commit/packet offered for review; no clearance claimed |
|0.1.8 APK replacement/public release/install | NOT PERFORMED | Previous delivered APK remains unchanged |

The167 groups are102 core/feature/hardening +47 Android API +18 UI-model groups. Sequence/cycle cases occur inside groups; they are not extra group counts. The oracle fails ignored accepted keys, duplication and permanent lockout. It is bounded and synthetic, not a proof over every Android schedule or hostile host. Production UI models explicitly omit WindowManager, Binder, real Looper scheduling, layout/hit-testing and One UI.

The remote-wrapper tests execute real API35 Java against a synthetic endpoint. They establish that the local success boolean acknowledges command dispatch, not host display/application. Native Binder transport remains NOT RUN. This finding is addressed by accurate contracts/help and a no-auto-retry policy; full host acceptance was not made observable.

## Five audit responses

F1: actual Settings resume and live callbacks synchronize all current controls without writes; private-read failure shows ON. F2/F5: root filtering cancels children with an unflagged CANCEL and stops repeats before swallowing a gesture tail. Full/partial/combined flags on DOWN/MOVE/UP/CANCEL, obscured DOWN to clean UP, clean DOWN to partial MOVE to clean UP, the next clean DOWN, hidden/disabled/detached and lifecycle cancellation are covered. Accessibility performClick/flick remains available; no hasWindowFocus condition was added.

F4: all26 precomposed voiced/semivoiced and12 small kana in both scripts are available to protected direct input. Actual IME models exercise text/visible/web password, null/unknown policies and base katakana; no password echo, reconstructed prior input, combining marks or learned writes. F3: detected remote failure retains ordinary local draft; successful dispatch clears once and may notify eligible learning even if a host silently drops it. No host reads, input logs, automatic resend or post-confirmation text cache was added.

## Evidence packet

Final evidence filenames start final-0.1.8- and include executed command receipts, complete host/API/UI logs, build, signed APK policy, signature, payload, tamper and preservation JSON, source export and public file manifests. Actual APK XML/text, badging, backup rules and DEX strings are included. Test source is in the source ZIP. The earlier model fixture compile failure is retained with its original label.

The intermediate unsigned build predates final help changes and is not delivered. Historical0.1.7 and older evidence/artifacts remain unchanged. Delivered0.1.7 is the audited baseline; its five findings are documented in the separate audit report/evidence already saved in the user's Library. Historical security/source test passes did not invalidate those later findings.

Unsigned final: **511,261 bytes**, SHA-256 **5f82bf06d99191a04813c05feea168a87111581c9f648a78f72cfa4290025487**. Certificate SHA-256: **54d17f7d3afe53d1c06c74984aa976150b7336f1843ac0869fa28876856b84fe**. Local receipts establish prepared bytes, not a cloud download-back. The final review/documentation commit and source/report ZIP hashes are recorded in the review receipt outside the source ZIP to avoid circular hashes.

[Threat model](THREAT_MODEL.md), [editing contract](COMPOSITION.md), [daily-use gate](DAILY_USE_READINESS.md), [device matrix](DEVICE_TEST_PLAN.md).
