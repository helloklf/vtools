package com.omarea.vtools.services

import android.app.ActivityManager
import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.omarea.common.shared.RawText
import com.omarea.common.shell.KeepShell
import com.omarea.common.shell.KernelProrp
import com.omarea.data.EventBus
import com.omarea.data.EventType
import com.omarea.library.shell.BatteryUtils
import com.omarea.library.shell.LMKUtils
import com.omarea.library.shell.PropsUtils
import com.omarea.library.shell.SwapUtils
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.scene_mode.SceneMode
import com.omarea.store.CpuConfigStorage
import com.omarea.store.SceneConfigStore
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

    private lateinit var mPowerManager: PowerManager
    private lateinit var mWakeLock: PowerManager.WakeLock
    override fun onHandleIntent(intent: Intent?) {
        mPowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager;
        /*
            标记值                   CPU  屏幕  键盘
            PARTIAL_WAKE_LOCK       开启  关闭  关闭
            SCREEN_DIM_WAKE_LOCK    开启  变暗  关闭
            SCREEN_BRIGHT_WAKE_LOCK 开启  变亮  关闭
            FULL_WAKE_LOCK          开启  变亮  变亮
        */
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "scene:BootService");
        mWakeLock.acquire(10 * 60 * 1000) // 默认限制10分钟

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
            this.hideNotification()
            return
        }
        updateNotification(getString(R.string.boot_script_running))
        EventBus.publish(EventType.BOOT_COMPLETED)
        autoBoot()
    }

    private fun autoBoot() {
        val keepShell = KeepShell()

        if (globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)) {
            keepShell.doCmdSync(CommonCmds.DisableSELinux)
        }

        val context = this.applicationContext
        val cpuConfigStorage = CpuConfigStorage(context!!)
        val cpuState = cpuConfigStorage.load()
        if (cpuState != null) {
            updateNotification(context.getString(R.string.boot_cpuset))
            cpuConfigStorage.applyCpuConfig(context, cpuConfigStorage.default())
        }

        val macChangeMode = globalConfig.getInt(SpfConfig.GLOBAL_SPF_MAC_AUTOCHANGE_MODE, 0)
        val mac = globalConfig.getString(SpfConfig.GLOBAL_SPF_MAC, "")
        if (!mac.isNullOrEmpty()) {
            when (macChangeMode) {
                SpfConfig.GLOBAL_SPF_MAC_AUTOCHANGE_MODE_1 -> {
                    updateNotification(getString(R.string.boot_modify_mac))
                    keepShell.doCmdSync("mac=\"$mac\"\n" + RawText.getRawText(context, R.raw.change_mac_1))
                }
                SpfConfig.GLOBAL_SPF_MAC_AUTOCHANGE_MODE_2 -> {
                    updateNotification(getString(R.string.boot_modify_mac))
                    keepShell.doCmdSync("mac=\"$mac\"\n" + RawText.getRawText(context, R.raw.change_mac_2))
                }
            }
        }

        //判断是否开启了充电加速和充电保护，如果开启了，自动启动后台服务
        val chargeConfig = getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
        if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false) || chargeConfig!!.getBoolean(SpfConfig.CHARGE_SPF_BP, false)) {
            updateNotification(getString(R.string.boot_charge_booster))
            BatteryUtils().setChargeInputLimit(chargeConfig.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, SpfConfig.CHARGE_SPF_QC_LIMIT_DEFAULT), this.applicationContext)
        }

        val globalPowercfg = globalConfig.getString(SpfConfig.GLOBAL_SPF_POWERCFG, "")
        if (!globalPowercfg.isNullOrEmpty()) {
            updateNotification(getString(R.string.boot_use_powercfg))

            val modeSwitcher = ModeSwitcher()
            if (modeSwitcher.modeConfigCompleted()) {
                modeSwitcher.executePowercfgMode(globalPowercfg, context.packageName)
            }
        }

        // 如果没有单独安装Magisk模块来处理虚拟内存，则在Scene的自启动中控制
        if (!keepShell.doCmdSync("getprop vtools.swap.controller").equals("magisk")) {
            if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP, false)) {
                enableSwap(keepShell, context)
            }

            if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false)) {
                val sizeVal = swapConfig.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0)
                val algorithm = swapConfig.getString(SpfConfig.SWAP_SPF_ALGORITHM, "")

                updateNotification(getString(R.string.boot_resize_zram))
                resizeZram(sizeVal, algorithm!!, keepShell, true)
            }

            if (swapConfig.contains(SpfConfig.SWAP_SPF_SWAPPINESS)) {
                keepShell.doCmdSync("echo 65 > /proc/sys/vm/swappiness\n")
                keepShell.doCmdSync("echo " + swapConfig.getInt(SpfConfig.SWAP_SPF_SWAPPINESS, 65) + " > /proc/sys/vm/swappiness\n")
            }

            if (swapConfig.contains(SpfConfig.SWAP_SPF_EXTRA_FREE_KBYTES)) {
                keepShell.doCmdSync("echo ${swapConfig.getInt(SpfConfig.SWAP_SPF_EXTRA_FREE_KBYTES, 29615)} > /proc/sys/vm/extra_free_kbytes\n")
            }

            if (swapConfig.contains(SpfConfig.SWAP_SPF_WATERMARK_SCALE)) {
                keepShell.doCmdSync("echo ${swapConfig.getInt(SpfConfig.SWAP_SPF_WATERMARK_SCALE, 100)} > /proc/sys/vm/watermark_scale_factor\n")
            }

            if (swapConfig.getBoolean(SpfConfig.SWAP_SPF_AUTO_LMK, false)) {
                updateNotification(getString(R.string.boot_lmk))

                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val info = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(info)
                LMKUtils().autoSetLMK(info.totalMem, keepShell)
            }
        }

        updateNotification(getString(R.string.boot_freeze))
        val launchedFreezeApp = SceneMode.getCurrentInstance()?.getLaunchedFreezeApp()
        val suspendMode = globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_SUSPEND, Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        for (item in SceneConfigStore(context).freezeAppList) {
            if (launchedFreezeApp == null || !launchedFreezeApp.contains(item)) {
                if (suspendMode) {
                    SceneMode.suspendApp(item)
                } else {
                    SceneMode.freezeApp(item)
                }
            }
        }

        keepShell.tryExit()
        hideNotification()
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

    private fun enableSwap(keepShell: KeepShell, context: Context) {
        updateNotification(getString(R.string.boot_swapon))
        val swapPriority = swapConfig.getInt(SpfConfig.SWAP_SPF_SWAP_PRIORITY, -2)
        val useLoop = swapConfig.getBoolean(SpfConfig.SWAP_SPF_SWAP_USE_LOOP, false)
        SwapUtils(context).swapOn(swapPriority, useLoop, keepShell)
    }

    /**
     * swapFirst：是否已开启可优先使用的swap，如果未开启，则在调整zram前，先将swappiness调为0，避免在回收时还在写入zram，导致回收时间变长！
     */
    private fun resizeZram(sizeVal: Int, algorithm: String = "", keepShell: KeepShell, swapFirst: Boolean = false) {
        keepShell.doCmdSync(
    "if [[ ! -e /dev/block/zram0 ]] && [[ -e /sys/class/zram-control ]]; then\n" +
         "  cat /sys/class/zram-control/hot_add\n" +
         "fi"
        )
        val currentSize = keepShell.doCmdSync("cat /sys/block/zram0/disksize")
        if (currentSize != "" + (sizeVal * 1024 * 1024L) || (algorithm.isNotEmpty() && algorithm != compAlgorithm)) {
            val sb = StringBuilder()
            sb.append("swappiness_bak=`cat /proc/sys/vm/swappiness`\n")
            if (!swapFirst) {
                sb.append("echo 0 > /proc/sys/vm/swappiness\n")
            }

            sb.append("echo 4 > /sys/block/zram0/max_comp_streams\n")
            sb.append("sync\n")

            sb.append("if [[ -f /sys/block/zram0/backing_dev ]]; then\n")
            sb.append("  backing_dev=$(cat /sys/block/zram0/backing_dev)\n")
            sb.append("fi\n")

            sb.append("echo 3 > /proc/sys/vm/drop_caches\n")
            sb.append("swapoff /dev/block/zram0 >/dev/null 2>&1\n")
            sb.append("echo 1 > /sys/block/zram0/reset\n")

            sb.append("if [[ -f /sys/block/zram0/backing_dev ]]; then\n")
            sb.append("  echo \"\$backing_dev\" > /sys/block/zram0/backing_dev\n")
            sb.append("fi\n")

            if (algorithm.isNotEmpty()) {
                sb.append("echo \"$algorithm\" > /sys/block/zram0/comp_algorithm\n")
            }

            if (sizeVal > 2047) {
                sb.append("echo " + sizeVal + "M > /sys/block/zram0/disksize\n")
            } else {
                sb.append("echo " + (sizeVal * 1024 * 1024L) + " > /sys/block/zram0/disksize\n")
            }

            sb.append("echo 4 > /sys/block/zram0/max_comp_streams\n")
            sb.append("mkswap /dev/block/zram0 >/dev/null 2>&1\n")
            sb.append("swapon /dev/block/zram0 -p 0 >/dev/null 2>&1\n")
            sb.append("echo \$swappiness_bak > /proc/sys/vm/swappiness")
            keepShell.doCmdSync(sb.toString())
        }
    }

    private var channelCreated = false
    private fun updateNotification(text: String) {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!channelCreated) {
                nm.createNotificationChannel(NotificationChannel("vtool-boot", getString(R.string.notice_channel_boot), NotificationManager.IMPORTANCE_LOW))
                channelCreated = true
            }
            NotificationCompat.Builder(this, "vtool-boot")
        } else {
            NotificationCompat.Builder(this)
        }
        nm.notify(900, builder.setSmallIcon(R.drawable.ic_menu_digital).setContentTitle(getString(R.string.notice_channel_boot)).setContentText(text).build())
    }

    private fun hideNotification() {
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

    override fun onDestroy() {
        hideNotification()
        mWakeLock.release()

        super.onDestroy()
    }
}
