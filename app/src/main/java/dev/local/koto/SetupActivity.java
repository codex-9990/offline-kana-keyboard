/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;

/** Launcher ignores all inbound extras. Setup changes require the user's tap. */
public final class SetupActivity extends SettingsActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Button enable = new Button(this); enable.setText("1. Android のキーボード設定を開く");
        content.addView(enable, 2);
        enable.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            ResolveInfo target = getPackageManager().resolveActivity(intent, 0);
            if (target != null && (target.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                intent.setComponent(new ComponentName(target.activityInfo.packageName, target.activityInfo.name));
                startActivity(intent);
            } else Toast.makeText(this, "Android 設定から画面キーボードを開いてください。", Toast.LENGTH_LONG).show();
        });
        Button choose = new Button(this); choose.setText("2. Koto を選択"); content.addView(choose, 3);
        choose.setOnClickListener(v -> ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker());
        TextView guide = new TextView(this); guide.setTextSize(16); guide.setPadding(0,dp(12),0,dp(12));
        guide.setText("Galaxy S24: 設定 → 一般管理 → キーボードリストと標準 → Koto を ON。"
            + "その後、標準キーボードで Koto を選んでください。表示名は One UI の版で異なることがあります。\n\n"
            + "Android のキーボード警告は、入力欄の文字を受け取るすべてのキーボードに共通です。"
            + "Koto はネットワーク権限も入力の保存もありません。\n\n"
            + "同じ開発用署名の新しい APK は既存の Koto に上書きできます。署名が違う場合は無理に削除せず確認してください。"
            + "この版は試作です。まず個人情報を含まない短い文で試してください。");
        content.addView(guide,4);
    }
}
