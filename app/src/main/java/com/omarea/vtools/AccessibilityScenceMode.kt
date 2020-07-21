package com.omarea.vtools

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Handler
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
import com.omarea.store.SpfConfig
import com.omarea.utils.AutoClick
import com.omarea.vtools.popup.FloatLogView
import com.omarea.xposed.XposedCheck
import java.util.*

/**
 * Created by helloklf on 2016/8/27.
 */
public class AccessibilityScenceMode : AccessibilityService() {
    private var flagRequestKeyEvent = true
    private var sceneConfigChanged: BroadcastReceiver? = null
    private var isLandscapf = false

    private var displayWidth = 1080
    private var displayHeight = 2340

    companion object {
        const val useXposed = false

        private var lastParsingThread: Long = 0
    }

    private var floatLogView: FloatLogView? = null

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
        return super.onStartCommand(intent, flags, startId)
    }

    internal var appSwitchHandler: AppSwitchHandler? = null


    /**
     * 屏幕配置改变（旋转、分辨率更改、DPI更改等）
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onScreenConfigurationChanged(newConfig)
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
        // TODO:去掉
        if (useXposed && XposedCheck.xposedIsRunning()) {
            flagRequestKeyEvent = SceneConfigStore(this.applicationContext).needKeyCapture()

            val info = serviceInfo
            if (flagRequestKeyEvent) {
                info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            } else {
                info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS // or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            }

            setServiceInfo(info);
            GlobalStatus.homeMessage = "正在通过Xposed运行场景模式";
        } else {
            flagRequestKeyEvent = SceneConfigStore(this.applicationContext).needKeyCapture()

            val info = serviceInfo // AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.notificationTimeout = 0
            info.packageNames = null

            if (flagRequestKeyEvent) {
                info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            } else {
                info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS // or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
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

        val spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (spf.getBoolean(SpfConfig.GLOBAL_SPF_SCENE_LOG, false)) {
            floatLogView = FloatLogView(this)
        }
        classicModel = spf.getBoolean(SpfConfig.GLOBAL_SPF_SCENE_CLASSIC, false)
    }

    private var classicModel = false

    // 经典模式
    private fun classicModelEvent(event: AccessibilityEvent) {
        if (event.packageName == null || event.className == null)
            return

        var packageName = event.packageName.toString()

        // com.miui.freeform 是miui的应用多窗口（快速回复、游戏模式QQ微信小窗口）管理器
        if (packageName == "android" || packageName == "com.android.systemui" || packageName == "com.miui.freeform" || packageName == "com.omarea.gesture" || packageName == "com.omarea.filter") {
            return
        }

        // 横屏时屏蔽 QQ、微信事件，因为游戏模式下通常会在横屏使用悬浮窗打开QQ 微信
        if (isLandscapf && (packageName == "com.tencent.mobileqq" || packageName == "com.tencent.mm")) {
            return
        }

        if (packageName.contains("packageinstaller")) {
            if (event.className == "com.android.packageinstaller.permission.ui.GrantPermissionsActivity") // MIUI权限控制器
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
        } else if (packageName == "com.android.permissioncontroller") { // 原生权限控制器
            return
        }

        // 针对一加部分系统的修复
        if ((packageName == "net.oneplus.h2launcher" || packageName == "net.oneplus.launcher") && event.className == "android.widget.LinearLayout") {
            return
        }

        if (event.source != null) {
            val logs = StringBuilder()
            logs.append("Scene窗口检测\n", "屏幕: ${displayHeight}x${displayWidth}\n")
            logs.append("事件: ${event.source.packageName}\n")

            val windows_ = windows
            if (windows_ == null || windows_.isEmpty()) {
                return
            } else if (windows_.size > 1) {
                var lastWindow: AccessibilityWindowInfo? = null
                val effectiveWindows = windows_.filter {
                    (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.isInPictureInPictureMode)) && (it.type == AccessibilityWindowInfo.TYPE_APPLICATION)
                }.sortedBy { it.layer }

                if (effectiveWindows.size > 0) {
                    lastWindow = effectiveWindows.get(0)

                    for (window in effectiveWindows) {
                        val wp = window.root?.packageName
                        if (wp != null) {
                            logs.append("\n层级: ${window.layer} ${wp}")
                        }
                    }

                    try {
                        val source = event.source
                        if (source == null || source.windowId != lastWindow.id) {
                            val windowRoot = lastWindow!!.root
                            if (windowRoot == null || windowRoot.packageName == null) {
                                return
                            }
                            packageName = windowRoot.packageName!!.toString()
                        }
                    } catch (ex: Exception) {
                        return
                    } finally {
                        logs.append("\n\n命中: $packageName")
                        floatLogView?.update(logs.toString())
                    }
                }
            }
            GlobalStatus.lastPackageName = packageName
            EventBus.publish(EventType.APP_SWITCH);
        } else {
            GlobalStatus.lastPackageName = packageName
            EventBus.publish(EventType.APP_SWITCH);
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!classicModel) {
            /*
            when(event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    Log.e(">>>>", "TYPE_WINDOW_CONTENT_CHANGED " + event.eventType)
                }
                AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                    Log.e(">>>>", "TYPE_WINDOWS_CHANGED " + event.eventType)
                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    Log.e(">>>>", "TYPE_WINDOW_STATE_CHANGED " + event.eventType)
                }
                else -> {
                    Log.e(">>>>", "???? " + event.eventType)
                }
            }
            */

            val packageName = event.packageName
            if (packageName != null) {
                if (packageName.contains("packageinstaller")) {
                    if (event.className == "com.android.packageinstaller.permission.ui.GrantPermissionsActivity") // MIUI权限控制器
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
                } else if (packageName == "com.android.permissioncontroller") { // 原生权限控制器
                    return
                }
            }

            modernModeEvent(event)
        } else {
            classicModelEvent(event)
        }
    }

    // 新的前台应用窗口判定逻辑
    private fun modernModeEvent(event: AccessibilityEvent? = null) {
        val windows_ = windows
        if (windows_ == null || windows_.isEmpty()) {
            return
        } else if (windows_.size > 1) {
            val effectiveWindows = windows_.filter {
                (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.isInPictureInPictureMode)) && (it.type == AccessibilityWindowInfo.TYPE_APPLICATION)
            } // .sortedBy { it.layer }

            if (effectiveWindows.size > 0) {
                try {
                    var lastWindow: AccessibilityWindowInfo? = null
                    val logs = if (floatLogView == null) null else StringBuilder()
                    logs?.run {
                        append("Scene窗口检测\n", "屏幕: ${displayHeight}x${displayWidth}\n")
                        if (event != null) {
                            append("事件: ${event.source?.packageName}\n")
                        } else {
                            append("事件: 主动轮询${Date().time / 1000}\n")
                        }
                    }
                    // TODO:
                    //      此前在MIUI系统上测试，只判定全屏显示（即窗口大小和屏幕分辨率完全一致）的应用，逻辑非常准确
                    //      但在类原生系统上表现并不好，例如：有缺口的屏幕或有导航键的系统，报告的窗口大小则可能不包括缺口高度区域和导航键区域高度
                    //      因此，现在将逻辑调整为：从所有应用窗口中选出最接近全屏的一个，判定为前台应用
                    //      当然，这并不意味着完美，只是暂时没有更好的解决方案……

                    var lastWindowSize = 0
                    for (window in effectiveWindows) {

                        val outBounds = Rect()
                        window.getBoundsInScreen(outBounds)

                        /*
                        val wp = window.root?.packageName
                        // 获取窗口 root节点 会有性能问题，因此去掉此判断逻辑
                        if (wp == null || wp == "android" || wp == "com.android.systemui" || wp == "com.miui.freeform" || wp == "com.omarea.gesture" || wp == "com.omarea.filter" || wp == "com.android.permissioncontroller") {
                            continue
                        }
                        */

                        logs?.run {
                            val wp = try {
                                window.root?.packageName
                            } catch (ex: java.lang.Exception) {
                                null
                            }
                            append("\n层级: ${window.layer} ${wp}\n类型: ${window.type} Rect[${outBounds.left},${outBounds.top},${outBounds.right},${outBounds.bottom}]")
                        }

                        val size = (outBounds.right - outBounds.left) * (outBounds.bottom - outBounds.top)
                        if (size >= lastWindowSize) {
                            lastWindow = window
                            lastWindowSize = size
                        }
                    }
                    logs?.append("\n")
                    if (lastWindow != null) {
                        val eventWindowId = event?.windowId
                        val lastWindowId = lastWindow.id

                        if (logs == null) {
                            if (eventWindowId == lastWindowId && event.packageName != null) {
                                val pa = event.packageName
                                GlobalStatus.lastPackageName = pa.toString()
                                EventBus.publish(EventType.APP_SWITCH)
                            } else {
                                lastParsingThread = System.currentTimeMillis()
                                // try {
                                val thread: Thread = WindowParsingThread(lastWindow, lastParsingThread)
                                thread.start()
                                if (event != null) {
                                    startColorPolling()
                                }
                                // thread.wait(300);
                                // } catch (Exception ignored){}
                            }
                        } else {
                            val wp = if (eventWindowId == lastWindowId) {
                                event.packageName
                            } else {
                                try {
                                    lastWindow.root.packageName
                                } catch (ex: java.lang.Exception) {
                                    null
                                }
                            }
                            if (wp != null) {
                                logs.append("\n此前: ${GlobalStatus.lastPackageName}")
                                GlobalStatus.lastPackageName = wp.toString()
                                EventBus.publish(EventType.APP_SWITCH)
                                if (event != null) {
                                    startColorPolling()
                                }
                            }

                            logs.append("\n现在: ${GlobalStatus.lastPackageName}")
                            floatLogView?.update(logs.toString())
                        }
                    } else {
                        logs?.append("\n现在: ${GlobalStatus.lastPackageName}")
                        floatLogView?.update(logs.toString())
                        return
                    }
                } catch (ex: Exception) {
                    return
                }
            }
        }
    }

    class WindowParsingThread constructor(private val windowInfo: AccessibilityWindowInfo, private val tid: Long) : Thread() {
        override fun run() {
            // 如果当前window锁属的APP处于未响应状态，此过程可能会等待5秒后超时返回null，因此需要在线程中异步进行此操作
            val wp = (try {
                windowInfo.root?.packageName
            } catch (ex: Exception) {
                null
            })

            if (lastParsingThread == tid && wp != null) {
                GlobalStatus.lastPackageName = wp.toString()
                EventBus.publish(EventType.APP_SWITCH)
            }
        }
    }

    private var pollingTimer: Timer? = null // 轮询定时器
    private var lastEventTime: Long = 0 // 最后一次触发事件的时间
    private val pollingTimeout: Long = 10000 // 轮询超时时间
    private val pollingInterval: Long = 3000 // 轮询间隔
    private fun startColorPolling() {
        stopColorPolling()
        lastEventTime = System.currentTimeMillis()
        if (pollingTimer == null) {
            pollingTimer = Timer()
            pollingTimer!!.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    val interval = System.currentTimeMillis() - lastEventTime
                    if (interval < pollingTimeout && interval >= pollingInterval) {
                        // Log.d(">>>>", "Scene Get Windows")
                        modernModeEvent()
                    } else {
                        stopColorPolling()
                    }
                }
            }, pollingInterval, pollingInterval)
        }
    }

    private fun stopColorPolling() {
        if (pollingTimer != null) {
            pollingTimer?.cancel()
            pollingTimer?.purge();
            pollingTimer = null
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