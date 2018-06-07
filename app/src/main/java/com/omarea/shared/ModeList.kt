package com.omarea.shared

import com.omarea.shell.Props
import com.omarea.vboot.R

/**
 * Created by Hello on 2018/06/03.
 */

open class ModeList {
    internal var DEFAULT = "balance";
    internal var POWERSAVE = "powersave";
    internal var PERFORMANCE = "performance";
    internal var FAST = "fast";
    internal var BALANCE = "balance";
    internal var IGONED = "igoned";

    internal fun getModName(mode:String) : String {
        when(mode) {
            POWERSAVE ->      return "省电模式"
            PERFORMANCE ->      return "性能模式"
            FAST ->      return "极速模式"
            BALANCE ->   return "均衡模式"
            else ->         return "未知模式"
        }
    }

    internal fun getModIcon(mode: String): Int {
        when (mode) {
            "powersave" -> return R.drawable.p1
            "balance" -> return R.drawable.p2
            "performance" -> return R.drawable.p3
            "fast" -> return R.drawable.p4
            else -> return R.drawable.p3
        }
    }

    internal fun getModImage(mode: String): Int {
        when (mode) {
            "powersave" -> return R.drawable.shortcut_p0
            "balance" -> return R.drawable.shortcut_p1
            "performance" -> return R.drawable.shortcut_p2
            "fast" -> return R.drawable.shortcut_p3
            else -> return R.drawable.shortcut_p2
        }
    }

    internal fun getCurrentPowerMode(): String? {
        return Props.getProp(Consts.PowerModeState)
    }
    internal fun getCurrentPowermodeApp(): String? {
        return Props.getProp(Consts.PowerModeApp)
    }
}
