package com.omarea.vtools.popup

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
import com.omarea.Scene
import com.omarea.data.EventBus
import com.omarea.data.EventType
import com.omarea.library.permissions.NotificationListener
import com.omarea.library.shell.LocationHelper
import com.omarea.library.shell.ProcessUtils
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.SceneConfigStore
import com.omarea.store.SpfConfig
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.vtools.R

/**
 * 弹窗辅助类
 *
 * @ClassName WindowUtils
 */
class FloatPowercfgSelector(context: Context) {
    private val mContext: Context = context.applicationContext
    private var mView: View? = null
    private var modeList = ModeSwitcher()

    /**
     * 显示弹出框
     *
     * @param context
     */
    fun open(packageName: String) {
        if (isShown!!) {
            return
        }

        isShown = true
        // 获取WindowManager
        mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        this.mView = setUpView(mContext, packageName)

        val params = LayoutParams()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this.mContext)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//6.0+
                    params.type = LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    params.type = LayoutParams.TYPE_SYSTEM_ALERT
                }
            } else {
                params.type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            }
        } else {
            params.type = LayoutParams.TYPE_SYSTEM_ALERT
        }

        // 设置flag

        val flags = LayoutParams.FLAG_ALT_FOCUSABLE_IM
        flags.and(LayoutParams.FLAG_FULLSCREEN)
        // | LayoutParams.FLAG_NOT_FOCUSABLE;
        // 如果设置了LayoutParams.FLAG_NOT_FOCUSABLE，弹出的View收不到Back键的事件
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
    fun close() {
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
        if (AccessibleServiceHelper().serviceRunning(mContext)) {
            val intent = Intent(mContext.getString(R.string.scene_appchange_action))
            intent.putExtra("app", app)
            intent.putExtra("mode", mode)
            mContext.sendBroadcast(intent)
        }
    }

    private fun setUpView(context: Context, packageName: String): View {
        val store = SceneConfigStore(context)
        val appConfig = store.getAppConfig(packageName)
        var needKeyCapture = store.needKeyCapture()

        val view = LayoutInflater.from(context).inflate(R.layout.fw_powercfg_selector, null)

        val spfPowercfg = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
        val globalSPF = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        val serviceRunning = AccessibleServiceHelper().serviceRunning(context)
        val dynamic = serviceRunning && globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)
        val defaultMode = globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, "balance")
        var selectedMode = (if (dynamic) spfPowercfg.getString(packageName, defaultMode) else modeList.getCurrentPowerMode())!!
        val modeConfigCompleted = ModeSwitcher().modeConfigCompleted()

        try {
            val pm = context.packageManager
            val packageInfo = pm.getPackageInfo(packageName, 0)
            (view.findViewById<View>(R.id.fw_title) as TextView).text = packageInfo.applicationInfo.loadLabel(pm).toString()
        } catch (ex: Exception) {
            (view.findViewById<View>(R.id.fw_title) as TextView).text = packageName
        }

        // 性能调节（动态响应）
        val fw_dynamic_state = view.findViewById<Switch>(R.id.fw_dynamic_state)
        fw_dynamic_state.isChecked = dynamic
        fw_dynamic_state.isEnabled = serviceRunning && modeConfigCompleted
        fw_dynamic_state.setOnClickListener {
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, (it as Switch).isChecked).apply()
            reStartService()
        }

        if (!context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false)) {
            view.findViewById<LinearLayout>(R.id.popup_window).setBackgroundColor(Color.WHITE)
            view.findViewById<TextView>(R.id.fw_title).setTextColor(Color.BLACK)
        }

        val btn_powersave = view.findViewById<TextView>(R.id.btn_powersave)
        val btn_defaultmode = view.findViewById<TextView>(R.id.btn_defaultmode)
        val btn_gamemode = view.findViewById<TextView>(R.id.btn_gamemode)
        val btn_fastmode = view.findViewById<TextView>(R.id.btn_fastmode)
        val fw_float_monitor = view.findViewById<ImageButton>(R.id.fw_float_monitor)
        val fw_float_task = view.findViewById<ImageButton>(R.id.fw_float_task)

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
                if (!packageName.equals(context.packageName)) {
                    if (selectedMode == defaultMode) {
                        spfPowercfg.edit().remove(packageName).apply()
                    } else {
                        spfPowercfg.edit().putString(packageName, selectedMode).apply()
                    }
                    reStartService(packageName, selectedMode)
                }
                EventBus.publish(EventType.SCENE_MODE_ACTION)
            }
        }

        if (modeConfigCompleted) {
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
        }

        val fw_app_light = view.findViewById<CheckBox>(R.id.fw_app_light)
        fw_app_light.isChecked = appConfig.aloneLight
        fw_app_light.setOnClickListener {
            val isChecked = (it as CheckBox).isChecked
            appConfig.aloneLight = isChecked
            store.setAppConfig(appConfig)

            notifyAppConfigChanged(context, packageName)
        }
        val fw_app_dis_notice = view.findViewById<CheckBox>(R.id.fw_app_dis_notice)
        fw_app_dis_notice.isChecked = appConfig.disNotice
        fw_app_dis_notice.setOnClickListener {
            val isChecked = (it as CheckBox).isChecked

            if (isChecked) {
                if (!NotificationListener().getPermission(context)) {
                    NotificationListener().setPermission(context)
                    Toast.makeText(context, context.getString(R.string.scene_need_notic_listing), Toast.LENGTH_SHORT).show()
                    it.isChecked = false
                    return@setOnClickListener
                }
            }
            appConfig.disNotice = isChecked
            store.setAppConfig(appConfig)

            notifyAppConfigChanged(context, packageName)
        }

        // 点击禁用按键
        val fw_app_dis_button = view.findViewById<CheckBox>(R.id.fw_app_dis_button)
        fw_app_dis_button.isChecked = appConfig.disButton
        fw_app_dis_button.setOnClickListener {
            val isChecked = (it as CheckBox).isChecked
            appConfig.disButton = isChecked
            store.setAppConfig(appConfig)
            if (isChecked && !needKeyCapture) {
                context.sendBroadcast(Intent(context.getString(R.string.scene_service_config_change_action)))
                needKeyCapture = true
            }

            notifyAppConfigChanged(context, packageName)
        }

        // GPS开关
        val fw_app_gps = view.findViewById<CheckBox>(R.id.fw_app_gps).apply {
            isChecked = appConfig.gpsOn
            setOnClickListener {
                val isChecked = (it as CheckBox).isChecked
                appConfig.gpsOn = isChecked
                store.setAppConfig(appConfig)
                if (isChecked) {
                    LocationHelper().enableGPS()
                } else {
                    LocationHelper().disableGPS()
                }
                notifyAppConfigChanged(context, packageName)
            }
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
                close()
            }
            false
        }

        // 点击back键可消除
        view.setOnKeyListener { v, keyCode, event ->
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    close()
                    true
                }
                else -> false
            }
        }

        // 性能监视悬浮窗开关
        fw_float_monitor.alpha = if (FloatMonitor.show == true) 1f else 0.5f
        fw_float_monitor.setOnClickListener {

            if (FloatMonitor.show == true) {
                FloatMonitor(context).hidePopupWindow()
                fw_float_monitor.alpha = 0.3f
            } else {
                FloatMonitor(context).showPopupWindow()
                fw_float_monitor.alpha = 1f
            }
        }

        // 进程管理器
        fw_float_task.alpha = if (FloatTaskManager.show) 1f else 0.5f
        fw_float_task.setOnClickListener {
            if (FloatTaskManager.show) {
                FloatTaskManager(context).hidePopupWindow()
                fw_float_task.alpha = 0.3f
            } else {
                if (ProcessUtils().supported(context)) {
                    FloatTaskManager(context).showPopupWindow()
                    fw_float_task.alpha = 1f
                } else {
                    Scene.toast("进程管理器暂未兼容你的手机！", Toast.LENGTH_SHORT)
                }
            }
        }

        if (!serviceRunning || packageName.equals(context.packageName)) {
            fw_app_light.isEnabled = false
            fw_app_dis_button.isEnabled = false
            fw_app_dis_notice.isEnabled = false
            fw_app_gps.isEnabled = false
        }

        view.findViewById<ImageButton>(R.id.fw_float_close).setOnClickListener {
            close()
        }

        updateUI.run()
        return view
    }

    private fun notifyAppConfigChanged(context: Context, packageName: String) {
        val intent = Intent(context.getString(R.string.scene_appchange_action))
        intent.putExtra("app", packageName)
        context.sendBroadcast(intent)
    }

    /**
     * 重启辅助服务
     */
    private fun reStartService() {
        if (AccessibleServiceHelper().serviceRunning(mContext)) {
            mContext.sendBroadcast(Intent(mContext.getString(R.string.scene_change_action)))
        }
    }

    companion object {
        private var mWindowManager: WindowManager? = null
        var isShown: Boolean? = false
    }
}