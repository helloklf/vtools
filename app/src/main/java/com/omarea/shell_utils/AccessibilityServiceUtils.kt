package com.omarea.shell_utils

import com.omarea.common.shell.KeepShellPublic

/**
 * Created by Hello on 2018/06/03.
 */

class AccessibilityServiceUtils {
    fun strartService(serviceName: String): Boolean {
        return KeepShellPublic.doCmdSync(
                "services=`settings get secure enabled_accessibility_services`;\n" +
                        "service='$serviceName';\n" +
                        "include=\$(echo \"\$services\" |grep \"\$service\")\n" +

                        "if [ ! -n \"\$services\" ]\n" +
                        "then\n" +
                        "   settings put secure enabled_accessibility_services \"\$service\"; \n" +
                        "elif [ ! -n \"\$include\" ]\n" +
                        "then\n" +
                        "   settings put secure enabled_accessibility_services \"\$services:\$service\"; \n" +
                        "else\n" +
                        "   settings put secure enabled_accessibility_services \"\$services\"; \n" +
                        "fi\n" +
                        "settings put secure accessibility_enabled 1;\n"
        ) != "error"
    }

    fun stopService(serviceName: String): Boolean {
        val servicesStr = KeepShellPublic.doCmdSync("settings get secure enabled_accessibility_services")
        if (servicesStr.contains(serviceName)) {
            val serviceBuilder = StringBuilder()
            val services = servicesStr.split(":")
            for (service in services) {
                if (service != serviceName) {
                    if (serviceBuilder.length > 0) {
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
