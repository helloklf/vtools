package com.omarea.shell.units

import android.content.Context
import com.omarea.shell.SuDo

/**
 * Created by Hello on 2018/06/03.
 */

class AccessibilityServiceStart {
    fun strartService(context: Context, serviceName: String): Boolean {
        return SuDo(context).execCmdSync(
                "services=`settings get secure enabled_accessibility_services`;\n" +
                        "service='$serviceName';\n" +
                        "echo \"\$services\" |grep -q \"\$service\"\n" +
                        "if [ \$? -gt -1 ]\n" +
                        "then\n" +
                        "\tsettings put secure enabled_accessibility_services \"\$services:\$service\"; \n" +
                        "fi\n" +
                        "settings put secure accessibility_enabled 1;\n" +
                        "am startservice -n \$service;\n"
        )
    }
}
