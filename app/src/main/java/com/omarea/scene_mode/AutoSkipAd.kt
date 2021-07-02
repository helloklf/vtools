package com.omarea.scene_mode

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.omarea.Scene
import com.omarea.store.AutoSkipConfigStore
import com.omarea.store.SpfConfig
import java.util.*

/**
 * Created by Hello on 2020/09/10.
 */
class AutoSkipAd(private val service: AccessibilityService) {
    private val autoSkipConfigStore = AutoSkipConfigStore(service.applicationContext)
    private var displayWidth: Int = 0
    private var displayHeight: Int = 0
    private val blackListCustom = service.getSharedPreferences(SpfConfig.AUTO_SKIP_BLACKLIST, Context.MODE_PRIVATE)

    companion object {
        private var lastClickedNodeValue: AccessibilityNodeInfo? = null
        private var lastClickedNode: AccessibilityNodeInfo?
            get() {
                return lastClickedNodeValue
            }
            set(value) {
                lastClickedNodeValue = value
                lastClickedNodeText = value?.text
            }
        private var lastClickedNodeText: CharSequence? = null
        private var lastClickedApp: String? = null
        private var lastActivity: String? = null
    }

    private val autoClickBase = AutoClickBase()

    private val blackList = arrayListOf("android", "com.android.systemui", "com.miui.home", "com.tencent.mobileqq", "com.tencent.mm", "com.omarea.vtools", "com.omarea.gesture", "com.android.settings")

    private fun preciseSkip(root: AccessibilityNodeInfo): Boolean {
        autoSkipConfigStore.getSkipViewId(lastActivity)?.run {
            val id = this
            root.findAccessibilityNodeInfosByViewId(id)?.run {
                for (i in indices) {
                    val node = get(i)
                    if (!(lastClickedNode == node)) {
                        lastClickedNode = node
                        lastClickedApp = root.packageName?.toString()
                        autoClickBase.clickNode(node) || autoClickBase.tryTouchNodeRect(node, service)
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
    private fun pointFilter(rect: Rect): Boolean {
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

    // 文字匹配正则
    private val textRegx1 = Regex("^[0-9]+[\\ss]*跳过[广告]*\$")
    private val textRegx2 = Regex("^[点击]*跳过[广告]*[\\ss]{0,}[0-9]+\$")

    // 如果自动点击成功，记录时间的eventTime（目的在于 同一时间发生的事件，不要重复执行多次点击）
    private var lastCompletedEventTime = 0L
    fun skipAd(event: AccessibilityEvent, precise: Boolean, displayWidth: Int, displayHeight: Int) {
        this.displayWidth = displayWidth
        this.displayHeight = displayHeight
        val packageName = event.packageName?.toString()
        if (packageName == null || this.blackList.contains(packageName) || blackListCustom.contains(packageName)) {
            return
        }

        val source = event.source ?: return

        val t = event.eventTime
        if (!(lastCompletedEventTime != t && t > lastCompletedEventTime)) {
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            lastActivity = event.className?.toString()
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
                            className == "android.widget.textview" || className.toLowerCase(Locale.getDefault()).contains("button")
                    ) {
                        val text = node.text.trim().replace(Regex("[\nsS秒]", RegexOption.MULTILINE), "").toString()
                        if (
                                text == "跳过" || text == "跳过广告" ||
                                textRegx1.matches(text) ||
                                textRegx2.matches(text)
                        ) {
                            if (!(lastClickedApp == packageName && (lastClickedNode == node && lastClickedNodeText == node.text))) {
                                val viewId = node.viewIdResourceName
                                val p = Rect()
                                node.getBoundsInScreen(p)
                                val splash = lastActivity?.toLowerCase(Locale.getDefault())?.contains("splash") == true
                                if (splash || pointFilter(p)) {
                                    // 尝试点子节点
                                    if (autoClickBase.clickNode(node)) {
                                        Log.d("@Scene", "SkipAD √ $packageName ${p} id: ${viewId}, text:" + node.text)
                                        Scene.toast("Scene自动点了(${text})", Toast.LENGTH_SHORT)
                                        return
                                    }

                                    // 尝试点父节点
                                    val pp = Rect()
                                    val wrapNode = node.parent
                                    if (wrapNode != null) {
                                        wrapNode.getBoundsInScreen(pp)
                                        if ((splash || pointFilter(pp)) && autoClickBase.clickNode(wrapNode)) {
                                            Log.d("@Scene", "SkipAD √ $packageName ${p} id: ${wrapNode.viewIdResourceName}, text:" + node.text)
                                            lastClickedApp = packageName.toString()
                                            lastClickedNode = node
                                            lastCompletedEventTime = t
                                            Scene.toast("Scene自动点了(${text})", Toast.LENGTH_SHORT)
                                            return
                                        }
                                    }

                                    // 尝试触摸子节点
                                    if (autoClickBase.tryTouchNodeRect(node, service)) {
                                        lastClickedApp = packageName.toString()
                                        lastClickedNode = node
                                        lastCompletedEventTime = t
                                        Scene.toast("Scene尝试触摸了(${text})", Toast.LENGTH_SHORT)
                                        return
                                    }
                                } else {
                                    /*
                                    // 去掉了跳过广告确认弹窗 因为目前此逻辑并不可靠
                                    val clickableNode = if (autoClickBase.nodeClickable(node)) {
                                        node
                                    } else {
                                        val wrapNode = node.parent
                                        if (autoClickBase.nodeClickable(wrapNode)) {
                                            wrapNode
                                        } else {
                                            null
                                        }
                                    }
                                    if (clickableNode != null) {
                                        FloatAdSkipConfirm(service).showConfirm(packageName, p) {
                                            autoClickBase.clickNode(clickableNode)
                                        }
                                        Log.d("@Scene", "SkipAD SKip -> $packageName, ${source.className} ${p} id: ${viewId}, text:" + node.text)
                                    }
                                    */
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


            /*
            var autoClickKeyWords: ArrayList<String> = object : ArrayList<String>() {
                init {
                    add("暂不升级")
                    add("忽略此版本")
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
                                    autoClickBase.touchOrClickNode(node, service)
                                    lastClickedApp = packageName.toString()
                                    lastClickedNode = node
                                    lastCompletedEventTime = t

                                    Scene.toast("Scene自动点了(${text})", Toast.LENGTH_SHORT)
                                }
                            }

                        }
                    }
                    break
                }
            }
            */
        } catch (ex: java.lang.Exception) {
            Log.e("@Scene", "SkipAD Error -> ${ex.message}")
        }
    }
}