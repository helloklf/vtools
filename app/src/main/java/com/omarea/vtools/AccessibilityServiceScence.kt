package com.omarea.vtools

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import com.omarea.shared.AutoClickService
import com.omarea.shared.BootService
import com.omarea.shared.CrashHandler
import com.omarea.shared.ServiceHelper

/**
 * Created by helloklf on 2016/8/27.
 */
class AccessibilityServiceScence : AccessibilityService() {
    private var flagReportViewIds = true
    private var flagRequestKeyEvent = true
    private var flagRetriveWindow = true
    private var flagRequestAccessbilityButton = false

    private var eventWindowStateChange = true
    private var eventWindowContentChange = false
    private var eventViewClick = false


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
    //builder.setContentTitle("Scene");
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

        val spf = getSharedPreferences("adv", Context.MODE_PRIVATE)
        flagReportViewIds = spf.getBoolean("adv_find_viewid", flagReportViewIds)
        flagRequestKeyEvent = spf.getBoolean("adv_keyevent", flagRequestKeyEvent)
        flagRetriveWindow = spf.getBoolean("adv_retrieve_window", flagRetriveWindow)

        eventWindowStateChange = spf.getBoolean("adv_event_window_state", eventWindowStateChange)
        eventWindowContentChange = spf.getBoolean("adv_event_content_change", eventWindowContentChange)
        eventViewClick = spf.getBoolean("adv_event_view_click", eventViewClick)

        val info = serviceInfo // AccessibilityServiceInfo();
        // We are interested in all types of accessibility events.
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        if (eventWindowStateChange) {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        }
        if (eventWindowContentChange) {
            info.eventTypes = info.eventTypes or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        }
        if (eventViewClick) {
            info.eventTypes = info.eventTypes or AccessibilityEvent.TYPE_VIEW_CLICKED
        }
        // We want to provide specific type of feedback.
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        // We want to receive events in a certain interval.
        info.notificationTimeout = 0;
        // We want to receive accessibility events only from certain packages.
        info.packageNames = null;

        info.flags = AccessibilityServiceInfo.DEFAULT
        if (flagRetriveWindow) {
            info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        if (flagReportViewIds) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        if (flagRequestKeyEvent) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && flagRequestAccessbilityButton) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON
        }

        setServiceInfo(info);
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

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
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
        if (packageName == "android" || packageName == "com.android.systemui") {
            return
        }
        // 针对一加部分系统的修复
        if ((packageName == "net.oneplus.h2launcher" || packageName == "net.oneplus.launcher") && event.className == "android.widget.LinearLayout") {
            return
        }

        if (flagRetriveWindow) {
            if (packageName.contains("packageinstaller")) {
                if (event.className == "com.android.packageinstaller.permission.ui.GrantPermissionsActivity")
                    return
                try {
                    AutoClickService().packageinstallerAutoClick(this.applicationContext, event)
                } catch (ex: Exception) {
                }
            } else if (packageName == "com.miui.securitycenter") {
                try {
                    AutoClickService().miuiUsbInstallAutoClick(event)
                } catch (ex: Exception) {
                }
                return
            }
        }

        /*
        val rootWindow = rootInActiveWindow
        val source = event.source //rootInActiveWindow //event.source

        if (source == null || rootWindow.windowId != source.windowId) {
            return
        }

        val windowInfo = source.window
        */
        if (flagReportViewIds) {
            val windows_ = windows
            if (windows_ == null || windows_.isEmpty()) {
                return
            }
            val windowInfo = windows_.lastOrNull()

            /**
            Window          层级(zOrder)
            --------------------------
            应用Window	    1~99
            子Window	    1000~1999
            系统Window	    2000~2999
             */

            val source = event.source
            if (source == null || windowInfo == null || source.windowId != windowInfo.id) {
                return
            }

            if (windowInfo.type == AccessibilityWindowInfo.TYPE_APPLICATION && windowInfo.isActive) {
                if (serviceHelper == null)
                    initServiceHelper()
                serviceHelper?.onFocusAppChanged(event.packageName.toString())
            } else {
                Log.d("vtool-dump", "[skip app:${packageName}]")
            }
        } else {
            if (serviceHelper == null)
                initServiceHelper()
            serviceHelper?.onFocusAppChanged(event.packageName.toString())
        }
        // event.recycle()
    }
    /*
    Thread(Runnable {
    val inst = Instrumentation()
    inst.sendKeyDownUpSync(event.keyCode)
    }).start()
    */

    private fun deestory() {
        Toast.makeText(applicationContext, "Scene - 辅助服务已关闭！", Toast.LENGTH_SHORT).show()
        if (serviceHelper != null) {
            serviceHelper!!.onInterrupt()
            serviceHelper = null
            stopSelf()
            System.exit(0)
        }
        //android.os.Process.killProcess(android.os.Process.myPid());
    }

    private var handler = Handler()
    private var downTime: Long = -1
    private var longClickTime: Long = 500;
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (serviceHelper == null)
            initServiceHelper()

        val keyCode = event.keyCode
        // 只阻止四大金刚键
        if (!(keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_APP_SWITCH || keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SEARCH)) {
            return super.onKeyEvent(event)
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            downTime = event.eventTime
            val currentDownTime = downTime
            val stopEvent = serviceHelper!!.onKeyDown()
            if (stopEvent) {
                handler.postDelayed({
                    if (downTime == currentDownTime) {
                        if (keyCode == KeyEvent.KEYCODE_HOME) {
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        } else if (keyCode == KeyEvent.KEYCODE_APP_SWITCH || keyCode == KeyEvent.KEYCODE_MENU) {
                            performGlobalAction(GLOBAL_ACTION_RECENTS)
                        }
                    }
                }, longClickTime)
            }
            return stopEvent
        } else if (event.action == KeyEvent.ACTION_UP) {
            downTime = -1
            return serviceHelper!!.onKeyDown()
        } else {
            return super.onKeyEvent(event)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        deestory()
        stopSelf()
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