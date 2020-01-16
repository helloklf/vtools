package com.omarea.scene_mode;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class TimingTaskReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (intent != null && intent.hasExtra("taskId")) {
                String taskId = intent.getStringExtra("taskId");
                if (taskId != null) {
                    Intent service = new Intent(context, SceneTaskIntentService.class);
                    service.putExtra("taskId", taskId);
                    context.startService(service);
                    Log.d(">>>>time", "场景模式 - 定时任务 触发");
                    Toast.makeText(context, "场景模式 - 定时任务 触发", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception ex) {
            Toast.makeText(context, "场景模式 - 定时任务 异常:\n" + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
