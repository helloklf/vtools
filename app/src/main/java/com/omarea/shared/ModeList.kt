package com.omarea.shared

/**
 * Created by Hello on 2018/06/03.
 */

open class ModeList {
    protected var DEFAULT = "balance";
    protected var POWERSAVE = "powersave";
    protected var PERFORMANCE = "performance";
    protected var FAST = "fast";
    protected var BALANCE = "balance";
    protected var IGONED = "igoned";

    protected fun getModName(mode:String) : String {
        when(mode) {
            POWERSAVE ->      return "省电模式"
            PERFORMANCE ->      return "性能模式"
            FAST ->      return "极速模式"
            BALANCE ->   return "均衡模式"
            else ->         return "未知模式"
        }
    }
}
