package com.omarea.scene_mode

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import com.omarea.common.shell.KeepShell
import com.omarea.model.TaskAction
import com.omarea.model.TimingTaskInfo
import com.omarea.shell_utils.FstrimUtils
import com.omarea.shell_utils.NetworkUtils
import com.omarea.shell_utils.SceneStandbyMode
import com.omarea.vtools.R
import com.omarea.vtools.services.CompileService
import java.lang.Exception

class TimingTaskExecutor(private val timingTask: TimingTaskInfo, private val context: Context) {
    private var nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    public fun run() {
        val actions = timingTask.taskActions
        actions?.run {
            var keepShell: KeepShell? = null

            actions.forEach {
                try {
                    when (it) {
                        TaskAction.AIRPLANE_MODE_OFF -> {
                            updateNotification("关闭飞行模式")
                            NetworkUtils(context).airModeOff()
                        }
                        TaskAction.AIRPLANE_MODE_ON -> {
                            updateNotification("打开飞行模式")
                            NetworkUtils(context).airModeOn()
                        }
                        TaskAction.COMPILE_EVERYTHING -> {
                            everythingDex2oatCompile()
                        }
                        TaskAction.COMPILE_SPEED -> {
                            speedDex2oatCompile()
                        }
                        TaskAction.FSTRIM -> {
                            updateNotification("执行fstrim")
                            if (keepShell == null) {
                                keepShell = KeepShell()
                            }
                            FstrimUtils(keepShell!!).run()
                        }
                        TaskAction.GPRS_OFF -> {
                            updateNotification("关闭数据网络")
                            NetworkUtils(context).mobileDataOff()
                        }
                        TaskAction.GPRS_ON -> {
                            updateNotification("打开数网络")
                            if (!this.contains(TaskAction.AIRPLANE_MODE_ON)) {
                                NetworkUtils(context).mobileDataOn()
                            }
                        }
                        TaskAction.GPS_OFF -> {
                            updateNotification("关闭GPS定位")
                            LocationHelper().disableGPS()
                        }
                        TaskAction.GPS_ON -> {
                            updateNotification("打开GPS定位")
                            LocationHelper().enableGPS()
                        }
                        TaskAction.STANDBY_MODE_OFF -> {
                            if (keepShell == null) {
                                keepShell = KeepShell()
                            }
                            updateNotification("关闭待机模式")
                            SceneStandbyMode(context, keepShell!!).off()
                        }
                        TaskAction.STANDBY_MODE_ON -> {
                            if (keepShell == null) {
                                keepShell = KeepShell()
                            }
                            updateNotification("打开待机模式")
                            SceneStandbyMode(context, keepShell!!).on()
                        }
                        TaskAction.WIFI_OFF -> {
                            updateNotification("关闭WIFI")
                            NetworkUtils(context).wifiOff()
                        }
                        TaskAction.WIFI_ON -> {
                            updateNotification("打开WIFI")
                            if (!this.contains(TaskAction.AIRPLANE_MODE_ON)) {
                                NetworkUtils(context).wifiOn()
                            }
                        }
                        else -> { }
                    }
                } catch (ex: Exception) {
                    Toast.makeText(context, "定时任务出错：" + ex.message, Toast.LENGTH_LONG).show()
                }
            }

            hideNotification()
            Thread.sleep(1000)
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

    private fun updateNotification(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel("vtool-boot", context.getString(R.string.notice_channel_task), NotificationManager.IMPORTANCE_LOW))
            nm.notify(900, NotificationCompat.Builder(context, "vtool-boot").setSmallIcon(R.drawable.ic_menu_digital).setContentTitle(context.getString(R.string.notice_channel_task)).setContentText(text).build())
        } else {
            nm.notify(900, NotificationCompat.Builder(context).setSmallIcon(R.drawable.ic_menu_digital).setContentTitle(context.getString(R.string.notice_channel_task)).setContentText(text).build())
        }
    }

    private fun hideNotification () {
        updateNotification(context.getString(R.string.notice_task_completed))
    }
}
