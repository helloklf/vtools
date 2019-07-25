package com.omarea.shell_utils

import android.content.Context
import com.omarea.common.shell.KeepShellPublic

/**
 * Created by Hello on 2018/06/03.
 */

class AccessibilityServiceUtils {
    fun strartService(context: Context, serviceName: String): Boolean {
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
}
