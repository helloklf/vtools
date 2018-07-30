package com.omarea.shared

import android.content.ContentResolver
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.omarea.AppConfigInfo
import com.omarea.shell.KeepShellPublic

class SceneMode private constructor(private var contentResolver: ContentResolver, private var store: AppConfigStore) {

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
    var screenBrightness = 100;
    var lastAppPackageName = "com.android.systemui"
    var config: AppConfigInfo? = null

    private fun backupState(): Int {
        try {
            mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
            screenBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
        return mode
    }

    private fun resumeState() {
        if (mode < 0)
            return
        try {
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
            contentResolver.notifyChange(Settings.System.getUriFor("screen_brightness_mode"), null)
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, screenBrightness)
            contentResolver.notifyChange(Settings.System.getUriFor("screen_brightness"), null)
            mode = -1
            screenBrightness = 100
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setScreenLight(level: Int): Boolean {
        try {
            var l = level
            if (l > 255) {
                l = 255
            } else if (l < 1) {
                l = 1
            }
            if (Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)) {
                contentResolver.notifyChange(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), null)
            } else {
                Log.e("screen_brightness", "修改亮度失败！")
                return false
            }
            if (Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, l)) {
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
        KeepShellPublic.doCmdSync("dumpsys deviceidle whitelist -$packageName;\ndumpsys deviceidle enable;\ndumpsys deviceidle enable all;\nam set-inactive $packageName true")
        // if (debugMode) showMsg("休眠 " + packageName)
    }

    /**
     * 杀死指定包名的应用
     */
    private fun killApp(packageName: String, showMsg: Boolean = true) {
        //keepShell2.doCmd("killall -9 $packageName;pkill -9 $packageName;pgrep $packageName |xargs kill -9;")
        KeepShellPublic.doCmdSync("am stop $packageName;am force-stop $packageName;")
        //if (debugMode && showMsg) showMsg("结束 " + packageName)
    }

    /**
     * 自动清理前一个应用后台
     */
    private fun autoBoosterApp(packageName: String) {
        if (config!!.disBackgroundRun) {
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
    fun onFocusdAppChange(packageName: String) {
        try {
            if (lastAppPackageName == packageName) {
                return
            }
            if (config != null) {
                try {
                    if (config!!.aloneLight && config!!.aloneLightValue > 0) {
                        val currentConfig = store.getAppConfig(config!!.packageName)
                        val sb = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                        if (currentConfig.aloneLightValue != sb) {
                            currentConfig.aloneLightValue = sb
                        }
                        store.setAppConfig(currentConfig)
                    }
                } catch (ex: Exception) {

                }
            }
            config = store.getAppConfig(packageName)
            autoBoosterApp(lastAppPackageName)
            if (config == null)
                return
            if (config!!.aloneLight && config!!.aloneLightValue > 0) {
                if (mode < 0) {
                    backupState()
                }
                if (config!!.aloneLightValue > 255) {
                    config!!.aloneLightValue = 255
                }
                setScreenLight(config!!.aloneLightValue)
            } else if (mode > -1) {
                resumeState()
                mode = -1
            }

            if (config!!.gpsOn) {
                val mode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE)
                backupLocationModeState()
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

            lastAppPackageName = packageName
        } catch (ex: Exception) {
            Log.e("onFocusdAppChange", ex.message)
        }
    }

    fun clearState() {
        lastAppPackageName = "com.android.systemui"
        restoreLocationModeState()
        resumeState()
        config = null
    }
}
