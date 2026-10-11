/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import dev.local.koto.core.LearningCoordinator;
import dev.local.koto.core.LearningFileStorage;
import dev.local.koto.core.LearningRepository;
import java.util.concurrent.Executors;

/** One application-scoped worker; no Activity context, input log or external path. */
public final class LearningStore {
    private static LearningCoordinator instance;
    private LearningStore() {}
    public static synchronized LearningCoordinator get(Context context) {
        if (instance==null) {
            Context app=context.getApplicationContext();
            SharedPreferences prefs=app.getSharedPreferences("controls",Context.MODE_PRIVATE);
            LearningRepository.Policy policy=new LearningRepository.Policy() {
                @Override public int days() { return prefs.getInt("learning_days",0); }
                @Override public boolean cleanupRequired() { return prefs.getBoolean("learning_cleanup_required",false); }
                @Override public boolean save(int days,boolean cleanup) {
                    return prefs.edit().putInt("learning_days",days).putBoolean("learning_cleanup_required",cleanup).commit();
                }
            };
            LearningRepository repository=new LearningRepository(
                    new LearningFileStorage(()->app.getNoBackupFilesDir().toPath()),policy);
            Handler main=new Handler(Looper.getMainLooper());
            instance=new LearningCoordinator(repository,Executors.newSingleThreadExecutor(),
                    task->main.post(task),System::currentTimeMillis);
        }
        return instance;
    }
}
