package com.omarea

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.ShellExecutor
import com.omarea.data_collection.ChargeCurve
import com.omarea.data_collection.EventBus
import com.omarea.data_collection.publisher.BatteryState
import com.omarea.data_collection.publisher.ScreenState
import com.omarea.permissions.Busybox
import com.omarea.scene_mode.TimingTaskManager
import com.omarea.scene_mode.TriggerEventMonitor
import com.omarea.utils.CrashHandler
import com.omarea.vtools.R

class Scene : Application() {
    companion object {
        private val handler = Handler(Looper.getMainLooper())
        public lateinit var context: Application
        public fun toast(message: String, time: Int) {
            handler.post {
                Toast.makeText(context, message, time).show()
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

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        CrashHandler().init(this)
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES) {
            setTheme(R.style.AppThemeNight)
        }

        context = this

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

        // 定时任务
        TimingTaskManager(this).updateAlarmManager()

        // 事件任务
        EventBus.subscibe(TriggerEventMonitor(this))

        // 充电曲线
        EventBus.subscibe(ChargeCurve(this))
    }
}
