package com.omarea.scene_mode

import android.content.ContentResolver
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.omarea.common.shell.KeepShellPublic
import com.omarea.model.AppConfigInfo
import com.omarea.store.AppConfigStore

class SceneMode private constructor(private var contentResolver: ContentResolver, private var store: AppConfigStore) {
    class FreezeAppHistory {
        var startTime: Long = 0
        var leaveTime: Long = 0
        var packageName: String = ""
    }

    companion object {
        private var freezList = ArrayList<FreezeAppHistory>()
        // 偏见应用解冻数量限制
        private val freezAppLimit = 5 // 5个
        // 偏见应用后台超时时间
        private val freezAppTimeLimit = 300000 // 5分钟

        var lastAppPackageName = "com.android.systemui"

        @Volatile
        var instance: SceneMode? = null

        fun getInstanceOrInit(contentResolver: ContentResolver? = null, store: AppConfigStore? = null): SceneMode? {
            if (instance == null) {
                if (contentResolver == null || store == null) {
                    return null
                }
                synchronized(SceneMode::class) {
                    instance = SceneMode(contentResolver, store)
                }
            }
            return instance!!
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

        /**
         * 当解冻的偏见应用数量超过限制，冻结最先解冻的应用
         */
        fun clearFreezeAppCountLimit() {
            while (freezList.size > freezAppLimit) {
                val firstItem = freezList.first()
                // val currentAppConfig = store.getAppConfig(firstItem.packageName)
                // if (currentAppConfig.freeze) {
                KeepShellPublic.doCmdSync("pm disable " + firstItem.packageName)
                // }
                freezList.remove(firstItem)
            }
        }

        /**
         * 冻结已经后台超时的偏见应用
         */
        fun clearFreezeAppTimeLimit() {
            val currentTime = System.currentTimeMillis()
            val clearList = freezList.filter { it.leaveTime > -1 && currentTime - it.leaveTime > freezAppTimeLimit && it.packageName != lastAppPackageName }
            clearList.forEach {
                // val currentAppConfig = store.getAppConfig(it.packageName)
                // if (currentAppConfig.freeze) {
                KeepShellPublic.doCmdSync("pm disable " + it.packageName)
                // }
                freezList.remove(it)
            }
        }
    }

    var brightnessMode = -1;
    var screenBrightness = -1;
    var currentAppConfig: AppConfigInfo? = null
    var lowPowerLevel = 2

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
        if (currentAppConfig != null) {
            return currentAppConfig!!.disNotice
        }
        return false
    }

    /**
     * 按键按下
     * @return 是否阻拦按键事件
     */
    fun onKeyDown(): Boolean {
        if (currentAppConfig != null) {
            return currentAppConfig!!.disButton
        }
        return false
    }

    /**
     * 休眠指定包名的应用
     */
    private fun dozeApp(packageName: String) {
        KeepShellPublic.doCmdSync(
                "dumpsys deviceidle whitelist -$packageName;\n" +
                        "dumpsys deviceidle enable\n" +
                        "dumpsys deviceidle enable all\n" +
                        "am set-inactive $packageName true 2>&1 > /dev/null\n" +
                        "am set-idle $packageName true 2>&1 > /dev/null" +
                        "am make-uid-idle --user current $packageName 2>&1 > /dev/null")
        // if (debugMode) showMsg("休眠 " + packageName)
    }

    /**
     * 杀死指定包名的应用
     */
    private fun killApp(packageName: String) {
        //keepShell2.doCmd("killall -9 $packageName;pkill -9 $packageName;pgrep $packageName |xargs kill -9;")
        // KeepShellPublic.doCmdSync("am stop $packageName;am force-stop $packageName;")
        KeepShellPublic.doCmdSync("am kill-all $packageName;am force-stop $packageName;")
    }

    /**
     * 自动清理前一个应用后台
     */
    private fun autoBoosterApp(packageName: String) {
        if (currentAppConfig == null) {
            return
        }
        val level = getBatteryCapacity()
        if (currentAppConfig!!.disBackgroundRun || (level > -1 && level < lowPowerLevel)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dozeApp(packageName)
            } else {
                killApp(packageName)
            }
        }
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


    private fun getBatteryCapacity(): Int {
        try {
            val batteryInfo = KeepShellPublic.doCmdSync("dumpsys battery")
            val batteryInfos = batteryInfo.split("\n")

            // 由于部分手机相同名称的参数重复出现，并且值不同，为了避免这种情况，加个额外处理，同名参数只读一次
            var levelReaded = false
            var batteryCapacity = -1

            for (item in batteryInfos) {
                val info = item.trim()
                val index = info.indexOf(":")
                if (index > Int.MIN_VALUE && index < info.length) {
                    val value = info.substring(info.indexOf(":") + 1).trim()
                    if (info.startsWith("level")) {
                        if (!levelReaded) {
                            try {
                                batteryCapacity = value.trim().toInt()
                            } catch (ex: java.lang.Exception) {
                            }
                            levelReaded = true
                        } else continue
                    }
                }
            }
            return batteryCapacity
        } catch (ex: java.lang.Exception) {
            return -1
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
    fun onAppLeave(appConfigInfo: AppConfigInfo) {
        // 离开偏见应用时，记录偏见应用最后活动时间
        if (appConfigInfo.freeze) {
            setFreezeAppLeaveTime(appConfigInfo.packageName)
        }
        if (appConfigInfo.aloneLight) {
            // 独立亮度 记录最后的亮度值
            try {
                val light = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                if (light != appConfigInfo.aloneLightValue) {
                    appConfigInfo.aloneLightValue = light
                    store.setAppConfig(appConfigInfo)
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
            if (lastAppPackageName != packageName)
                autoBoosterApp(lastAppPackageName)

            if (currentAppConfig != null) {
                onAppLeave(currentAppConfig!!)
            }

            currentAppConfig = store.getAppConfig(packageName)
            if (currentAppConfig == null) {
                restoreLocationModeState()
                resumeState()
                restoreHeaddUp()
            } else {
                if (currentAppConfig!!.aloneLight) {
                    backupState()
                    autoLightOff(currentAppConfig!!.aloneLightValue)
                } else {
                    resumeState()
                }

                if (currentAppConfig!!.gpsOn) {
                    backupLocationModeState()
                    val mode = Settings.Secure.getString(contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED)
                    if (!mode.contains("gps")) {
                        LocationHelper().enableGPS()
                    }
                } else {
                    restoreLocationModeState()
                }

                if (currentAppConfig!!.disNotice) {
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

                if (currentAppConfig!!.freeze) {
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
        currentAppConfig = null
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
