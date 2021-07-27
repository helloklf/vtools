package com.omarea.vtools.popup

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
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
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.omarea.Scene
import com.omarea.data.GlobalStatus
import com.omarea.library.shell.*
import com.omarea.store.SpfConfig
import com.omarea.ui.FloatMonitorBatteryView
import com.omarea.ui.FloatMonitorChartView
import com.omarea.vtools.R
import java.util.*

class FloatMonitor(private val mContext: Context) {
    private var cpuLoadUtils = CpuLoadUtils()
    private var CpuFrequencyUtil = CpuFrequencyUtils()

    private val globalSPF = mContext.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

    /**
     * 显示弹出框
     * @param context
     */
    fun showPopupWindow(): Boolean {
        if (show!!) {
            return true
        }
        if (batteryManager == null) {
            batteryManager = mContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        }

        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(mContext)) {
            Toast.makeText(mContext, mContext.getString(R.string.permission_float), Toast.LENGTH_LONG).show()
            return false
        }

        show = true
        // 获取WindowManager
        mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val view = setUpView(mContext)

        val params = LayoutParams()
        val monitorStorage = mContext.getSharedPreferences("float_monitor_storage", Context.MODE_PRIVATE)

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

        params.gravity = Gravity.TOP or Gravity.LEFT
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
        } else {
        }

        try {
            mWindowManager!!.addView(view, params)
            mView = view

            // 添加触摸事件
            view.setOnTouchListener(object : View.OnTouchListener {
                private var isTouchDown = false
                private var touchStartX = 0f
                private var touchStartY = 0f
                private var touchStartRawX = 0f
                private var touchStartRawY = 0f
                private var touchStartTime = 0L
                private var lastClickTime = 0L

                private fun onClick() {
                    try {
                        if (System.currentTimeMillis() - lastClickTime < 300) {
                            hidePopupWindow()
                        } else {
                            lastClickTime = System.currentTimeMillis()
                        }
                    } catch (ex: Exception) {}
                }

                @SuppressLint("ClickableViewAccessibility")
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    if (event != null) {
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                touchStartX = event.getX()
                                touchStartY = event.getY()
                                touchStartRawX = event.rawX
                                touchStartRawY = event.rawY
                                isTouchDown = true
                                touchStartTime = System.currentTimeMillis()
                            }
                            MotionEvent.ACTION_MOVE -> {
                                if (isTouchDown) {
                                    params.x = (event.rawX - touchStartX).toInt()
                                    params.y = (event.rawY - touchStartY).toInt()
                                    mWindowManager!!.updateViewLayout(v, params)
                                }
                            }
                            MotionEvent.ACTION_UP -> {
                                if (System.currentTimeMillis() - touchStartTime < 180) {
                                    if (Math.abs(event.rawX - touchStartRawX) < 15 && Math.abs(event.rawY - touchStartRawY) < 15) {
                                        onClick()
                                    } else {
                                        monitorStorage.edit().putInt("x", params.x).putInt("y", params.y).apply()
                                    }
                                }
                                isTouchDown = false
                                if (Math.abs(event.rawX - touchStartRawX) > 15 || Math.abs(event.rawY - touchStartRawY) > 15) {
                                    return true
                                }
                            }
                            MotionEvent.ACTION_OUTSIDE,
                            MotionEvent.ACTION_CANCEL -> {
                                isTouchDown = false
                            }
                        }
                    }
                    return false
                }
            })

            startTimer()

            return true
        } catch (ex: Exception) {
            Scene.toast("FloatMonitor Error\n" + ex.message)
            return false
        }
    }

    private fun stopTimer() {
        if (timer != null) {
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
    private var cpuChart: FloatMonitorChartView? = null
    private var cpuFreqText: TextView? = null
    private var gpuChart: FloatMonitorChartView? = null
    private var gpuPanel: View? = null
    private var gpuFreqText: TextView? = null
    private var temperaturePanel: View? = null
    private var temperatureChart: FloatMonitorBatteryView? = null
    private var temperatureText: TextView? = null
    private var batteryLevelText: TextView? = null
    private var chargerView: ImageView? = null
    private var otherInfo: TextView? = null

    private var activityManager: ActivityManager? = null
    private var myHandler = Handler(Looper.getMainLooper())
    private val info = ActivityManager.MemoryInfo()

    private var totalMem = 0
    private var availMem = 0
    private var coreCount = -1;
    private var showOtherInfo = false
    private var clusters = ArrayList<Array<String>>()
    private var clustersFreq = ArrayList<String>()

    private val fpsUtils = FpsUtils()
    private var batteryManager: BatteryManager? = null

    private fun whiteBoldSpan(text: String): SpannableString {
        return SpannableString(text).apply {
            setSpan(ForegroundColorSpan(Color.WHITE), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private var configSpf: SharedPreferences? = null
    private val config: SharedPreferences
        get () {
            if (configSpf == null) {
                val soc = PlatformUtils().getCPUName()
                configSpf = mContext.getSharedPreferences(soc, Context.MODE_PRIVATE)
            }
            return configSpf!!
    }

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
        val gpuFreq = GpuUtils.getGpuFreq() + "Mhz"
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

        val cpuFreq = maxFreq.toString() // CpuFrequencyUtils.getCurrentFrequency()

        activityManager!!.getMemoryInfo(info)

        var cpuLoad = cpuLoadUtils.cpuLoadSum
        if (cpuLoad < 0) {
            cpuLoad = 0.toDouble();
        }

        // 电池电流
        val batteryCurrentNow = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val batteryCurrentNowMa = if (batteryCurrentNow != null) {
            (batteryCurrentNow / globalSPF.getInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT))
        } else {
            null
        }

        // GPU内存使用
        val gpuMemoryUsage = GpuUtils.getMemoryUsage()

        val otherInfoBuilder = SpannableStringBuilder()
        if (showOtherInfo) {
            totalMem = (info.totalMem / 1024 / 1024f).toInt()
            availMem = (info.availMem / 1024 / 1024f).toInt()
            val ramInfoText = "#RAM  " + ((totalMem - availMem) * 100 / totalMem).toString() + "%"

            otherInfoBuilder.run {
                append(whiteBoldSpan(ramInfoText))
                append("\n")

                if (gpuMemoryUsage != null) {
                    append(whiteBoldSpan("#GMEM " + gpuMemoryUsage))
                    append("\n")
                }

                for ((clusterIndex, cluster) in clusters.withIndex()) {
                    if (clusterIndex != 0) {
                        append("\n")
                    }
                    if (cluster.isNotEmpty()) {
                        try {
                            val title = "#" + cluster[0] + "~" + cluster[cluster.size - 1] + "  " + subFreqStr(clustersFreq.get(clusterIndex)) + "Mhz";
                            append(whiteBoldSpan(title))

                            val otherInfos = StringBuilder("")
                            for (core in cluster) {
                                otherInfos.append("\nCPU").append(core).append("  ")
                                val load = loads.get(core.toInt())
                                if (load != null) {
                                    if (load < 10) {
                                        otherInfos.append(" ")
                                    }
                                    otherInfos.append(load.toInt()).append("%")
                                } else {
                                    otherInfos.append("×")
                                }
                            }
                            append(otherInfos.toString())
                        } catch (ex: Exception) {
                        }
                    }
                }

                fpsUtils.currentFps?.run {
                    append("\n")
                    append(whiteBoldSpan("#FPS  $this"))
                }

                batteryCurrentNowMa?.run {
                    if (this > -20000 && this < 20000) {
                        append("\n")

                        val batteryInfo = "#BAT  " + (if (this > 0) ("+" + this) else this) + "mA"
                        append(whiteBoldSpan(batteryInfo))
                    }
                }
            }
        }

        val temperature = GlobalStatus.updateBatteryTemperature()

        myHandler.post {
            if (showOtherInfo) {
                otherInfo?.setText(null)

                otherInfo?.text = otherInfoBuilder
            }

            cpuChart!!.setData(100f, (100 - cpuLoad).toFloat())
            cpuFreqText!!.text = subFreqStr(cpuFreq) + "Mhz"

            gpuFreqText!!.text = gpuFreq
            if (gpuLoad > -1) {
                gpuChart!!.setData(100f, (100f - gpuLoad))
            }

            temperatureChart!!.setData(100f, 100f - GlobalStatus.batteryCapacity, temperature)
            temperatureText!!.setText(temperature.toString() + "°C")
            batteryLevelText!!.setText(GlobalStatus.batteryCapacity.toString() + "%")
            chargerView!!.visibility = (if (GlobalStatus.batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                View.VISIBLE
            } else {
                View.GONE
            })
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
        // updateInfo()
    }

    /**
     * 隐藏弹出框
     */
    fun hidePopupWindow() {
        stopTimer()
        if (show!! && null != mView) {
            try {
                mWindowManager?.removeViewImmediate(mView)
            } catch (ex: Exception) {}
            mView = null
            show = false
        }
    }

    @SuppressLint("ApplySharedPref", "ClickableViewAccessibility")
    private fun setUpView(context: Context): View {
        view = LayoutInflater.from(context).inflate(R.layout.fw_monitor, null)
        gpuPanel = view!!.findViewById(R.id.fw_gpu)
        temperaturePanel = view!!.findViewById(R.id.fw_battery)

        cpuChart = view!!.findViewById(R.id.fw_cpu_load)
        gpuChart = view!!.findViewById(R.id.fw_gpu_load)
        temperatureChart = view!!.findViewById(R.id.fw_battery_chart)

        cpuFreqText = view!!.findViewById(R.id.fw_cpu_freq)
        gpuFreqText = view!!.findViewById(R.id.fw_gpu_freq)
        temperatureText = view!!.findViewById(R.id.fw_battery_temp)
        batteryLevelText = view!!.findViewById<TextView>(R.id.fw_battery_level)
        chargerView = view!!.findViewById<ImageView>(R.id.fw_charger)
        otherInfo = view!!.findViewById<TextView>(R.id.fw_other_info)

        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        view!!.setOnClickListener {
            try {
                otherInfo?.visibility = if (showOtherInfo) View.GONE else View.VISIBLE
                // it.findViewById<View>(R.id.fw_ram_info).visibility = if (showOtherInfo) View.GONE else View.VISIBLE
                it.findViewById<LinearLayout>(R.id.fw_chart_list).orientation = if (showOtherInfo) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
                (mView as LinearLayout).orientation = if (showOtherInfo) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                showOtherInfo = !showOtherInfo
            } catch (ex: Exception) {}
        }

        return view!!
    }

    companion object {
        private var mWindowManager: WindowManager? = null
        public var show: Boolean? = false

        @SuppressLint("StaticFieldLeak")
        private var mView: View? = null
        private var timer: Timer? = null
    }
}