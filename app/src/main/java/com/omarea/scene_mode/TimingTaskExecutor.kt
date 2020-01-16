package com.omarea.scene_mode

import android.content.Context
import android.content.Intent
import com.omarea.model.TaskAction
import com.omarea.model.TimingTaskInfo
import com.omarea.shell_utils.NetworkUtils
import com.omarea.vtools.R
import com.omarea.vtools.services.CompileService

class TimingTaskExecutor(private val timingTask: TimingTaskInfo, private val context: Context) {
    public fun run() {
        val actions = timingTask.taskActions
        actions?.run {
            actions.forEach {
                when (it) {
                    TaskAction.AIRPLANE_MODE_OFF -> {
                        NetworkUtils(context).airModeOff()
                    }
                    TaskAction.AIRPLANE_MODE_ON -> {
                        NetworkUtils(context).airModeOn()
                    }
                    TaskAction.COMPILE_EVERYTHING -> {
                        everythingDex2oatCompile()
                    }
                    TaskAction.COMPILE_SPEED -> {
                        speedDex2oatCompile()
                    }
                    TaskAction.FSTRIM -> {
                        // TODO:
                    }
                    TaskAction.GPRS_OFF -> {
                        NetworkUtils(context).mobileDataOff()
                    }
                    TaskAction.GPRS_ON -> {
                        if (!this.contains(TaskAction.AIRPLANE_MODE_ON)) {
                            NetworkUtils(context).mobileDataOn()
                        }
                    }
                    TaskAction.GPS_OFF -> {
                        LocationHelper().disableGPS()
                    }
                    TaskAction.GPS_ON -> {
                        LocationHelper().enableGPS()
                    }
                    TaskAction.STANDBY_MODE_OFF -> {
                        // TODO:
                    }
                    TaskAction.STANDBY_MODE_ON -> {
                        // TODO:
                    }
                    TaskAction.WIFI_OFF -> {
                        NetworkUtils(context).wifiOff()
                    }
                    TaskAction.WIFI_ON -> {
                        if (!this.contains(TaskAction.AIRPLANE_MODE_ON)) {
                            NetworkUtils(context).wifiOn()
                        }
                    }
                    else -> { }
                }
            }
        }
    }

    private fun speedDex2oatCompile () {
        val service = Intent(context, CompileService::class.java)
        service.action = context.getString(R.string.scene_speed_compile)
        context.startService(service)
    }
    private fun everythingDex2oatCompile () {
        val service = Intent(context, CompileService::class.java)
        service.action = context.getString(R.string.scene_everything_compile)
        context.startService(service)
    }
}
