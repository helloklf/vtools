package com.omarea.vtools.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.omarea.vtools.services.BootService
import com.omarea.shared.SpfConfig
import com.omarea.vtools.services.ServiceBattery


class ReceiverBoot : BroadcastReceiver() {
    private lateinit var chargeConfig: SharedPreferences
    override fun onReceive(context: Context, intent: Intent) {
        try {
            chargeConfig = context.getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
            //判断是否开启了充电加速和充电保护，如果开启了，自动启动后台服务
            if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false) || chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_BP, false)) {
                try {
                    val i = Intent(context, ServiceBattery::class.java)
                    context.startService(i)
                } catch (ex: Exception) {
                }
            }
            val service = Intent(context, BootService::class.java)
            //service.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(service)
        } catch (ex: Exception) {

        }
    }
}
