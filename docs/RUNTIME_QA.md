# Android runtime QA setup proposal

**Nothing below is installed, no SDK agreement is accepted, and no device was changed.** This proposal makes the remaining setup approval concrete. Existing JDK 17/Gradle 8.9 suffice; no NDK or Android Studio IDE is needed for this version.

## Exact official packages

The stable-channel snapshot comes from Google's [SDK repository](https://dl.google.com/android/repository/repository2-3.xml) and [AOSP system-image repository](https://dl.google.com/android/repository/sys-img/android/sys-img2-3.xml).

| SDK package | Revision | Compressed bytes |
|---|---|---:|
| platforms;android-35 | 2 | 64,273,788 |
| build-tools;35.0.1 | 35.0.1 | 61,959,297 |
| cmdline-tools;latest | 23.0 | 181,052,239 |
| platform-tools | 37.0.1 | 9,054,187 |
| emulator | 37.2.12, stable | 349,654,171 |
| system-images;android-35;default;x86_64 | 2, AOSP | 782,404,023 |

Total archives: **1,448,397,705 bytes**, about **1.35 GiB** compressed. Planning estimate: **10–12 GiB disk** for unpacked SDK/image, AVD storage and caches; **2–4 GiB RAM** for one emulator. These are allowances, not benchmarks. The AOSP image avoids Google Play services. No preview package or paid/cloud service is proposed.

All selected packages identify `android-sdk-license`. Installation requires explicit approval of these downloads/packages and the applicable **Android SDK License Agreement** presented on the official [Android tools download page](https://developer.android.com/studio). No license acceptance occurs implicitly. SDK approval does not authorize physical-phone installation or IME activation.

## Host feasibility and bounded next step

Host inspection: Linux x86_64, five visible logical CPUs, about 18,007 MiB RAM / 17,287 MiB available, 29.6 GiB free workspace disk. **/dev/kvm is absent.** The official [acceleration guide](https://developer.android.com/studio/run/emulator-acceleration) explains Linux KVM. Current [command-line documentation](https://developer.android.com/studio/run/emulator-commandline) marks `-accel off` software CPU emulation unsupported and very slow; it requires the explicit `ANDROID_I_WANT_MY_TCG=yes` opt-in. The supported validation route therefore needs an approved KVM-capable host; SDK installation alone cannot provide that here. No opt-in, permissions, virtualization or kernel changes were made.

After approval: install only these official packages into an isolated SDK directory; run conventional Gradle build/lint and inspect its merged/packaged manifests. Create an API 35 AOSP AVD, check acceleration, select its explicit emulator serial, and run [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md) using a synthetic fixture. Restore the emulator's original IME afterward. If software emulation is unusable, use an already approved host with KVM instead of silently adding cloud infrastructure.

Galaxy S24 validation requires separate physical-device approval. An emulator cannot validate One UI, real S24 latency or OEM transfer. Until observed results are recorded, all Android/device rows stay **NOT RUN** and this remains a development trial.

## Minimum 0.1.7 native runtime gate — NOT RUN

A supported approved API 35 KVM emulator and a separately approved S24 test are still required. The APK, source and host tests are ready for review; setup approval and execution have not happened. Appname/field, Android/One UI version, inputType and imeOptions are needed for the affected-app synthetic retest; actual user text is not needed.

Build a local offline fixture with a native EditText and a local WebView editable field. Keep it separate from the production keyboard. Exercise real InputMethodService/InputConnection/Binder/Looper dispatch, including a fixture wrapper that delays/coalesces updateSelection notifications rather than only calling a JVM proxy. The fixture must assert final synthetic text and accepted-key/confirmation counts; it must fail on an ignored accepted key, premature confirmation or conversion lockout. The production keyboard must retain zero logging and no surrounding reads.

Minimum cases:

1. Start a fresh field with a synthetic prefix. Type にほん: host unchanged, local row/candidates visible. Confirm 日本, type ご before earlier callbacks, flush ordered/coalesced/no-motion updates, confirm ご, and repeat continuously. Each accepted key and expected confirmation must appear exactly once without restarting the field.
2. Preview/cancel before coalesced3→3/late3→2 notifications; continue with another key, local deletion/modifier, space, mode changes and explicit cancel. No draft disappears or moves to the host implicitly. Preview cancellation remains usable if explicit commit is rejected.
3. Move the actual native caret/selected range with draft present, including keyboard arrows and external host taps. Confirm at that actual position; preserve prefix/host-owned composing text. Empty-draft native DEL must work for selected text, emoji/combining clusters and local WebView. Unsupported finish/commit must show failure with editable draft, never be counted as successful conversion.
4. Fresh focus/password/NO_SUGGESTIONS/numeric fields must not receive old draft state. Password entry must be direct without draft/candidate/accessibility echo; NO_PERSONALIZED_LEARNING and manual private mode must keep static conversion usable without learned reads/writes. Return from settings to the same editor, invalidate old learned previews/taps/queued choices and inspect no-backup files using only synthetic records.
5. Normal body/WEB_EDIT_TEXT, URL/search and local WebView page/notepad fixture variants:normal/NO_SUGGESTIONS,NO_PERSON andFORCE_ASCII combinations. Manual fields keep editable local draft, show fixed candidates only after first変換, never learned/segmented output. Edits/focus close requests;loading/notification/late work cannot reopen them. URI/email/FORCE_ASCII startABC and preserve ASCII keys;Unicode rejection remains editable without bypassing the host. Compare the user's browser contexts only after the actual browser is identified.
6. Rotation, hide/show, field restart and held-key cancellation: same-session relayout may retain local draft; hide or fresh start deliberately cancels it. Confirmed text stays once; no stale candidate/repeat crosses fields. Check portrait/landscape/font scaling/insets, haptics and latency on S24.

Diagnostic output belongs only in the synthetic fixture: operation ordering, API return booleans, cursor integers, field flags, counters and pass/fail. Do not log input strings, collect surrounding text or request actual user typing. Fixture text assertions need not print their payload. Record exact results and target versions; restore the emulator's prior IME. SDK setup authorization remains separate from physical-device authorization.

An emulator pass cannot establish One UI behavior or the user's affected-app resolution. That claim requires the separately approved affected-app S24 retest with synthetic data. Until then the relevant rows stay NOT RUN and Library replacement remains under the independent-review hold.
