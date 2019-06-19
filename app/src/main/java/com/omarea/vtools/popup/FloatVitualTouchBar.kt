package com.omarea.vtools.popup

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.Toast
import com.omarea.ui.TouchBarView
import com.omarea.vtools.R

/**
 * 弹窗辅助类
 *
 * @ClassName WindowUtils
 */
class FloatVitualTouchBar(context: AccessibilityService, var isLandscapf: Boolean = false) {
    private var bottomView: View? = null
    private var leftView: View? = null
    private var rightView: View? = null

    private var mContext: AccessibilityService? = context

    private var lastEventTime = 0L
    private var lastEvent = -1

    /**
     * 获取导航栏高度
     * @param context
     * @return
     */
    fun getNavBarHeight(context: Context): Int {
        val resourceId: Int
        val rid = context.resources.getIdentifier("config_showNavigationBar", "bool", "android")
        if (rid != 0) {
            resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return context.resources.getDimensionPixelSize(resourceId)
        } else {
            return 0
        }
    }

    /**
     * 隐藏弹出框
     */
    fun hidePopupWindow() {
        if (this.bottomView != null) {
            mWindowManager!!.removeView(this.bottomView)
        }
        if (this.leftView != null) {
            mWindowManager!!.removeView(this.leftView)
        }
        if (this.rightView != null) {
            mWindowManager!!.removeView(this.rightView)
        }
        // KeepShellPublic.doCmdSync("wm overscan reset")
    }

    /**
     * dp转换成px
     */
    private fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    private fun performGlobalAction(context: AccessibilityService, event: Int) {
        if (isLandscapf && (lastEventTime + 1500 < System.currentTimeMillis() || lastEvent != event)) {
            lastEvent = event
            lastEventTime = System.currentTimeMillis()
            Toast.makeText(context, "请重复手势~", Toast.LENGTH_SHORT).show()
        } else {
            context.performGlobalAction(event)
        }
    }

    @SuppressLint("ApplySharedPref", "ClickableViewAccessibility")
    private fun setBottomView(context: AccessibilityService): View {
        val view = LayoutInflater.from(context).inflate(R.layout.fw_vitual_touch_bar, null)

        val bar = view.findViewById<TouchBarView>(R.id.bottom_touch_bar)

        bar.setOnClickListener {
            performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_HOME)
        }
        bar.setOnLongClickListener {
            performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
            return@setOnLongClickListener false
        }
        bar.setSize(LayoutParams.MATCH_PARENT, dp2px(context, 12f), TouchBarView.BOTTOM)

        val params = WindowManager.LayoutParams()

        // 设置window type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//6.0+
            params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY // WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        params.format = PixelFormat.TRANSLUCENT
        params.width = LayoutParams.MATCH_PARENT
        params.height = LayoutParams.WRAP_CONTENT
        params.gravity = Gravity.BOTTOM
        params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN
        // WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        // LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE

        val navHeight = getNavBarHeight(mContext!!)
        if (navHeight > 0) {
            /*
                val display = mWindowManager!!.getDefaultDisplay()
                val p = Point()
                display.getRealSize(p)
                params.y = -navHeight
                params.x = 0
            */
            /*
                KeepShellPublic.doCmdSync("wm overscan 0,0,0,-" + navHeight)
            */
        } else {
        }
        mWindowManager!!.addView(view, params)

        return view
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setLeftView(context: AccessibilityService): View {
        val view = LayoutInflater.from(context).inflate(R.layout.fw_vitual_touch_bar, null)
        val bar = view.findViewById<TouchBarView>(R.id.bottom_touch_bar)

        bar.setOnLongClickListener { _ ->
            performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
            return@setOnLongClickListener false
        }
        bar.setOnClickListener { _ ->
            performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_BACK)
            return@setOnClickListener
        }

        val height = context.resources.displayMetrics.heightPixels
        val width = context.resources.displayMetrics.widthPixels
        var minSize = width
        var maxSize = height
        if (height < width) {
            minSize = height
            maxSize = width
        }

        if (isLandscapf) {
            bar.setSize(dp2px(context, 12f), (minSize * 0.6).toInt(), TouchBarView.LEFT)
        } else {
            bar.setSize(dp2px(context, 12f), (minSize * 1.4).toInt(), TouchBarView.LEFT)
        }

        val params = WindowManager.LayoutParams()

        // 类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//6.0+
            params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        params.format = PixelFormat.TRANSLUCENT

        params.width = LayoutParams.WRAP_CONTENT
        params.height = LayoutParams.WRAP_CONTENT

        params.gravity = Gravity.START or Gravity.BOTTOM
        params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_FULLSCREEN or LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_LAYOUT_NO_LIMITS
        // WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        // LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE

        mWindowManager!!.addView(view, params)

        return view
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setRightView(context: AccessibilityService): View {
        val view = LayoutInflater.from(context).inflate(R.layout.fw_vitual_touch_bar, null)

        val bar = view.findViewById<TouchBarView>(R.id.bottom_touch_bar)

        bar.setOnLongClickListener { _ ->
            performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
            return@setOnLongClickListener false
        }
        bar.setOnClickListener { _ ->
            performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_BACK)
            return@setOnClickListener
        }

        val height = context.resources.displayMetrics.heightPixels
        val width = context.resources.displayMetrics.widthPixels
        var minSize = width
        var maxSize = height
        if (height < width) {
            minSize = height
            maxSize = width
        }

        if (isLandscapf) {
            bar.setSize(dp2px(context, 12f), (minSize * 0.6).toInt(), TouchBarView.RIGHT)
        } else {
            bar.setSize(dp2px(context, 12f), (minSize * 1.4).toInt(), TouchBarView.RIGHT)
        }

        val params = WindowManager.LayoutParams()

        // 类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//6.0+
            params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        params.format = PixelFormat.TRANSLUCENT

        params.width = LayoutParams.WRAP_CONTENT
        params.height = LayoutParams.WRAP_CONTENT

        params.gravity = Gravity.END or Gravity.BOTTOM
        params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_FULLSCREEN or LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_LAYOUT_NO_LIMITS
        // WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        // LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE

        mWindowManager!!.addView(view, params)

        return view
    }

    companion object {
        private var mWindowManager: WindowManager? = null
    }

    init {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(mContext)) {
            Toast.makeText(mContext, "你开启了Scene按键模拟（虚拟导航条）功能，但是未授予“显示悬浮窗/在应用上层显示”权限", Toast.LENGTH_LONG).show()
        } else {
            // 获取WindowManager
            mWindowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            try {
                this.bottomView = setBottomView(mContext!!)
                this.leftView = setLeftView(mContext!!)
                this.rightView = setRightView(mContext!!)
            } catch (ex: Exception) {
                Log.d("异常", "" + ex.message)
                Toast.makeText(mContext, "启动虚拟导航手势失败！", Toast.LENGTH_LONG).show()
            }
        }
    }
}