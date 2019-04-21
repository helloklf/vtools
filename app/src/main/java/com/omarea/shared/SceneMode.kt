package com.omarea.shared

import android.content.ContentResolver
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.omarea.shared.model.AppConfigInfo
import com.omarea.shell.KeepShellPublic

class SceneMode private constructor(private var contentResolver: ContentResolver, private var store: AppConfigStore) {
    private class FreezeAppHistory {
        var lastTime:Long = 0
        var packageName: String = ""
    }

    private var freezList = ArrayList<FreezeAppHistory>()
    // 偏见应用解冻数量限制
    private val freezAppLimit = 5
    // 偏见应用后台超时时间
    private val freezAppTimeLimit = 120000

    companion object {
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
    }

    var mode = -1;
    // var screenBrightness = -1;
    var lastAppPackageName = "com.android.systemui"
    var config: AppConfigInfo? = null
    var lowPowerLevel = 2

    private fun backupState(): Int {
        try {
            mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
            // screenBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
        return mode
    }

    private fun resumeState() {
        try {
            if (mode > -1) {
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
                contentResolver.notifyChange(Settings.System.getUriFor("screen_brightness_mode"), null)
                mode = -1
            }
            /*
            if (screenBrightness > -1) {
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, screenBrightness)
                contentResolver.notifyChange(Settings.System.getUriFor("screen_brightness"), null)
                screenBrightness = -1
            }
            */
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun autoLightOff(): Boolean {
        try {
            if (Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)) {
                contentResolver.notifyChange(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), null)
            } else {
                Log.e("screen_brightness", "修改亮度失败！")
                return false
            }
            /*
            if (Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, lightValue)) {
                contentResolver.notifyChange(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), null)
            } else {
                Log.e("screen_brightness", "修改亮度失败！")
                return false
            }
            */
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
        if (config != null) {
            return config!!.disNotice
        }
        return false
    }

    /**
     * 按键按下
     * @return 是否阻拦按键事件
     */
    fun onKeyDown(): Boolean {
        if (config != null) {
            return config!!.disButton
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
    private fun killApp(packageName: String, showMsg: Boolean = true) {
        //keepShell2.doCmd("killall -9 $packageName;pkill -9 $packageName;pgrep $packageName |xargs kill -9;")
        // KeepShellPublic.doCmdSync("am stop $packageName;am force-stop $packageName;")
        KeepShellPublic.doCmdSync("am kill-all $packageName;am force-stop $packageName;")
        //if (debugMode && showMsg) showMsg("结束 " + packageName)
    }

    /**
     * 自动清理前一个应用后台
     */
    private fun autoBoosterApp(packageName: String) {
        if (config == null) {
            return
        }
        val level = getBatteryCapacity()
        if (config!!.disBackgroundRun || (level > -1 && level < lowPowerLevel)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dozeApp(packageName)
            } else {
                killApp(packageName)
            }
        }
    }

    private var locationMode = -1
    private fun backupLocationModeState() {
        // TODO:优化
        locationMode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
    }

    private fun restoreLocationModeState() {
        if (locationMode > -1) {
            Settings.Secure.putInt(contentResolver, Settings.Secure.LOCATION_MODE, locationMode)
            contentResolver.notifyChange(Settings.System.getUriFor(Settings.Secure.LOCATION_MODE), null)
            locationMode = -1
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
     * 前台应用切换
     */
    fun onFocusdAppChange(packageName: String, foceUpdateConfig: Boolean = false) {
        try {
            if (lastAppPackageName == packageName && !foceUpdateConfig) {
                return
            }
            if (lastAppPackageName != packageName)
                autoBoosterApp(lastAppPackageName)

            // 离开偏见应用时，记录偏见应用最后活动时间
            if (config != null && config!!.freeze) {
                updateFreezeAppHistory(config!!.packageName)
            }

            config = store.getAppConfig(packageName)
            if (config == null) {
                restoreLocationModeState()
                resumeState()
                restoreHeaddUp()
            } else {
                if (config!!.aloneLight) {
                    backupState()
                    autoLightOff()
                } else {
                    resumeState()
                }

                if (config!!.gpsOn) {
                    backupLocationModeState()
                    val mode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
                    if (mode != Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {
                        Settings.Secure.putInt(contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_HIGH_ACCURACY)
                        contentResolver.notifyChange(Settings.System.getUriFor(Settings.Secure.LOCATION_MODE), null)
                    }
                } else {
                    restoreLocationModeState()
                }

                if (config!!.disNotice) {
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
            }

            lastAppPackageName = packageName
        } catch (ex: Exception) {
            Log.e("onFocusdAppChange", ex.message)
        }
    }

    fun updateAppConfig() {
        if (!lastAppPackageName.isEmpty()) {
            onFocusdAppChange(lastAppPackageName, true)
        }
    }

    fun clearState() {
        lastAppPackageName = "com.android.systemui"
        restoreLocationModeState()
        resumeState()
        config = null
    }

    fun onScreenOff() {
        clearFreezeApp()
    }

    /**
     * 添加偏见历史记录
     */
    fun updateFreezeAppHistory(packageName:String) {
        for (it in freezList) {
            if (it.packageName == packageName) {
                freezList.remove(it)
                break
            }
        }
        val history = FreezeAppHistory()
        history.lastTime = System.currentTimeMillis()
        history.packageName = packageName

        freezList.add(history)
        clearFreezeAppCountLimit()
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

    /**
     * 当解冻的偏见应用数量超过限制，冻结最先解冻的应用
     */
    fun clearFreezeAppCountLimit() {
        while (freezList.size > freezAppLimit) {
            val firstItem = freezList.first()
            val config = store.getAppConfig(firstItem.packageName)
            if (config.freeze) {
                KeepShellPublic.doCmdSync("pm disable " + firstItem.packageName)
            }
            freezList.remove(firstItem)
        }
    }

    /**
     * 冻结已经后台超时的偏见应用
     */
    fun clearFreezeAppTimeLimit() {
        val currentTime = System.currentTimeMillis()
        val clearList = freezList.filter { currentTime - it.lastTime> freezAppTimeLimit && it.packageName != lastAppPackageName }
        clearList.forEach {
            val config = store.getAppConfig(it.packageName)
            if (config.freeze) {
                KeepShellPublic.doCmdSync("pm disable " + it.packageName)
            }
            freezList.remove(it)
        }
    }
}
