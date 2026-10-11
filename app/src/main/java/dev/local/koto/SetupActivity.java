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
    }
}
