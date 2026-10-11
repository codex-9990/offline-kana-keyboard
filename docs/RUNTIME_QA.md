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

Host inspection: Linux x86_64, five visible logical CPUs, about 18,007 MiB RAM / 17,287 MiB available, 29.6 GiB free workspace disk. **/dev/kvm is absent.** The official [acceleration guide](https://developer.android.com/studio/run/emulator-acceleration) explains Linux KVM, and the [command-line guide](https://developer.android.com/studio/run/emulator-commandline) documents emulator options. Software CPU emulation with `-accel off` may be slow or impractical; success is not assumed. No permissions, host virtualization or kernel changes will occur without approval.

After approval: install only these official packages into an isolated SDK directory; run conventional Gradle build/lint and inspect its merged/packaged manifests. Create an API 35 AOSP AVD, check acceleration, select its explicit emulator serial, and run [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md) using a synthetic fixture. Restore the emulator's original IME afterward. If software emulation is unusable, use an already approved host with KVM instead of silently adding cloud infrastructure.

Galaxy S24 validation requires separate physical-device approval. An emulator cannot validate One UI, real S24 latency or OEM transfer. Until observed results are recorded, all Android/device rows stay **NOT RUN** and this remains a development trial.
