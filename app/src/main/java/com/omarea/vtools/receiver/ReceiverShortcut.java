package com.omarea.vtools.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.omarea.shell.KeepShellPublic;

public class ReceiverShortcut extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.hasExtra("packageName")) {
            String packageName = intent.getStringExtra("packageName");
            KeepShellPublic.INSTANCE.doCmdSync("pm disable " + packageName);
        }
    }
}
