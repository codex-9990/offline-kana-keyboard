/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/** Only these two non-text preferences can be written. No dictionary or history. */
public class SettingsActivity extends Activity {
    protected LinearLayout content;
    protected int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    protected TextView text(String value, int size) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size);
        view.setPadding(0, dp(12), 0, dp(12)); content.addView(view); return view;
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(40), dp(24), dp(36)); scroll.addView(content); setContentView(scroll);
        text("Koto キーボード", 28);
        text("端末内の単語変換。ネットワーク権限なし。", 16);
        SharedPreferences prefs = getSharedPreferences("controls", MODE_PRIVATE);
        int height = Math.max(44, Math.min(76, prefs.getInt("row_height", 58)));
        TextView heightLabel = text("キーの高さ: " + height + " dp", 18);
        SeekBar slider = new SeekBar(this); slider.setMax(32); slider.setProgress(height - 44);
        slider.setContentDescription("キーの高さ。44 から 76 dp"); content.addView(slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar view, int value, boolean user) {
                heightLabel.setText("キーの高さ: " + (44 + value) + " dp");
                if (user) prefs.edit().putInt("row_height", 44 + value).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar view) {}
            @Override public void onStopTrackingTouch(SeekBar view) {}
        });
        CheckBox haptic = new CheckBox(this); haptic.setText("キーの振動 (端末の振動設定に従う)");
        haptic.setChecked(prefs.getBoolean("haptics", false)); content.addView(haptic);
        haptic.setOnCheckedChangeListener((view, value) -> prefs.edit().putBoolean("haptics", value).apply());
        text("入力: 中央をタップ。左・上・右・下にフリックで他のかな。゛小 で濁点・半濁点・小文字を切替。\n\n"
            + "候補をタップして単語を確定。変換で候補が入力欄に仮表示されます。確定、次のかな、ABC、← → で選んだ単語を確定。"
            + "変換中の ⌫ は一度目で読みへ戻り、もう一度で文字を削除します。空白は確定後にスペースを入れます。\n\n"
            + "ABC は英語、123 は数字・記号。記号 1 / 記号 2 で全 ASCII 記号を切替。$ は最初の記号画面、~ は記号 2 です。"
            + "かなの 〜 と ASCII の ~ は異なります。⌫ と ← → は触れるとすぐ動き、長押しで繰り返します。離すと停止します。\n\n"
            + "入力先の独自操作は「実行」と操作名を表示します。↵ は改行です。確定を押しただけでは入力先の操作を実行しません。\n\n"
            + "この版は SKK 辞書の単語変換です。文章の文脈を使う変換や学習はありません。横画面はキー高さ 44 dp、全画面入力なし。", 16);
        text("プライバシー", 21);
        text("入力の履歴、学習、個人辞書、クリップボード機能、音声入力はありません。パスワード欄では候補も表示しません。"
            + "保存するのは高さと振動の設定のみ。バックアップ・端末間転送の対象から除外しています。\n\n"
            + "入力先のアプリには確定した文字と通常欄の変換中の文字が渡ります。入力先アプリや改変された OS の通信は、このキーボードの保証の範囲外です。", 15);
        text("GPL-2.0-or-later · SKK 辞書", 18);
        text("本アプリと同梱 SKK 辞書は GPL v2 またはそれ以降で利用・改変・再配布できます。ソースと辞書の著作権表示、ライセンスを配布物に含めます。", 15);
        try {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            try (java.io.InputStream input = getAssets().open("NOTICE.txt")) {
                byte[] bytes = new byte[2048]; int count;
                while ((count = input.read(bytes)) != -1) output.write(bytes, 0, count);
            }
            text(new String(output.toByteArray(), java.nio.charset.StandardCharsets.UTF_8), 12);
        } catch (java.io.IOException ignored) { text("著作権表示は配布ソースの NOTICE を参照してください。", 12); }
    }
}
