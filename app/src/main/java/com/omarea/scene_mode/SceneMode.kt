package com.omarea.scene_mode

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.omarea.common.shell.KeepShell
import com.omarea.common.shell.KeepShellPublic
import com.omarea.model.SceneConfigInfo
import com.omarea.store.SceneConfigStore
import com.omarea.shell_utils.GApps

class SceneMode private constructor(context: Context, private var store: SceneConfigStore) {
    private var lastAppPackageName = "com.android.systemui"
    private var contentResolver: ContentResolver = context.contentResolver
    private val freezeUseSuspend = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    private var freezList = ArrayList<FreezeAppHistory>()
    // 偏见应用解冻数量限制
    private val freezAppLimit = 5 // 5个
    // 偏见应用后台超时时间
    private val freezAppTimeLimit = 300000 // 5 分钟

    companion object {
        @Volatile
        private var instance: SceneMode? = null

        // 获取当前实例
        fun getCurrentInstance(): SceneMode? {
            return instance
        }

        // 获取当前实例或初始化
        fun getInstanceOrInit(context: Context, store: SceneConfigStore): SceneMode? {
            if (instance == null) {
                synchronized(SceneMode::class) {
                    instance = SceneMode(context, store)
                }
            }
            return instance!!
        }
    }

    class FreezeAppHistory {
        var startTime: Long = 0
        var leaveTime: Long = 0
        var packageName: String = ""
    }


    fun getLaunchedFreezeApp(): List<String> {
        return freezList.map { it.packageName }
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
        while (freezList.size > freezAppLimit) {
            freezeApp(freezList.first(), KeepShellPublic.getDefaultInstance())
        }
    }

    // 冻结已经后台超时的偏见应用
    fun clearFreezeAppTimeLimit() {
        val currentTime = System.currentTimeMillis()
        freezList.filter {
            it.leaveTime > -1 && currentTime - it.leaveTime > freezAppTimeLimit && it.packageName != lastAppPackageName
        }.forEach { freezeApp(it, KeepShellPublic.getDefaultInstance()) }
    }

    // 冻结指定应用
    fun freezeApp(app: FreezeAppHistory, keepShell: KeepShell) {
        val currentAppConfig = store.getAppConfig(app.packageName)
        if (currentAppConfig.freeze) {
            freezeApp(app.packageName)
        }
        freezList.remove(app)
    }

    fun freezeApp(app: String) {
        if (app.equals("com.android.vending")) {
            GApps().disable(KeepShellPublic.getDefaultInstance());
        } else {
            if (freezeUseSuspend) {
                KeepShellPublic.doCmdSync("pm suspend ${app}\nam force-stop ${app}")
            } else {
                KeepShellPublic.doCmdSync("pm disable ${app}")
            }
        }
    }

    var brightnessMode = -1;
    var screenBrightness = -1;
    var currentSceneConfig: SceneConfigInfo? = null

    private fun backupState(): Int {
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

    private fun resumeState() {
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

    private fun autoLightOff(lightValue: Int = -1): Boolean {
        try {
            if (Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)) {
                contentResolver.notifyChange(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), null)
            } else {
                Log.e("screen_brightness", "修改亮度失败！")
                return false
            }

            if (lightValue > -1 && Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, lightValue)) {
                contentResolver.notifyChange(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), null)
            } else {
                Log.e("screen_brightness", "修改亮度失败！")
                return false
            }
        } catch (ex: Exception) {
            return false
        }
        return true
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
    private fun backupLocationModeState() {
        if (locationMode == "none") {
            locationMode = Settings.Secure.getString(contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED)
        }
    }

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
    private fun backupHeadUp() {
        if (headsup < 0) {
            try {
                headsup = Settings.Global.getInt(contentResolver, "heads_up_notifications_enabled")
            } catch (ex: Exception) {
            }
        }
    }

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
    }

    /**
     * 前台应用切换
     */
    fun onAppEnter(packageName: String, foceUpdateConfig: Boolean = false) {
        try {
            if (lastAppPackageName == packageName && !foceUpdateConfig) {
                return
            }

            if (currentSceneConfig != null) {
                onAppLeave(currentSceneConfig!!)
            }

            currentSceneConfig = store.getAppConfig(packageName)
            if (currentSceneConfig == null) {
                restoreLocationModeState()
                resumeState()
                restoreHeaddUp()
            } else {
                if (currentSceneConfig!!.aloneLight) {
                    backupState()
                    autoLightOff(currentSceneConfig!!.aloneLightValue)
                } else {
                    resumeState()
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
            }

            lastAppPackageName = packageName
        } catch (ex: Exception) {
            Log.e("onAppEnter", "" + ex.message)
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
        resumeState()
        currentSceneConfig = null
    }

    fun onScreenOff() {
        clearFreezeApp()
    }

    /**
     * 冻结所有解冻的偏见应用
     */
    fun clearFreezeApp() {
        while (freezList.size > 0) {
            val firstItem = freezList.first()
            val config = store.getAppConfig(firstItem.packageName)
            if (config.freeze) {
                KeepShellPublic.doCmdSync("pm disable " + firstItem.packageName)
            }
            freezList.remove(firstItem)
        }
    }
}
