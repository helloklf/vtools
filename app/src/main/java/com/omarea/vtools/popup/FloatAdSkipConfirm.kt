package com.omarea.vtools.popup

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.omarea.Scene
import com.omarea.vtools.R

class FloatAdSkipConfirm(mContext: Context) {
    companion object {
        private var show: Boolean = false
        private var lastApp: CharSequence? = null
    }

    private val wm = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View = LayoutInflater.from(mContext).inflate(R.layout.fw_ad_skip_confirm, null)
    private var onConfirm: Runnable? = null

    init {
        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            wm.removeView(view)
            show = false
        }

        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            wm.removeView(view)
            show = false

            onConfirm?.run()
        }
    }

    private var params: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        height = WindowManager.LayoutParams.WRAP_CONTENT
        width = WindowManager.LayoutParams.MATCH_PARENT
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        format = PixelFormat.TRANSLUCENT

        // 类型
        type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        if (mContext is AccessibilityService && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//6.0+
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        format = PixelFormat.TRANSLUCENT
        x = 0
        y = 0

        flags = WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        gravity = Gravity.BOTTOM or Gravity.FILL_HORIZONTAL
    }


    public fun showConfirm(packageName: CharSequence, rect: Rect, onConfirm: Runnable) {
        if (!show) {
            wm.addView(view, params)
            show = true
            this.onConfirm = onConfirm
            lastApp = packageName

            Scene.postDelayed({
                if (lastApp == packageName) {
                    hideConfirm()
                }
            }, 3000)
        }
    }

    public fun hideConfirm () {
        if (show) {
            wm.removeView(view)
            show = false
        }
    }
}