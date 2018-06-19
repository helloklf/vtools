package com.omarea.shared

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.omarea.shell.units.AccessibilityServiceStart
import com.omarea.vboot.AccessibilityServiceVTools

/**
 * Created by Hello on 2018/06/03.
 */

class AccessibleServiceHelper {
    //判断服务是否激活
    fun serviceIsRunning(context: Context): Boolean {
        return serviceIsRunning(context, "AccessibilityServiceVTools")
    }
    fun serviceIsRunning(context: Context, serviceName: String): Boolean {
        val m = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val serviceInfos = m.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (serviceInfo in serviceInfos) {
            if (serviceInfo.id.endsWith(serviceName)) {
                return true
            }
        }
        return false
    }

    fun startServiceUseRoot(context: Context): Boolean {
        return AccessibilityServiceStart().strartService(context, "${context.packageName}/${AccessibilityServiceVTools::class.java.name}")
    }
}
