package com.omarea.vtools

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.omarea.data_collection.EventBus
import com.omarea.data_collection.EventType
import com.omarea.store.SpfConfig
import com.omarea.vtools.services.BatteryService
import com.omarea.vtools.services.BootService
import com.omarea.vtools.services.CompileService


class ReceiverBoot : BroadcastReceiver() {
    companion object {
        var running: Boolean = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (running) {
            return
        }

        try {
            val chargeConfig: SharedPreferences = context.getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
            //判断是否开启了充电加速和充电保护，如果开启了，自动启动后台服务
            if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false) || chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_BP, false)) {
                try {
                    val i = Intent(context, BatteryService::class.java)
                    context.startService(i)
                } catch (ex: Exception) {
                }
            }
            val service = Intent(context, BootService::class.java)
            //service.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(service)
            running = true
        } catch (ex: Exception) {
        }
    }
}
