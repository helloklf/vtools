package com.omarea.vtools

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationEffect.DEFAULT_AMPLITUDE
import android.os.Vibrator
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.Button
import android.widget.Toast
import com.omarea.shared.SpfConfig

/**
 * 弹窗辅助类
 *
 * @ClassName WindowUtils
 */
class FloatVitualTouchBar// 获取应用的Context
(context: AccessibilityService) {
    private var mView: View? = null
    private var mContext: AccessibilityService? = context
    private var sharedPreferences: SharedPreferences? = null
    private var reversalLayout = false

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
        mWindowManager = mContext!!
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager

        this.mView = setUpView(mContext!!)

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
        // WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

        // 设置flag

        val flags = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        // | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        // 如果设置了WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE，弹出的View收不到Back键的事件
        params.flags = flags
        // 不设置这个弹出框的透明遮罩显示为黑色
        params.format = PixelFormat.TRANSLUCENT
        // FLAG_NOT_TOUCH_MODAL不阻塞事件传递到后面的窗口
        // 设置 FLAG_NOT_FOCUSABLE 悬浮窗口较小时，后面的应用图标由不可长按变为可长按
        // 不设置这个flag的话，home页的划屏会有问题

        params.width = LayoutParams.MATCH_PARENT
        params.height = LayoutParams.WRAP_CONTENT

        params.gravity = Gravity.BOTTOM
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        mWindowManager!!.addView(mView, params)
    }

    /**
     * 隐藏弹出框
     */
    fun hidePopupWindow() {
        if (isShown!! &&
                null != this.mView) {
            mWindowManager!!.removeView(mView)
            isShown = false
        }
    }


    @SuppressLint("ApplySharedPref")
    private fun setUpView(context: AccessibilityService): View {
        val view = LayoutInflater.from(context).inflate(R.layout.fw_vitual_touch_bar, null)

        val btn1 = view.findViewById<Button>(R.id.vitual_touch_bar_1)
        val btn2 = view.findViewById<Button>(R.id.vitual_touch_bar_2)
        val btn3 = view.findViewById<Button>(R.id.vitual_touch_bar_3)

        val vibrator = Runnable {
            val vibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(20, 10), DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(longArrayOf(20, 10), -1)
            }
        }

        btn1.setOnClickListener {
            vibrator.run()
            if (!reversalLayout) {
                context.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            } else {
                context.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            }
        }
        btn1.setOnLongClickListener {
            vibrator.run()
            if (reversalLayout) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                }
            }
            true
        }
        btn2.setOnClickListener {
            vibrator.run()
            context.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        }
        btn2.setOnLongClickListener {
            vibrator.run()
            context.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            true
        }
        btn3.setOnClickListener {
            vibrator.run()
            if (!reversalLayout) {
                context.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            } else {
                context.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            }
        }
        btn3.setOnLongClickListener {
            vibrator.run()
            if (!reversalLayout) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                }
            }
            true
        }
        return view
    }

    companion object {
        private var mWindowManager: WindowManager? = null
        var isShown: Boolean? = false
    }

    init {
        sharedPreferences = context.getSharedPreferences(SpfConfig.KEY_EVENT_ONTHER_CONFIG_SPF, Context.MODE_PRIVATE)
        reversalLayout = sharedPreferences!!.getBoolean(SpfConfig.CONFIG_SPF_TOUCH_BAR_MAP, false)
        sharedPreferences!!.registerOnSharedPreferenceChangeListener(object : SharedPreferences.OnSharedPreferenceChangeListener {
            override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
                if (key == SpfConfig.CONFIG_SPF_TOUCH_BAR_MAP) {
                    reversalLayout = sharedPreferences!!.getBoolean(SpfConfig.CONFIG_SPF_TOUCH_BAR_MAP, false)
                } else if (key == SpfConfig.CONFIG_SPF_TOUCH_BAR) {
                    if (!sharedPreferences!!.getBoolean(SpfConfig.CONFIG_SPF_TOUCH_BAR, false)) {
                        hidePopupWindow()
                    } else {
                        showPopupWindow()
                    }
                }
            }
        })
        if (!sharedPreferences!!.getBoolean(SpfConfig.CONFIG_SPF_TOUCH_BAR, false)) {
            hidePopupWindow()
        } else {
            showPopupWindow()
        }
    }
}