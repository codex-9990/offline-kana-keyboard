# Development-signed trial 0.1.6

Version **0.1.6**, Android version code **7**, reuses the existing approved development certificate. The APK is **not debuggable**. Signing credentials stay private and outside source/deliverables. No new key, device action or public APK release was created.

- Filename: offline-kana-keyboard-0.1.6-trial.apk
- Size: **512,678 bytes**
- APK SHA-256: **2ba3e39ca74d7da11015be808a1014ed4fcde6e67ff77b4cab332bfe3505d9ff**
- Certificate SHA-256: **54d17f7d3afe53d1c06c74984aa976150b7336f1843ac0869fa28876856b84fe**
- Application source: **0aa62da1c7428341556a035864fd715be2212d61**
- Signature: PASS, v2/v3, API 26–35, one expected signer, zero errors/warnings.
- Payload comparison: PASS, all 10 signed/unsigned ZIP entries byte-identical; CRC valid.
- Signed APK checks: PASS,20, including zero permissions and no composing-span reclamation code.
- Tamper rejection: PASS, same four-argument verifier accepts valid control and rejects modified DEX with one signature error; temporary copies removed.
- Delivery: user's Library after independent review;0.1.6 replacement NOT PERFORMED.
- Installation/runtime QA: NOT RUN.

## Preserved earlier APKs

| Version | SHA-256 | Current scope |
|---|---|---|
|0.1.0|1c966eb5f24993d214c0b3a3ef48b09c181a25fa8067ad3804091a1c25367cd3|Original debug trial retained unchanged|
|0.1.1|8d890c246f16fd6cf74152c803e2cbedeba7482d1e723173f4376f68514cc60a|Retained unchanged|
|0.1.2|2ae7f36610cdee7e80276b4aa9ae02435897892825e370223ad54d8590a7564f|User reported conversion failure; retained unchanged|
|0.1.3|eac599e1a5d88657b907822c69989d912483dbcd69c80e27511ed7b532ffb1e4|Delivered historical trial; known cursor/duplication regressions|
|0.1.4|a59d345db489e7a6833ca9ea958f85f8782d06b33a4ced1f91ef92a2f0f47c65|Local only; known coalesced callback regression|
|0.1.5|cb20b566e777f9f7ad67fc692ef6431441b2ddb1cd25bd966103519c6742aa45|Local only; release-blocking accepted-key loss/lockout|

Official Google Maven **apksig 8.7.3**, Apache-2.0, is pinned in [signing-toolchain.lock.json](signing-toolchain.lock.json) and not packaged. [AOSP apksig](https://android.googlesource.com/platform/tools/apksig/+/refs/heads/main/README.md), [Android signing documentation](https://developer.android.com/tools/apksigner).

Regular builds remain unsigned. Future signed updates must preserve the certificate and increase version code. Never alter a signed APK. Signature integrity does not establish native behavior, conversion quality or privacy under a compromised OS; follow [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md).
