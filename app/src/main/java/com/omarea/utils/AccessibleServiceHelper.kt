package com.omarea.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.omarea.shell_utils.AccessibilityServiceUtils
import com.omarea.vtools.AccessibilityScenceMode

/**
 * Created by Hello on 2018/06/03.
 */

class AccessibleServiceHelper {
    // 场景模式服务是否正在运行
    fun serviceRunning(context: Context): Boolean {
        return serviceRunning(context, "AccessibilityScenceMode")
    }

    // 启动场景模式服务
    fun startSceneModeService(context: Context): Boolean {
        return AccessibilityServiceUtils().strartService(
                "${context.packageName}/${AccessibilityScenceMode::class.java.name}"
        )
    }

    // 停止场景模式服务
    fun stopSceneModeService(context: Context): Boolean {
        return AccessibilityServiceUtils().stopService(
                "${context.packageName}/${AccessibilityScenceMode::class.java.name}"
        )
    }

    fun serviceRunning(context: Context, serviceName: String): Boolean {
        val m = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val serviceInfos = m.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (serviceInfo in serviceInfos) {
            if (serviceInfo.id.endsWith(serviceName)) {
                return true
            }
        }
        return false
    }
}
