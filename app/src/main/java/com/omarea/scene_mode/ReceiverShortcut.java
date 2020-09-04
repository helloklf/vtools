package com.omarea.scene_mode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.omarea.store.SpfConfig;

// 应用偏见（添加完快捷方式后冻结应用）
public class ReceiverShortcut extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.hasExtra("packageName")) {
            String packageName = intent.getStringExtra("packageName");
            if (packageName.equals(context.getPackageName())) {
                return;
            }

            SharedPreferences config = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE);
            boolean useSuspendMode = config.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_SUSPEND, Build.VERSION.SDK_INT >= Build.VERSION_CODES.P);
            if (useSuspendMode) {
                SceneMode.Companion.suspendApp(packageName);
            } else {
                SceneMode.Companion.freezeApp(packageName);
            }
        }
    }
}
