package com.omarea.scene_mode

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import android.os.PowerManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.omarea.common.shell.KeepShell
import com.omarea.common.shell.KeepShellPublic
import com.omarea.library.shell.FstrimUtils
import com.omarea.library.shell.ZenModeUtils
import com.omarea.model.CustomTaskAction
import com.omarea.model.TaskAction
import com.omarea.vtools.R
import java.util.*

class TaskActionsExecutor(
        private val taskActions: ArrayList<TaskAction>?,
        private val customTaskActions: ArrayList<CustomTaskAction>?,
        private val context: Context) {
    private var nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    private lateinit var mPowerManager: PowerManager
    private lateinit var mWakeLock: PowerManager.WakeLock
    private var currentShell: KeepShell? = null
    private var isCommonShell = false
    private val keepShell: KeepShell
        get() {
            if (currentShell == null) {
                val commonInstance = KeepShellPublic.getInstance("TaskActionsExecutor", true)
                if (commonInstance.isIdle) {
                    currentShell = commonInstance
                    isCommonShell = true
                } else {
                    currentShell = KeepShell();
                }
            }
            return currentShell!!
        }

    public fun run() {
        mPowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager;
        /*
            标记值                   CPU  屏幕  键盘
            PARTIAL_WAKE_LOCK       开启  关闭  关闭
            SCREEN_DIM_WAKE_LOCK    开启  变暗  关闭
            SCREEN_BRIGHT_WAKE_LOCK 开启  变亮  关闭
            FULL_WAKE_LOCK          开启  变亮  变亮
        */
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "scene:TaskActionsExecutor");
        mWakeLock.acquire(600 * 1000) // 默认限制10分钟

        taskActions?.forEach {
            try {
                when (it) {
                    TaskAction.FSTRIM -> {
                        updateNotification("执行fstrim")
                        FstrimUtils(keepShell).run()
                    }
                    TaskAction.STANDBY_MODE_OFF -> {
                        updateNotification("关闭待机模式")
                        SceneStandbyMode(context, keepShell).off()
                    }
                    TaskAction.STANDBY_MODE_ON -> {
                        updateNotification("打开待机模式")
                        SceneStandbyMode(context, keepShell).on()
                    }
                    TaskAction.ZEN_MODE_ON -> {
                        updateNotification("打开勿扰模式")
                        ZenModeUtils(context).on()
                    }
                    TaskAction.ZEN_MODE_OFF -> {
                        updateNotification("关闭勿扰模式")
                        ZenModeUtils(context).off()
                    }
                    else -> {
                    }
                }
            } catch (ex: Exception) {
                Toast.makeText(context, "定时任务出错：" + ex.message, Toast.LENGTH_LONG).show()
            }
        }

        customTaskActions?.forEach {
            updateNotification(it.Name)
            keepShell.doCmdSync(it.Command)
        }

        if (!isCommonShell) {
            currentShell?.tryExit()
        }
        hideNotification()

        mWakeLock.release()
    }

    private fun speedDex2oatCompile() {
        keepShell.doCmdSync("nohup cmd package compile -m speed -a >/dev/null 2>&1 &")
    }

    private fun everythingDex2oatCompile() {
        keepShell.doCmdSync("nohup cmd package compile -m everything -a >/dev/null 2>&1 &")
    }

    private var channelCreated = false
    private fun updateNotification(text: String) {
        val notic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!channelCreated) {
                nm.createNotificationChannel(NotificationChannel("vtools-task", context.getString(R.string.notice_channel_task), NotificationManager.IMPORTANCE_LOW))
                channelCreated = true
            }
            NotificationCompat.Builder(context, "vtools-task")
        } else {
            NotificationCompat.Builder(context)
        }
        nm.notify(920, notic.setSmallIcon(R.drawable.ic_clock).setWhen(System.currentTimeMillis()).setContentTitle(context.getString(R.string.notice_channel_task)).setContentText(text).build())
    }

    private fun hideNotification() {
        updateNotification(context.getString(R.string.notice_task_completed))
    }
}
