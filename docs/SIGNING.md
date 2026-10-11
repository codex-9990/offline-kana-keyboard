# Corrected development-signed trial

Version **0.1.1**, Android version code **2**, reuses the existing approved development certificate. The APK is **not debuggable**. Signing credentials are retained privately and excluded from GitHub and deliverables. No new key, device operation or public APK release was created for this iteration.

## Artifact and public certificate

- Filename: offline-kana-keyboard-0.1.1-trial.apk
- Size: **488,102 bytes**
- APK SHA-256: 8d890c246f16fd6cf74152c803e2cbedeba7482d1e723173f4376f68514cc60a
- Public certificate SHA-256: 54d17f7d3afe53d1c06c74984aa976150b7336f1843ac0869fa28876856b84fe
- Compiled application source: 9adf477030a17468feca1761f28791dbd7d8776c
- Signature: **PASS**, v2/v3, API 26–35, one expected signer, zero errors/warnings.
- Signed/unsigned payload comparison: **PASS**, all 10 ZIP entries byte-identical.
- Signed APK privacy/content: **PASS**, 19 checks, including zero permissions and debuggable=false.
- Deliberate DEX tampering: **PASS**, altered payload rejected; temporary copy removed.
- Delivery route: user's Library; no public APK release.
- Android installation/runtime QA: **NOT RUN**.

The original signed 0.1.0 APK remains unchanged: 484,006 bytes, SHA-256 1c966eb5f24993d214c0b3a3ef48b09c181a25fa8067ad3804091a1c25367cd3. It is debuggable and serves as the historical audit baseline. See [SECURITY_AUDIT.md](SECURITY_AUDIT.md).

## Method and updates

Signing and independent cryptographic verification use Google's **apksig 8.7.3** public ApkSigner/ApkVerifier APIs from official Google Maven, Apache-2.0. Its hash is pinned in [signing-toolchain.lock.json](signing-toolchain.lock.json); it is not packaged in the app. See [AOSP apksig](https://android.googlesource.com/platform/tools/apksig/+/refs/heads/main/README.md) and [Android signing documentation](https://developer.android.com/tools/apksigner).

Unsigned output is retained separately. Verification requires the expected certificate and checks API 26–35 with v2/v3; v4 and key rotation are not used. Resources remain stored/four-byte aligned. No SDK agreement was accepted to use these Maven tools.

Regular builds stay unsigned. Separately signed updates must preserve the certificate and increase version code. Keep credentials out of exports, never modify a signed APK, and verify its hash before delivery. Signature validation establishes integrity and identity, not runtime correctness or daily-use readiness. Follow [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md).
