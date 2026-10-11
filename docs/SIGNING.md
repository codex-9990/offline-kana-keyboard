# Development-signed trial 0.1.5

Version **0.1.5**, Android version code **6**, reuses the existing approved development certificate. The APK is **not debuggable**. Signing credentials stay private and outside source/deliverables. No new key, device operation or public APK release was created.

- Filename: offline-kana-keyboard-0.1.5-trial.apk
- Size: **512,678 bytes**
- APK SHA-256: **cb20b566e777f9f7ad67fc692ef6431441b2ddb1cd25bd966103519c6742aa45**
- Certificate SHA-256: **54d17f7d3afe53d1c06c74984aa976150b7336f1843ac0869fa28876856b84fe**
- Application source: **c186fb47a0f4ff086444d3908994291d4dabf213**
- Signature: PASS, v2/v3, API 26–35, one expected signer, zero errors/warnings.
- Signed/unsigned payload comparison: PASS, all 10 ZIP entries.
- Signed APK checks: PASS, 19 checks, including zero permissions and debugging disabled.
- Tamper detection: PASS, altered DEX rejected; temporary copy removed.
- Private delivery channel: user's Library; no public APK release.
- Current delivery status: prepared locally; held pending independent review.
- Android installation/runtime QA: NOT RUN.

The original 0.1.0 SHA-256 1c966eb5f24993d214c0b3a3ef48b09c181a25fa8067ad3804091a1c25367cd3 and 0.1.1 SHA-256 8d890c246f16fd6cf74152c803e2cbedeba7482d1e723173f4376f68514cc60a remain unchanged.

Delivered 0.1.2 is also preserved unchanged: SHA-256 2ae7f36610cdee7e80276b4aa9ae02435897892825e370223ad54d8590a7564f. Its reported composing-state defect and new regression are documented in [COMPOSITION.md](COMPOSITION.md).

Delivered 0.1.3 is preserved unchanged: SHA-256 eac599e1a5d88657b907822c69989d912483dbcd69c80e27511ed7b532ffb1e4. The independent cursor/delayed-span regressions are addressed in 0.1.5; historical signature success did not establish correct editor behavior.

The undelivered local 0.1.4 APK is also preserved unchanged: SHA-256 a59d345db489e7a6833ca9ea958f85f8782d06b33a4ced1f91ef92a2f0f47c65. Its coalesced preview/cancel regression is documented in [COMPOSITION.md](COMPOSITION.md).

Signing/independent cryptographic verification use official Google Maven **apksig 8.7.3**, Apache-2.0, with the pinned hash in [signing-toolchain.lock.json](signing-toolchain.lock.json). The tool is not packaged. See [AOSP apksig](https://android.googlesource.com/platform/tools/apksig/+/refs/heads/main/README.md) and [Android signing documentation](https://developer.android.com/tools/apksigner).

Regular builds remain unsigned. Later signed updates must preserve the certificate and increase version code. Never alter a signed APK. Integrity/signature validation does not establish Android behavior or conversion quality; follow [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md).
