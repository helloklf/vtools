package com.omarea.library.shell

import com.omarea.common.shell.KeepShellPublic

/**
 * Created by Hello on 2018/06/03.
 */

class AccessibilityServiceUtils {
    /*
    # 使用Shell启动服务

    settings put secure accessibility_enabled 0
    services=`settings get secure enabled_accessibility_services`
    service='com.omarea.vtools/com.omarea.vtools.AccessibilityScenceMode'
    include=`echo "$services" | grep "$service"`
    if [ ! -n "$services" ]; then
      settings put secure enabled_accessibility_services "$service"
    elif [ ! -n "$include" ]; then
      settings put secure enabled_accessibility_services "$services:$service"
    fi
    settings put secure accessibility_enabled 1
    */

    fun stopService(serviceName: String): Boolean {
        val servicesStr = KeepShellPublic.doCmdSync("settings get secure enabled_accessibility_services")
        if (servicesStr.contains(serviceName)) {
            val serviceBuilder = StringBuilder()
            val services = servicesStr.split(":")
            for (service in services) {
                if (service != serviceName) {
                    if (serviceBuilder.isNotEmpty()) {
                        serviceBuilder.append(":")
                    }
                    serviceBuilder.append(service)
                }
            }
            KeepShellPublic.doCmdSync("settings put secure enabled_accessibility_services $serviceBuilder\nsettings put secure accessibility_enabled 1")
        }
        return true
    }
}
