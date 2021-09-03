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


class ReceiverCpuAffinity : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        try {
            intent.extras?.run {
                val app = getString("app")
                val mode = getString("mode")
            }
        } catch (ex: Exception) {
        }
    }
}
