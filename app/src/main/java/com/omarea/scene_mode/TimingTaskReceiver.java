package com.omarea.scene_mode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class TimingTaskReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(">>>>time", "场景模式 - 定时任务 触发");
        Toast.makeText(context, "场景模式 - 定时任务 触发", Toast.LENGTH_SHORT).show();
    }
}
