package com.omarea.vtools

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import com.omarea.data_collection.EventBus
import com.omarea.data_collection.EventType
import com.omarea.data_collection.GlobalStatus
import com.omarea.scene_mode.AppSwitchHandler
import com.omarea.store.SceneConfigStore
import com.omarea.utils.AutoClick
import com.omarea.utils.CrashHandler
import com.omarea.xposed.XposedCheck

/**
 * Created by helloklf on 2016/8/27.
 */
public class AccessibilityScenceMode : AccessibilityService() {
    private var flagReportViewIds = true
    private var flagRequestKeyEvent = true
    private var flagRetriveWindow = true
    private var flagRequestAccessbilityButton = false
    private var eventWindowStateChange = true
    private var eventWindowContentChange = false
    private var eventViewClick = false
    private var sceneConfigChanged: BroadcastReceiver? = null
    private var isLandscapf = false

    private var displayWidth = 1080
    private var displayHeight = 2340

    companion object {
        const val useXposed = false
    }

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
        return super.onStartCommand(intent, flags, startId)
    }

    internal var appSwitchHandler: AppSwitchHandler? = null


    /**
     * 屏幕配置改变（旋转、分辨率更改、DPI更改等）
     */
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        if (newConfig != null) {
            onScreenConfigurationChanged(newConfig)
        }
    }

    private fun onScreenConfigurationChanged(newConfig: Configuration) {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            isLandscapf = false
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscapf = true
        }
        getDisplaySize()
    }

    private fun getDisplaySize() {
        // 重新获取屏幕分辨率
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getRealSize(point)
        if (point.x != displayWidth || point.y != displayHeight) {
            displayWidth = point.x
            displayHeight = point.y
        }
    }

    private fun updateConfig() {
        if (useXposed && XposedCheck.xposedIsRunning()) {
            val spf = getSharedPreferences("adv", Context.MODE_PRIVATE)
            flagRequestKeyEvent = spf.getBoolean("adv_keyevent", SceneConfigStore(this.applicationContext).needKeyCapture())

            val info = serviceInfo
            if (flagRequestKeyEvent) {
                info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            } else {
                info.eventTypes = 0
            }

            setServiceInfo(info);
            GlobalStatus.homeMessage = "正在通过Xposed运行场景模式";
        } else {
            val spf = getSharedPreferences("adv", Context.MODE_PRIVATE)
            flagReportViewIds = spf.getBoolean("adv_find_viewid", flagReportViewIds)
            flagRequestKeyEvent = spf.getBoolean("adv_keyevent", SceneConfigStore(this.applicationContext).needKeyCapture())
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
    }

    public override fun onServiceConnected() {
        // 获取屏幕方向
        onScreenConfigurationChanged(this.resources.configuration)

        serviceIsConnected = true
        updateConfig()
        if (sceneConfigChanged == null) {
            sceneConfigChanged = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    updateConfig()
                    Toast.makeText(context, "性能调节配置参数已更新，将在下次切换应用时生效！", Toast.LENGTH_SHORT).show()
                }
            }
            registerReceiver(sceneConfigChanged, IntentFilter(getString(R.string.scene_key_capture_change_action)))
        }
        super.onServiceConnected()

        if (appSwitchHandler == null) {
            appSwitchHandler = AppSwitchHandler(this)
            EventBus.subscibe(appSwitchHandler)
        }
        getDisplaySize()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName == null || event.className == null)
            return

        var packageName = event.packageName.toString()

        // com.miui.freeform 是miui的应用多窗口（快速回复、游戏模式QQ微信小窗口）管理器
        if (packageName == "android" || packageName == "com.android.systemui" || packageName == "com.miui.freeform" || packageName == "com.omarea.gesture") {
            return
        }

        // 横屏时屏蔽 QQ、微信事件，因为游戏模式下通常会在横屏使用悬浮窗打开QQ 微信
        if (isLandscapf && (packageName == "com.tencent.mobileqq" || packageName == "com.tencent.mm")) {
            return
        }

        /*
         if (appSwitchHandler!!.isIgnoredApp(packageName, isLandscapf)) {
             return
         }
        */

        if (flagRetriveWindow) {
            if (packageName.contains("packageinstaller")) {
                if (event.className == "com.android.packageinstaller.permission.ui.GrantPermissionsActivity")
                    return

                try {
                    AutoClick().packageinstallerAutoClick(this.applicationContext, event)
                } catch (ex: Exception) {
                }
            } else if (packageName == "com.miui.securitycenter") {
                try {
                    AutoClick().miuiUsbInstallAutoClick(this.applicationContext, event)
                } catch (ex: Exception) {
                }
                return
            }
        }

        if (flagReportViewIds && event.source != null) {
            val windows_ = windows
            if (windows_ == null || windows_.isEmpty()) {
                return
            } else if (windows_.size > 1) {
                val effectiveWindows = windows_.filter {
                    (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.isInPictureInPictureMode)) && (it.type == AccessibilityWindowInfo.TYPE_APPLICATION)
                }.sortedBy { it.layer }

                if (effectiveWindows.size > 0) {
                    try {
                        var lastWindowPackageName: String? = null
                        for (window in effectiveWindows) {
                            val wp = window.root?.packageName
                            if (wp == null || packageName == "android" || packageName == "com.android.systemui" || packageName == "com.miui.freeform" || packageName == "com.omarea.gesture") {
                                continue
                            }
                            val outBounds = Rect()
                            window.getBoundsInScreen(outBounds)
                            // Log.d(">>>>", "${wp} left:${outBounds.left}, top:${outBounds.top}, right:${outBounds.right}, bottom:${outBounds.bottom}")
                            if (outBounds.left == 0 && outBounds.top == 0 && (outBounds.right == displayWidth || outBounds.right == displayHeight) && (outBounds.bottom == displayWidth || outBounds.bottom == displayHeight)) {
                                lastWindowPackageName = wp.toString()
                            }
                        }
                        if (lastWindowPackageName != null) {
                            packageName = lastWindowPackageName
                            GlobalStatus.lastPackageName = packageName
                            EventBus.publish(EventType.APP_SWITCH);
                        } else {
                            return
                        }
                    } catch (ex: Exception) {
                        return
                    }
                }
            }
        }
    }

    private fun deestory() {
        Toast.makeText(applicationContext, "Scene - 辅助服务已关闭！", Toast.LENGTH_SHORT).show()
        if (appSwitchHandler != null) {
            appSwitchHandler?.onInterrupt()
            appSwitchHandler = null
            // disableSelf()
            stopSelf()
            System.exit(0)
        }
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
        if (appSwitchHandler == null)
            return false

        val keyCode = event.keyCode
        // 只阻止四大金刚键
        if (!(keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_APP_SWITCH || keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SEARCH)) {
            return super.onKeyEvent(event)
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            downTime = event.eventTime
            val currentDownTime = downTime
            val stopEvent = appSwitchHandler!!.onKeyDown()
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
            return appSwitchHandler!!.onKeyDown()
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
    }

    override fun onDestroy() {
        this.deestory()
    }
}