# Development-signed trial

A development-only signing identity was created following explicit user approval. The signed trial APK was verified and successfully delivered through the user's Library. Signing credentials are retained privately for compatible later trial updates and excluded from GitHub and all delivery archives. No device installation, IME activation or public APK release was performed.

## Artifact and public certificate

- Filename: `offline-kana-keyboard-0.1.0-debug-trial.apk`
- Size: **484,006 bytes**
- APK SHA-256: `1c966eb5f24993d214c0b3a3ef48b09c181a25fa8067ad3804091a1c25367cd3`
- Public certificate SHA-256: `54d17f7d3afe53d1c06c74984aa976150b7336f1843ac0869fa28876856b84fe`
- Signature verification: **PASS**, v2 and v3, API 26–35, one expected signer, zero errors and warnings.
- Signed/unsigned payload comparison: **PASS**, all 10 ZIP entries identical, including manifest, DEX, resources and dictionary.
- Signed APK privacy/content audit: **PASS**, all 17 checks; zero requested permissions and unchanged backup exclusions.
- Deliberate DEX tampering: **PASS**, corrupted copy rejected by signature verification and removed.
- Library delivery: **PASS**, successful save confirmed. No public download link or public APK release is provided.
- Android installation and runtime QA: **NOT RUN**. Signature verification does not validate Android editor behavior or Galaxy S24 usability.

The machine-readable [TRIAL_DELIVERY.json](TRIAL_DELIVERY.json) contains the public artifact, certificate and verification receipt. It contains no credentials, Library identifiers or private storage information.

## Verification method

Signing and independent verification used Google's **apksig 8.7.3** public `ApkSigner` and `ApkVerifier` APIs. See [AOSP apksig](https://android.googlesource.com/platform/tools/apksig/+/refs/heads/main/README.md) and [Android signing documentation](https://developer.android.com/tools/apksigner). The library came from official Google Maven under Apache-2.0; its published checksum was checked, and SHA-256 is recorded in [signing-toolchain.lock.json](signing-toolchain.lock.json). It is a build tool and is not packaged in the application. No Android SDK agreement was accepted or global software installed.

The signing operation kept the unsigned build separate. Verification checked the signed output against the expected public certificate fingerprint. v2 and v3 are enabled; v4 and key rotation are not used. The resource table remains stored and aligned to four bytes. All application payload bytes and privacy restrictions remain unchanged.

## Trial updates

The regular build commands still produce an unsigned APK. Approved trial builds can be signed separately with the retained development identity. Later compatible updates must preserve the signing certificate and increase the Android version code as needed. Do not modify an APK after signing, and verify the resulting signature and artifact hash before distribution.

This trial remains a prototype with word-level SKK conversion. Follow [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md) before relying on it for daily use. Host tests and signature verification do not replace Android runtime testing.
