package com.omarea.scene_mode

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.omarea.Scene
import java.util.*

/**
 * Created by Hello on 2020/09/10.
 */
class AutoSkipAd {
    companion object {
        private var lstClickedNode: AccessibilityNodeInfo? = null
        private var lstClickedApp: String? = null
    }

    private val autoClickBase = AutoClickBase()

    private var autoClickKeyWords: ArrayList<String> = object : ArrayList<String>() {
        init {
            add("暂不升级")
            add("忽略此版本")
            // add("跳过广告")
            // add("Next")
            // add("skip")
            // add("Skip")
        }
    }

    private val blackList = arrayListOf("com.android.systemui", "com.miui.home", "com.tencent.mobileqq", "com.tencent.mm", "com.omarea.vtools", "com.omarea.gesture")
    fun skipAd(service: AccessibilityService, event: AccessibilityEvent) {
        val packageName = event.packageName
        if (packageName == null || this.blackList.contains(event.packageName)) {
            return
        }

        val source = event.source ?: return

        try {
            source.findAccessibilityNodeInfosByText("跳过")?.run {
                var node: AccessibilityNodeInfo
                for (i in indices) {
                    node = get(i)
                    val className = node.className.toString().toLowerCase(Locale.getDefault())
                    if (
                            className == "android.widget.textview" ||
                            className.toLowerCase(Locale.getDefault()).contains("button")
                    ) {
                        val text = node.text.trim()
                        if (text == "跳过" || text == "跳过广告" || Regex("^[0-9]+[\\ss]*跳过[广告]*\$").matches(text) || Regex("^跳过[广告]*[\\ss]{0,}[0-9]+\$").matches(text)) {
                            if (!(lstClickedApp == packageName && lstClickedNode == node)) {
                                autoClickBase.touchOrClickNode(node, service, true)
                                lstClickedApp = packageName.toString()
                                lstClickedNode = node
                                Scene.toast("Scene自动点了(${text})", Toast.LENGTH_SHORT)
                                return
                            }
                        } else {
                            Log.d("@Scene", "SkipAD -> $className；" + node.text)
                        }
                    } else {
                        Log.d("@Scene", "SkipAD -> $className；" + node.text)
                    }
                }
            }

            for (ki in autoClickKeyWords.indices) {
                val nextNodes = source.findAccessibilityNodeInfosByText(autoClickKeyWords[ki])
                val keyword = autoClickKeyWords[ki]
                if (nextNodes != null && nextNodes.isNotEmpty()) {
                    var node: AccessibilityNodeInfo
                    for (i in nextNodes.indices) {
                        node = nextNodes[i]
                        val className = node.className.toString().toLowerCase(Locale.getDefault())
                        if (
                                className == "android.widget.textview" ||
                                className.toLowerCase(Locale.getDefault()).contains("button")
                        ) {
                            val text = node.text.trim()
                            if (text == keyword) {
                                if (!(lstClickedApp == packageName && lstClickedNode == node)) {
                                    autoClickBase.touchOrClickNode(node, service, true)
                                    lstClickedApp = packageName.toString()
                                    lstClickedNode = node
                                    Scene.toast("Scene自动点了(${text})", Toast.LENGTH_SHORT)
                                }
                            }

                        }
                    }
                    break
                }
            }
        } catch (ex: java.lang.Exception) {
            Log.e("@Scene", "SkipAD Error -> ${ex.message}")
        }
    }
}