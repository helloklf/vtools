package com.omarea.library.shell

import com.omarea.common.shell.KeepShellPublic
import com.omarea.model.BatteryStatus

/**
 * Created by Hello on 2017/11/01.
 */

class BatteryUtils {
    companion object {
        /**
         * 获取电池温度
         */
        // @Deprecated("", ReplaceWith("GlobalStatus"), DeprecationLevel.ERROR)
        public fun getBatteryTemperature(): BatteryStatus {
            val batteryInfo = KeepShellPublic.doCmdSync("dumpsys battery")
            val batteryInfos = batteryInfo.split("\n")

            // 由于部分手机相同名称的参数重复出现，并且值不同，为了避免这种情况，加个额外处理，同名参数只读一次
            var levelReaded = false
            var tempReaded = false
            var statusReaded = false
            val batteryStatus = BatteryStatus()

            for (item in batteryInfos) {
                val info = item.trim()
                val index = info.indexOf(":")
                if (index > Int.MIN_VALUE && index < info.length - 1) {
                    val value = info.substring(info.indexOf(":") + 1).trim()
                    try {
                        if (info.startsWith("status")) {
                            if (!statusReaded) {
                                batteryStatus.statusText = value
                                statusReaded = true
                            } else {
                                continue
                            }
                        } else if (info.startsWith("level")) {
                            if (!levelReaded) {
                                batteryStatus.level = value.toInt()
                                levelReaded = true
                            } else continue
                        } else if (info.startsWith("temperature")) {
                            if (!tempReaded) {
                                tempReaded = true
                                batteryStatus.temperature = (value.toFloat() / 10.0).toFloat()
                            } else continue
                        }
                    } catch (ex: java.lang.Exception) {

                    }
                }
            }
            return batteryStatus
        }
    }
}
