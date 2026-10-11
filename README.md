# Koto — Offline Japanese Keyboard 0.1.8

日本語12キーフリック、英語QWERTY、端末内の漢字候補を備えたAndroidキーボードです。学習は初期OFF。ネットワーク権限、広告、解析、音声入力、クリップボード機能はありません。

**0.1.8は個人での常用に向けた修正候補です。独立レビュー待ちで、Android実機・Galaxy S24では未検証です。常用に耐えることを確認した版ではありません。** すでに配布した0.1.7は変更せず保存しています。今回の候補APKはまだ配布していません。

## 入力方法

通常のかな・英字・記号は、キーボード内の「未確定の読み」に表示されます。候補タップ、または「確定」で入力欄に送ります。空白やモード切替では確定しません。未確定入力は最大256コードポイントです。上限では確定、削除、取消のいずれかを行ってから続けます。

「変換」で仮選択し、「確定」で送ります。仮選択中の最初の削除は読みへ戻し、その後は読みの末尾を削除します。続けて入力すると元の読みに1回だけ追加します。「取消」は未確定入力全体を破棄します。カーソルキーは入力先のカーソルを動かし、読みは保持します。後の確定は入力先の実際の選択位置に送ります。

**キーボードを隠す、新しい欄に移る、入力セッションが再開始する場合は未確定入力を破棄します。** 同じセッション内の配置変更は保持しますが、回転に伴ってOSが入力を再開始すれば破棄されます。英字・記号も通常欄では確定が必要で、入力先の自動補完には未確定入力が渡りません。

候補を自動表示しない文章欄（NO_SUGGESTIONS）でも、読みを入力して「変換」を押すと固定辞書で手動変換できます。保存済み学習や自動分割候補は使いません。メール・URL・FORCE_ASCII欄はABCから始まり、日本語は「かな」→読み→「変換」です。ユーザーから報告されたブラウザのURL/検索欄とページ欄の不具合は、そのブラウザ・S24で再検証していません。

パスワード、数値、電話、日時、種類不明の欄は直接入力し、未確定表示・候補・学習を使いません。直接入力で濁音・半濁音・小文字を入れるには「濁・小 選択」で完成文字を選びます。ひらがな・カタカナを切り替えできます。**直前の文字は置き換えません。** すでに入力した文字を修正する場合は、入力先で削除してから完成文字を選んでください。パスワードの周囲の文字を読み取ったり、元の文字を記憶したりしません。

## 0.1.8の修正

- 設定画面は再表示時と変更通知で現在の私用モード、高さ、振動、感度を読み直します。表示更新で設定を書き戻しません。私用モードを読み取れない場合はONと表示します。
- 設定画面・キーボードのルートで全面/部分的な重なりを示すタッチを拒否します。保持中のキーと通常のボタンへ取消を届け、残りのタッチを新しいDOWNまたはライフサイクルのリセットまで抑制します。フローティングウィンドウを閉じる案内を表示します。非表示・無効化・切離しでもリピートを止めます。
- 直接入力に完成した濁音/半濁音26文字と小文字12文字、両方の文字種を追加しました。通常の未確定かなの濁点操作は維持します。
- Androidの送信処理が成功を返しても、入力先が実際に文字を反映したとは確認できないことをヘルプに明記しました。送信後は入力先で確認してください。

**入力先が送信エラーを返したと検出できた場合は、通常欄の読みを保持します。エラーが返らず、入力先が後で文字制限等で反映しなかった場合は検出できず、読みは既に消えていることがあります。** 自動再送も、送信後の再編集用の入力文保持も行いません。学習ONで対象の単語を送信した場合、入力先に反映されなくてもその選択が記録されることがあります。これは確認されたAndroidのAPI契約の限界です。[編集と送信の契約](docs/COMPOSITION.md)。

## Controls and conversion

Conventional twelve-key Japanese flick and staggered English QWERTY include candidates, symbols, cursor movement and labelled editor actions. All 95 printable ASCII characters are accessible. Flick distance is 8–40 dp, default/reset 22 dp. Height is 44–76 dp, default 58; landscape keys use 44 dp. Haptics default off; there is no fullscreen extraction. The Settings try pad displays its last kana only and resets when hidden. Accessibility click/flick actions remain available after blocked touch gestures. Native S24 layout, touch targets, insets and haptics remain unmeasured.

The licensed SKK-JISYO.ML dictionary has 48,750 keys / 67,342 candidates. Kana/katakana fallbacks remain reachable within 64 candidates. Ordinary fields also offer bounded heuristic phrase proposals labelled 分割. This is dictionary-based conversion with limited segmentation/conjugation rules, rather than Mozc or full contextual grammar conversion. Homophones, names and longer text need manual selection. Explicit-only fields use fixed dictionary and kana fallbacks. No surrounding host text is read. [Conversion scope and Mozc assessment](docs/CONVERSION.md).

Learning defaults off. Settings provide Off / 1 / 7 / 30 / 90 days, clear-all and private mode. Only eligible explicit dictionary-exact lexical choices dispatched without a detected error may record reading, word and last-selection time. Raw kana, generated phrases, previews, cancellation and detected failures do not record. Private/password/NO_PERSONALIZED_LEARNING/NO_SUGGESTIONS/sensitive policies block remembered queries and recording. Limits are 128 records / 32 KiB, one fixed no-backup data/temp pair, one worker and at most 32 pending choices. Clear/Off invalidates pending choices; an operation already beyond its final privacy check can finish. Expiry is checked on use; exact-time stopped-app deletion and physical secure erasure are not promised. [Local learning](docs/LOCAL_LEARNING.md).

## Privacy and verification

The final signed APK requests zero permissions, disables debugging/backup/cleartext, protects the necessary exported IME service with BIND_INPUT_METHOD, and keeps Settings internal. Only the launcher is otherwise exported. All nine legacy/cloud/device-transfer backup domains are excluded. Source and DEX checks found no network, logging, analytics, speech, clipboard, upload or surrounding-text-read path.

Confirmed ordinary text and direct characters go to the host app, which may transmit them. Host apps, OS/root compromise, privileged accessibility/screen capture and reliable memory erasure are outside the keyboard's no-egress boundary. Legitimate floating windows can cause touch rejection; native reachability/overlay behavior remains untested. Airplane mode alone is not privacy proof. [Threat model](docs/THREAT_MODEL.md).

PASS: 102 core/feature/hardening groups, 47 Android API host groups, 18 production UI model groups, 236,824 bounded sequences, 3,000 repeated session cycles, 56 help + 6 compatibility mutations, 30 source checks and 27 signed APK checks. Signature/payload/tamper results are in [VERIFICATION.md](docs/VERIFICATION.md). Models execute production code with explicit shims; they do not validate native WindowManager, Binder, Looper, UI hit-testing or One UI. Android/S24 runtime, backup/transfer, latency and the reported browser remain NOT RUN. [Daily-use gate](docs/DAILY_USE_READINESS.md).

## Build, review and licensing

Use Linux x86_64, a complete JDK 17+, Python 3 and the existing pinned compiler cache:

    export JAVA_HOME=/path/to/jdk-17
    python3 scripts/prepare_dictionary.py --check
    bash scripts/test_host.sh
    bash scripts/test_android_adapter.sh
    python3 tests/ui-model/run_ui_model.py
    python3 scripts/build_aosp.py

A first build requires an explicit --fetch for three pinned recognized compiler artifacts. This candidate reused the existing cache without installing an SDK or accepting an agreement. The optional UI model uses JAVA_HOME and requires the host tests and Android API jar first. Conventional Gradle/lint and native Android testing need an approved SDK/emulator environment. [Toolchain](docs/TOOLCHAIN.md), [runtime setup](docs/RUNTIME_QA.md), [device matrix](docs/DEVICE_TEST_PLAN.md).

Outputs are dist/offline-kana-keyboard-0.1.8-trial-unsigned.apk and the separately development-signed dist/offline-kana-keyboard-0.1.8-trial.apk; version code 9. Candidate application commit: **679cc413005fe15ff53b39a0ee906646d96462d8**. Signed APK: **516,774 bytes**, SHA-256 **c1fc2b00570e27e0ddb6c501beae2559946c31f4d4277632376151e29d19e8d5**. The same approved development certificate is reused. Signing credentials are excluded from source and evidence. [Signing receipt](docs/SIGNING.md).

Application, tests, docs and dictionary are GPL-2.0-or-later with original dictionary source/header, notices and license text. Gradle wrapper utilities are separately Apache-2.0. No Sumire/Gboard/Mozc code or binary was adopted.

The existing [source repository](https://github.com/codex-9990/offline-kana-keyboard) is the review route. Candidate APK delivery to the user's Library remains held pending independent review. No public APK release or device installation/IME activation occurred. Prior delivered0.1.7 and earlier artifacts are preserved;0.1.3–0.1.5 have known composition regressions and should not be used for daily typing.
