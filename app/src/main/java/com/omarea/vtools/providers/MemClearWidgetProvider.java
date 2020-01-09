package com.omarea.vtools.providers;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.omarea.common.shell.KeepShellPublic;
import com.omarea.vtools.R;

public class MemClearWidgetProvider extends AppWidgetProvider {
    public static final String CLICK_ACTION = "com.omarea.vtools.widget.CLICK"; // 点击事件的广播ACTION

    private Bitmap drawRamStatusView(Context context) {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        Resources resources = context.getResources();

        activityManager.getMemoryInfo(memoryInfo);
        float totalMem = (memoryInfo.totalMem / 1024 / 1024f);
        float availMem = (memoryInfo.availMem / 1024 / 1024f);
        float ratioState = ((totalMem - availMem) / totalMem) * 100;
        int size = 256;
        int strokeWidth = 26;
        int radius = 60;
        int padding = 45;

        //这边直接用canvas画，然后保存
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(0, 0, size, size), radius, radius, bgPaint);
        // canvas.drawColor(Color.WHITE);


        //边框画笔
        Paint cyclePaint = new Paint();
        cyclePaint.setAntiAlias(true);
        cyclePaint.setStyle(Paint.Style.STROKE);
        cyclePaint.setStrokeWidth(strokeWidth);

        cyclePaint.setColor(0x44888888); //Color.parseColor("#888888")
        canvas.drawArc(new RectF(padding, padding, size - padding, size - padding), 0f, 360f, false, cyclePaint);
        if (ratioState > 90) {
            cyclePaint.setColor(resources.getColor(R.color.color_load_veryhight));
        } else if (ratioState > 75) {
            cyclePaint.setColor(resources.getColor(R.color.color_load_hight));
        } else if (ratioState > 20) {
            cyclePaint.setColor(resources.getColor(R.color.color_load_mid));
        } else {
            cyclePaint.setColor(resources.getColor(R.color.color_load_low));
        }
        cyclePaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(new RectF(padding, padding, size - padding, size - padding), -90f, (ratioState * 3.6f) + 1f, false, cyclePaint);

        canvas.save(); //保存
        //  canvas.restore(); //

        return bitmap;
    }

    /**
     * 每次窗口小部件被更新都调用一次该方法
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_mem_clear);
        Intent intent = new Intent(context, MemClearWidgetProvider.class); // new Intent(CLICK_ACTION);
        intent.putExtra("ids", appWidgetIds);
        intent.setAction(CLICK_ACTION);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 13000, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setImageViewBitmap(R.id.widget_mem, drawRamStatusView(context));
        remoteViews.setOnClickPendingIntent(R.id.widget_mem, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    }

    /**
     * 接收窗口小部件点击时发送的广播
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {
        super.onReceive(context, intent);
        if (CLICK_ACTION.equals(intent.getAction())) {
            final PendingResult pendingResult = goAsync();
            Toast.makeText(context, "稍等~", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    KeepShellPublic.INSTANCE.doCmdSync("sync && echo 3 > /proc/sys/vm/drop_caches && echo 1 > /proc/sys/vm/compact_memory");
                    Toast.makeText(context, "缓存已回收~", Toast.LENGTH_SHORT).show();

                    if (intent.hasExtra("ids")) {
                        triggerUpdate(context, intent.getExtras().getIntArray("ids"));
                    }
                    pendingResult.finish();
                }
            }, 100);
        }
    }

    public static void triggerUpdate(Context context, int[] ids) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.setClass(context.getApplicationContext(), MemClearWidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

    /**
     * 每删除一次窗口小部件就调用一次
     */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    /**
     * 当最后一个该窗口小部件删除时调用该方法
     */
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    /**
     * 当该窗口小部件第一次添加到桌面时调用该方法
     */
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    /**
     * 当小部件大小改变时
     */
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    /**
     * 当小部件从备份恢复时调用该方法
     */
    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);
    }
}
