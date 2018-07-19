package com.omarea.shared

import android.content.ContentResolver
import android.os.Build
import android.provider.Settings
import com.omarea.shell.KeepShellSync

class SceneMode private constructor(private var contentResolver: ContentResolver, private var store: AppConfigStore) {

    companion object {
        @Volatile
        var instance: SceneMode? = null

        fun getInstanceOrInit (contentResolver: ContentResolver? = null, store: AppConfigStore? = null) : SceneMode?{
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
    var config: AppConfigStore.AppConfigInfo? = null

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

    private fun setScreenLight(level: Byte) {
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
        contentResolver.notifyChange(Settings.System.getUriFor("screen_brightness_mode"), null)
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, level.toInt())
        contentResolver.notifyChange(Settings.System.getUriFor("screen_brightness"), null)
    }

    fun onNotificationPosted(): Boolean {
        if (config != null) {
            return config!!.disNotice
        }
        return false
    }

    fun onKeyDown(): Boolean {
        if (config != null) {
            return config!!.disButton
        }
        return false
    }

    //休眠指定包名的应用
    private fun dozeApp(packageName: String) {
        KeepShellSync.doCmdSync("dumpsys deviceidle whitelist -$packageName;\ndumpsys deviceidle enable;\ndumpsys deviceidle enable all;\nam set-inactive $packageName true")
        // if (debugMode) showMsg("休眠 " + packageName)
    }

    //杀死指定包名的应用
    private fun killApp(packageName: String, showMsg: Boolean = true) {
        //keepShell2.doCmd("killall -9 $packageName;pkill -9 $packageName;pgrep $packageName |xargs kill -9;")
        KeepShellSync.doCmdSync("am stop $packageName;am force-stop $packageName;")
        //if (debugMode && showMsg) showMsg("结束 " + packageName)
    }

    private fun autoBoosterApp(packageName: String) {
        if (config!!.disBackgroundRun) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dozeApp(packageName)
            } else {
                killApp(packageName)
            }
        }
    }

    fun onFocusdAppChange (packageName: String) {
        if (lastAppPackageName == packageName) {
            return
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
            setScreenLight(config!!.aloneLightValue.toByte())
        } else if (mode > -1) {
            resumeState()
            mode = -1
        }
        lastAppPackageName = packageName
    }
}
