package com.omarea.shell_utils

import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.KernelProrp
import java.util.*

class SensorUtils {
    fun sensorList(): HashMap<String, String> {
        val result = KeepShellPublic.doCmdSync("ls /sys/class/thermal/*/temp")
        val sensors = HashMap<String, String>()
        if (result != "error") {
            // /sys/class/thermal/thermal_zone53/temp
            for (path in result.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (path.isNotBlank()) {
                    val paths = path.trim().split("/")
                    sensors.put(paths[3], path.trim())
                }
            }
        }
        return HashMap()
    }

    fun sensorTemp(sensor: String): String = KernelProrp.getProp("/sys/class/thermal/$sensor/temp")
}
