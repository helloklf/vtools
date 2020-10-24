package com.omarea.vtools

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.omarea.Scene


class ReceiverCompileState : BroadcastReceiver() {
    companion object {
        private var channelCreated = false
        private lateinit var nm: NotificationManager
    }

    init {
        nm = Scene.context.getSystemService(IntentService.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun updateNotification(title: String, text: String, total: Int, current: Int, autoCancel: Boolean = true) {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!channelCreated) {
                nm.createNotificationChannel(NotificationChannel("vtool-compile", "后台编译", NotificationManager.IMPORTANCE_LOW))
                channelCreated = true
            }
            NotificationCompat.Builder(Scene.context, "vtool-compile")
        } else {
            NotificationCompat.Builder(Scene.context)
        }

        nm.notify(990, builder
                .setSmallIcon(R.drawable.process)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(autoCancel)
                .setProgress(total, current, false)
                .build())
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            intent.extras?.run {
                val current = getInt("current")
                val total = getInt("total")
                val packageName = getString("packageName")!!
                if (total == current) {
                    updateNotification("complete!", context.getString(R.string.dex2oat_completed), 100, 100, true)
                } else {
                    updateNotification(context.getString(R.string.dex2oat_reset_running), packageName, total, current)
                }
            }
        } catch (ex: Exception) {
        }
    }
}
