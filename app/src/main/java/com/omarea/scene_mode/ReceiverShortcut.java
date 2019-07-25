package com.omarea.scene_mode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.omarea.common.shell.KeepShellPublic;

public class ReceiverShortcut extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.hasExtra("packageName")) {
            String packageName = intent.getStringExtra("packageName");
            if (packageName.equals(context.getPackageName())) {
                return;
            }
            KeepShellPublic.INSTANCE.doCmdSync("pm disable " + packageName);
        }
    }
}
