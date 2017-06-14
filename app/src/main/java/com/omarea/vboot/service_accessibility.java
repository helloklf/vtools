package com.omarea.vboot;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

import com.omarea.shared.AutoClickService;
import com.omarea.shared.ServiceHelper;

/**
 * Created by helloklf on 2016/8/27.
 */
public class service_accessibility extends AccessibilityService {

    @Override
    public void onCreate() {
        super.onCreate();

        /*
        Notification.Builder builder = new Notification.Builder(this);
        //Intent mIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://blog.csdn.net/itachi85/"));
        Intent mIntent = new Intent(getApplicationContext(),activity_accessibility_service_settings.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mIntent, 0);
        builder.setContentIntent(pendingIntent);
        builder.setSmallIcon(R.drawable.linux);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.linux));
        builder.setAutoCancel(true);
        //RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notxxx_layout);
        //builder.setContent(remoteViews);
        //builder.setContentTitle("微工具箱");
        //builder.setContentInfo("增强服务正在运行，点此进入设置");
        //Notification.Action action = new Notification.Action(R.drawable.p3,"性能",pendingIntent);
        //Notification.Action action1 = new Notification.Action(R.drawable.p2,"均衡",pendingIntent);
        //Notification.Action action2 = new Notification.Action(R.drawable.p1,"省电",pendingIntent);
        //builder.addAction(action);
        //builder.addAction(action1);
        //builder.addAction(action2);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //notificationManager.notify(74545342, builder.build());

        startForeground(74545342, builder.build());//该方法已创建通知管理器，设置为前台优先级后，点击通知不再自动取消
        */
    }

    ServiceHelper serviceHelper = null;

    @Override
    public void onServiceConnected() {
        /*
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        // We are interested in all types of accessibility events.
        info.eventTypes = AccessibilityEvent.TYPE_WINDOWS_CHANGED;
        // We want to provide specific type of feedback.
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
        // We want to receive events in a certain interval.
        info.notificationTimeout = 100;
        // We want to receive accessibility events only from certain packages.
        info.packageNames = null;
        setServiceInfo(info);
        */
        super.onServiceConnected();

        if (serviceHelper == null)
            serviceHelper = new ServiceHelper(getApplicationContext());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String packageName = event.getPackageName().toString().toLowerCase();

        /*if(
            packageName.equals("com.android.packageinstaller")||
            packageName.equals("com.miui.packageinstaller")||
            packageName.equals("com.mokee.packageinstaller")||
            packageName.equals("com.google.android.packageinstaller"))*/
        if (packageName.contains("packageinstaller")) {
            packageName = "com.android.packageinstaller";
            new AutoClickService().packageinstallerAutoClick(event);
        } else if (packageName.equals("com.miui.securitycenter")) {
            new AutoClickService().miuiUsbInstallAutoClick(event);
            return;
        }
        if (serviceHelper != null)
            serviceHelper.onAccessibilityEvent(packageName);
    }

    @Override
    public void onInterrupt() {
        if (serviceHelper != null)
            serviceHelper.onInterrupt();
        //android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void onDestroy() {
        if (serviceHelper != null)
            serviceHelper.onInterrupt();
        //android.os.Process.killProcess(android.os.Process.myPid());
    }
}
