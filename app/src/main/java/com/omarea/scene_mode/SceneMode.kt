package com.omarea.scene_mode

import android.app.ActivityManager
import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.omarea.Scene
import com.omarea.library.shell.CGroupMemoryUtlis
import com.omarea.library.shell.LocationHelper
import com.omarea.library.shell.SwapUtils
import com.omarea.model.SceneConfigInfo
import com.omarea.scene.AccessibilityScene
import com.omarea.store.SceneConfigStore
import com.omarea.store.SpfConfig
import com.omarea.vtools.popup.FloatMonitorMini
import com.omarea.vtools.popup.FloatScreenRotation
import java.util.*

class SceneMode private constructor(private val context: AccessibilityScene, private var store: SceneConfigStore) {
    private var lastAppPackageName = "com.android.systemui"
    private var contentResolver: ContentResolver = context.contentResolver
    private val config = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

    private val floatScreenRotation = FloatScreenRotation(context)

    companion object {

        @Volatile
        private var instance: SceneMode? = null

        // 获取当前实例
        fun getCurrentInstance(): SceneMode? {
            return instance
        }

        // 获取当前实例或初始化
        fun getInstanceOrInit(context: AccessibilityScene, store: SceneConfigStore): SceneMode? {
            if (instance == null) {
                synchronized(SceneMode::class) {
                    instance = SceneMode(context, store)
                }
            }
            return instance!!
        }
    }

    var brightnessMode = -1;
    var screenBrightness = -1;
    var currentSceneConfig: SceneConfigInfo? = null

    // 备份亮度设置
    private fun backupBrightnessState(): Int {
        if (brightnessMode == -1) {
            try {
                brightnessMode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
                screenBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
            }
        }
        return brightnessMode
    }

    // 恢复亮度设置
    private fun resumeBrightnessState() {
        try {
            val modeBackup = brightnessMode;
            if (modeBackup > -1) {
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, modeBackup)
                contentResolver.notifyChange(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), null)
            }
            brightnessMode = -1
            if (screenBrightness > -1 && modeBackup == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, screenBrightness)
                contentResolver.notifyChange(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), null)
            }
            screenBrightness = -1
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 关闭自动亮度
    private fun autoLightOff(lightValue: Int = -1): Boolean {
        try {
            if (Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)) {
                contentResolver.notifyChange(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), null)
            } else {
                return false
            }

            if (lightValue > -1 && Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, lightValue)) {
                contentResolver.notifyChange(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), null)
            } else {
                return false
            }
        } catch (ex: Exception) {
            return false
        }
        return true
    }

    // 设置屏幕旋转
    private fun updateScreenRotation() {
        currentSceneConfig?.run {
            floatScreenRotation.update(this)
        }
    }

    /**
     * 收到了通知时
     * @return 是否拦截
     */
    fun onNotificationPosted(): Boolean {
        if (currentSceneConfig != null) {
            return currentSceneConfig!!.disNotice
        }
        return false
    }

    /**
     * 按键按下
     * @return 是否阻拦按键事件
     */
    fun onKeyDown(): Boolean {
        if (currentSceneConfig != null) {
            return currentSceneConfig!!.disButton
        }
        return false
    }

    private var locationMode = "none"
    // 是否需要在离开应用时隐藏迷你性能监视器
    private var hideMonitorOnLeave = false

    // 备份定位设置
    private fun backupLocationModeState() {
        if (locationMode == "none") {
            locationMode = Settings.Secure.getString(contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED)
        }
    }

    // 还原定位设置
    private fun restoreLocationModeState() {
        if (locationMode != "none") {
            if (!locationMode.contains("gps")) {
                if (locationMode.contains("network")) {
                    LocationHelper().disableGPS()
                } else {
                    LocationHelper().disableLocation()
                }
            }
            locationMode = "none"
        }
    }

    private var headsup = -1

    // 备份悬浮通知
    private fun backupHeadUp() {
        if (headsup < 0) {
            try {
                headsup = Settings.Global.getInt(contentResolver, "heads_up_notifications_enabled")
            } catch (ex: Exception) {
            }
        }
    }

    // 还原悬浮通知
    private fun restoreHeaddUp() {
        try {
            if (headsup > -1) {
                Settings.Global.putInt(contentResolver, "heads_up_notifications_enabled", headsup)
                contentResolver.notifyChange(Settings.System.getUriFor("heads_up_notifications_enabled"), null)
                headsup = -1
            }
        } catch (ex: Exception) {

        }
    }

    /**
     * 从应用离开时
     */
    fun onAppLeave(sceneConfigInfo: SceneConfigInfo) {
        if (sceneConfigInfo.aloneLight) {
            // 独立亮度 记录最后的亮度值
            try {
                val light = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                if (light != sceneConfigInfo.aloneLightValue) {
                    sceneConfigInfo.aloneLightValue = light
                    store.setAppConfig(sceneConfigInfo)
                }
            } catch (ex: java.lang.Exception) {
            }
        }

        // 实验性新特性（cgroup/memory自动配置）
        if (sceneConfigInfo.fgCGroupMem != sceneConfigInfo.bgCGroupMem) {
                CGroupMemoryUtlis(Scene.context).run {
                    if (isSupported) {
                        if (sceneConfigInfo.bgCGroupMem?.isNotEmpty() == true) {
                            setGroupAutoDelay(this, sceneConfigInfo.packageName!!, sceneConfigInfo.bgCGroupMem)
                            // Scene.toast(sceneConfigInfo.packageName!! + "退出，cgroup调为[${sceneConfigInfo.bgCGroupMem}]\n(Scene试验性功能)")
                        } else {
                            setGroup(sceneConfigInfo.packageName!!, "")
                            // Scene.toast(sceneConfigInfo.packageName!! + "退出，cgroup调为[/]\n(Scene试验性功能)")
                        }
                    } else {
                        Scene.toast("你的内核不支持cgroup设置！\n(Scene试验性功能)")
                    }
                }
        }

        // KeepShellPublic.doCmdSync("pm suspend ${sceneConfigInfo.packageName}")
    }

    /**
     * 前台应用切换
     */
    fun onAppEnter(packageName: String, forceUpdateConfig: Boolean = false) {
        if (lastAppPackageName == packageName && !forceUpdateConfig) {
            return
        }
        synchronized(this) {
            try {
                lastAppPackageName = packageName
                if (currentSceneConfig != null && currentSceneConfig?.packageName != packageName) {
                    onAppLeave(currentSceneConfig!!)
                }

                currentSceneConfig = store.getAppConfig(packageName)
                if (currentSceneConfig == null) {
                    restoreLocationModeState()
                    resumeBrightnessState()
                    restoreHeaddUp()
                    stoptMemoryDynamicBooster()
                } else {
                    if (currentSceneConfig!!.aloneLight) {
                        backupBrightnessState()
                        autoLightOff(currentSceneConfig!!.aloneLightValue)
                    } else {
                        resumeBrightnessState()
                    }

                    if (currentSceneConfig!!.showMonitor) {
                        if (FloatMonitorMini.show != true) {
                            Scene.post {
                                hideMonitorOnLeave = FloatMonitorMini(context).showPopupWindow()
                            }
                        }
                    } else if (hideMonitorOnLeave) {
                        Scene.post {
                            FloatMonitorMini(context).hidePopupWindow()
                        }
                        hideMonitorOnLeave = false
                    }

                    if (currentSceneConfig!!.gpsOn) {
                        backupLocationModeState()
                        val mode = Settings.Secure.getString(contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED)
                        if (!mode.contains("gps")) {
                            LocationHelper().enableGPS()
                        }
                    } else {
                        restoreLocationModeState()
                    }

                    if (currentSceneConfig!!.disNotice) {
                        try {
                            val mode = Settings.Global.getInt(contentResolver, "heads_up_notifications_enabled")
                            backupHeadUp()
                            if (mode != 0) {
                                Settings.Global.putInt(contentResolver, "heads_up_notifications_enabled", 0)
                                contentResolver.notifyChange(Settings.System.getUriFor("heads_up_notifications_enabled"), null)
                            }
                        } catch (ex: Exception) {
                        }
                    } else {
                        restoreHeaddUp()
                    }

                    // 实验性新特性（cgroup/memory自动配置）
                    if (currentSceneConfig?.fgCGroupMem?.isNotEmpty() == true || currentSceneConfig?.bgCGroupMem != currentSceneConfig?.fgCGroupMem) {
                        CGroupMemoryUtlis(Scene.context).run {
                            if (isSupported) {
                                setGroup(currentSceneConfig!!.packageName!!, currentSceneConfig!!.fgCGroupMem)
                                // Scene.toast("进入" + currentSceneConfig!!.packageName!! + "，cgroup调为[${currentSceneConfig!!.fgCGroupMem}]\n(Scene试验性功能)")
                            } else {
                                Scene.toast("你的内核不支持cgroup设置！\n(Scene试验性功能)")
                            }
                        }
                    }

                    // if (packageName.equals("com.miHoYo.Yuanshen") || packageName.equals("com.tencent.tmgp.sgame")) {
                    if (currentSceneConfig?.dynamicBoostMem == true) {
                        startMemoryDynamicBooster()
                    } else {
                        stoptMemoryDynamicBooster()
                    }
                }

                updateScreenRotation()
            } catch (ex: Exception) {
                Log.e(">>>>", "" + ex.message)
            }
        }
    }

    private fun setGroupAutoDelay(util: CGroupMemoryUtlis, app: String, mode: String) {
        if (mode == "scene_limit") {
            Scene.postDelayed({
                if (currentSceneConfig?.packageName != app) {
                    util.setGroup(app, mode)
                }
            }, 3000)
        } else if (mode == "scene_limit2") {
            Scene.postDelayed({
                if (currentSceneConfig?.packageName != app) {
                    util.setGroup(app, mode)
                }
            }, 8000)
        } else {
            util.setGroup(app, mode)
        }
    }

    private var am: ActivityManager? = null
    private var memoryWatchTimer: Timer? = null
    private var swapUtils: SwapUtils? = null
    fun startMemoryDynamicBooster() {
        //获取运行内存的信息
        if (am == null) {
            am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        }
        val info = ActivityManager.MemoryInfo()

        memoryWatchTimer = Timer().apply {
            if (swapUtils == null) {
                swapUtils = SwapUtils(context)
            }
            schedule(object : TimerTask() {
                override fun run() {
                    am?.getMemoryInfo(info)
                    val total = info.totalMem
                    val availMem = info.availMem
                    val raito = availMem.toDouble() / total

                    if (raito < 0.16 && raito > 0.0) {
                        swapUtils?.forceKswapd(0)
                    }
                }
            }, 3000, 10000)
        }
    }

    private fun stoptMemoryDynamicBooster() {
        if (memoryWatchTimer != null) {
            memoryWatchTimer?.cancel()
            memoryWatchTimer = null
        }
    }

    fun updateAppConfig() {
        if (!lastAppPackageName.isEmpty()) {
            onAppEnter(lastAppPackageName, true)
        }
    }

    fun clearState() {
        lastAppPackageName = "com.android.systemui"
        restoreLocationModeState()
        resumeBrightnessState()
        currentSceneConfig = null
        floatScreenRotation.remove()
    }

    fun onScreenOn() {
        // 屏幕点亮后恢复屏幕自动旋转设置
        updateScreenRotation()
    }

    fun onScreenOff() {
        // 息屏时暂停屏幕旋转修改
        floatScreenRotation.remove()
    }
}
