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
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShell
import com.omarea.common.shell.KernelProrp
import com.omarea.shared.*
import com.omarea.shell.Props
import com.omarea.shell.cpucontrol.CpuFrequencyUtils
import com.omarea.shell.cpucontrol.ThermalControlUtils
import com.omarea.shell.units.BatteryUnit
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
    private lateinit var nm: NotificationManager

    override fun onHandleIntent(intent: Intent?) {
        nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
        updateNotification(getString(R.string.boot_script_running))
        autoBoot()
    }


    private fun autoBoot() {
        val keepShell = KeepShell()

        if (globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)) {
            keepShell.doCmdSync(CommonCmds.DisableSELinux)
        }

        val context = this.applicationContext
        val cpuState = CpuConfigStorage().loadBootConfig(context)
        if (cpuState != null) {
            updateNotification(getString(R.string.boot_cpuset))

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
                updateNotification(getString(R.string.boot_modify_mac))

                keepShell.doCmdSync("chmod 0755 /sys/class/net/wlan0/address\n" +
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

        //判断是否开启了充电加速和充电保护，如果开启了，自动启动后台服务
        val chargeConfig = getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
        if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false) || chargeConfig!!.getBoolean(SpfConfig.CHARGE_SPF_BP, false)) {
            updateNotification(getString(R.string.boot_charge_booster))

            try {
                val intent = Intent(context, ServiceBattery::class.java)
                startService(intent)
            } catch (ex: Exception) {
            }
            BatteryUnit().setChargeInputLimit(chargeConfig.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, 3300), this.applicationContext)
        }

        val globalPowercfg = globalConfig.getString(SpfConfig.GLOBAL_SPF_POWERCFG, "")
        if (!globalPowercfg.isNullOrEmpty()) {
            updateNotification(getString(R.string.boot_use_powercfg))

            val modeList = ModeList()
            val configInstaller = ConfigInstaller()
            if (configInstaller.configInstalled()) {
                modeList.executePowercfgMode(globalPowercfg, context!!.packageName)
            } else {
                if (configInstaller.dynamicSupport(context!!)) {
                    configInstaller.installPowerConfig(context);
                    modeList.executePowercfgMode(globalPowercfg)
                }
            }
        }

        if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false)) {
            updateNotification(getString(R.string.boot_resize_zram))

            val sizeVal = swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0)
            val algorithm = swapConfig.getString(SpfConfig.SWAP_SPF_ALGORITHM, "")
            resizeZram(sizeVal, algorithm!!, keepShell)
        }

        if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP, false)) {
            Thread.sleep(10000)
            updateNotification(getString(R.string.boot_swapon))
            val swapControlScript = FileWrite.writePrivateShellFile("addin/swap_control.sh", "addin/swap_control.sh", context)
            if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP_USE_LOOP, false) && swapControlScript != null) {
                if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP_FIRST, false)) {
                    keepShell.doCmdSync("sh $swapControlScript enable_swap 32760\n")
                } else {
                    keepShell.doCmdSync("sh $swapControlScript enable_swap\n")
                }
            } else {
                if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP_FIRST, false)) {
                    keepShell.doCmdSync("swapon /data/swapfile -p 32760\n")
                } else {
                    keepShell.doCmdSync("swapon /data/swapfile\n")
                }
            }
        }

        if (swapConfig.contains(SpfConfig.SWAP_SPF_SWAPPINESS)) {
            keepShell.doCmdSync("echo 65 > /proc/sys/vm/swappiness\n")
            keepShell.doCmdSync("echo " + swapConfig.getInt(SpfConfig.SWAP_SPF_SWAPPINESS, 65) + " > /proc/sys/vm/swappiness\n")
        }

        if (globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_STARTED_FSTRIM, false)) {
            updateNotification(getString(R.string.boot_trim))
            val trimCmd = StringBuilder()
            trimCmd.append("setprop vtools.boot 1\n")
            trimCmd.append("fstrim /data\n")
            trimCmd.append("fstrim /system\n")
            trimCmd.append("fstrim /cache\n")
            trimCmd.append("fstrim /vendor\n")
            keepShell.doCmdSync(trimCmd.toString())
        }


        if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_AUTO_LMK, false)) {
            updateNotification(getString(R.string.boot_lmk))

            val activityManager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            LMKUnit().autoSetLMK(info.totalMem, keepShell)
        }

        updateNotification(getString(R.string.boot_freeze))
        for (item in AppConfigStore(context).freezeAppList) {
            keepShell.doCmdSync("pm disable " + item)
        }

        keepShell.tryExit()
        Thread.sleep(2000)
        stopSelf()
    }

    /**
     * 获取当前的ZRAM压缩算法
     */
    var compAlgorithm: String
        get() {
            val compAlgorithmItems = KernelProrp.getProp("/sys/block/zram0/comp_algorithm").split(" ")
            val result = compAlgorithmItems.find {
                it.startsWith("[") && it.endsWith("]")
            }
            if (result != null) {
                return result.replace("[", "").replace("]", "").trim()
            }
            return ""
        }
        set(value) {
            KernelProrp.setProp("/sys/block/zram0/comp_algorithm", value)
        }


    fun resizeZram(sizeVal: Int, algorithm: String = "", keepShell: KeepShell) {
        val currentSize = keepShell.doCmdSync("cat /sys/block/zram0/disksize")
        if (currentSize != "" + (sizeVal * 1024 * 1024L) || (algorithm.isNotEmpty() && algorithm != compAlgorithm)) {
            val sb = StringBuilder()
            sb.append("echo 3 > /sys/block/zram0/max_comp_streams\n")
            sb.append("sync\n")
            sb.append("echo 3 > /proc/sys/vm/drop_caches\n")
            sb.append("swapoff /dev/block/zram0 >/dev/null 2>&1\n")
            sb.append("echo 1 > /sys/block/zram0/reset\n")

            if (algorithm.isNotEmpty()) {
                sb.append("echo \"$algorithm\" > /sys/block/zram0/comp_algorithm\n")
            }

            if (sizeVal > 2047) {
                sb.append("echo " + sizeVal + "M > /sys/block/zram0/disksize\n")
            } else {
                sb.append("echo " + (sizeVal * 1024 * 1024L) + " > /sys/block/zram0/disksize\n")
            }

            sb.append("echo 3 > /sys/block/zram0/max_comp_streams\n")
            sb.append("mkswap /dev/block/zram0 >/dev/null 2>&1\n")
            sb.append("swapon /dev/block/zram0 >/dev/null 2>&1\n")
            keepShell.doCmdSync(sb.toString())
        }
    }


    private fun updateNotification(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel("vtool-boot", getString(R.string.notice_channel_boot), NotificationManager.IMPORTANCE_LOW))
            nm.notify(900, NotificationCompat.Builder(this, "vtool-boot").setSmallIcon(R.drawable.ic_menu_digital).setContentTitle(getString(R.string.notice_channel_boot)).setContentText(text).build())
        } else {
            nm.notify(900, NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_menu_digital).setContentTitle(getString(R.string.notice_channel_boot)).setContentText(text).build())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bootCancel) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.cancel(900)
            } else {
                nm.cancel(900)
            }
        } else {
            updateNotification(getString(R.string.boot_success))
        }
        // System.exit(0)
    }
}
