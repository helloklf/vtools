package com.omarea.vtools

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.omarea.store.SpfConfig
import com.omarea.vtools.services.BootService


class ReceiverBoot : BroadcastReceiver() {
    companion object {
        var bootCompleted: Boolean = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (bootCompleted) {
            return
        }
        bootCompleted = true

        val globalConfig = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        val mode = globalConfig.getString(SpfConfig.GLOBAL_SPF_POWERCFG, "")
        if (mode.isNullOrEmpty()) {
            return
        }

        try {
            val service = Intent(context, BootService::class.java)
            context.startService(service)
        } catch (ex: Exception) {
        }
    }
}
