package com.omarea.vboot

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.*
import com.omarea.shared.ModeList
import com.omarea.shared.SpfConfig
import com.omarea.shared.helper.NotifyHelper

/**
 * 弹窗辅助类
 *
 * @ClassName WindowUtils
 */
class FloatPowercfgSelector {
    private var mView: View? = null
    private var mContext: Context? = null
    private var modeList = ModeList()

    /**
     * 显示弹出框
     *
     * @param context
     */
    fun showPopupWindow(context: Context, packageName: String) {
        if (isShown!!) {
            return
        }

        isShown = true
        // 获取应用的Context
        this.mContext = context.applicationContext
        // 获取WindowManager
        mWindowManager = mContext!!
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager

        this.mView = setUpView(context, packageName)

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
        params.height = LayoutParams.MATCH_PARENT

        params.gravity = Gravity.CENTER

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
    private fun setUpView(context: Context, packageName: String): View {
        val view = LayoutInflater.from(context).inflate(R.layout.fw_powercfg_selector, null)


        val spfPowercfg = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
        val mode = spfPowercfg.getString(packageName, "balance")

        try {
            val pm = context.packageManager
            val packageInfo = pm.getPackageInfo(packageName, 0)
            (view.findViewById<View>(R.id.fw_title) as TextView).text = packageInfo.applicationInfo.loadLabel(pm).toString()
        } catch (ex: Exception) {
            (view.findViewById<View>(R.id.fw_title) as TextView).text = packageName
        }

        val btn_powersave = view.findViewById<TextView>(R.id.btn_powersave)
        val btn_defaultmode = view.findViewById<TextView>(R.id.btn_defaultmode)
        val btn_gamemode = view.findViewById<TextView>(R.id.btn_gamemode)
        val btn_fastmode = view.findViewById<TextView>(R.id.btn_fastmode)

        var selectedMode = mode
        val updateUI = Runnable {
            btn_powersave.text = "省电"
            btn_defaultmode.text = "均衡"
            btn_gamemode.text = "游戏"
            btn_fastmode.text = "极速"
            when (selectedMode) {
                modeList.BALANCE -> btn_defaultmode.text = "均衡 √"
                modeList.PERFORMANCE -> btn_gamemode.text = "游戏 √"
                modeList.POWERSAVE -> btn_powersave!!.text = "省电 √"
                modeList.FAST -> btn_fastmode!!.text = "极速 √"
            }
        }
        btn_powersave.setOnClickListener {
            selectedMode = modeList.POWERSAVE
            updateUI.run()
        }
        btn_defaultmode.setOnClickListener {
            selectedMode = modeList.BALANCE
            updateUI.run()
        }
        btn_gamemode.setOnClickListener {
            selectedMode = modeList.PERFORMANCE
            updateUI.run()
        }
        btn_fastmode.setOnClickListener {
            selectedMode = modeList.FAST
            updateUI.run()
        }


        val positiveBtn = view.findViewById<View>(R.id.positiveBtn) as Button
        positiveBtn.setOnClickListener {
            // 隐藏弹窗
            hidePopupWindow()
            if (selectedMode != mode) {
                modeList.executePowercfgModeOnce(selectedMode, packageName)
                modeList.setCurrent(selectedMode, packageName)
            }
            it.postDelayed(Runnable {
                NotifyHelper(context, true).notify()
            }, 1000)
            if (view.findViewById<CheckBox>(R.id.save_config).isChecked) {
                spfPowercfg.edit().putString(packageName, selectedMode).commit()
            }

            //Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            //context.startActivity(intent);
        }

        val negativeBtn = view.findViewById<View>(R.id.negativeBtn) as Button
        negativeBtn.setOnClickListener { hidePopupWindow() }

        // 点击窗口外部区域可消除
        // 这点的实现主要将悬浮窗设置为全屏大小，外层有个透明背景，中间一部分视为内容区域
        // 所以点击内容区域外部视为点击悬浮窗外部
        val popupWindowView = view.findViewById<View>(R.id.popup_window)// 非透明的内容区域

        view.setOnTouchListener { v, event ->
            val x = event.x.toInt()
            val y = event.y.toInt()
            val rect = Rect()
            popupWindowView.getGlobalVisibleRect(rect)
            if (!rect.contains(x, y)) {
                hidePopupWindow()
            }
            false
        }

        // 点击back键可消除
        view.setOnKeyListener { v, keyCode, event ->
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    hidePopupWindow()
                    true
                }
                else -> false
            }
        }
        updateUI.run()
        return view

    }

    companion object {
        private var mWindowManager: WindowManager? = null
        var isShown: Boolean? = false
    }
}