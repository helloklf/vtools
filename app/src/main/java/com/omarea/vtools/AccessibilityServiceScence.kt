package com.omarea.vtools

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import com.omarea.shared.AppConfigStore
import com.omarea.shared.AutoClick
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
    private var layerDetection = true

    private var eventWindowStateChange = true
    private var eventWindowContentChange = false
    private var eventViewClick = false
    private var sceneConfigChanged: BroadcastReceiver? = null

    private var lastPackageName = ""

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
        CrashHandler().init(this)
        /*
        try {
            val service = Intent(this, BootService::class.java)
            //service.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startService(service)
        } catch (ex: Exception) {
        }
        */

        return super.onStartCommand(intent, flags, startId)
    }

    internal var serviceHelper: ServiceHelper? = null

    private fun updateConfig() {
        val spf = getSharedPreferences("adv", Context.MODE_PRIVATE)
        flagReportViewIds = spf.getBoolean("adv_find_viewid", flagReportViewIds)
        flagRequestKeyEvent = spf.getBoolean("adv_keyevent", AppConfigStore(this.applicationContext).needKeyCapture())
        flagRetriveWindow = spf.getBoolean("adv_retrieve_window", flagRetriveWindow)

        eventWindowStateChange = spf.getBoolean("adv_event_window_state", eventWindowStateChange)
        eventWindowContentChange = spf.getBoolean("adv_event_content_change", eventWindowContentChange)
        eventViewClick = spf.getBoolean("adv_event_view_click", eventViewClick)

        val info = serviceInfo // AccessibilityServiceInfo();
        // We are interested in all types of accessibility events.
        info.eventTypes = AccessibilityEvent.TYPE_WINDOWS_CHANGED
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
    }

    public override fun onServiceConnected() {
        serviceIsConnected = true
        updateConfig()
        if (sceneConfigChanged == null) {
            sceneConfigChanged = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    updateConfig()
                    Toast.makeText(context, "动态响应配置参数已更新，将在下次切换应用时生效！", Toast.LENGTH_SHORT).show()
                }
            }
            registerReceiver(sceneConfigChanged, IntentFilter(getString(R.string.scene_key_capture_change_action)))
        }
        super.onServiceConnected()
        if (serviceHelper == null)
            serviceHelper = ServiceHelper(this)
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
        try {
            if (event.packageName == null || event.className == null)
                return

            var packageName = event.packageName.toString()
            if (packageName == "android" || packageName == "com.android.systemui") {
                return
            }
            if (packageName.contains("packageinstaller")) {
                if (event.className == "com.android.packageinstaller.permission.ui.GrantPermissionsActivity")
                    return

                if (flagRetriveWindow) {
                    try {
                        AutoClick().packageinstallerAutoClick(this.applicationContext, event)
                    } catch (ex: Exception) {
                    }
                }
            } else if (packageName == "com.miui.securitycenter") {
                try {
                    AutoClick().miuiUsbInstallAutoClick(event)
                } catch (ex: Exception) {
                }
                return
            }

            // 针对一加部分系统的修复
            if ((packageName == "net.oneplus.h2launcher" || packageName == "net.oneplus.launcher") && event.className == "android.widget.LinearLayout") {
                return
            }

            if (flagReportViewIds && event.source != null) {
                val windows_ = windows
                if (windows_ == null || windows_.isEmpty()) {
                    return
                } else if (windows_.size > 1) {
                    // Log.d("onAccessibilityEvent", ">>>" + event.contentChangeTypes)
                    var lastWindow: AccessibilityWindowInfo? = null

                    /**
                    Window          层级(zOrder - window.layer)
                    --------------------------------------------
                    应用Window	    1~99
                    子Window	    1000~1999
                    系统Window	    2000~2999
                     */
                    val effectiveWindows =  windows_.filter {
                        (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.isInPictureInPictureMode)) && (it.type == AccessibilityWindowInfo.TYPE_APPLICATION)
                    }.sortedBy { it.layer }

                    if (effectiveWindows.size > 0) {
                        if (layerDetection) {
                            /*
                            for (window in windows_.iterator()) {
                                if (window.type == AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER) {
                                    Log.d("onAccessibilityEvent", "分屏组件")
                                } else if (window.isFocused) {
                                    if (window.root != null) {
                                        Log.d("onAccessibilityEvent", "active " + window.root.packageName + "   " + window.id + "  " + window.layer)
                                    } else {
                                        Log.d("onAccessibilityEvent", "active " + window.title + "   " + window.id + "  " + window.layer)
                                    }
                                } else {
                                    Log.d("onAccessibilityEvent", "inactive " + window.title + "   " + window.id + "  " + window.layer)
                                }
                            }
                            */
                            lastWindow = effectiveWindows.get(0)

                            try {
                                val source = event.source
                                if (source == null || source.windowId != lastWindow.id) {
                                    val windowRoot = lastWindow!!.root
                                    if (windowRoot == null || windowRoot.packageName == null) {
                                        return
                                    }
                                    packageName = windowRoot.packageName!!.toString()
                                    Log.d("Scene Fix Top App", "" + windowRoot.packageName)
                                }
                            } catch (ex: Exception) {
                                return
                            }
                        } else {
                            for (window in effectiveWindows.iterator()) {
                                if (window.isFocused) {
                                    // Log.d("onAccessibilityEvent", "" + window.title + "   " + window.layer)
                                    lastWindow = window
                                } else {
                                    // Log.d("onAccessibilityEvent", "inactive " + window.title + "   " + window.layer)
                                }
                            }

                            if (lastWindow == null) {
                                return
                            }

                            val windowInfo = lastWindow
                            try {
                                val source = event.source
                                if (source == null || source.windowId != windowInfo.id) {
                                    return
                                }
                            } catch (ex: Exception) {
                            }
                        }
                    }
                }
                lastPackageName = packageName
                serviceHelper?.onFocusAppChanged(lastPackageName)
            } else {
                lastPackageName = packageName
                serviceHelper?.onFocusAppChanged(lastPackageName)
            }

            handler.postDelayed({
                if (lastPackageName != packageName) {
                    val lastEvent = topAppPackageName()
                    if (lastEvent.isNotEmpty() && lastEvent != lastPackageName) {
                        serviceHelper?.onFocusAppChanged(lastEvent)
                        lastPackageName = lastEvent
                    }
                }
            }, 2000)
        } finally {
            // event.recycle()
        }
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
            // disableSelf()
            stopSelf()
            System.exit(0)
        }
        //android.os.Process.killProcess(android.os.Process.myPid());
    }

    private var handler = Handler()
    private var downTime: Long = -1
    private var longClickTime: Long = 500;
    private var serviceIsConnected = false
    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Log.d("onKeyEvent", "keyCode " + event.keyCode);
        if (!serviceIsConnected) {
            return false
        }
        if (serviceHelper == null)
            return false

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
        if (sceneConfigChanged != null) {
            unregisterReceiver(sceneConfigChanged)
            sceneConfigChanged = null
        }
        serviceIsConnected = false
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