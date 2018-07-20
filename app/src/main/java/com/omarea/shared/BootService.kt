package com.omarea.shared

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.omarea.shell.Props
import com.omarea.shell.SysUtils
import com.omarea.vtools.R

/**
 * Created by Hello on 2017/12/27.
 */

class BootService : IntentService("vtools-boot") {
    private lateinit var swapConfig: SharedPreferences
    private lateinit var globalConfig: SharedPreferences
    private var isFirstBoot = true

    override fun onHandleIntent(intent: Intent?) {
        swapConfig = this.getSharedPreferences(SpfConfig.SWAP_SPF, Context.MODE_PRIVATE)
        globalConfig = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        if (globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_START_DELAY, false)) {
            Thread.sleep(25 * 1000)
        } else {
            Thread.sleep(2000)
        }
        val r = Props.getProp("vtools.boot")
        if (!r.isEmpty()) {
            isFirstBoot = false
            return
        }
        Thread(Runnable {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(NotificationChannel("vtool-boot", getString(R.string.notice_channel_boot), NotificationManager.IMPORTANCE_LOW))
                nm.notify(1, NotificationCompat.Builder(this, "vtool-boot").setSmallIcon(R.drawable.ic_menu_digital).setSubText(getString(R.string.app_name)).setContentText(getString(R.string.boot_script_running)).build())
            } else {
                nm.notify(1, NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_menu_digital).setSubText(getString(R.string.app_name)).setContentText(getString(R.string.boot_script_running)).build())
            }
        }).start()
        autoBoot()
    }



    private fun autoBoot() {
        val sb = StringBuilder()

        if (globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, true)) {
            sb.append(Consts.DisableSELinux)
            sb.append("\n\n")
        }

        if (globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_MAC_AUTOCHANGE, false)) {
            val mac = globalConfig.getString(SpfConfig.GLOBAL_SPF_MAC, "")
            if (mac != "") {
                sb.append("chmod 0644 /sys/class/net/wlan0/address\n" +
                        "svc wifi disable\n" +
                        "ifconfig wlan0 down\n" +
                        "echo '$mac' > /sys/class/net/wlan0/address\n" +
                        "ifconfig wlan0 hw ether '$mac'\n" +
                        "chmod 0644 /sys/devices/soc/a000000.qcom,wcnss-wlan/wcnss_mac_addr\n" +
                        "echo '$mac' > /sys/devices/soc/a000000.qcom,wcnss-wlan/wcnss_mac_addr\n" +
                        "ifconfig wlan0 up\n" +
                        "svc wifi enable\n\n")
            }
        }

        if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP, false) || swapConfig.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false)) {
            if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false)) {
                val sizeVal = swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0)
                sb.append("if [ `cat /sys/block/zram0/disksize` != '" + sizeVal + "000000' ] ; then ")
                sb.append("swapoff /dev/block/zram0 2>/dev/null;")
                sb.append("echo 1 > /sys/block/zram0/reset;")
                sb.append("echo " + sizeVal + "000000 > /sys/block/zram0/disksize;")
                sb.append("mkswap /dev/block/zram0 2> /dev/null;")
                sb.append("fi;")
                sb.append("\n")
                sb.append("swapon /dev/block/zram0 2> /dev/null;")
            }
            sb.append("mkswap /dev/block/zram0 2> /dev/null;")
            sb.append("swapon /dev/block/zram0 2> /dev/null;")

            //sb.append("swapon /data/swapfile -p 32767;")
            if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP_FIRST, false))
                sb.append("swapon /data/swapfile -p 32760;")
            else
                sb.append("swapon /data/swapfile;")
            sb.append("echo 3 > /sys/block/zram0/max_comp_streams;")

            sb.append("echo 65 > /proc/sys/vm/swappiness;")
            sb.append("echo " + swapConfig.getInt(SpfConfig.SWAP_SPF_SWAPPINESS, 65) + " > /proc/sys/vm/swappiness;")
        }

        sb.append("\n\n")
        sb.append("setprop vtools.boot 1")
        sb.append("\n\n")
        SysUtils.executeCommandWithOutput(true, sb.toString())

        if (globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_DOZELIST_AUTOSET, false)) {
            val sb2 = StringBuilder("")
            sb2.append("\n\n")
            val spf = getSharedPreferences(SpfConfig.WHITE_LIST_SPF, Context.MODE_PRIVATE)
            for (item in spf.all) {
                if (item.value == true) {
                    sb2.append("dumpsys deviceidle whitelist +${item.key} > null\n")
                } else {
                    sb2.append("dumpsys deviceidle whitelist -${item.key} > null\n")
                }
                sb2.append("\n")
            }
            sb2.append("\n\n")
            sb2.append("\n\n")
            sb2.append("setprop vtools.boot 2")
            sb2.append("\n\n")

            Thread.sleep(120 * 1000)
            SysUtils.executeCommandWithOutput(true, sb2.toString())
            stopSelf()
        } else {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel("vtool-boot", getString(R.string.notice_channel_boot), NotificationManager.IMPORTANCE_LOW))
            nm.notify(1, NotificationCompat.Builder(this, "vtool-boot").setSmallIcon(R.drawable.ic_menu_digital).setSubText(getString(R.string.app_name)).setContentText(getString(R.string.boot_success)).build())
        } else {
            nm.notify(1, NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_menu_digital).setSubText(getString(R.string.app_name)).setContentText(getString(R.string.boot_success)).build())
        }
        System.exit(0)
    }
}
