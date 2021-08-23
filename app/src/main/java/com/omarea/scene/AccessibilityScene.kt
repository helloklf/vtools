package com.omarea.scene

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.*
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
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
import com.omarea.scene_mode.AppSwitchHandler
import com.omarea.store.SpfConfig
import com.omarea.vtools.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

public class AccessibilityScene : AccessibilityService() {
    private var sceneConfigChanged: BroadcastReceiver? = null
    private var isLandscap = false

    private var displayWidth = 1080
    private var displayHeight = 2340

    // 标准模式下会多次分析和检测窗口内容提高准确性同时保证及时性，一次窗口变化可能会检测3~5次
    // 而低功耗模式则以减少检测频率为主，尽可能降低性能消耗，通常只检测1~2次
    private var lowPowerMode = true

    companion object {
        private var lastAnalyseThread: Long = 0
    }

    internal var appSwitchHandler: AppSwitchHandler? = null

    private lateinit var spf: SharedPreferences

    /**
     * 屏幕配置改变（旋转、分辨率更改、DPI更改等）
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onScreenConfigurationChanged(newConfig)
    }

    private fun onScreenConfigurationChanged(newConfig: Configuration) {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            isLandscap = false
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscap = true
        }
        getDisplaySize()
    }

    private fun getDisplaySize() {
        // 重新获取屏幕分辨率
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getRealSize(point)
        if (point.x != displayWidth || point.y != displayHeight) {
            displayWidth = point.x
            displayHeight = point.y
        }
    }

    private fun updateConfig() {
        val info = serviceInfo // AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED

        info.notificationTimeout = 0

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 0
        info.packageNames = null

        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS

        serviceInfo = info
    }

    public override fun onServiceConnected() {
        spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, MODE_PRIVATE)

        // 获取屏幕方向
        onScreenConfigurationChanged(this.resources.configuration)

        serviceIsConnected = true

        updateConfig()
        if (sceneConfigChanged == null) {
            sceneConfigChanged = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    updateConfig()
                    Scene.toast("辅助服务配置已更新~", Toast.LENGTH_SHORT)
                }
            }
            registerReceiver(sceneConfigChanged, IntentFilter(getString(R.string.scene_service_config_change_action)))
        }
        super.onServiceConnected()

        if (appSwitchHandler == null) {
            appSwitchHandler = AppSwitchHandler(this)
        }

        getDisplaySize()
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
            when (packageName) {
                "com.omarea.gesture",
                "com.omarea.filter",
                "com.android.permissioncontroller" -> {
                    return
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
            if (lowPowerMode) {
                val eventId = UUID.randomUUID()
                lastTriggerEvent = eventId

                handler.postDelayed({
                    if (lastTriggerEvent == eventId) {
                        modernModeEvent(null)
                    }
                }, 100)
            } else {
                modernModeEvent(event)
            }
        }
    }

    private var lastTriggerEvent: UUID? = null

    private var lastWindowChanged = 0L
    private var lastOriginEventTime = 0L

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

    public fun notifyScreenOn() {
        this.modernModeEvent(null)
    }


    // 新的前台应用窗口判定逻辑
    private fun modernModeEvent(event: AccessibilityEvent? = null) {
        val effectiveWindows = this.getEffectiveWindows()

        if (effectiveWindows.isNotEmpty()) {
            try {
                var lastWindow: AccessibilityWindowInfo? = null
                // TODO:
                //      此前在MIUI系统上测试，只判定全屏显示（即窗口大小和屏幕分辨率完全一致）的应用，逻辑非常准确
                //      但在类原生系统上表现并不好，例如：有缺口的屏幕或有导航键的系统，报告的窗口大小则可能不包括缺口高度区域和导航键区域高度
                //      因此，现在将逻辑调整为：从所有应用窗口中选出最接近全屏的一个，判定为前台应用
                //      当然，这并不意味着完美，只是暂时没有更好的解决方案……

                var lastWindowSize = 0
                var lastWindowFocus = false
                for (window in effectiveWindows) {
                    /*
                    val wp = window.root?.packageName
                    // 获取窗口 root节点 会有性能问题，因此去掉此判断逻辑
                    if (wp == null || wp == "android" || wp == "com.android.systemui" || wp == "com.miui.freeform" || wp == "com.omarea.gesture" || wp == "com.omarea.filter" || wp == "com.android.permissioncontroller") {
                        continue
                    }
                    */
                    if (isLandscap) {
                        val outBounds = Rect()
                        window.getBoundsInScreen(outBounds)

                        val size = (outBounds.right - outBounds.left) * (outBounds.bottom - outBounds.top)
                        if (size >= lastWindowSize) {
                            lastWindow = window
                            lastWindowSize = size
                        }
                    } else {
                        val windowFocused = (window.isActive || window.isFocused)

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

                if (lastWindow != null) {
                    val eventWindowId = event?.windowId
                    val lastWindowId = lastWindow.id

                    if (eventWindowId == lastWindowId && event.packageName != null) {
                        val pa = event.packageName
                        GlobalStatus.lastPackageName = pa.toString()
                        EventBus.publish(EventType.APP_SWITCH)
                    } else {
                        lastAnalyseThread = System.currentTimeMillis()
                        windowAnalyse(lastWindow, lastAnalyseThread)
                        if (event != null) {
                            startActivityPolling()
                        }
                    }
                } else {
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
                // 全面屏手势操作期间，处于非Focused状态
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
                GlobalStatus.lastPackageName = wp.toString()
                EventBus.publish(EventType.APP_SWITCH)
            }
        }
    }

    private var pollingTimer: Timer? = null // 轮询定时器
    private var lastEventTime: Long = 0 // 最后一次触发事件的时间
    private val pollingTimeout: Long = 10000 // 轮询超时时间
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

    private fun destory() {
        Toast.makeText(applicationContext, "Scene - 辅助服务已关闭！", Toast.LENGTH_SHORT).show()
        if (appSwitchHandler != null) {
            appSwitchHandler?.onInterrupt()
            appSwitchHandler = null
            // disableSelf()
            stopSelf()
            // System.exit(0)
        }
    }

    private var handler = Handler(Looper.getMainLooper())
    private var serviceIsConnected = false

    override fun onUnbind(intent: Intent?): Boolean {
        if (sceneConfigChanged != null) {
            unregisterReceiver(sceneConfigChanged)
            sceneConfigChanged = null
        }
        serviceIsConnected = false
        destory()
        stopSelf()
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        this.destory()
    }
}