# Development-signed candidate — 0.1.8

Version0.1.8/code9 reuses the existing approved development certificate. The final APK is not debuggable. Signing credentials remain outside source and deliverables. No new key, device action or public APK release was created.

| Item | Exact candidate |
|---|---|
| Filename | offline-kana-keyboard-0.1.8-trial.apk |
| Size |516,774 bytes |
| APK SHA-256 |c1fc2b00570e27e0ddb6c501beae2559946c31f4d4277632376151e29d19e8d5 |
| Certificate SHA-256 |54d17f7d3afe53d1c06c74984aa976150b7336f1843ac0869fa28876856b84fe |
| Application source |679cc413005fe15ff53b39a0ee906646d96462d8 |
| Unsigned size |511,261 bytes |
| Unsigned SHA-256 |5f82bf06d99191a04813c05feea168a87111581c9f648a78f72cfa4290025487 |
| Signature |PASS, v2/v3 API26–35, one expected signer, zero errors/warnings |
| Payload/CRC |PASS —10 entries byte-identical;both ZIP CRC valid |
| Signed APK policy |PASS,27 checks, zero permissions and no composing-span reclamation |
| Tamper rejection |PASS —altered compressed DEX rejected with1 signature error;temporary copy removed |
| Independent review |PENDING |
|0.1.8 APK delivery/public release |NOT PERFORMED |
| Installation/native Android/S24 QA |NOT RUN |

The final source and report packet hashes are recorded in the external review receipt. Application Java/resources/assets/build/version inputs match the application commit above. The later documentation/review commit also includes a portable JAVA_HOME selection in the UI test runner; it does not change the APK payload.

The previously delivered0.1.7 remains unchanged at SHA-256 df40f0691e3d975f80b603e457fdb7cceecd43b12983f0737ca7d31ac1824e03,512,678 bytes. Its exact audit found five bounded issues and its host/artifact test passes did not establish daily usability.0.1.8 is prepared for independent review before replacement delivery.

## Earlier artifacts

The earlier hash table below is retained from the previous signing record. All these files and delivered0.1.7 are rechecked by the current preservation receipt. No old APK is overwritten. The pre-help-review0.1.6 variant was never delivered and also remains preserved.

| Version | SHA-256 | Current scope |
|---|---|---|
|0.1.0|1c966eb5f24993d214c0b3a3ef48b09c181a25fa8067ad3804091a1c25367cd3|Original debug trial retained unchanged|
|0.1.1|8d890c246f16fd6cf74152c803e2cbedeba7482d1e723173f4376f68514cc60a|Retained unchanged|
|0.1.2|2ae7f36610cdee7e80276b4aa9ae02435897892825e370223ad54d8590a7564f|User reported conversion failure; retained unchanged|
|0.1.3|eac599e1a5d88657b907822c69989d912483dbcd69c80e27511ed7b532ffb1e4|Delivered historical trial; known cursor/duplication regressions|
|0.1.4|a59d345db489e7a6833ca9ea958f85f8782d06b33a4ced1f91ef92a2f0f47c65|Local only; known coalesced callback regression|
|0.1.5|cb20b566e777f9f7ad67fc692ef6431441b2ddb1cd25bd966103519c6742aa45|Local only; release-blocking accepted-key loss/lockout|
|0.1.6|5bb7e8dcde0a9cb6f275bd6e97f6fe896b1aa6d63fadec1e124ac40f74c0910a|Delivered limited trial;native QA unrun;NO_SUGGESTIONS was direct-only|

Official Google Maven apksig8.7.3 is pinned in [signing-toolchain.lock.json](signing-toolchain.lock.json), is Apache-2.0 and is not packaged. [AOSP apksig](https://android.googlesource.com/platform/tools/apksig/+/refs/heads/main/README.md), [Android signing documentation](https://developer.android.com/tools/apksigner).

Regular builds remain unsigned. Future signed updates must preserve the certificate and increase version code. Never alter a signed APK. Signature integrity does not establish conversion quality, native usability or privacy on a compromised OS. See [daily-use gate](DAILY_USE_READINESS.md) and [device plan](DEVICE_TEST_PLAN.md).
