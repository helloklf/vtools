package com.omarea.vtools.services

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.omarea.common.shell.KeepShell
import com.omarea.data.EventBus
import com.omarea.data.EventType
import com.omarea.library.shell.PropsUtils
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.SpfConfig
import com.omarea.vtools.R

/**
 * Created by Hello on 2017/12/27.
 */

class BootService : IntentService("vtools-boot") {
    private lateinit var globalConfig: SharedPreferences
    private var isFirstBoot = true
    private var bootCancel = false
    private lateinit var nm: NotificationManager

    private lateinit var mPowerManager: PowerManager
    private lateinit var mWakeLock: PowerManager.WakeLock
    override fun onHandleIntent(intent: Intent?) {
        mPowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager;
        /*
            标记值                   CPU  屏幕  键盘
            PARTIAL_WAKE_LOCK       开启  关闭  关闭
            SCREEN_DIM_WAKE_LOCK    开启  变暗  关闭
            SCREEN_BRIGHT_WAKE_LOCK 开启  变亮  关闭
            FULL_WAKE_LOCK          开启  变亮  变亮
        */
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "scene:BootService");
        mWakeLock.acquire(60 * 60 * 1000) // 默认限制60分钟

        nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        globalConfig = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        Thread.sleep(2000)
        val r = PropsUtils.getProp("vtools.c.boot")
        if (!r.isEmpty()) {
            isFirstBoot = false
            bootCancel = true
            this.hideNotification()
            return
        }
        updateNotification(getString(R.string.boot_script_running))
        EventBus.publish(EventType.BOOT_COMPLETED)
        autoBoot()
    }

    private fun autoBoot() {
        val keepShell = KeepShell()

        val context = this.applicationContext

        val globalPowercfg = globalConfig.getString(SpfConfig.GLOBAL_SPF_POWERCFG, "")
        if (!globalPowercfg.isNullOrEmpty()) {
            updateNotification(getString(R.string.boot_use_powercfg))

            val modeList = ModeSwitcher()
            if (modeList.modeConfigCompleted()) {
                modeList.executePowercfgMode(globalPowercfg, context.packageName)
            }
        }

        keepShell.tryExit()
        hideNotification()
    }

    private var channelCreated = false
    private fun updateNotification(text: String) {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!channelCreated) {
                nm.createNotificationChannel(NotificationChannel("vtool-boot", getString(R.string.notice_channel_boot), NotificationManager.IMPORTANCE_LOW))
                channelCreated = true
            }
            NotificationCompat.Builder(this, "vtool-boot")
        } else {
            NotificationCompat.Builder(this)
        }
        nm.notify(900, builder.setSmallIcon(R.drawable.ic_menu_digital).setContentTitle(getString(R.string.notice_channel_boot)).setContentText(text).build())
    }

    private fun hideNotification() {
        if (bootCancel) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.cancel(900)
            } else {
                nm.cancel(900)
            }
        } else {
            updateNotification(getString(R.string.boot_success))
        }
        // System.exit(0)
    }

    override fun onDestroy() {
        hideNotification()
        mWakeLock.release()

        super.onDestroy()
    }
}
