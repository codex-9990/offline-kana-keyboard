# Development-signed trial 0.1.2

Version **0.1.2**, Android version code **3**, reuses the existing approved development certificate. The APK is **not debuggable**. Signing credentials stay private and outside source/deliverables. No new key, device operation or public APK release was created.

- Filename: offline-kana-keyboard-0.1.2-trial.apk
- Size: **512,678 bytes**
- APK SHA-256: **2ae7f36610cdee7e80276b4aa9ae02435897892825e370223ad54d8590a7564f**
- Certificate SHA-256: **54d17f7d3afe53d1c06c74984aa976150b7336f1843ac0869fa28876856b84fe**
- Application source: **a0fac115d6fe042f390d82fb2ffd30bf57d88d5f**
- Signature: PASS, v2/v3, API 26–35, one expected signer, zero errors/warnings.
- Signed/unsigned payload comparison: PASS, all 10 ZIP entries.
- Signed APK checks: PASS, 19 checks, including zero permissions and debugging disabled.
- Tamper detection: PASS, altered DEX rejected; temporary copy removed.
- Private delivery channel: user's Library; no public APK release.
- Android installation/runtime QA: NOT RUN.

The original 0.1.0 SHA-256 1c966eb5f24993d214c0b3a3ef48b09c181a25fa8067ad3804091a1c25367cd3 and 0.1.1 SHA-256 8d890c246f16fd6cf74152c803e2cbedeba7482d1e723173f4376f68514cc60a remain unchanged.

Signing/independent cryptographic verification use official Google Maven **apksig 8.7.3**, Apache-2.0, with the pinned hash in [signing-toolchain.lock.json](signing-toolchain.lock.json). The tool is not packaged. See [AOSP apksig](https://android.googlesource.com/platform/tools/apksig/+/refs/heads/main/README.md) and [Android signing documentation](https://developer.android.com/tools/apksigner).

Regular builds remain unsigned. Later signed updates must preserve the certificate and increase version code. Never alter a signed APK. Integrity/signature validation does not establish Android behavior or conversion quality; follow [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md).
