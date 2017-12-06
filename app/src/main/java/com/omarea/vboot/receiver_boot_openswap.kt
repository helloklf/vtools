package com.omarea.vboot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.omarea.shared.SpfConfig
import java.io.DataOutputStream
import java.io.IOException

class receiver_boot_openswap : BroadcastReceiver() {
    private var p: Process? = null
    internal var out: DataOutputStream? = null

    internal fun tryExit() {
        try {
            if (out != null)
                out!!.close()
        } catch (ex: Exception) {
        }

        try {
            p!!.destroy()
        } catch (ex: Exception) {
        }

    }

    @JvmOverloads internal fun DoCmd(cmd: String, isRedo: Boolean = false) {
        Thread(Runnable {
            try {
                tryExit()
                if (p == null || isRedo || out == null) {
                    tryExit()
                    p = Runtime.getRuntime().exec("su")
                    out = DataOutputStream(p!!.outputStream)
                }
                out!!.writeBytes(cmd)
                out!!.writeBytes("\n")
                out!!.flush()
                //out!!.close()
            } catch (e: IOException) {
                //重试一次
                if (!isRedo)
                    DoCmd(cmd, true)
                else {

                }
            }
        }).start()
    }

    override fun onReceive(context: Context, intent: Intent) {
        //判断是否开启了充电加速和充电保护，如果开启了，自动启动后台服务
        var chargeConfig = context.getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
        if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false) || chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_BP, false)) {
            try {
                val i = Intent(context, BatteryService::class.java)
                context.startService(i)
            } catch (ex: Exception) {

            }
        }

        var sb = StringBuilder("setenforce 0\n")
        val swapConfig = context.getSharedPreferences(SpfConfig.SWAP_SPF, Context.MODE_PRIVATE)
        if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP, false) || swapConfig.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false)) {

            if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false)) {
                sb.append("if [ `cat /sys/block/zram0/disksize` != '" + swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0) + "000000' ] ; then ")
                sb.append("swapoff /dev/block/zram0 >/dev/null 2>&1;")
                sb.append("echo 1 > /sys/block/zram0/reset;")
                sb.append("echo " + swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0) + "000000 > /sys/block/zram0/disksize;")
                sb.append("mkswap /dev/block/zram0 >/dev/null 2>&1;")
                sb.append("swapon /dev/block/zram0 >/dev/null 2>&1;")
                sb.append("fi;\n")
            }

            if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP_FIRST, false)) {
                sb.append("swapon /data/swapfile -p 32767\n")
                //sb.append("swapoff /dev/block/zram0\n")
            } else {
                sb.append("swapon /data/swapfile\n")
            }

            sb.append("echo 65 > /proc/sys/vm/swappiness\n")
            sb.append("echo " + swapConfig.getInt(SpfConfig.SWAP_SPF_SWAPPINESS, 65) + " > /proc/sys/vm/swappiness\n")
        }
        //sb.append("sh /data/data/me.piebridge.brevent/brevent.sh;");
        DoCmd(sb.toString())
    }
}
