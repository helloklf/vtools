package com.omarea.vtools.popup

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.TextView
import android.widget.Toast
import com.omarea.library.shell.*
import com.omarea.store.SpfConfig
import com.omarea.vtools.R
import java.util.*

class FloatMonitorGame(private val mContext: Context) {
    private var timer: Timer? = null
    private var startMonitorTime = 0L
    private var cpuLoadUtils = CpuLoadUtils()
    private var CpuFrequencyUtil = CpuFrequencyUtils()

    private val globalSPF = mContext.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

    /**
     * 显示弹出框
     * @param context
     */
    fun showPopupWindow() {
        if (show!!) {
            return
        }
        startMonitorTime = System.currentTimeMillis()
        if (batteryManager == null) {
            batteryManager = mContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        }

        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(mContext)) {
            Toast.makeText(mContext, mContext.getString(R.string.permission_float), Toast.LENGTH_LONG).show()
            return
        }

        show = true
        mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        mView = setUpView(mContext)

        val params = LayoutParams()
        val monitorStorage = mContext.getSharedPreferences("float_monitor2_storage", Context.MODE_PRIVATE)

        // 类型
        params.type = LayoutParams.TYPE_SYSTEM_ALERT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//6.0+
            params.type = LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            params.type = LayoutParams.TYPE_SYSTEM_ALERT
        }
        params.format = PixelFormat.TRANSLUCENT

        params.width = LayoutParams.WRAP_CONTENT
        params.height = LayoutParams.WRAP_CONTENT

        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.x = monitorStorage.getInt("x", 0)
        params.y = monitorStorage.getInt("y", 0)

        params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_FULLSCREEN

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val navHeight = 0
        if (navHeight > 0) {
            val display = mWindowManager!!.getDefaultDisplay()
            val p = Point()
            display.getRealSize(p)
            params.y = -navHeight
            params.x = 0
        }
        mWindowManager!!.addView(mView, params)

        startTimer()
    }

    private fun stopTimer() {
        if (this.timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    private fun subFreqStr(freq: String?): String {
        if (freq == null) {
            return ""
        }
        if (freq.length > 3) {
            return freq.substring(0, freq.length - 3)
        } else if (freq.isEmpty()) {
            return "0"
        } else {
            return freq
        }
    }

    private var view: View? = null
    private var cpuLoadTextView: TextView? = null
    private var gpuLoadTextView: TextView? = null
    private var gpuPanel: View? = null
    private var temperaturePanel: View? = null
    private var temperatureText: TextView? = null
    private var fpsText: TextView? = null

    private var activityManager: ActivityManager? = null
    private var myHandler = Handler(Looper.getMainLooper())
    private var batteryUnit = BatteryUtils()
    private val info = ActivityManager.MemoryInfo()
    private var coreCount = -1
    private var clusters = ArrayList<Array<String>>()
    private var clustersFreq = ArrayList<String>()

    private val fpsUtils = FpsUtils()
    private var batteryManager: BatteryManager? = null

    private fun updateInfo() {
        if (coreCount < 1) {
            coreCount = CpuFrequencyUtil.getCoreCount()
            clusters = CpuFrequencyUtil.getClusterInfo()
        }
        clustersFreq.clear()
        for (coreIndex in 0 until clusters.size) {
            clustersFreq.add(CpuFrequencyUtil.getCurrentFrequency(coreIndex))
        }
        val loads = cpuLoadUtils.cpuLoad
        val gpuLoad = GpuUtils.getGpuLoad()

        var maxFreq = 0
        for (item in clustersFreq) {
            if (item.isNotEmpty()) {
                try {
                    val freq = item.toInt()
                    if (freq > maxFreq) {
                        maxFreq = freq
                    }
                } catch (ex: Exception) {
                }
            }
        }

        activityManager!!.getMemoryInfo(info)

        var cpuLoad = cpuLoadUtils.cpuLoadSum
        if (cpuLoad < 0) {
            cpuLoad = 0.toDouble()
        }

        val batteryStatus = batteryUnit.getBatteryTemperature()

        val fps = fpsUtils.currentFps

        myHandler.post {
            cpuLoadTextView?.text = cpuLoad.toInt().toString() + "%"
            if (gpuLoad > -1) {
                gpuLoadTextView?.text = gpuLoad.toInt().toString() + "%"
            } else {
                gpuLoadTextView?.text = "--"
            }

            temperatureText!!.setText(batteryStatus.temperature.toString())
            if (fps != null) {
                fpsText?.text = fps.toString()
            }
        }
    }

    private fun startTimer() {
        stopTimer()
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                updateInfo()
            }
        }, 0, 1500)
        updateInfo()
    }

    /**
     * 隐藏弹出框
     */
    fun hidePopupWindow() {
        stopTimer()
        if (show!! && null != mView) {
            mWindowManager!!.removeView(mView)
            mView = null
            show = false
        }
    }

    @SuppressLint("ApplySharedPref", "ClickableViewAccessibility")
    private fun setUpView(context: Context): View {
        view = LayoutInflater.from(context).inflate(R.layout.fw_monitor_game, null)
        gpuPanel = view!!.findViewById(R.id.fw_gpu)
        temperaturePanel = view!!.findViewById(R.id.fw_battery)

        cpuLoadTextView = view!!.findViewById(R.id.fw_cpu_load)
        gpuLoadTextView = view!!.findViewById(R.id.fw_gpu_load)
        temperatureText = view!!.findViewById(R.id.fw_battery_temp)
        fpsText = view!!.findViewById(R.id.fw_fps)

        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        return view!!
    }

    companion object {
        private var mWindowManager: WindowManager? = null
        public var show: Boolean? = false

        @SuppressLint("StaticFieldLeak")
        private var mView: View? = null
    }
}