package com.omarea.utils

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.omarea.store.SpfConfig
import java.util.*

/**
 * Created by Hello on 2017/4/8.
 */
class AutoClick {
    internal var autoClickKeyWords: ArrayList<String> = object : ArrayList<String>() {
        init {
            add("下一步")
            add("下一步")
            add("Next")
            add("Next")
        }
    }
    internal var autoClickKeyWords2: ArrayList<String> = object : ArrayList<String>() {
        init {
            add("安装")
            add("Install")
            add("安裝")
            add("完成")
            add("Done")
        }
    }

    fun packageinstallerAutoClick(context: Context, event: AccessibilityEvent) {
        if (!context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
                        .getBoolean(SpfConfig.GLOBAL_SPF_AUTO_INSTALL, false) || event.source == null)
            return

        for (ki in autoClickKeyWords.indices) {
            val nextNodes = event.source.findAccessibilityNodeInfosByText(autoClickKeyWords[ki])
            if (nextNodes != null && !nextNodes.isEmpty()) {
                var node: AccessibilityNodeInfo
                for (i in nextNodes.indices) {
                    node = nextNodes[i]
                    if (node.className.toString().toLowerCase().contains("button") && node.isEnabled) {
                        if (node.text != autoClickKeyWords[ki])
                            continue
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        try {
                            Thread.sleep(300)
                        } catch (ex: Exception) {
                        }

                    }
                }
            }
        }


        for (ki in autoClickKeyWords.indices) {

            val nodes = event.source.findAccessibilityNodeInfosByText(autoClickKeyWords2[ki])
            if (nodes != null && !nodes.isEmpty()) {
                var node: AccessibilityNodeInfo
                for (i in nodes.indices) {
                    node = nodes[i]
                    if (node.className.toString().toLowerCase().contains("button")) {
                        if (!node.isEnabled) {
                            node.isEnabled = true
                        }
                        if (node.text != autoClickKeyWords2[ki])
                            continue
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                }
            }
        }
    }

    fun miuiUsbInstallAutoClick(context: Context, event: AccessibilityEvent) {
        if (!context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
                        .getBoolean(SpfConfig.GLOBAL_SPF_AUTO_INSTALL, false) || event.source == null)
            return

        val next2Nodes = event.source.findAccessibilityNodeInfosByText("继续安装")
        if (next2Nodes != null && !next2Nodes.isEmpty()) {
            var node: AccessibilityNodeInfo
            for (i in next2Nodes.indices) {
                node = next2Nodes[i]
                if (node.className.toString().toLowerCase().contains("button")) {
                    if (!node.isEnabled) {
                        node.isEnabled = true
                    }
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
        }
    }
}
