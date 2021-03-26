package com.omarea.scene_mode

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi

/**
 * Created by Hello on 2020/09/10.
 */
open class AutoClickBase {
    @RequiresApi(Build.VERSION_CODES.N)
    private fun buildGesture(rect: Rect): GestureDescription {
        val width = rect.right - rect.left
        val height = rect.bottom - rect.top
        val position = Point(rect.left + (width / 2), rect.top + (height / 2))
        val builder = GestureDescription.Builder()
        val p = Path()
        p.moveTo(position.x.toFloat(), position.y.toFloat())
        builder.addStroke(GestureDescription.StrokeDescription(p, 0L, 20L))
        val gesture = builder.build()
        return gesture
    }

    fun tryTouchNodeRect(node: AccessibilityNodeInfo, service: AccessibilityService): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val rect = Rect()
            node.getBoundsInScreen(rect)

            /*
            try {
                if (!node.isFocusable) {
                    node.isFocusable = true
                }
                node.isFocusable = true
            } catch (ex: Exception) {
                // Log.e("@Scene", "" + ex.message)
            }
            */

            return service.dispatchGesture(buildGesture(rect), object : AccessibilityService.GestureResultCallback() {
                @Override
                override fun onCompleted(gestureDescription: GestureDescription) {
                    super.onCompleted(gestureDescription);
                    // Log.d("@Scene", "onCompleted: 完成..........");
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    super.onCancelled(gestureDescription);
                    // Log.d("@Scene", "onCancelled: 取消..........");
                }
            }, null)
        }
        return false
    }

    fun nodeClickable(node: AccessibilityNodeInfo): Boolean {
        return node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
    }

    /**
     * 普通点击
     */
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        if (nodeClickable(node)) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        return false
    }
}