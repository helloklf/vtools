package com.omarea.vtools.services

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.PowerManager.PARTIAL_WAKE_LOCK
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.omarea.Scene
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShell
import com.omarea.vtools.R
import java.nio.charset.Charset
import java.util.*

/**
 * 后台编译应用
 */
class CompileService : IntentService("vtools-compile") {
    companion object {
        var compiling = false
    }

    private var compileCanceled = false
    private var keepShell = KeepShell(true)
    private lateinit var nm: NotificationManager
    private var compile_method = "speed"
    private var channelCreated = false

    private fun getAllPackageNames(): ArrayList<String> {
        val packageManager: PackageManager = packageManager
        val packageInfos = packageManager.getInstalledApplications(0)
        val list = ArrayList<String>()/*在数组中存放数据*/
        for (i in packageInfos.indices) {
            list.add(packageInfos[i].packageName)
        }
        list.remove(packageName)
        // Google gms服务，每次编译都会重新编译，不知什么情况！
        list.remove("com.google.android.gms")
        return list
    }

    private fun updateNotification(title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel("vtool-compile", "后台编译", NotificationManager.IMPORTANCE_LOW))
            nm.notify(990, NotificationCompat.Builder(this, "vtool-compile").setSmallIcon(R.drawable.process)
                    .setContentTitle(title)
                    .setContentText(text)
                    .build())
        } else {
            nm.notify(990, NotificationCompat.Builder(this).setSmallIcon(R.drawable.process).setSubText(title).setContentText(text).build())
        }
    }

    private fun updateNotification(title: String, text: String, total: Int, current: Int, autoCancel: Boolean = true) {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!channelCreated) {
                nm.createNotificationChannel(NotificationChannel("vtool-compile", "后台编译", NotificationManager.IMPORTANCE_LOW))
                channelCreated = true
            }
            NotificationCompat.Builder(this, "vtool-compile")
        } else {
            NotificationCompat.Builder(this)
        }

        nm.notify(990, builder
                .setSmallIcon(R.drawable.process)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(autoCancel)
                .setProgress(total, current, false)
                .build())
    }

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
        mWakeLock = mPowerManager.newWakeLock(PARTIAL_WAKE_LOCK, "scene:CompileService");
        mWakeLock.acquire(60 * 60 * 1000) // 默认限制60分钟

        nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (compiling) {
            compileCanceled = true
            this.hideNotification()
            return
        }

        if (intent != null) {
            if (intent.action == getString(R.string.scene_speed_compile)) {
                compile_method = "speed"
            } else if (intent.action == getString(R.string.scene_speed_profile_compile)) {
                compile_method = "speed-profile"
            } else if (intent.action == getString(R.string.scene_everything_compile)) {
                compile_method = "everything"
            } else if (intent.action == getString(R.string.scene_reset_compile)) {
                compile_method = "reset"
            }
        }

        compiling = true

        val packageNames = getAllPackageNames()
        val total = packageNames.size
        var current = 0
        if (compile_method == "reset") {
            val cmdBuilder = StringBuilder()
            for (packageName in packageNames) {
                if (true) {
                    updateNotification(getString(R.string.dex2oat_reset_running), packageName, total, current)
                    cmdBuilder.append("am broadcast -n com.omarea.vtools/com.omarea.vtools.ReceiverCompileState --ei current $current --ei total $total --es packageName $packageName\n")
                    cmdBuilder.append("cmd package compile --reset ${packageName}\n")
                    current++
                } else {
                    break
                }
            }
            cmdBuilder.append("am broadcast -n com.omarea.vtools/com.omarea.vtools.ReceiverCompileState --ei current $total --ei total $total --es packageName OK\n")
            val cache = "/dex2oat/reset.sh"
            if (FileWrite.writePrivateFile(cmdBuilder.toString().toByteArray(Charset.defaultCharset()), cache, this.applicationContext)) {
                val shellFile = FileWrite.getPrivateFilePath(this.applicationContext, cache)
                keepShell.doCmdSync("sh " + shellFile + " >/dev/null 2>&1 &")
            }
            keepShell.tryExit()
            compileCanceled = true
            Scene.Companion.toast("重置过程中手机会有点卡，请耐心等待~", Toast.LENGTH_LONG)
        } else {
            for (packageName in packageNames) {
                if (true) {
                    updateNotification(getString(R.string.dex2oat_compiling) + "[" + compile_method + "]", "[$current/$total]$packageName", total, current)
                    keepShell.doCmdSync("cmd package compile -m ${compile_method} ${packageName}")
                    current++
                } else {
                    break
                }
            }
            keepShell.doCmdSync("cmd package compile -m ${compile_method} ${packageName}")
        }
        this.hideNotification()
        keepShell.tryExit()
        compiling = false
    }

    private fun hideNotification() {
        if (compileCanceled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.cancel(990)
            } else {
                nm.cancel(990)
            }
        } else {
            updateNotification("complete!", getString(R.string.dex2oat_completed), 100, 100, true)
        }
        // System.exit(0)
    }

    override fun onDestroy() {
        this.hideNotification()
        mWakeLock.release()

        super.onDestroy()
    }
}
