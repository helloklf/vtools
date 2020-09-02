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
# 额外空余内存(kbytes)
extra_free_kbytes=98304
*/

/**
 * Created by Hello on 2017/11/01.
 */

class SwapModuleUtils {
    // 是否已经安装用于处理自启动的magisk模块
    val magiskModuleInstalled = RootFile.fileExists("/data/swap_config.conf") && PropsUtils.getProp("vtools.swap.controller") == "magisk"

    private val swapEnable = "swap"
    private val swapSize = "swap_size"
    private val swapPriority = "swap_priority"
    private val swapUseLoop = "swap_use_loop"

    private val zramEnable = "zram"
    private val zramSize = "zram_size"
    private val zramCompAlgorithm = "comp_algorithm"

    private val swappiness = "swappiness"
    private val extraFreeKbytes = "extra_free_kbytes"

    private fun getProp(prop: String):String {
        return KeepShellPublic.doCmdSync("cat /data/swap_config.conf | grep -v '^#' | grep \"^${prop}=\" | cut -f2 -d '='")
    }

    private fun setProp(prop: String, value: Any) {
        KeepShellPublic.doCmdSync("busybox sed -i 's/^$prop=.*/$prop=$value/' /data/swap_config.conf")
    }

    fun saveModuleConfig(spf:SharedPreferences) {
        setProp(swapEnable, spf.getBoolean(SpfConfig.SWAP_SPF_SWAP, false))
        setProp(swapSize, spf.getInt(SpfConfig.SWAP_SPF_SWAP_SWAPSIZE, 0))
        setProp(swapPriority, spf.getInt(SpfConfig.SWAP_SPF_SWAP_PRIORITY, 0))
        setProp(swapUseLoop, spf.getBoolean(SpfConfig.SWAP_SPF_SWAP_USE_LOOP, false))

        setProp(zramEnable, spf.getBoolean(SpfConfig.SWAP_SPF_ZRAM, false))
        setProp(zramSize, spf.getInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, 0))
        setProp(zramCompAlgorithm, "" + spf.getString(SpfConfig.SWAP_SPF_ALGORITHM, "lzo"))

        setProp(swappiness, spf.getInt(SpfConfig.SWAP_SPF_SWAPPINESS, 0))
        setProp(extraFreeKbytes, spf.getInt(SpfConfig.SWAP_MIN_FREE_KBYTES, 0))
    }

    fun loadModuleConfig(spf:SharedPreferences) {
        val editor = spf.edit()

        try {
            editor.putBoolean(SpfConfig.SWAP_SPF_SWAP, getProp(swapEnable) == "true")
            editor.putInt(SpfConfig.SWAP_SPF_SWAP_SWAPSIZE, getProp(swapSize).toInt())
            editor.putInt(SpfConfig.SWAP_SPF_SWAP_PRIORITY, getProp(swapPriority).toInt())
            editor.putBoolean(SpfConfig.SWAP_SPF_SWAP_USE_LOOP, getProp(swapUseLoop) == "true")
        } catch (ex: Exception){
        }

        try {
            editor.putBoolean(SpfConfig.SWAP_SPF_ZRAM, getProp(zramEnable) == "true")
            editor.putInt(SpfConfig.SWAP_SPF_ZRAM_SIZE, getProp(zramSize).toInt())
            editor.putString(SpfConfig.SWAP_SPF_ALGORITHM, getProp(zramCompAlgorithm))
        } catch (ex: Exception){
        }

        try {
            editor.putInt(SpfConfig.SWAP_SPF_SWAPPINESS, getProp(swappiness).toInt())
            editor.putInt(SpfConfig.SWAP_MIN_FREE_KBYTES, getProp(extraFreeKbytes).toInt())
        } catch (ex: Exception) {}

        editor.apply()
    }
}