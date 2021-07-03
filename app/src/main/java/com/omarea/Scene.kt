package com.omarea

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.ShellExecutor
import com.omarea.data.EventBus
import com.omarea.data.customer.ScreenOffCleanup
import com.omarea.data.publisher.BatteryState
import com.omarea.data.publisher.ScreenState
import com.omarea.permissions.Busybox
import com.omarea.store.SpfConfig
import com.omarea.vtools.R

class Scene : Application() {
    companion object {
        private val handler = Handler(Looper.getMainLooper())
        public lateinit var context: Application
        public lateinit var thisPackageName: String
        private var nightMode = false
        private var config: SharedPreferences? = null
        public val isNightMode: Boolean
            get() {
                return nightMode
            }

        public fun getBoolean(key: String, defaultValue: Boolean): Boolean {
            if (config == null) {
                config = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
            }
            return config!!.getBoolean(key, defaultValue)
        }

        public fun getString(key: String, defaultValue: String): String? {
            if (config == null) {
                config = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
            }
            return config!!.getString(key, defaultValue)
        }

        public fun toast(message: String, time: Int) {
            handler.post {
                Toast.makeText(context, message, time).show()
            }
        }

        public fun toast(message: String) {
            handler.post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        public fun toast(message: Int, time: Int) {
            handler.post {
                Toast.makeText(context, message, time).show()
            }
        }

        public fun post(runnable: Runnable) {
            handler.post(runnable)
        }

        public fun postDelayed(runnable: Runnable, delayMillis: Long) {
            handler.postDelayed(runnable, delayMillis)
        }
    }

    // 锁屏状态监听
    private lateinit var screenState: ScreenState

    private var lastThemeId = R.style.AppTheme
    private fun setAppTheme(theme: Int) {
        if (lastThemeId != theme) {
            setTheme(theme)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        /*
        try {
            val theme = (if ((newConfig.uiMode and Configuration.UI_MODE_NIGHT_YES) != 0) {
                R.style.AppThemeNight
            } else {
                R.style.AppTheme
            })
            setAppTheme(theme)
        } catch (ex: Exception) {
        }
        */
        nightMode = ((newConfig.uiMode and Configuration.UI_MODE_NIGHT_YES) != 0)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        /*
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES) {
            setAppTheme(R.style.AppThemeNight)
        }
        */
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES) {
            nightMode = true
        }

        context = this
        thisPackageName = this.packageName

        // 安装busybox
        if (!Busybox.systemBusyboxInstalled()) {
            ShellExecutor.setExtraEnvPath(
                FileWrite.getPrivateFilePath(this, getString(R.string.toolkit_install_path))
            )
        }

        // 锁屏状态检测
        screenState = ScreenState(this)
        screenState.autoRegister()

        // 电池状态检测
        BatteryState(context).registerReceiver()

        // 息屏后关闭所有监视器悬浮窗
        EventBus.subscibe(ScreenOffCleanup(context))
    }
}
