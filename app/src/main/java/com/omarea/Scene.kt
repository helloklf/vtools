package com.omarea

import android.app.Application
import android.content.Context
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.ShellExecutor
import com.omarea.data_collection.publisher.ScreenState
import com.omarea.permissions.Busybox
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

        screenState = ScreenState(this)
        screenState.autoRegister()

        // 电池状态检测
        BatteryService.startBatteryService(this)
    }
}
