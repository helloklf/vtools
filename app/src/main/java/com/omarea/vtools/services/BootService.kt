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
import com.omarea.charger_booster.ServiceBattery
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShell
import com.omarea.common.shell.KernelProrp
import com.omarea.scene_mode.ModeConfigInstaller
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.scene_mode.SceneMode
import com.omarea.shell_utils.*
import com.omarea.store.AppConfigStore
import com.omarea.store.CpuConfigStorage
import com.omarea.store.SpfConfig
import com.omarea.utils.CommonCmds
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
        val r = PropsUtils.getProp("vtools.boot")
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
                    CpuFrequencyUtil.setCoreOnlineState(i, cpuState.coreOnline[i])
                }
            }

            // CPU
            for (cluster in 0 until cpuState.cpuClusterStatuses.size) {
                val config = cpuState.cpuClusterStatuses[cluster]
                if (config.governor.isNotEmpty()) {
                    CpuFrequencyUtil.setGovernor(config.governor, 0, context);
                }
                if (config.min_freq.isNotEmpty()) {
                    CpuFrequencyUtil.setMinFrequency(config.min_freq, 0, context);
                }
                if (config.max_freq.isNotEmpty()) {
                    CpuFrequencyUtil.setMaxFrequency(config.max_freq, 0, context);
                }
            }

            // Boost
            if (cpuState.boost.isNotEmpty()) {
                CpuFrequencyUtil.setSechedBoostState(cpuState.boost == "1", context);
            }
            if (cpuState.boostFreq.isNotEmpty()) {
                CpuFrequencyUtil.setInputBoosterFreq(cpuState.boostFreq);
            }
            if (cpuState.boostTime.isNotEmpty()) {
                CpuFrequencyUtil.setInputBoosterTime(cpuState.boostTime);
            }

            // GPU
            if (cpuState.adrenoGovernor.isNotEmpty()) {
                CpuFrequencyUtil.setAdrenoGPUGovernor(cpuState.adrenoGovernor);
            }
            if (cpuState.adrenoMinFreq.isNotEmpty()) {
                CpuFrequencyUtil.setAdrenoGPUMinFreq(cpuState.adrenoMinFreq);
            }
            if (cpuState.adrenoMaxFreq.isNotEmpty()) {
                CpuFrequencyUtil.setAdrenoGPUMaxFreq(cpuState.adrenoMaxFreq);
            }
            if (cpuState.adrenoMinPL.isNotEmpty()) {
                CpuFrequencyUtil.setAdrenoGPUMinPowerLevel(cpuState.adrenoMinPL);
            }
            if (cpuState.adrenoMaxPL.isNotEmpty()) {
                CpuFrequencyUtil.setAdrenoGPUMaxPowerLevel(cpuState.adrenoMaxPL);
            }
            if (cpuState.adrenoDefaultPL.isNotEmpty()) {
                CpuFrequencyUtil.setAdrenoGPUDefaultPowerLevel(cpuState.adrenoDefaultPL);
            }

            // exynos
            if (CpuFrequencyUtil.exynosHMP()) {
                CpuFrequencyUtil.setExynosHotplug(cpuState.exynosHotplug);
                CpuFrequencyUtil.setExynosHmpDown(cpuState.exynosHmpDown);
                CpuFrequencyUtil.setExynosHmpUP(cpuState.exynosHmpUP);
                CpuFrequencyUtil.setExynosBooster(cpuState.exynosHmpBooster);
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
            BatteryUtils().setChargeInputLimit(chargeConfig.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, 3300), this.applicationContext)
        }

        val globalPowercfg = globalConfig.getString(SpfConfig.GLOBAL_SPF_POWERCFG, "")
        if (!globalPowercfg.isNullOrEmpty()) {
            updateNotification(getString(R.string.boot_use_powercfg))

            val modeList = ModeSwitcher()
            val configInstaller = ModeConfigInstaller()
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
            val sizeVal = swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0)
            val algorithm = swapConfig.getString(SpfConfig.SWAP_SPF_ALGORITHM, "")
            if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP, false) && swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP_FIRST, false)) {
                enableSwap(keepShell, context)
                updateNotification(getString(R.string.boot_resize_zram))
                resizeZram(sizeVal, algorithm!!, keepShell, true)
            } else {
                updateNotification(getString(R.string.boot_resize_zram))
                resizeZram(sizeVal, algorithm!!, keepShell)
                if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP, false)) {
                    enableSwap(keepShell, context)
                }
            }
        } else if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP, false)) {
            enableSwap(keepShell, context)
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
            LMKUtils().autoSetLMK(info.totalMem, keepShell)
        }

        updateNotification(getString(R.string.boot_freeze))
        val launchedFreezeApp = SceneMode.getLaunchedFreezeApp()
        for (item in AppConfigStore(context).freezeAppList) {
            if (!launchedFreezeApp.contains(item)) {
                keepShell.doCmdSync("pm disable " + item)
            }
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

    fun enableSwap(keepShell: KeepShell, context: Context) {
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

    /**
     * swapFirst：是否已开启可优先使用的swap，如果未开启，则在调整zram前，先将swappiness调为0，避免在回收时还在写入zram，导致回收时间变长！
     */
    fun resizeZram(sizeVal: Int, algorithm: String = "", keepShell: KeepShell, swapFirst: Boolean = false) {
        val currentSize = keepShell.doCmdSync("cat /sys/block/zram0/disksize")
        if (currentSize != "" + (sizeVal * 1024 * 1024L) || (algorithm.isNotEmpty() && algorithm != compAlgorithm)) {
            val sb = StringBuilder()
            sb.append("swappiness_bak=`cat /proc/sys/vm/swappiness`\n")
            if (!swapFirst) {
                sb.append("echo 0 > /proc/sys/vm/swappiness\n")
            }
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
            sb.append("echo \$swappiness_bak > /proc/sys/vm/swappiness")
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
