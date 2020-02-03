package com.omarea

import android.app.Application
import android.content.Context
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.ShellExecutor
import com.omarea.data_collection.ChargeCurve
import com.omarea.data_collection.EventBus
import com.omarea.data_collection.publisher.ScreenState
import com.omarea.permissions.Busybox
import com.omarea.scene_mode.TimingTaskManager
import com.omarea.scene_mode.TriggerEventMonitor
import com.omarea.vtools.R
import com.omarea.vtools.services.BatteryService

class Scene : Application() {
    // 锁屏状态监听
    private lateinit var screenState: ScreenState

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        if (!Busybox.systemBusyboxInstalled()) {
            ShellExecutor.setExtraEnvPath(
                FileWrite.getPrivateFilePath(this, getString(R.string.toolkit_install_path))
            )
        }

        // 锁屏状态检测
        screenState = ScreenState(this)
        screenState.autoRegister()

        // 电池状态检测
        BatteryService.startBatteryService(this)

        // 定时任务
        TimingTaskManager(this).updateAlarmManager()

        // 事件任务
        EventBus.subscibe(TriggerEventMonitor(this))

        // 充电曲线
        EventBus.subscibe(ChargeCurve(this))
    }
}
