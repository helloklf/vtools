package com.omarea.vtools.services

import android.app.ActivityManager
import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.omarea.common.shell.KeepShell
import com.omarea.common.shell.KernelProrp
import com.omarea.shared.*
import com.omarea.shell.Props
import com.omarea.shell.cpucontrol.CpuFrequencyUtils
import com.omarea.shell.cpucontrol.ThermalControlUtils
import com.omarea.shell.units.LMKUnit
import com.omarea.vtools.R

/**
 * Created by Hello on 2017/12/27.
 */

class BootService : IntentService("vtools-boot") {
    private lateinit var swapConfig: SharedPreferences
    private lateinit var globalConfig: SharedPreferences
    private var isFirstBoot = true
    private var bootCancel = false


    val FastChangerBase =
    //"chmod 0777 /sys/class/power_supply/usb/pd_active;" +
            "chmod 0777 /sys/class/power_supply/usb/pd_allowed;" +
                    //"echo 1 > /sys/class/power_supply/usb/pd_active;" +
                    "echo 1 > /sys/class/power_supply/usb/pd_allowed;" +
                    "chmod 0666 /sys/class/power_supply/main/constant_charge_current_max;" +
                    "chmod 0666 /sys/class/qcom-battery/restricted_current;" +
                    "chmod 0666 /sys/class/qcom-battery/restricted_charging;" +
                    "echo 0 > /sys/class/qcom-battery/restricted_charging;" +
                    "echo 0 > /sys/class/power_supply/battery/restricted_charging;" +
                    "echo 0 > /sys/class/power_supply/battery/safety_timer_enabled;" +
                    "chmod 0666 /sys/class/power_supply/bms/temp_warm;" +
                    "echo 500 > /sys/class/power_supply/bms/temp_warm;" +
                    "chmod 0666 /sys/class/power_supply/battery/constant_charge_current_max;\n"

    private fun computeLeves(qcLimit: Int): StringBuilder {
        val arr = StringBuilder()
        if (qcLimit > 300) {
            var level = 300
            while (level < qcLimit) {
                arr.append("echo ${level}000 > /sys/class/power_supply/battery/constant_charge_current_max\n")
                arr.append("echo ${level}000 > /sys/class/power_supply/main/constant_charge_current_max\n")
                arr.append("echo ${level}000 > /sys/class/qcom-battery/restricted_current\n")
                level += 300
            }
        }
        arr.append("echo ${qcLimit}000 > /sys/class/power_supply/battery/constant_charge_current_max\n")
        arr.append("echo ${qcLimit}000 > /sys/class/power_supply/main/constant_charge_current_max\n")
        arr.append("echo ${qcLimit}000 > /sys/class/qcom-battery/restricted_current\n\n")
        return arr;
    }

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
            bootCancel = true
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

        if (globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)) {
            sb.append(CommonCmds.DisableSELinux)
            sb.append("\n\n")
        }

        val context = this.applicationContext
        val cpuState = CpuConfigStorage().loadBootConfig(context)
        if (cpuState != null) {
            // thermal
            if (cpuState.coreControl.isNotEmpty()) {
                ThermalControlUtils.setCoreControlState(cpuState.coreControl == "1", context);
            }
            if (cpuState.msmThermal.isNotEmpty()) {
                ThermalControlUtils.setTheramlState(cpuState.msmThermal == "Y", context);
            }
            if (cpuState.vdd.isNotEmpty()) {
                ThermalControlUtils.setVDDRestrictionState(cpuState.vdd == "1", context);
            }

            // core online
            if (cpuState.coreOnline != null && cpuState.coreOnline.size > 0) {
                for (i in 0 until cpuState.coreOnline.size) {
                    CpuFrequencyUtils.setCoreOnlineState(i, cpuState.coreOnline[i])
                }
            }

            // CPU
            if (cpuState.cluster_little_governor.isNotEmpty()) {
                CpuFrequencyUtils.setGovernor(cpuState.cluster_little_governor, 0, context);
            }
            if (cpuState.cluster_little_min_freq.isNotEmpty()) {
                CpuFrequencyUtils.setMinFrequency(cpuState.cluster_little_min_freq, 0, context);
            }
            if (cpuState.cluster_little_max_freq.isNotEmpty()) {
                CpuFrequencyUtils.setMaxFrequency(cpuState.cluster_little_max_freq, 0, context);
            }
            if (cpuState.cluster_big_governor.isNotEmpty()) {
                CpuFrequencyUtils.setGovernor(cpuState.cluster_big_governor, 1, context);
            }
            if (cpuState.cluster_big_min_freq.isNotEmpty()) {
                CpuFrequencyUtils.setMinFrequency(cpuState.cluster_big_min_freq, 1, context);
            }
            if (cpuState.cluster_big_max_freq.isNotEmpty()) {
                CpuFrequencyUtils.setMaxFrequency(cpuState.cluster_big_max_freq, 1, context);
            }

            // Boost
            if (cpuState.boost.isNotEmpty()) {
                CpuFrequencyUtils.setSechedBoostState(cpuState.boost == "1", context);
            }
            if (cpuState.boostFreq.isNotEmpty()) {
                CpuFrequencyUtils.setInputBoosterFreq(cpuState.boostFreq);
            }
            if (cpuState.boostTime.isNotEmpty()) {
                CpuFrequencyUtils.setInputBoosterTime(cpuState.boostTime);
            }

            // GPU
            if (cpuState.adrenoGovernor.isNotEmpty()) {
                CpuFrequencyUtils.setAdrenoGPUGovernor(cpuState.adrenoGovernor);
            }
            if (cpuState.adrenoMinFreq.isNotEmpty()) {
                CpuFrequencyUtils.setAdrenoGPUMinFreq(cpuState.adrenoMinFreq);
            }
            if (cpuState.adrenoMaxFreq.isNotEmpty()) {
                CpuFrequencyUtils.setAdrenoGPUMaxFreq(cpuState.adrenoMaxFreq);
            }
            if (cpuState.adrenoMinPL.isNotEmpty()) {
                CpuFrequencyUtils.setAdrenoGPUMinPowerLevel(cpuState.adrenoMinPL);
            }
            if (cpuState.adrenoMaxPL.isNotEmpty()) {
                CpuFrequencyUtils.setAdrenoGPUMaxPowerLevel(cpuState.adrenoMaxPL);
            }
            if (cpuState.adrenoDefaultPL.isNotEmpty()) {
                CpuFrequencyUtils.setAdrenoGPUDefaultPowerLevel(cpuState.adrenoDefaultPL);
            }

            // exynos
            if (CpuFrequencyUtils.exynosHMP()) {
                CpuFrequencyUtils.setExynosHotplug(cpuState.exynosHotplug);
                CpuFrequencyUtils.setExynosHmpDown(cpuState.exynosHmpDown);
                CpuFrequencyUtils.setExynosHmpUP(cpuState.exynosHmpUP);
                CpuFrequencyUtils.setExynosBooster(cpuState.exynosHmpBooster);
            }

            if (!cpuState.cpusetBackground.isNullOrEmpty()) {
                KernelProrp.setProp("/dev/cpuset/background/cpus", cpuState.cpusetBackground)
            }
            if (!cpuState.cpusetSysBackground.isNullOrEmpty()) {
                KernelProrp.setProp("/dev/cpuset/system-background/cpus", cpuState.cpusetSysBackground)
            }
            if (!cpuState.cpusetForeground.isNullOrEmpty()) {
                KernelProrp.setProp("/dev/cpuset/foreground/cpus", cpuState.cpusetForeground)
            }
            if (!cpuState.cpusetForegroundBoost.isNullOrEmpty()) {
                KernelProrp.setProp("/dev/cpuset/foreground/boost/cpus", cpuState.cpusetForegroundBoost)
            }
            if (!cpuState.cpusetTopApp.isNullOrEmpty()) {
                KernelProrp.setProp("/dev/cpuset/top-app/cpus", cpuState.cpusetTopApp)
            }
        }

        if (globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_MAC_AUTOCHANGE, false)) {
            val mac = globalConfig.getString(SpfConfig.GLOBAL_SPF_MAC, "")
            if (mac != "") {
                sb.append("chmod 0755 /sys/class/net/wlan0/address\n" +
                        "svc wifi disable\n" +
                        "ifconfig wlan0 down\n" +
                        "echo '$mac' > /sys/class/net/wlan0/address\n" +
                        "ifconfig wlan0 hw ether '$mac'\n" +
                        "chmod 0755 /sys/devices/soc/a000000.qcom,wcnss-wlan/wcnss_mac_addr\n" +
                        "echo '$mac' > /sys/devices/soc/a000000.qcom,wcnss-wlan/wcnss_mac_addr\n" +
                        "ifconfig wlan0 up\n" +
                        "svc wifi enable\n\n")
            }
        }


        val chargeConfig = getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
        if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false) || chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_BP, false)) {
            sb.append(FastChangerBase)
            val qcLimit = chargeConfig.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, 5000)
            sb.append(computeLeves(qcLimit).toString())
        }

        val globalPowercfg = globalConfig.getString(SpfConfig.GLOBAL_SPF_POWERCFG, "")
        if (!globalPowercfg.isNullOrEmpty()) {
            val modeList = ModeList()
            val configInstaller = ConfigInstaller()
            if (configInstaller.configInstalled()) {
                modeList.executePowercfgMode(globalPowercfg, context!!.packageName)
            } else {
                val stringBuilder = StringBuilder()
                modeList.executePowercfgMode(globalPowercfg)
                ConfigInstaller().installPowerConfig(context!!, stringBuilder.toString());
            }
        }

        if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP, false) || swapConfig.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false)) {
            if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false)) {
                val sizeVal = swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0)
                sb.append("if [ `cat /sys/block/zram0/disksize` != '" + (sizeVal * 1024 * 1024L) + "' ] ; then ")
                sb.append("swapoff /dev/block/zram0 2>/dev/null\n")
                sb.append("echo 1 > /sys/block/zram0/reset\n")
                if (sizeVal > 2047) {
                    sb.append("echo " + sizeVal + "M > /sys/block/zram0/disksize\n")
                } else {
                    sb.append("echo " + (sizeVal * 1024 * 1024L) + " > /sys/block/zram0/disksize\n")
                }
                sb.append("mkswap /dev/block/zram0 2> /dev/null\n")
                sb.append("fi\n")
                sb.append("\n")
                sb.append("swapon /dev/block/zram0 2> /dev/null\n")
            }
            if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP, false)) {
                //sb.append("swapon /data/swapfile -p 32767;")
                if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP_FIRST, false))
                    sb.append("swapon /data/swapfile -p 32760\n")
                else
                    sb.append("swapon /data/swapfile\n")
            }
        }

        sb.append("echo 3 > /sys/block/zram0/max_comp_streams\n")

        sb.append("echo 65 > /proc/sys/vm/swappiness\n")
        sb.append("echo " + swapConfig.getInt(SpfConfig.SWAP_SPF_SWAPPINESS, 65) + " > /proc/sys/vm/swappiness\n")

        sb.append("\n\n")
        sb.append("setprop vtools.boot 1")
        sb.append("\n\n")
        sb.append("fstrim /data\n")
        sb.append("fstrim /system\n")
        sb.append("fstrim /cache\n")
        sb.append("fstrim /vendor\n")
        val keepShell = KeepShell()
        keepShell.doCmdSync(sb.toString())

        /*
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
            keepShell.doCmdSync(sb2.toString())
        }
        */

        if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_AUTO_LMK, false)) {
            val activityManager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            LMKUnit().autoSetLMK(info.totalMem, keepShell)
        }

        for (item in AppConfigStore(context).freezeAppList) {
            keepShell.doCmdSync("pm disable " + item)
        }

        keepShell.tryExit()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!bootCancel) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(NotificationChannel("vtool-boot", getString(R.string.notice_channel_boot), NotificationManager.IMPORTANCE_LOW))
                nm.notify(1, NotificationCompat.Builder(this, "vtool-boot").setSmallIcon(R.drawable.ic_menu_digital).setSubText(getString(R.string.app_name)).setContentText(getString(R.string.boot_success)).build())
            } else {
                nm.notify(1, NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_menu_digital).setSubText(getString(R.string.app_name)).setContentText(getString(R.string.boot_success)).build())
            }
        }
        // System.exit(0)
    }
}
