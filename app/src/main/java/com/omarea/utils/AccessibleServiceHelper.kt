package com.omarea.utils

import android.content.Context
import com.omarea.library.basic.AccessibleServiceState
import com.omarea.library.shell.AccessibilityServiceUtils
import com.omarea.scene.AccessibilityScence

/**
 * Created by Hello on 2018/06/03.
 */

class AccessibleServiceHelper {
    // 场景模式服务是否正在运行
    fun serviceRunning(context: Context): Boolean {
        return AccessibleServiceState().serviceRunning(context, AccessibilityScence::class.java.simpleName)
    }

    // 停止场景模式服务
    fun stopSceneModeService(context: Context): Boolean {
        return AccessibilityServiceUtils().stopService("${context.packageName}/${AccessibilityScence::class.java.name}")
    }

    fun serviceRunning(context: Context, serviceName: String): Boolean {
        return AccessibleServiceState().serviceRunning(context, serviceName)
    }
}
