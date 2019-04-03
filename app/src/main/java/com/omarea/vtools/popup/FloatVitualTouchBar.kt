package com.omarea.vtools.popup

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.LinearLayout
import android.widget.Toast
import com.omarea.shared.SpfConfig
import com.omarea.shell.KeepShellPublic
import com.omarea.vtools.R
import java.lang.Exception

/**
 * 弹窗辅助类
 *
 * @ClassName WindowUtils
 */
class FloatVitualTouchBar(context: AccessibilityService) {
    private var bottomView: View? = null
    private var leftView: View? = null
    private var rightView: View? = null

    private var mContext: AccessibilityService? = context
    private var sharedPreferences: SharedPreferences? = context.getSharedPreferences(SpfConfig.KEY_EVENT_ONTHER_CONFIG_SPF, Context.MODE_PRIVATE)
    private var touchLayout = 0
    private var allowTap = false
    var isLandscapf = false

    private var lastEventTime = 0L
    private var lastEvent = -1
    private var vibratorOn = false
    private var vibrator: Vibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator

    /**
     * 显示弹出框
     *
     * @param context
     */
    private fun showPopupWindow() {
        if (isShown!!) {
            return
        }
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(mContext)) {
            Toast.makeText(mContext, "你开启了Scene按键模拟（虚拟导航条）功能，但是未授予“显示悬浮窗/在应用上层显示”权限", Toast.LENGTH_LONG).show()
            return
        }

        isShown = true
        // 获取WindowManager
        mWindowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        vibratorOn = sharedPreferences!!.getBoolean(SpfConfig.CONFIG_SPF_TOUCH_BAR_VIBRATOR, false)
        allowTap = sharedPreferences!!.getBoolean(SpfConfig.CONFIG_SPF_TOUCH_BAR_TAP, false)

        try {
            this.bottomView = setBottomView(mContext!!)
            if (touchLayout == 2) {
                this.leftView = setLeftView(mContext!!)
                this.rightView = setRightView(mContext!!)
            }
        } catch (ex: Exception) {
            Log.d("异常", ex.message)
        }
    }

    /**
     * 获取导航栏高度
     * @param context
     * @return
     */
    fun getNavBarHeight(context: Context): Int {
        var resourceId: Int
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
        if (isShown!!) {
            if (this.bottomView != null) {
                mWindowManager!!.removeView(this.bottomView)
            }
            if (this.leftView != null) {
                mWindowManager!!.removeView(this.leftView)
            }
            if (this.rightView != null) {
                mWindowManager!!.removeView(this.rightView)
            }
            KeepShellPublic.doCmdSync("wm overscan reset")
            isShown = false
        }
    }

    /**
     * dp转换成px
     */
    private fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    private fun performGlobalAction(context: AccessibilityService, event: Int) {
        if (isLandscapf && (lastEventTime + 1000 < System.currentTimeMillis() || lastEvent != event)) {
            lastEvent = event
            lastEventTime = System.currentTimeMillis()
            Toast.makeText(context, "请重复手势~", Toast.LENGTH_SHORT).show()
        } else {
            if (vibratorOn) {
                vibrator.cancel()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(20, 10), DEFAULT_AMPLITUDE))
                    vibrator.vibrate(VibrationEffect.createOneShot(1, 1))
                } else {
                    vibrator.vibrate(longArrayOf(20, 10), -1)
                }
            }
            context.performGlobalAction(event)
        }
    }

    @SuppressLint("ApplySharedPref", "ClickableViewAccessibility")
    private fun setBottomView(context: AccessibilityService): View {
        val view = LayoutInflater.from(context).inflate(R.layout.fw_vitual_touch_bar, null)

        val bar = view.findViewById<LinearLayout>(R.id.bottom_touch_bar)
        val gust = GestureDetector(context, object : GestureDetector.OnGestureListener {
            // 定义手势动作亮点之间的最小距离
            val FLIP_DISTANCE = dp2px(context, 70f)

            override fun onLongPress(e: MotionEvent?) {
                if (touchLayout != 2) {
                    if (e != null) {
                        if (e.getX() < bar.width * 0.33) {
                        } else if (e.getX() > bar.width * 0.67) {
                        } else {
                            performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                        }
                    }
                }
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
                return false
            }

            override fun onDown(e: MotionEvent?): Boolean {
                return false
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                if (e != null && allowTap) {
                    if (touchLayout != 2) {
                        if (e.getX() < bar.width * 0.33) {
                            if (touchLayout == 1) {
                                performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
                            } else {
                                performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_BACK)
                            }
                        } else if (e.getX() > bar.width * 0.67) {
                            if (touchLayout == 1) {
                                performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_BACK)
                            } else {
                                performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
                            }
                        } else {
                            performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_HOME)
                        }
                    }
                }
                return false
            }

            override fun onShowPress(e: MotionEvent?) {
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                if (touchLayout == 2) {
                    performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_HOME)
                } else {
                    // 如果第一个触点事件的X坐标大于第二个触点事件的X坐标超过FLIP_DISTANCE
                    // 也就是手势从右向左滑
                    if (e1!!.getX() - e2!!.getX() > FLIP_DISTANCE) {
                        if (touchLayout == 1) {
                            performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
                        } else if (touchLayout == 0) {
                            performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_BACK)
                        }
                    }
                    // 如果第二个触点事件的X坐标大于第一个触点事件的X坐标超过FLIP_DISTANCE
                    // 也就是手势从右向左滑
                    else if (e2.getX() - e1.getX() > FLIP_DISTANCE) {
                        if (touchLayout == 1) {
                            performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_BACK)
                        } else {
                            performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
                        }
                    } else if (e1.getY() - e2.getY() > FLIP_DISTANCE) {
                        val bw = bar.width
                        var event = AccessibilityService.GLOBAL_ACTION_HOME
                        if (e1.getX() < bw * 0.33 && e1.getX() < bw * 0.33) {
                            if (touchLayout == 1) {
                                event = AccessibilityService.GLOBAL_ACTION_RECENTS
                            } else {
                                event = AccessibilityService.GLOBAL_ACTION_BACK
                            }
                        } else if (e1.getX() > bw * 0.7 && e1.getX() > bw * 0.7) {
                            if (touchLayout == 1) {
                                event = AccessibilityService.GLOBAL_ACTION_BACK
                            } else {
                                event = AccessibilityService.GLOBAL_ACTION_RECENTS
                            }
                        }
                        performGlobalAction(context, event)
                    } else {
                        return false;
                    }
                }
                return false;
            }
        })
        bar.setOnTouchListener { v, event ->
            gust.onTouchEvent(event)
        }

        val params = WindowManager.LayoutParams()

        // 类型
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        // 设置window type
        //params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//6.0+
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        params.format = PixelFormat.TRANSLUCENT

        params.width = LayoutParams.MATCH_PARENT
        params.height = dp2px(context, 8f)

        params.gravity = Gravity.BOTTOM
        params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_FULLSCREEN or LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_LAYOUT_NO_LIMITS
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
            if (touchLayout == 2) {
                KeepShellPublic.doCmdSync("wm overscan 0,0,0,-" + navHeight)
            }
        } else {
        }
        mWindowManager!!.addView(view, params)

        return view
    }

    private fun setLeftView(context: AccessibilityService): View {
        val view = LayoutInflater.from(context).inflate(R.layout.fw_vitual_touch_bar, null)

        val bar = view.findViewById<LinearLayout>(R.id.bottom_touch_bar)
        val gust = GestureDetector(context, object : GestureDetector.OnGestureListener {
            // 定义手势动作亮点之间的最小距离
            val FLIP_DISTANCE = dp2px(context, 70f)

            override fun onLongPress(e: MotionEvent?) {
                if (e != null) {
                    performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_BACK)
                }
            }
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
                return false
            }
            override fun onDown(e: MotionEvent?): Boolean {
                return false
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                return false
            }

            override fun onShowPress(e: MotionEvent?) {
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                // 如果第一个触点事件的X坐标大于第二个触点事件的X坐标超过FLIP_DISTANCE
                // 也就是手势从右向左滑
                if (e2!!.getX() - e1!!.getX() > FLIP_DISTANCE) {
                    performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_BACK)
                }
                //return true;
                return false;
            }
        })
        bar.setOnTouchListener { v, event ->
            gust.onTouchEvent(event)
        }

        val params = WindowManager.LayoutParams()

        // 类型
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        // 设置window type
        //params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//6.0+
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        params.format = PixelFormat.TRANSLUCENT

        params.width = dp2px(context, 12f)
        params.height = (context.resources.displayMetrics.widthPixels * 1.2).toInt()

        params.gravity = Gravity.START or Gravity.BOTTOM
        params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_FULLSCREEN or LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_LAYOUT_NO_LIMITS
        // WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        // LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE

        mWindowManager!!.addView(view, params)

        return view
    }

    private fun setRightView(context: AccessibilityService): View {
        val view = LayoutInflater.from(context).inflate(R.layout.fw_vitual_touch_bar, null)

        val bar = view.findViewById<LinearLayout>(R.id.bottom_touch_bar)
        val gust = GestureDetector(context, object : GestureDetector.OnGestureListener {
            // 定义手势动作亮点之间的最小距离
            val FLIP_DISTANCE = dp2px(context, 70f)

            override fun onLongPress(e: MotionEvent?) {
                if (e != null) {
                    performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
                }
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
                return false
            }

            override fun onDown(e: MotionEvent?): Boolean {
                return false
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                return false
            }

            override fun onShowPress(e: MotionEvent?) {
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                if (e1!!.x - e2!!.x > FLIP_DISTANCE) {
                    performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_BACK)
                    // performGlobalAction(context, AccessibilityService.GLOBAL_ACTION_RECENTS)
                }
                return false;
            }
        })
        bar.setOnTouchListener { v, event ->
            gust.onTouchEvent(event)
        }

        val params = WindowManager.LayoutParams()

        // 类型
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        // 设置window type
        //params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//6.0+
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        params.format = PixelFormat.TRANSLUCENT

        params.width = dp2px(context, 12f)
        params.height = (context.resources.displayMetrics.widthPixels * 1.2).toInt()

        params.gravity = Gravity.END or Gravity.BOTTOM
        params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_FULLSCREEN or LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_LAYOUT_NO_LIMITS
        // WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        // LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE

        mWindowManager!!.addView(view, params)

        return view
    }

    companion object {
        private var mWindowManager: WindowManager? = null
        var isShown: Boolean? = false
    }

    init {
        touchLayout = sharedPreferences!!.getInt(SpfConfig.CONFIG_SPF_TOUCH_BAR_MAP, 0)
        sharedPreferences!!.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == SpfConfig.CONFIG_SPF_TOUCH_BAR_MAP) {
                touchLayout = sharedPreferences!!.getInt(SpfConfig.CONFIG_SPF_TOUCH_BAR_MAP, 0)
            } else if (key == SpfConfig.CONFIG_SPF_TOUCH_BAR) {
                if (sharedPreferences!!.getInt(SpfConfig.CONFIG_SPF_TOUCH_BAR_MAP, 0) == 0) {
                    hidePopupWindow()
                } else {
                    showPopupWindow()
                }
            }
        }
        if (!sharedPreferences!!.getBoolean(SpfConfig.CONFIG_SPF_TOUCH_BAR, false)) {
            hidePopupWindow()
        } else {
            showPopupWindow()
        }
    }
}