package com.omarea.scene_mode

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEventSource
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.omarea.Scene
import com.omarea.common.shell.KeepShellAsync
import com.omarea.common.shell.KeepShellPublic
import com.omarea.store.AutoSkipConfigStore
import java.util.*

/**
 * Created by Hello on 2020/09/10.
 */
class AutoSkipAd(private val service: AccessibilityService) {
    private val autoSkipConfigStore = AutoSkipConfigStore(service.applicationContext)
    companion object {
        private var lstClickedNode: AccessibilityNodeInfo? = null
        private var lstClickedApp: String? = null
        private var lastActivity:String? = null
        private var ids = arrayListOf(
                "com.ruanmei.ithome:id/tv_skip",            // IT之家
                "com.miui.systemAdSolution:id/view_skip",   // MIUI广告
                "com.baidu.searchbox:id/splash_ad_btn_ski", // 百度
                "com.baidu.tieba:id/splash_ad_btn_skip"     // 百度贴吧
        )
    }
    init {
        ids.clear()
        ids.addAll(KeepShellPublic.doCmdSync("cat /cache/ids.log").split("\n"))
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

    private val blackList = arrayListOf("android", "com.android.systemui", "com.miui.home", "com.tencent.mobileqq", "com.tencent.mm", "com.omarea.vtools", "com.omarea.gesture")

    private fun preciseSkip(root: AccessibilityNodeInfo): Boolean {
        autoSkipConfigStore.getSkipViewId(lastActivity)?.run {
            val id = this
            root.findAccessibilityNodeInfosByViewId(id)?.run {
                for (i in indices) {
                    val node = get(i)
                    if (!(lstClickedNode == node)) {
                        lstClickedNode = node
                        lstClickedApp = root.packageName?.toString()
                        autoClickBase.touchOrClickNode(node, service, true)
                        Scene.toast("Scene自动点了(${id})", Toast.LENGTH_SHORT)
                    }
                }
                return true
            }
        }
        return false
    }

    fun skipAd(event: AccessibilityEvent, precise: Boolean) {
        val packageName = event.packageName
        if (packageName == null || this.blackList.contains(event.packageName)) {
            return
        }

        val source = event.source ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            lastActivity = event.className?.toString()
        } else if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val viewId = event.source?.viewIdResourceName // 有些跳过按钮不是文字来的 // if (event.text?.contains("跳过") == true) event.source?.viewIdResourceName else null
            if (viewId != null && !ids.contains(viewId)) {
                ids.add(viewId)
                KeepShellAsync.getInstance("skip-ad").doCmd("echo '$lastActivity $viewId' >> /cache/ids.log")
                Scene.toast("点击了$viewId")
            }
        }
        if (preciseSkip(source)) {
            return
        }

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
                        val text = node.text.trim().toString()
                        if (
                                text == "跳过" || text == "跳过广告" ||
                                Regex("^[0-9]+[\\ss]*跳过[广告]*\$").matches(text) ||
                                Regex("^[点击]*跳过[广告]*[\\ss]{0,}[0-9]+\$").matches(text)
                        ) {
                            if (!(lstClickedApp == packageName && lstClickedNode == node)) {
                                val viewId = node.viewIdResourceName
                                val p = Rect()
                                node.getBoundsInScreen(p)
                                val parentId = node.parent.viewIdResourceName
                                val pParentId = node.parent.parent.viewIdResourceName
                                Log.d("@Scene", "SkipAD √ $packageName ${p} ${viewId}" + node.text)
                                autoClickBase.touchOrClickNode(node, service, true)
                                lstClickedApp = packageName.toString()
                                lstClickedNode = node
                                Scene.toast("Scene自动点了(${text})", Toast.LENGTH_SHORT)
                                if (viewId != null && !ids.contains(viewId)) {
                                    ids.add(viewId)
                                    KeepShellAsync.getInstance("skip-ad").doCmd("echo '$viewId' >> /cache/ids.log")
                                }
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
                            val text = node.text.trim().toString()
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