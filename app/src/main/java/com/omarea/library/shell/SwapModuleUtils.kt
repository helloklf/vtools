package com.omarea.library.shell

import android.content.SharedPreferences
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.RootFile
import com.omarea.store.SpfConfig

/*
# 配置示例

# 是否启用swap
swap=true
# swap大小(MB)，部分设备超过2047会开启失败
swap_size=1536
# swap使用顺序（0:与zram同时使用，-1:用完zram后再使用，5:优先于zram使用）
swap_priority=0
# 是否挂载为回环设备(如非必要，不建议开启)
swap_use_loop=false

# 是否启用zram
zram=true
# zram大小(MB)，部分设备超过2047会开启失败
zram_size=1536
# zram压缩算法(可设置的值取决于内核支持)
comp_algorithm=lzo

# 使用zram、swap的积极性
swappiness=100
# extra_free_kbytes(kbytes)
extra_free_kbytes=98304
*/

/**
 * Created by Hello on 2017/11/01.
 */

class SwapModuleUtils {
    private var installed: Boolean? = null

    // 是否已经安装用于处理自启动的magisk模块
    val magiskModuleInstalled: Boolean
        get () {
            if (installed == null) {
                installed = RootFile.fileExists("/data/swap_config.conf") && PropsUtils.getProp("vtools.swap.controller") == "magisk"
            }

            return installed!!
        }

    private val swapEnable = "swap"
    private val swapSize = "swap_size"
    private val swapPriority = "swap_priority"
    private val swapUseLoop = "swap_use_loop"

    private val zramEnable = "zram"
    private val zramSize = "zram_size"
    private val zramCompAlgorithm = "comp_algorithm"

    private val swappiness = "swappiness"
    private val extraFreeKbytes = "extra_free_kbytes"
    private val watermarkScaleFactor = "watermark_scale_factor"

    private fun getProp(prop: String): String {
        return KeepShellPublic.doCmdSync("cat /data/swap_config.conf | grep -v '^#' | grep \"^${prop}=\" | cut -f2 -d '='")
    }

    private fun getProp(config: List<String>, prop: String): String {
        val result = config.find { it.startsWith("$prop=") }
        if (result != null) {
            return result.subSequence(prop.length + 1, result.length).toString()
        }
        return ""
    }

    private fun setProp(prop: String, value: Any) {
        KeepShellPublic.doCmdSync("busybox sed -i 's/^$prop=.*/$prop=$value/' /data/swap_config.conf")
    }

    // 保存模块配置
    fun saveModuleConfig(spf: SharedPreferences) {
        setProp(swapEnable, spf.getBoolean(SpfConfig.SWAP_SPF_SWAP, false))
        setProp(swapSize, spf.getInt(SpfConfig.SWAP_SPF_SWAP_SWAPSIZE, 0))
        setProp(swapPriority, spf.getInt(SpfConfig.SWAP_SPF_SWAP_PRIORITY, 0))
        setProp(swapUseLoop, spf.getBoolean(SpfConfig.SWAP_SPF_SWAP_USE_LOOP, false))

        setProp(zramEnable, spf.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false))
        setProp(zramSize, spf.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0))
        setProp(zramCompAlgorithm, "" + spf.getString(SpfConfig.SWAP_SPF_ALGORITHM, "lzo"))

        setProp(swappiness, spf.getInt(SpfConfig.SWAP_SPF_SWAPPINESS, 65))
        setProp(extraFreeKbytes, spf.getInt(SpfConfig.SWAP_SPF_EXTRA_FREE_KBYTES, 29615))
        setProp(watermarkScaleFactor, spf.getInt(SpfConfig.SWAP_SPF_WATERMARK_SCALE, 100))
    }

    // 加载模块配置
    fun loadModuleConfig(spf: SharedPreferences) {
        // 如果模块没有安装，就不要再去读取配置了
        if (!magiskModuleInstalled) {
            return
        }

        val editor = spf.edit()
        val savedConfig = KeepShellPublic.doCmdSync("cat /data/swap_config.conf").split("\n")

        try {
            editor.putBoolean(SpfConfig.SWAP_SPF_SWAP, getProp(savedConfig, swapEnable) == "true")
            editor.putInt(SpfConfig.SWAP_SPF_SWAP_SWAPSIZE, getProp(savedConfig, swapSize).toInt())
            editor.putInt(SpfConfig.SWAP_SPF_SWAP_PRIORITY, getProp(savedConfig, swapPriority).toInt())
            editor.putBoolean(SpfConfig.SWAP_SPF_SWAP_USE_LOOP, getProp(savedConfig, swapUseLoop) == "true")
        } catch (ex: Exception) {
        }

        try {
            editor.putBoolean(SpfConfig.SWAP_SPF_ZRAM, getProp(savedConfig, zramEnable) == "true")
            editor.putInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, getProp(savedConfig, zramSize).toInt())
            editor.putString(SpfConfig.SWAP_SPF_ALGORITHM, getProp(savedConfig, zramCompAlgorithm))
        } catch (ex: Exception) {
        }

        try {
            editor.putInt(SpfConfig.SWAP_SPF_SWAPPINESS, getProp(savedConfig, swappiness).toInt())
            editor.putInt(SpfConfig.SWAP_SPF_EXTRA_FREE_KBYTES, getProp(savedConfig, extraFreeKbytes).toInt())
        } catch (ex: Exception) {
        }
        try {
            editor.putInt(SpfConfig.SWAP_SPF_WATERMARK_SCALE, getProp(savedConfig, watermarkScaleFactor).toInt())
        } catch (ex: Exception) {
        }

        editor.apply()
    }

    // 当前模块版本
    fun getModuleVersion(): Int {
        val version = PropsUtils.getProp("vtools.swap.module")
        if (version == "error") {
            return Int.MAX_VALUE
        } else if (version == "") {
            return 0
        } else {
            try {
                return version.toInt()
            } catch (ex:java.lang.Exception) {

            }
        }
        return 0
    }
}