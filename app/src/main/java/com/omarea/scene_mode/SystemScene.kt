package com.omarea.scene_mode

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.omarea.common.shell.KeepShellAsync
import com.omarea.common.shell.KeepShellPublic
import com.omarea.library.shell.LocationHelper
import com.omarea.store.SpfConfig


/**
 * Created by SYSTEM on 2018/07/19.
 */

class SystemScene(private var context: Context) {
    private var spfAutoConfig: SharedPreferences = context.getSharedPreferences(SpfConfig.BOOSTER_SPF_CFG_SPF, Context.MODE_PRIVATE)
    private var keepShell = KeepShellAsync(context)

    // 是否开启飞行模式
    private fun isAirModeOn(): Boolean {
        return Settings.System.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
    }

    private fun isWifiApOpen(context: Context): Boolean {
        try {
            val manager = context.getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
            //通过放射获取 getWifiApState()方法
            val method = manager.javaClass.getDeclaredMethod("getWifiApState")
            //调用getWifiApState() ，获取返回值
            val state = method.invoke(manager) as Int
            //通过放射获取 WIFI_AP的开启状态属性
            val field = manager.javaClass.getDeclaredField("WIFI_AP_STATE_ENABLED")
            //获取属性值
            val value = field.get(manager) as Int
            //判断是否开启
            return state == value
        } catch (e: Exception) {
        }

        return false
    }

    private fun onScreenOffDisableNetwork() {
        if (spfAutoConfig.getBoolean(SpfConfig.WIFI + SpfConfig.OFF, false)) {
            KeepShellPublic.doCmdSync("svc wifi disable")
        }
        if (spfAutoConfig.getBoolean(SpfConfig.DATA + SpfConfig.OFF, false)) {
            KeepShellPublic.doCmdSync("svc data disable")
        }
    }

    private fun onScreenOnEnableNetwork() {
        if (isAirModeOn()) {
            return
        }
        if (spfAutoConfig.getBoolean(SpfConfig.WIFI + SpfConfig.ON, false)) {
            KeepShellPublic.doCmdSync("svc wifi enable")
        }
        if (spfAutoConfig.getBoolean(SpfConfig.DATA + SpfConfig.ON, false)) {
            KeepShellPublic.doCmdSync("svc data enable")
        }
    }

    fun onScreenOn() {
        if (spfAutoConfig.getBoolean(SpfConfig.FORCEDOZE + SpfConfig.OFF, false)) {
            KeepShellPublic.doCmdSync("dumpsys deviceidle unforce\ndumpsys deviceidle enable all\n")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || !isWifiApOpen(context)) {
            onScreenOnEnableNetwork()
        }

        if (spfAutoConfig.getBoolean(SpfConfig.NFC + SpfConfig.ON, false))
            KeepShellPublic.doCmdSync("svc nfc enable")

        if (spfAutoConfig.getBoolean(SpfConfig.GPS + SpfConfig.ON, false)) {
            LocationHelper().enableGPS()
        }

        val lowPowerMode = spfAutoConfig.getBoolean(SpfConfig.FORCEDOZE + SpfConfig.OFF, false)
        if (lowPowerMode) {
            switchLowPowerModeShell(false)
        }
    }

    fun onScreenOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || !isWifiApOpen(context)) {
            onScreenOffDisableNetwork()
        }

        if (spfAutoConfig.getBoolean(SpfConfig.NFC + SpfConfig.OFF, false))
            KeepShellPublic.doCmdSync("svc nfc disable")


        if (spfAutoConfig.getBoolean(SpfConfig.GPS + SpfConfig.OFF, false)) {
            LocationHelper().disableGPS()
        }

        val lowPowerMode = spfAutoConfig.getBoolean(SpfConfig.FORCEDOZE + SpfConfig.ON, false)
        if (lowPowerMode || spfAutoConfig.getBoolean(SpfConfig.POWERSAVE + SpfConfig.ON, false)) {
            backToHome()
            // 强制Doze: dumpsys deviceidle force-idle
            KeepShellPublic.doCmdSync("dumpsys deviceidle enable\ndumpsys deviceidle enable all\n")
            KeepShellPublic.doCmdSync("dumpsys deviceidle force-idle\n")
            keepShell.doCmd("dumpsys deviceidle step\ndumpsys deviceidle step\ndumpsys deviceidle step\ndumpsys deviceidle step\necho 3 > /proc/sys/vm/drop_caches\n")
        }
        if (lowPowerMode) {
            switchLowPowerModeShell(true)
        }
    }

    /**
     * 返回桌面
     */
    private fun backToHome() {
        try {
            // context.performGlobalAction(GLOBAL_ACTION_HOME)
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            val res = context.getPackageManager().resolveActivity(intent, 0)!!
            if (res.activityInfo == null) {
            } else if (res.activityInfo.packageName == "android") {
            } else {
                try {
                    val home = res.activityInfo.packageName
                    context.startActivity(Intent().setComponent(ComponentName(home, res.activityInfo.name)))
                } catch (ex: java.lang.Exception) {
                }
            }
        } catch (ex: Exception) {
        }
    }

    private var lowPowerModeShell: String? = ""
    private fun switchLowPowerModeShell(powersave: Boolean) {
        if (lowPowerModeShell == null) {
            return
        }
        if (lowPowerModeShell!!.isEmpty()) {
            lowPowerModeShell = com.omarea.common.shared.FileWrite.writePrivateShellFile("addin/power_save_set.sh", "addin/power_save_set.sh", context)
        }
        if (lowPowerModeShell.isNullOrEmpty()) {
            return
        }
        keepShell.doCmd("sh \"$lowPowerModeShell\" " + if (powersave) "1" else "0")
    }
}
