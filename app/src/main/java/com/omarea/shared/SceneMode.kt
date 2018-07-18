package com.omarea.shared

import android.content.ContentResolver
import android.provider.Settings

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

    fun onFocusdAppChange (packageName: String) {
        if (lastAppPackageName == packageName) {
            return
        }
        config = store.getAppConfig(packageName)
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
    }
}
