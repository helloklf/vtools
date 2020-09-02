package com.omarea.library.basic

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager

class AccessibleServiceState {
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