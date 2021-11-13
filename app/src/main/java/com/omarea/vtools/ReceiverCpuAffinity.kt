package com.omarea.vtools

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


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
