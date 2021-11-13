package com.omarea.vtools

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.*
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.util.LruCache
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import com.omarea.Scene
import com.omarea.data.EventBus
import com.omarea.data.EventType
import com.omarea.data.GlobalStatus
import com.omarea.data.IEventReceiver
import com.omarea.library.basic.InputMethodApp
import com.omarea.library.basic.LauncherApps
import com.omarea.library.calculator.Flags
import com.omarea.scene_mode.AppSwitchHandler
import com.omarea.scene_mode.AutoClickInstall
import com.omarea.scene_mode.AutoSkipAd
import com.omarea.store.SpfConfig
import com.omarea.utils.AutoSkipCloudData
import com.omarea.vtools.popup.FloatLogView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by helloklf on 2016/8/27.
 */
public class AccessibilityScenceMode : AccessibilityService(), IEventReceiver {
    override val isAsync: Boolean
        get() = false

    override fun onSubscribe() {
    }

    override fun onUnsubscribe() {
    }

    private var isLandscape = false
    private var inputMethods = ArrayList<String>()

    private var displayWidth = 1080
    private var displayHeight = 2340
    // 是否是平板
    private var isTablet: Boolean = false

    companion object {
        private var lastAnalyseThread: Long = 0
    }

    private var floatLogView: FloatLogView? = null

    internal var appSwitchHandler: AppSwitchHandler? = null

    private lateinit var spf: SharedPreferences

    // 跳过广告功能需要忽略的App
    private var skipAdIgnoredApps = ArrayList<String>().apply {
        add("com.android.systemui")
    }

    /**
     * 屏幕配置改变（旋转、分辨率更改、DPI更改等）
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onScreenConfigurationChanged(newConfig)
    }

    private fun getIsLandscape(): Boolean {
        val config = resources.configuration
        val orientation = config.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return true
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return false
        }
        return false
    }

    private fun onScreenConfigurationChanged(newConfig: Configuration) {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            isLandscape = false
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true
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

        isTablet = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    private fun updateConfig() {
        val info = serviceInfo // AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED

        info.notificationTimeout = 0

        if (spf.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_INSTALL, false) || spf.getBoolean(SpfConfig.GLOBAL_SPF_SKIP_AD, false)) {
            info.eventTypes = Flags(info.eventTypes).addFlag(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
            if (spf.getBoolean(SpfConfig.GLOBAL_SPF_SKIP_AD, false)) {
                // 仅用于调试时捕获广告按钮，发布时硬移除此flag
                // info.eventTypes = Flags(info.eventTypes).addFlag(AccessibilityEvent.TYPE_VIEW_CLICKED)
            }
        }

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 0
        info.packageNames = null

        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS

        // 捕获实体按键实践
        // info.flags = Flags(info.flags).addFlag(AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS)

        serviceInfo = info
    }

    override fun onCreate() {
        super.onCreate()
        EventBus.subscribe(this)
    }

    override fun eventFilter(eventType: EventType): Boolean {
        return eventType == EventType.SERVICE_DEBUG || eventType == EventType.SERVICE_UPDATE || eventType == EventType.SCREEN_ON || eventType == EventType.STATE_RESUME
    }

    override fun onReceive(eventType: EventType, data: HashMap<String, Any>?) {
        if (eventType == EventType.SERVICE_DEBUG) {
            if (setLogView()) {
                modernModeEvent()
            }
        } else if (eventType == EventType.SCREEN_ON) {
            if (!serviceIsConnected) {
                Scene.toast("辅助服务已失效，请重新激活辅助服务！")
            }
        } else if (eventType == EventType.STATE_RESUME) {
            modernModeEvent(null)
        } else if (eventType == EventType.SERVICE_UPDATE) {
            updateConfig()
            Scene.toast("辅助服务配置已更新~", Toast.LENGTH_SHORT)
        }
    }

    private fun setLogView(): Boolean {
        val showLogView = spf.getBoolean(SpfConfig.GLOBAL_SPF_SCENE_LOG, false)
        if (showLogView && floatLogView == null) {
            floatLogView = FloatLogView(this)
            return true
        } else if (!showLogView && floatLogView != null) {
            floatLogView?.hide()
            floatLogView = null
        }
        return false
    }

    public override fun onServiceConnected() {
        super.onServiceConnected()
        spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        // 获取屏幕方向
        onScreenConfigurationChanged(this.resources.configuration)

        serviceIsConnected = true

        updateConfig()

        if (appSwitchHandler == null) {
            appSwitchHandler = AppSwitchHandler(this)
        }

        getDisplaySize()
        setLogView()
        if (spf.getBoolean(SpfConfig.GLOBAL_SPF_SKIP_AD, false) && spf.getBoolean(SpfConfig.GLOBAL_SPF_SKIP_AD_PRECISE, false)) {
            AutoSkipCloudData().updateConfig(this, false)
        }

        // 获取输入法
        GlobalScope.launch(Dispatchers.IO) {
            inputMethods = InputMethodApp(applicationContext).getInputMethods()
            skipAdIgnoredApps.addAll(LauncherApps(applicationContext).launcherApps)
            skipAdIgnoredApps.addAll(inputMethods)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        /* // 开发过程中用于分析界面点击（捕获广告按钮）
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED || event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            val viewId = event.source?.viewIdResourceName // 有些跳过按钮不是文字来的 // if (event.text?.contains("跳过") == true) event.source?.viewIdResourceName else null
            Log.d("@Scene", "点击了[$viewId]，在 ${event.className}")
        }
        */

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
            when {
                packageName == "com.omarea.gesture" || packageName == "com.omarea.filter" -> {
                    return
                }
                /*
                packageName == "com.android.systemui" -> {
                    return
                }
                */
                // packageName == "com.omarea.vtools" -> return
                packageName.contains("packageinstaller") -> {
                    if (event.className == "com.android.packageinstaller.permission.ui.GrantPermissionsActivity") // MIUI权限控制器
                        return

                    try {
                        AutoClickInstall().packageinstallerAutoClick(this, event)
                    } catch (ex: Exception) {
                    }
                }
                packageName == "com.miui.securitycenter" -> {
                    try {
                        AutoClickInstall().miuiUsbInstallAutoClick(this, event)
                    } catch (ex: Exception) {
                    }
                    return
                }
                packageName == "com.android.permissioncontroller" -> { // 原生权限控制器
                    return
                }
                spf.getBoolean(SpfConfig.GLOBAL_SPF_SKIP_AD, false) -> {
                    trySkipAD(event)
                }
            }
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            return
        }

        val t = event.eventTime
        if (lastOriginEventTime != t && t > lastOriginEventTime) {
            lastOriginEventTime = t
            lastWindowChanged = System.currentTimeMillis()
            modernModeEvent(event)
        }
    }

    private var lastWindowChanged = 0L
    private var lastOriginEventTime = 0L
    private var autoSkipAd: AutoSkipAd? = null
    private fun trySkipAD(event: AccessibilityEvent) {
        // 只在窗口界面发生变化后的3秒内自动跳过广告，可以降低性能消耗，并降低误点几率
        if (System.currentTimeMillis() - lastWindowChanged < 3000) {
            if (autoSkipAd == null) {
                autoSkipAd = AutoSkipAd(this)
            }

            val packageName = event.packageName
            if (packageName == null || skipAdIgnoredApps.contains(packageName) || event.className === "android.widget.EditText") {
                // Log.d("@Scene", "SkipAD -> ignore")
                return
            }

            autoSkipAd?.skipAd(event, spf.getBoolean(SpfConfig.GLOBAL_SPF_SKIP_AD_PRECISE, false), displayWidth, displayHeight)
        }
    }

    private val blackTypeList = arrayListOf(
            AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY,
            AccessibilityWindowInfo.TYPE_INPUT_METHOD,
            AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER,
            AccessibilityWindowInfo.TYPE_SYSTEM
    )

    private val blackTypeListBasic = arrayListOf(
            AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY,
            AccessibilityWindowInfo.TYPE_INPUT_METHOD,
            AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER
    )

    public fun getEffectiveWindows(includeSystemApp: Boolean = false): List<AccessibilityWindowInfo> {
        val windowsList = windows
        if (windowsList != null && windowsList.size > 1) {
            val effectiveWindows = windowsList.filter {
                // 现在不过滤画中画应用了，因为有遇到像Telegram这样的应用，从画中画切换到全屏后仍检测到处于画中画模式，并且类型是 -1（可能是MIUI魔改出来的），但对用户来说全屏就是前台应用
                if (includeSystemApp) {
                    !blackTypeListBasic.contains(it.type)
                } else {
                    !blackTypeList.contains(it.type)
                }

                // (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.isInPictureInPictureMode)) && (it.type == AccessibilityWindowInfo.TYPE_APPLICATION)
            } // .sortedBy { it.layer }
            return effectiveWindows
        }
        return ArrayList()
    }

    public fun getForegroundApps(): Array<String> {
        val windows = this.getEffectiveWindows(true)
        return windows.map {
            it.root?.packageName
        }.filter { it != null && it != "com.android.systemui" }.map { it.toString() }.toTypedArray()
    }

    // 新的前台应用窗口判定逻辑
    private fun modernModeEvent(event: AccessibilityEvent? = null) {
        val effectiveWindows = this.getEffectiveWindows()

        if (effectiveWindows.isNotEmpty()) {
            try {
                var lastWindow: AccessibilityWindowInfo? = null
                // 最小窗口分辨率要求
                val minWindowSize = if (isLandscape && !isTablet) {
                    // 横屏时关注窗口大小，以显示区域大的主应用（平板设备不过滤窗口大小）
                    // 屏幕一半大小，用于判断窗口是否是小窗（比屏幕一半大小小的的应用认为是窗口化运行）
                    displayHeight * displayWidth / 2
                } else {
                    // 竖屏时以焦点窗口为前台应用，不关心窗口大小
                    0
                }

                val logs = if (floatLogView == null) null else StringBuilder()
                logs?.run {
                    append("Scene窗口检测\n", "屏幕: ${displayHeight}x${displayWidth}")
                    if (isLandscape) {
                        append(" 横向")
                    } else {
                        append(" 竖向")
                    }
                    if (isTablet) {
                        append(" Tablet")
                    }
                    append("\n")
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
                var lastWindowFocus = false

                // 无焦点窗口（一般处于过渡动画或窗口切换过程中）
                if (effectiveWindows.find { it.isActive || it.isFocused } == null) {
                    return
                }

                for (window in effectiveWindows) {
                    /*
                    val wp = window.root?.packageName
                    // 获取窗口 root节点 会有性能问题，因此去掉此判断逻辑
                    if (wp == null || wp == "android" || wp == "com.android.systemui" || wp == "com.miui.freeform" || wp == "com.omarea.gesture" || wp == "com.omarea.filter" || wp == "com.android.permissioncontroller") {
                        continue
                    }
                    */
                    if (isLandscape) {
                        val outBounds = Rect()
                        window.getBoundsInScreen(outBounds)

                        logs?.run {
                            val windowFocused = (window.isActive || window.isFocused)

                            val wp = try {
                                window.root?.packageName
                            } catch (ex: java.lang.Exception) {
                                null
                            }
                            append("\n层级: ${window.layer} ${wp} Focused：${windowFocused}\n类型: ${window.type} Rect[${outBounds.left},${outBounds.top},${outBounds.right},${outBounds.bottom}]")
                        }

                        val size = (outBounds.right - outBounds.left) * (outBounds.bottom - outBounds.top)
                        if (size >= lastWindowSize) {
                            lastWindow = window
                            lastWindowSize = size
                        }
                    } else {
                        val windowFocused = (window.isActive || window.isFocused)

                        logs?.run {
                            val outBounds = Rect()
                            window.getBoundsInScreen(outBounds)

                            val wp = try {
                                window.root?.packageName
                            } catch (ex: java.lang.Exception) {
                                null
                            }
                            append("\n层级: ${window.layer} ${wp} Focused：${windowFocused}\n类型: ${window.type} Rect[${outBounds.left},${outBounds.top},${outBounds.right},${outBounds.bottom}]")
                        }

                        if (lastWindowFocus && !windowFocused) {
                            continue
                        }

                        val outBounds = Rect()
                        window.getBoundsInScreen(outBounds)
                        val size = (outBounds.right - outBounds.left) * (outBounds.bottom - outBounds.top)
                        if (size >= lastWindowSize || (windowFocused && !lastWindowFocus)) {
                            lastWindow = window
                            lastWindowSize = size
                            lastWindowFocus = windowFocused
                        }
                    }
                }
                logs?.append("\n")
                if (lastWindow != null && lastWindowSize >= minWindowSize) {
                    val eventWindowId = event?.windowId
                    val lastWindowId = lastWindow.id

                    if (logs == null) {
                        if (eventWindowId == lastWindowId && event.packageName != null) {
                            val pa = event.packageName
                            if (!(isLandscape && inputMethods.contains(pa))) {
                                GlobalStatus.lastPackageName = pa.toString()
                                EventBus.publish(EventType.APP_SWITCH)
                            }
                        } else {
                            lastAnalyseThread = System.currentTimeMillis()
                            windowAnalyse(lastWindow, lastAnalyseThread)
                            if (event != null) {
                                startActivityPolling()
                            }
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
                        // MIUI 优化，打开MIUI多任务界面时当做没有发生应用切换
                        if (wp?.equals("com.miui.home") == true) {
                            /*
                            val node = root?.findAccessibilityNodeInfosByText("小窗应用")?.firstOrNull()
                            Log.d("Scene-MIUI", "" + node?.parent?.viewIdResourceName)
                            Log.d("Scene-MIUI", "" + node?.viewIdResourceName)
                            */
                            val node = lastWindow.root?.findAccessibilityNodeInfosByViewId("com.miui.home:id/txtSmallWindowContainer")?.firstOrNull()
                            if (node != null) {
                                return
                            }
                        }
                        if (wp != null) {
                            logs.append("\n此前: ${GlobalStatus.lastPackageName}")
                            val pa = wp.toString()
                            if (!(isLandscape  && inputMethods.contains(pa))) {
                                GlobalStatus.lastPackageName = pa
                                EventBus.publish(EventType.APP_SWITCH)
                            }
                            if (event != null) {
                                startActivityPolling()
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

    // 窗口id缓存（检测到相同的窗口id时，直接读取缓存的packageName，避免重复分析窗口节点获取packageName，降低性能消耗）
    private val windowIdCaches = LruCache<Int, String>(10)
    // 利用协程分析窗口
    private fun windowAnalyse(windowInfo: AccessibilityWindowInfo, tid: Long) {
        GlobalScope.launch(Dispatchers.IO) {
            var root: AccessibilityNodeInfo? = null
            val windowId = windowInfo.id
            val wp = (try {
                val cache = windowIdCaches.get(windowId)
                if (cache == null) {
                    // 如果当前window锁属的APP处于未响应状态，此过程可能会等待5秒后超时返回null，因此需要在线程中异步进行此操作
                    root = (try {
                        windowInfo.root
                    } catch (ex: Exception) {
                        null
                    })
                    root?.packageName.apply {
                        if (this != null) {
                            windowIdCaches.put(windowId, toString())
                        }
                    }
                } else {
                    // Log.d("@Scene", "windowCacheHit " + cache)
                    cache
                }
            } catch (ex: Exception) {
                null
            })
            // MIUI 优化，打开MIUI多任务界面时当做没有发生应用切换
            if (wp?.equals("com.miui.home") == true) {
                // 手势滑动过程中，桌面面处于非Focused状态
                if (!windowInfo.isFocused) {
                    return@launch
                }
                /*
                val node = root?.findAccessibilityNodeInfosByText("小窗应用")?.firstOrNull()
                Log.d("Scene-MIUI", "" + node?.parent?.viewIdResourceName)
                Log.d("Scene-MIUI", "" + node?.viewIdResourceName)
                */
                val node = root?.findAccessibilityNodeInfosByViewId("com.miui.home:id/txtSmallWindowContainer")?.firstOrNull()
                if (node != null) {
                    return@launch
                }
            }

            if (lastAnalyseThread == tid && wp != null) {
                val pa = wp.toString()
                if (!(isLandscape && inputMethods.contains(pa))) {
                    GlobalStatus.lastPackageName = pa
                    EventBus.publish(EventType.APP_SWITCH)
                }
            }
        }
    }

    private var pollingTimer: Timer? = null // 轮询定时器
    private var lastEventTime: Long = 0 // 最后一次触发事件的时间
    private val pollingTimeout: Long = 7000 // 轮询超时时间
    private val pollingInterval: Long = 3000 // 轮询间隔
    private fun startActivityPolling(delay: Long? = null) {
        stopActivityPolling()
        synchronized(this) {
            lastEventTime = System.currentTimeMillis()
            if (pollingTimer == null) {
                pollingTimer = Timer()
                pollingTimer?.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        val interval = System.currentTimeMillis() - lastEventTime
                        if (interval <= pollingTimeout) {
                            // Log.d(">>>>", "Scene Get Windows")
                            modernModeEvent()
                        } else {
                            stopActivityPolling()
                        }
                    }
                }, delay ?: pollingInterval, pollingInterval)
            }
        }
    }

    private fun stopActivityPolling() {
        synchronized(this) {
            if (pollingTimer != null) {
                pollingTimer?.cancel()
                pollingTimer?.purge()
                pollingTimer = null
            }
        }
    }

    private fun destroy() {
        EventBus.unsubscribe(this)
        if (appSwitchHandler != null) {
            appSwitchHandler?.run {
                EventBus.unsubscribe(this)
            }
            appSwitchHandler = null
            Toast.makeText(applicationContext, "Scene - 辅助服务已关闭！", Toast.LENGTH_SHORT).show()
            // disableSelf()
            stopSelf()
        }
    }

    private var serviceIsConnected = false

    override fun onUnbind(intent: Intent?): Boolean {
        serviceIsConnected = false
        destroy()
        stopSelf()
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        this.destroy()
    }
}