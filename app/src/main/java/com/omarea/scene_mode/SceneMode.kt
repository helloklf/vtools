package com.omarea.scene_mode

import android.app.ActivityManager
import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.omarea.Scene
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.library.shell.*
import com.omarea.model.SceneConfigInfo
import com.omarea.store.SceneConfigStore
import com.omarea.store.SpfConfig
import com.omarea.vtools.AccessibilityScenceMode
import com.omarea.vtools.popup.FloatMonitorMini
import com.omarea.vtools.popup.FloatScreenRotation
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList

class SceneMode private constructor(private val context: AccessibilityScenceMode, private var store: SceneConfigStore) {
    private var lastAppPackageName = "com.android.systemui"
    private var contentResolver: ContentResolver = context.contentResolver
    private var freezList = ArrayList<FreezeAppHistory>()
    private val config = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

    // 偏见应用解冻数量限制
    private val freezeAppLimit: Int
        get() {
            return config.getInt(SpfConfig.GLOBAL_SPF_FREEZE_ITEM_LIMIT, 5)
        }

    // 偏见应用后台超时时间
    private val freezeAppTimeLimit: Int
        get() {
            return config.getInt(SpfConfig.GLOBAL_SPF_FREEZE_TIME_LIMIT, 2) * 60 * 1000
        }

    // 是否使用suspend命令冻结应用，不隐藏图标
    private val suspendMode: Boolean
        get() {
            return config.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_SUSPEND, Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        }

    private val floatScreenRotation = FloatScreenRotation(context)

    public fun cancelFreezeAppThread() {
        PropsUtils.setPorp("vtools.freeze_delay", "")
    }

    public class FreezeAppThread(
        private val context: Context,
        private val ignoreState: Boolean = false,
        private val delaySecond: Int = 0
    ) : Thread() {
        override fun run() {
            val globalConfig = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
            val launchedFreezeApp = if (ignoreState) null else getCurrentInstance()?.getLaunchedFreezeApp()
            val suspendMode = globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_SUSPEND, Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            val targetApps = ArrayList<String>()
            for (item in SceneConfigStore(context).freezeAppList) {
                if (launchedFreezeApp == null || !launchedFreezeApp.contains(item)) {
                    targetApps.add(item)
                }
            }
            if (targetApps.size > 0) {
                val cmds = StringBuilder("freeze_apps=\"")
                targetApps.forEach {
                    cmds.append("${it}\n")
                }
                cmds.append("\"\n")

                val writeSuccess = FileWrite.writePrivateFile(
                        cmds.toString().toByteArray(Charset.defaultCharset()),
                        "freeze_apps.sh",
                        context)
                val mode = if (suspendMode) "suspend" else "disable"
                val apps = if (writeSuccess) FileWrite.getPrivateFilePath(context, "freeze_apps.sh") else  null
                val executor = FileWrite.writePrivateShellFile("addin/freeze_executor.sh", "freeze_executor.sh", context)

                if (executor != null && apps != null) {
                    val delay = if (delaySecond > 0) ("" + delaySecond) else ""
                    KeepShellPublic.doCmdSync("nohup $executor $mode $apps $delay >/dev/null 2>&1 &")
                }
            }
        }
    }

    companion object {

        @Volatile
        private var instance: SceneMode? = null

        // 获取当前实例
        fun getCurrentInstance(): SceneMode? {
            return instance
        }

        // 创建一个新实例
        fun getNewInstance(context: AccessibilityScenceMode, store: SceneConfigStore): SceneMode? {
            if (instance != null) {
                instance?.clearState()
            }
            instance = SceneMode(context, store)
            return instance!!
        }

        fun suspendApp(app: String) {
            if (app.equals("com.android.vending")) {
                GAppsUtilis().disable(KeepShellPublic.secondaryKeepShell);
            } else {
                KeepShellPublic.doCmdSync("pm suspend ${app}\nam force-stop ${app} || am kill current ${app}")
            }
        }

        fun freezeApp(app: String) {
            if (app.equals("com.android.vending")) {
                GAppsUtilis().disable(KeepShellPublic.secondaryKeepShell);
            } else {
                KeepShellPublic.doCmdSync("pm disable ${app}")
            }
        }

        fun unfreezeApp(app: String) {
            getCurrentInstance()?.setFreezeAppLeaveTime(app)

            if (app.equals("com.android.vending")) {
                GAppsUtilis().enable(KeepShellPublic.secondaryKeepShell);
            } else {
                KeepShellPublic.doCmdSync("pm unsuspend ${app}\npm enable ${app}")
            }
        }
    }

    class FreezeAppHistory {
        var startTime: Long = 0
        var leaveTime: Long = 0
        var packageName: String = ""
    }


    fun getLaunchedFreezeApp(): List<String> {
        val apps = ArrayList<String>().apply {
            addAll(freezList.map { it.packageName })
        }
        val configList = SceneConfigStore(context).freezeAppList
        context.getForegroundApps().forEach {
            if (configList.contains(it) && !apps.contains(it)) {
                apps.add(it)
                setFreezeAppStartTime(it)
            }
        }
        return apps
    }

    fun setFreezeAppLeaveTime(packageName: String) {
        val currentHistory = removeFreezeAppHistory(packageName)

        val history = if (currentHistory != null) currentHistory else FreezeAppHistory()
        history.leaveTime = System.currentTimeMillis()
        history.packageName = packageName

        freezList.add(history)
        clearFreezeAppCountLimit()
    }

    fun setFreezeAppStartTime(packageName: String) {
        removeFreezeAppHistory(packageName)

        val history = FreezeAppHistory()
        history.startTime = System.currentTimeMillis()
        history.leaveTime = -1
        history.packageName = packageName

        freezList.add(history)
        clearFreezeAppCountLimit()
    }

    fun removeFreezeAppHistory(packageName: String): FreezeAppHistory? {
        for (it in freezList) {
            if (it.packageName == packageName) {
                freezList.remove(it)
                return it
            }
        }
        return null
    }

    // 当解冻的偏见应用数量超过限制，冻结最先解冻的应用
    fun clearFreezeAppCountLimit() {
        if (freezeAppLimit > 0 && freezList.size > freezeAppLimit) {
            val foregroundApps = context.getForegroundApps()
            while (freezList.size > freezeAppLimit) {
                val app = freezList.first()
                if (!foregroundApps.contains(app.packageName)) {
                    freezeApp(app)
                }
            }
        }
    }

    // 冻结已经后台超时的偏见应用
    fun clearFreezeAppTimeLimit() {
        val freezAppTimeLimit = this.freezeAppTimeLimit
        if (freezAppTimeLimit > 0) {
            val currentTime = System.currentTimeMillis()
            val targetApps = freezList.filter {
                it.leaveTime > -1 && currentTime - it.leaveTime > freezAppTimeLimit && it.packageName != lastAppPackageName
            }
            if (targetApps.isNotEmpty()) {
                val foregroundApps = context.getForegroundApps()
                targetApps.forEach {
                    if(!foregroundApps.contains(it.packageName)) {
                        freezeApp(it)
                    }
                }
            }
        }
    }

    // 冻结指定应用
    fun freezeApp(app: FreezeAppHistory) {
        val currentAppConfig = store.getAppConfig(app.packageName)
        if (currentAppConfig.freeze) {
            if (suspendMode) {
                suspendApp(app.packageName)
            } else {
                freezeApp(app.packageName)
            }
        }
        freezList.remove(app)
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
        // 离开偏见应用时，记录偏见应用最后活动时间
        if (sceneConfigInfo.freeze) {
            setFreezeAppLeaveTime(sceneConfigInfo.packageName)
        }

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

                    if (currentSceneConfig!!.freeze) {
                        setFreezeAppStartTime(packageName)
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
        if (mode == "scene_bg") {
            Scene.postDelayed({
                if (currentSceneConfig?.packageName != app) {
                    util.setGroup(app, mode)
                }
            }, 3000)
        } else if (mode == "scene_cache") {
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
        instance = null
    }

    fun onScreenOn() {
        // 屏幕点亮后恢复屏幕自动旋转设置
        updateScreenRotation()
    }

    fun onScreenOff() {
        // 息屏时暂停屏幕旋转修改
        floatScreenRotation.remove()
    }


    fun onScreenOffDelay() {
    }
}
