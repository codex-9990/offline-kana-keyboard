# Conversion choice and readiness

## This version

The keyboard uses an actual established offline dictionary: SKK-JISYO.ML, from `skk-dev/dict` commit `0a164e6b990c5eb5b59eb7d8789f08865dc2f644`. Its JSON source is 2,548,328 bytes, Git blob `89dba326fd271d250b550fc46f3fe3f2df667ea4`, SHA-256 `885ed8e2aa38c0249c79fe428aed9c419133984ddff7fcef69035ea886773887`.

The source dictionary's own header explicitly applies GPL v2 or later. The new application is GPL-2.0-or-later, retains the original source/header/notice, ships a full GPL text, and displays notices in settings. The Gradle wrapper is a separate Apache-2.0 utility. No assumption of license compatibility was taken from a project's README claim.

`scripts/prepare_dictionary.py` produces 48,750 keys and 67,342 candidates. The transform strips annotations and skips unsupported Lisp/numeric expressions instead of evaluating them. None were removed from this particular ML source. Runtime lookup does not mutate the dictionary or rank candidates using typing history.

The lookup handles exact readings, basic okurigana by matching a stem and its SKK Roman initial, and hiragana/katakana fallback. Examples checked against the real bundled data: 日本, 東京, 漢字, 学校, 橋, 食べる, 書く. Unknown input still commits as kana. At most 64 candidates are shown. Version 0.1.1 reserves space for the distinct hiragana/katakana fallbacks, including ambiguous readings such as `こう`; late dictionary words can still be omitted.

This is not a full SKK interaction model and not a contextual conversion engine. There is no sentence segmentation, linguistic cost model, phrase ranking or adaptive learning. Some mechanically formed okurigana candidates can be wrong. The dictionary is medium-large, not the complete SKK L dictionary, and its source data is older than modern Mozc vocabularies. Type and select one word at a time.

## Mozc assessment

The current [official Android build document](https://github.com/google/mozc/blob/master/docs/build_mozc_for_android.md) describes building `libmozc.so` with Bazelisk, C++ and Python 3.12+, downloading NDK r29 through `update_deps.py`, then building `android/jni:native_libs`. It explicitly says the complete Android client was deprecated at revision `afb03dd`.

This saved environment has Python 3.12 and GCC, but no Bazel/Bazelisk, Android SDK/NDK or emulator. NDK/toolchain setup would need explicit approval where it accepts new agreements. Building a shared object also does not supply a working modern IME client. These facts made it impractical as the initial vertical slice; no Mozc source or binaries were falsely labeled as integrated.

For full conversion, the next practical path is to pin a Mozc revision and dependencies, build arm64-v8a for S24 and x86_64 for emulator testing, integrate its native session/protocol APIs behind a conversion interface, and implement segment/candidate navigation. Disable engine learning/storage by default and honor restricted fields before introducing any user dictionary. Audit the complete native dependency/data package, licenses, file writes and outward paths. Then compare sentence conversion and latency on the actual S24 and at least one emulator.

## Other foundations inspected

[KazumaProject/JapaneseKeyboard (Sumire)](https://github.com/KazumaProject/JapaneseKeyboard) has an MIT code license. The inspected `dev` manifest requests RECORD_AUDIO, enables backup and cleartext traffic, and declares additional components. Its build file has multiple modules/flavors and data generation. Those facts do not prove a privacy violation, and the source was not exhaustively audited. They mean its README's offline claim cannot substitute for reviewing the assembled application. No source or binary was adopted into Koto.

Direct GitHub downloads are blocked in this saved environment. The connected GitHub reader could not return the very large L dictionary, but could return the UTF-8 ML Git blob. The saved ML bytes were verified against that pinned blob and its SHA-256. No tiny hard-coded dictionary is used for the app; small fixtures exist only in tests.
