package com.omarea.scene_mode

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.omarea.Scene
import com.omarea.store.AutoSkipConfigStore
import java.util.*

/**
 * Created by Hello on 2020/09/10.
 */
class AutoSkipAd(private val service: AccessibilityService) {
    private val autoSkipConfigStore = AutoSkipConfigStore(service.applicationContext)
    private var displayWidth: Int = 0
    private var displayHeight: Int = 0

    companion object {
        private var lastClickedNodeValue: AccessibilityNodeInfo? = null
        private var lastClickedNode: AccessibilityNodeInfo?
            get() {
                return lastClickedNodeValue
            }
            set (value) {
                lastClickedNodeValue = value
                lastClickedNodeText = value?.text
            }
        private var lastClickedNodeText: CharSequence? = null
        private var lastClickedApp: String? = null
        private var lastActivity:String? = null
    }

    private val autoClickBase = AutoClickBase()

    private var autoClickKeyWords: ArrayList<String> = object : ArrayList<String>() {
        /*
        init {
            add("暂不升级")
            add("忽略此版本")
        }
        */
    }

    private val blackList = arrayListOf("android", "com.android.systemui", "com.miui.home", "com.tencent.mobileqq", "com.tencent.mm", "com.omarea.vtools", "com.omarea.gesture")

    private fun preciseSkip(root: AccessibilityNodeInfo): Boolean {
        autoSkipConfigStore.getSkipViewId(lastActivity)?.run {
            val id = this
            root.findAccessibilityNodeInfosByViewId(id)?.run {
                for (i in indices) {
                    val node = get(i)
                    if (!(lastClickedNode == node)) {
                        lastClickedNode = node
                        lastClickedApp = root.packageName?.toString()
                        autoClickBase.touchOrClickNode(node, service, true)
                        Scene.toast("Scene自动点了(${id})", Toast.LENGTH_SHORT)
                    }
                }
                return true
            }
        }
        return false
    }

    // 根据元素位置判断是否要当做广告跳过按钮点击
    // 一般来说，广告跳过按钮都在屏幕四角区域
    // 因此过滤掉非四角区域的按钮有助于降低误点率
    private fun pointFilter(rect: Rect):Boolean {
        val top = rect.top.toFloat()
        val bottom = displayHeight - rect.bottom.toFloat()
        val left = rect.left.toFloat()
        val right = displayWidth - rect.right.toFloat()
        val minSide = if (displayHeight > displayWidth) displayWidth else displayHeight
        val xMax = minSide * 0.3f
        val yMax = minSide * 0.28f
        if (top > yMax && bottom > yMax) {
            Log.d("@Scene", "Y Filter ${top} ${bottom} ${yMax}")
            return false
        }
        if (left > xMax && right > xMax) {
            Log.d("@Scene", "X Filter ${left} ${right} ${xMax}")
            return false
        }
        // Log.d("@Scene", "Y Filter ${top} ${bottom} ${yMax}")
        // Log.d("@Scene", "X Filter ${left} ${right} ${xMax}")
        return true
    }

    fun skipAd(event: AccessibilityEvent, precise: Boolean, displayWidth: Int, displayHeight: Int) {
        this.displayWidth = displayWidth
        this.displayHeight = displayHeight
        val packageName = event.packageName
        if (packageName == null || this.blackList.contains(event.packageName)) {
            return
        }

        val source = event.source ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            lastActivity = event.className?.toString()
        } else if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val viewId = event.source?.viewIdResourceName // 有些跳过按钮不是文字来的 // if (event.text?.contains("跳过") == true) event.source?.viewIdResourceName else null
            Log.d("@Scene", "点击了$viewId")
        }
        if (precise && preciseSkip(source)) {
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
                            if (!(lastClickedApp == packageName && (lastClickedNode == node && lastClickedNodeText == node.text))) {
                                val viewId = node.viewIdResourceName
                                val p = Rect()
                                node.getBoundsInScreen(p)
                                if (pointFilter(p)) {
                                    val parentId = node.parent.viewIdResourceName
                                    val pParentId = node.parent.parent.viewIdResourceName
                                    Log.d("@Scene", "SkipAD √ $packageName ${p} ${viewId}" + node.text)
                                    autoClickBase.touchOrClickNode(node, service, true)
                                    lastClickedApp = packageName.toString()
                                    lastClickedNode = node
                                    Scene.toast("Scene自动点了(${text})", Toast.LENGTH_SHORT)
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
                                if (!(lastClickedApp == packageName && lastClickedNode == node)) {
                                    autoClickBase.touchOrClickNode(node, service, true)
                                    lastClickedApp = packageName.toString()
                                    lastClickedNode = node
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