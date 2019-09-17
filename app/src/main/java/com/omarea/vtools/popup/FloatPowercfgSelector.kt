package com.omarea.vtools.popup

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.*
import com.omarea.permissions.NotificationListener
import com.omarea.scene_mode.AlwaysNotification
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.AppConfigStore
import com.omarea.store.SpfConfig
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.vtools.R

/**
 * 弹窗辅助类
 *
 * @ClassName WindowUtils
 */
class FloatPowercfgSelector {
    private var mView: View? = null
    private var mContext: Context? = null
    private var modeList = ModeSwitcher()

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this.mContext)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//6.0+
                    params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
            } else {
                params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            }
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        // 设置flag

        val flags = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        flags.and(WindowManager.LayoutParams.FLAG_FULLSCREEN)
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
        params.windowAnimations = R.style.windowAnim

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

    /**
     * 重启辅助服务
     */
    private fun reStartService(app: String, mode: String) {
        if (AccessibleServiceHelper().serviceRunning(mContext!!)) {
            val intent = Intent(mContext!!.getString(R.string.scene_appchange_action))
            intent.putExtra("app", app)
            intent.putExtra("mode", mode)
            mContext!!.sendBroadcast(intent)
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun setUpView(context: Context, packageName: String): View {
        val store = AppConfigStore(context)
        val appConfig = store.getAppConfig(packageName)
        var needKeyCapture = store.needKeyCapture()

        val view = LayoutInflater.from(context).inflate(R.layout.fw_powercfg_selector, null)

        val spfPowercfg = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
        val globalSPF = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        val dynamic = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, true)
        val defaultMode = globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, "balance")
        var selectedMode = if (dynamic) spfPowercfg.getString(packageName, defaultMode) else modeList.getCurrentPowerMode()

        try {
            val pm = context.packageManager
            val packageInfo = pm.getPackageInfo(packageName, 0)
            (view.findViewById<View>(R.id.fw_title) as TextView).text = packageInfo.applicationInfo.loadLabel(pm).toString()
        } catch (ex: Exception) {
            (view.findViewById<View>(R.id.fw_title) as TextView).text = packageName
        }

        view.findViewById<CheckBox>(R.id.fw_dynamic_state).isChecked = dynamic

        if (!context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false)) {
            view.findViewById<LinearLayout>(R.id.popup_window).setBackgroundColor(Color.WHITE)
            view.findViewById<TextView>(R.id.fw_title).setTextColor(Color.BLACK)
        }

        val btn_powersave = view.findViewById<TextView>(R.id.btn_powersave)
        val btn_defaultmode = view.findViewById<TextView>(R.id.btn_defaultmode)
        val btn_gamemode = view.findViewById<TextView>(R.id.btn_gamemode)
        val btn_fastmode = view.findViewById<TextView>(R.id.btn_fastmode)
        val fw_float_monitor = view.findViewById<ImageButton>(R.id.fw_float_monitor)
        val fw_screen_record = view.findViewById<ImageButton>(R.id.fw_screen_record)

        val updateUI = Runnable {
            btn_powersave.setTextColor(0x66ffffff)
            btn_defaultmode.setTextColor(0x66ffffff)
            btn_gamemode.setTextColor(0x66ffffff)
            btn_fastmode.setTextColor(0x66ffffff)
            when (selectedMode) {
                ModeSwitcher.BALANCE -> btn_defaultmode.setTextColor(Color.WHITE)
                ModeSwitcher.PERFORMANCE -> btn_gamemode.setTextColor(Color.WHITE)
                ModeSwitcher.POWERSAVE -> btn_powersave.setTextColor(Color.WHITE)
                ModeSwitcher.FAST -> btn_fastmode.setTextColor(Color.WHITE)
            }
        }
        val switchMode = Runnable {
            updateUI.run()
            modeList.executePowercfgMode(selectedMode, packageName)
            if (dynamic) {
                if (selectedMode == defaultMode) {
                    spfPowercfg.edit().remove(packageName).commit()
                } else {
                    spfPowercfg.edit().putString(packageName, selectedMode).commit()
                }
                reStartService(packageName, selectedMode)
            }

            AlwaysNotification(context, true).notify()
        }

        btn_powersave.setOnClickListener {
            selectedMode = ModeSwitcher.POWERSAVE
            switchMode.run()
        }
        btn_defaultmode.setOnClickListener {
            selectedMode = ModeSwitcher.BALANCE
            switchMode.run()
        }
        btn_gamemode.setOnClickListener {
            selectedMode = ModeSwitcher.PERFORMANCE
            switchMode.run()
        }
        btn_fastmode.setOnClickListener {
            selectedMode = ModeSwitcher.FAST
            switchMode.run()
        }
        val fw_app_light = view.findViewById<CheckBox>(R.id.fw_app_light)
        fw_app_light.isChecked = appConfig.aloneLight
        fw_app_light.setOnClickListener {
            val isChecked = (it as CheckBox).isChecked
            appConfig.aloneLight = isChecked
            store.setAppConfig(appConfig)

            val intent = Intent(context.getString(R.string.scene_appchange_action))
            intent.putExtra("app", packageName)
            context.sendBroadcast(intent)
        }
        val fw_app_dis_notice = view.findViewById<CheckBox>(R.id.fw_app_dis_notice)
        fw_app_dis_notice.isChecked = appConfig.disNotice
        fw_app_dis_notice.setOnClickListener {
            appConfig.disNotice = (it as CheckBox).isChecked
            store.setAppConfig(appConfig)

            val intent = Intent(context.getString(R.string.scene_appchange_action))
            intent.putExtra("app", packageName)
            context.sendBroadcast(intent)
        }

        // 点击禁用通知
        val fw_app_dis_button = view.findViewById<CheckBox>(R.id.fw_app_dis_button)
        fw_app_dis_button.isChecked = appConfig.disButton
        fw_app_dis_button.setOnClickListener {
            val isChecked = (it as CheckBox).isChecked
            if (isChecked) {
                if (!NotificationListener().getPermission(context)) {
                    NotificationListener().setPermission(context)
                    Toast.makeText(context, context.getString(R.string.scene_need_notic_listing), Toast.LENGTH_SHORT).show()
                    it.isChecked = false
                    return@setOnClickListener
                }
            }
            appConfig.disButton = isChecked
            store.setAppConfig(appConfig)
            if (isChecked && !needKeyCapture) {
                context.sendBroadcast(Intent(context.getString(R.string.scene_key_capture_change_action)))
                needKeyCapture = true
            }

            val intent = Intent(context.getString(R.string.scene_appchange_action))
            intent.putExtra("app", packageName)
            context.sendBroadcast(intent)
        }

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

        fw_float_monitor.alpha = if (FloatMonitor.isShown == true) 1f else 0.5f
        fw_float_monitor.setOnClickListener {
            if (FloatMonitor.isShown == true) {
                FloatMonitor(context).hidePopupWindow()
                fw_float_monitor.alpha = 0.3f
            } else {
                FloatMonitor(context).showPopupWindow()
                fw_float_monitor.alpha = 1f
            }
        }

        view.findViewById<ImageButton>(R.id.fw_float_close).setOnClickListener {
            hidePopupWindow()
        }

        updateUI.run()
        return view
    }

    companion object {
        private var mWindowManager: WindowManager? = null
        var isShown: Boolean? = false
    }
}