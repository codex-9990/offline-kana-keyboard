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
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.view.View;
import dev.local.koto.core.FlickLayout;
import dev.local.koto.core.FlickSensitivity;
import dev.local.koto.core.LearningCoordinator;
import dev.local.koto.core.LearningData;
import java.util.ArrayList;
import java.util.List;

/** Controls and opt-in local lexical learning. The trial pad never learns or saves input. */
public class SettingsActivity extends Activity {
    protected LinearLayout content;
    private LearningCoordinator learning;
    private TextView learningStatus,demoStatus;
    private RadioGroup retention;
    private final List<RadioButton> periodButtons=new ArrayList<>();
    private Button clearLearning;
    private boolean clearing;
    private final List<KeyView> demoKeys=new ArrayList<>();
    private static final int[] PERIODS={0,1,7,30,90};
    private final Runnable learningChanged=this::updateLearning;
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
        text("フリック感度",21);
        int distance=FlickSensitivity.clamp(prefs.getInt("flick_distance",FlickSensitivity.DEFAULT_DP));
        TextView distanceLabel=text("判定距離: " + distance + " dp（小さい値ほど高感度）",16);
        SeekBar flickSlider=new SeekBar(this); flickSlider.setMax(FlickSensitivity.MAX_DP-FlickSensitivity.MIN_DP);
        flickSlider.setProgress(distance-FlickSensitivity.MIN_DP);
        flickSlider.setContentDescription("フリック判定距離。8 から 40 dp。小さい値ほど短い動きでフリック"); content.addView(flickSlider);
        flickSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar view,int value,boolean user) {
                int dp=FlickSensitivity.MIN_DP+value;
                distanceLabel.setText("判定距離: " + dp + " dp（小さい値ほど高感度）");
                for (KeyView key:demoKeys) key.flickDistance(dp);
                if (user) prefs.edit().putInt("flick_distance",dp).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar view) {}
            @Override public void onStopTrackingTouch(SeekBar view) {}
        });
        Button reset=new Button(this); reset.setText("感度を標準の 22 dp に戻す"); content.addView(reset);
        reset.setOnClickListener(v->{
            prefs.edit().putInt("flick_distance",FlickSensitivity.DEFAULT_DP).apply();
            flickSlider.setProgress(FlickSensitivity.DEFAULT_DP-FlickSensitivity.MIN_DP);
        });
        text("お試し：ここは入力欄へ送らず、学習も保存もしません。最後の 1 文字だけ表示します。",15);
        demoStatus=text("お試し: 未入力",18);
        LinearLayout pad=new LinearLayout(this); pad.setOrientation(LinearLayout.HORIZONTAL);
        pad.setMotionEventSplittingEnabled(false); content.addView(pad,new LinearLayout.LayoutParams(-1,dp(72)));
        for (int i=0;i<3;i++) {
            final int index=i;
            KeyView key=new KeyView(this,FlickLayout.kana(i,0),FlickLayout.key(i),
                    direction->demoStatus.setText("お試し: "+FlickLayout.kana(index,direction)),false,false,false,distance);
            demoKeys.add(key); pad.addView(key,new LinearLayout.LayoutParams(0,-1,1));
        }
        text("端末内の予測候補・保存期間",21);
        text("初期設定はオフです。同梱辞書で完全一致する単語を候補タップ、またはキーボード内の仮選択を「確定」で明示的に確定し、入力先への送信に成功した場合だけ、"
                + "読み・単語・最後に選んだ時刻をこのアプリ内に保存します。読みの前方一致にも候補として使います。"
                + "空白・続けて入力・モード切替・カーソル移動では確定せず、学習もしません。"
                + "通常の入力文や入力欄の周囲、取り消し、プレビューだけ、失敗した確定、分割候補は保存しません。",15);
        learning=LearningStore.get(this);
        retention=new RadioGroup(this); retention.setOrientation(RadioGroup.VERTICAL);
        retention.setContentDescription("端末内学習の保存期間。初期はオフ"); content.addView(retention);
        String[] periodLabels={"オフ（学習データを削除）","1 日","7 日","30 日","90 日"};
        for (int i=0;i<PERIODS.length;i++) {
            final int days=PERIODS[i];
            RadioButton option=new RadioButton(this); option.setId(View.generateViewId()); option.setText(periodLabels[i]);
            periodButtons.add(option); retention.addView(option);
            // Only a click changes policy; programmatic radio refresh cannot write settings.
            option.setOnClickListener(v->{
                LearningCoordinator.State current=learning.state();
                if (!current.busy && days!=current.days) learning.configure(days);
                updateLearning();
            });
        }
        learningStatus=text("学習データを確認中",15);
        clearLearning=new Button(this); clearLearning.setText("学習データをすべて削除"); content.addView(clearLearning);
        clearLearning.setOnClickListener(v->{ if (learning.clear()) { clearing=true; updateLearning(); } });
        CheckBox privateMode=new CheckBox(this); privateMode.setText("私用モード：学習と保存済み候補を使わない");
        privateMode.setChecked(prefs.getBoolean("incognito",false)); content.addView(privateMode);
        privateMode.setOnCheckedChangeListener((view,value)->prefs.edit().putBoolean("incognito",value).apply());
        text("期限は最後に明示的に選んだ時刻からです。期限を過ぎた候補は使わず、次の起動・表示・候補読取り時に背景で整理します。"
                + "アプリ停止中に指定時刻で削除する仕組みではありません。期間を短くすると古い記録を削除します。"
                + "オフは記録を削除して保存を止めます。全削除後も期間設定は残り、以後の明示選択で新しく学習します。"
                + "パスワード、学習禁止、候補禁止、メール・URL・氏名・住所などの欄では学習しません。"
                + "通常欄での機密情報は自動では判別できないため、私用モードをご利用ください。"
                + "全削除後も同じ単語が同梱辞書の候補に出ることはあります。入力先に渡した文字も削除しません。",15);
        text("学習は最大 128 件・32 KiB。古い記録から整理し、ファイル処理は背景でまとめて行います。"
                + "一時ファイルも最大 32 KiB で、起動・書込み完了時に整理します。辞書本体と表示設定は全削除後も残ります。"
                + "保存や削除に失敗すると学習を停止して表示します。通常のファイル削除であり、物理的な復元不能消去は保証しません。",15);
        updateLearning();
        text("入力: 中央をタップ。左・上・右・下にフリックで他のかな。゛小 で濁点・半濁点・小文字を切替。\n\n"
            + "通常のかな・英字・記号はキーボード内の未確定欄で編集し、確定するまで入力欄へ送りません。"
            + "候補をタップ、または「確定」で送ります。「変換」はキーボード内だけで候補を仮表示します。"
            + "次の文字を入力すると仮選択を取り消し、元の読みに追加します。"
            + "ABC・かな・記号の切替は仮選択を取り消して読みを保持します。"
            + "空白は未確定入力に追加され、確定しません。← → は入力欄のカーソルを動かして読みと仮選択を保持します。確定時の実際の選択位置へ送ります。\n\n"
            + "仮選択中の ⌫ は一度目で読みへ戻り、もう一度で文字を削除します。未確定入力が空なら入力欄の文字を削除します。"
            + "「取消」は未確定入力をすべて破棄します。"
            + "キーボードを隠す・別の入力欄へ移る・入力が再開始されると未確定入力は取り消されます。送る前に「確定」を押してください。"
            + "設定を開いて非表示になる場合も同じです。同じ入力の再配置では読みを保持しますが、回転などで非表示や入力の再開始が起きると取り消されます。"
            + "未確定欄は 256 字までです。上限では自動確定せず、確定・削除・取消で続けられます。"
            + "パスワード・数値・電話・日時・種類不明の欄は直接入力し、未確定欄や候補を表示しません。\n\n"
            + "ABC は英語、123 は数字・記号。記号 1 / 記号 2 で全 ASCII 記号を切替。$ は最初の記号画面、~ は記号 2 です。"
            + "かなの 〜 と ASCII の ~ は異なります。⌫ と ← → は触れるとすぐ動き、長押しで繰り返します。離すと停止します。\n\n"
            + "入力先の独自操作は「実行」と操作名を表示します。↵ は改行です。確定を押しただけでは入力先の操作を実行しません。\n\n"
            + "同梱 SKK 辞書で変換します。長い読みには辞書で分割した候補も表示します（分割の表示）。"
            + "分割と一部の丁寧語の規則を使う簡易方式で、文脈・文法・名前を正確に判断するものではありません。"
            + "学習 OFF でも動作し、分割候補は保存しません。横画面はキー高さ 44 dp、全画面入力なし。", 16);
        text("手動辞書変換: 自動候補を要求しない通常の文章欄（候補禁止フラグ）でも、読みをキーボード内で編集できます。"
            + "読みを入力して「変換」を押したときだけ、同梱の固定辞書の候補を表示します。候補タップまたは「確定」で送ります。"
            + "保存済み学習の参照・保存と、自動の分割候補は行いません。読みを変更すると候補を閉じ、再び「変換」を押すまで表示しません。"
            + "メール・URL・英字優先（FORCE_ASCII）の欄は ABC から始まります。日本語は「かな」→読み→「変換」で手動変換します。"
            + "英字キーはいつでも使えます。入力先が日本語の送信を拒否する場合は、読みを保持して編集・取消できます。"
            + "入力先の文字制限を回避する機能ではありません。パスワード・数値・電話・日時・種類不明の欄では手動変換もしません。", 16);
        text("プライバシー", 21);
        text("文章の入力履歴、クリップボード機能、音声入力はありません。パスワード欄では候補や未確定表示を行いません。"
            + "学習をオンにした場合だけ明示選択の最小限の単語記録を保存します。設定と学習データはバックアップ・端末間転送から除外しています。\n\n"
            + "未確定の読みや仮選択はキーボード内に保持し、入力先には送りません。"
            + "確定した通常入力・手動変換と、パスワード・数値・電話・日時・種類不明欄の直接入力は入力先アプリに渡ります。"
            + "入力先アプリや改変された OS の通信は、このキーボードの保証の範囲外です。", 15);
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
    private void updateLearning() {
        if (learning==null || retention==null) return;
        LearningCoordinator.State state=learning.state();
        for (RadioButton option:periodButtons) option.setEnabled(!state.busy);
        clearLearning.setEnabled(!state.busy);
        int index=0; for (int i=0;i<PERIODS.length;i++) if (PERIODS[i]==state.days) index=i;
        retention.check(periodButtons.get(index).getId());
        String message=state.busy ? "背景で処理中。完了まで学習候補は使いません。"
                : state.failed ? "保存または削除が完了しませんでした。学習は停止中です。全削除で再試行してください。"
                : "保存済み " + state.count(System.currentTimeMillis()) + " 件 / 最大 " + LearningData.MAX_ENTRIES + " 件"
                    + (state.days==0 ? " · 学習 OFF" : " · 保存期間 " + state.days + " 日");
        if (clearing && !state.busy) {
            if (!state.failed) message="学習データを削除しました。辞書本体と設定は残っています。";
            clearing=false;
        }
        learningStatus.setText(message);
    }
    @Override protected void onResume() {
        super.onResume(); if (learning!=null) { learning.addListener(learningChanged); learning.maintain(); updateLearning(); }
    }
    @Override protected void onPause() {
        if (learning!=null) learning.removeListener(learningChanged);
        for (KeyView key:demoKeys) key.cancelGesture();
        if (demoStatus!=null) demoStatus.setText("お試し: 未入力");
        super.onPause();
    }
}
