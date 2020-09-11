package com.omarea.scene_mode

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.omarea.store.SpfConfig
import java.util.ArrayList

/**
 * Created by Hello on 2020/09/10.
 */
open class AutoClickBase {
    @RequiresApi(Build.VERSION_CODES.N)
    private fun buildGesture(rect: Rect): GestureDescription {
        val width = rect.right - rect.left
        val height = rect.bottom - rect.top
        val position = Point(rect.left + (width/ 2), rect.top + (height / 2))
        val builder = GestureDescription.Builder()
        val p = Path()
        p.moveTo(position.x.toFloat(), position.y.toFloat())
        builder.addStroke(GestureDescription.StrokeDescription(p, 0L, 10L))
        val gesture = builder.build()
        return gesture
    }

    /**
     * 触摸或点击按钮（由于模拟触摸可靠性比 直接ACTION_CLICK更可靠，所以只要支持就优先使用模拟触摸）
     */
    fun touchOrClickNode (node: AccessibilityNodeInfo, service:AccessibilityService, reClick: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            service.dispatchGesture(buildGesture(rect), object : AccessibilityService.GestureResultCallback() {
                @Override
                override fun onCompleted(gestureDescription: GestureDescription) {
                    super.onCompleted(gestureDescription);
                    if (reClick) {
                        clickNode(node)
                    }
                    // Log.d("@Scene", "onCompleted: 完成..........");
                }

                override fun onCancelled(gestureDescription:GestureDescription) {
                    super.onCancelled(gestureDescription);
                    // Log.d("@Scene", "onCompleted: 取消..........");
                }
            }, null)
        } else {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    /**
     * 普通点击
     */
    fun clickNode(node: AccessibilityNodeInfo) {
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
}