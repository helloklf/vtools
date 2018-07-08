package com.omarea.vboot

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.omarea.shared.AutoClickService
import com.omarea.shared.BootService
import com.omarea.shared.CrashHandler
import com.omarea.shared.ServiceHelper

/**
 * Created by helloklf on 2016/8/27.
 */
class AccessibilityServiceVTools : AccessibilityService() {

    /*
override fun onCreate() {
    super.onCreate()

    Notification.Builder builder = new Notification.Builder(this);
    //Intent mIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://blog.csdn.net/itachi85/"));
    Intent mIntent = new Intent(getApplicationContext(),AccessibilitySettingsActivity.class);
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
    }
    */

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val crashHandler = CrashHandler()
        crashHandler.init(this)

        initServiceHelper()

        try {
            val service = Intent(this, BootService::class.java)
            //service.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startService(service)
        } catch (ex: Exception) {
        }

        return super.onStartCommand(intent, flags, startId)
    }

    internal var serviceHelper: ServiceHelper? = null


    public override fun onServiceConnected() {
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
        super.onServiceConnected()
    }

    private fun initServiceHelper() {
        if (serviceHelper == null)
            serviceHelper = ServiceHelper(this)
    }

    private fun tryGetActivity(componentName: ComponentName): ActivityInfo? {
        try {
            return packageManager.getActivityInfo(componentName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
    }

    fun topAppPackageName(): String {
        var packageName = "";

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            val end = System.currentTimeMillis();
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager?
            if (null == usageStatsManager) {
                return packageName;
            }
            val events = usageStatsManager.queryEvents((end - 5 * 1000), end);
            if (null == events) {
                return packageName;
            }
            val usageEvent = UsageEvents.Event();
            var lastMoveToFGEvent: UsageEvents.Event? = null;
            while (events.hasNextEvent()) {
                events.getNextEvent(usageEvent);
                if (usageEvent.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastMoveToFGEvent = usageEvent
                }
            }
            if (lastMoveToFGEvent != null) {
                packageName = lastMoveToFGEvent.getPackageName();
            }
        }
        return packageName;
    }

    var lastPackage = ""
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName == null || event.className == null)
            return
        //android.app.AlertDialog

        //val root = rootInActiveWindow
        //if (root == null) {
        //    return
        //}
        /*
        if(event.source != null && event.source.window != null)
            if (event.source.window.type != -1) {

            }
        if(event.source.parent != null && event.source.window != null) {
            if (event.source.window.type != -1) {

            }
        }
        val componentName = ComponentName(
                event.packageName.toString(),
                event.className.toString()
        )
        val activityInfo = tryGetActivity(componentName)
        val isActivity = activityInfo != null
        if(isActivity) {

        }
        */

        val packageName = event.packageName.toString().toLowerCase()
        //修复傻逼一加桌面文件夹抢占焦点导致的问题
        /*
        if ((packageName == "net.oneplus.h2launcher" || packageName == "net.oneplus.launcher") && event.className == "android.widget.LinearLayout") {
            return
        }
        */
        if (packageName.contains("packageinstaller")) {
            if (event.className == "com.android.packageinstaller.permission.ui.GrantPermissionsActivity")
                return
            AutoClickService().packageinstallerAutoClick(this.applicationContext, event)
        } else if (packageName == "com.miui.securitycenter") {
            AutoClickService().miuiUsbInstallAutoClick(event)
            return
        }

        if (packageName == "com.android.systemui" || lastPackage == packageName)
            return
        val source = rootInActiveWindow //event.source
        if (source == null) {
            return
        }
        val windowInfo = source.window

        if (windowInfo != null && windowInfo.type == AccessibilityWindowInfo.TYPE_APPLICATION && windowInfo.isActive) {
            lastPackage = packageName
            if (serviceHelper == null)
                initServiceHelper()
            serviceHelper?.onFocusAppChanged(event.packageName.toString())
        } else {

        }
    }
    /*
    Thread(Runnable {
        val inst = Instrumentation()
        inst.sendKeyDownUpSync(event.keyCode) }).start()
        */

    private fun deestory() {
        if (serviceHelper != null) {
            serviceHelper!!.onInterrupt()
            serviceHelper = null
            stopSelf()
        }
        //android.os.Process.killProcess(android.os.Process.myPid());
    }

    override fun onUnbind(intent: Intent?): Boolean {
        deestory()
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {
        //this.deestory()
        Log.e("onInterrupt", "Service Interrupt")
    }

    override fun onDestroy() {
        this.deestory()
    }
}