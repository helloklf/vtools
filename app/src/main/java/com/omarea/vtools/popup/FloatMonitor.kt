package com.omarea.vtools.popup

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.omarea.shell_utils.BatteryUtils
import com.omarea.shell_utils.CpuFrequencyUtil
import com.omarea.shell_utils.CpuLoadUtils
import com.omarea.shell_utils.GpuUtils
import com.omarea.ui.FloatMonitorBatteryView
import com.omarea.ui.FloatMonitorChartView
import com.omarea.vtools.R
import java.util.*

class FloatMonitor(context: Context) {
    private var mContext: Context? = context
    private var timer: Timer? = null
    private var startMonitorTime = 0L
    private var cpuLoadUtils = CpuLoadUtils()

    /**
     * 显示弹出框
     * @param context
     */
    fun showPopupWindow() {
        if (isShown!!) {
            return
        }
        startMonitorTime = System.currentTimeMillis()

        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(mContext)) {
            Toast.makeText(mContext, mContext!!.getString(R.string.permission_float), Toast.LENGTH_LONG).show()
            return
        }

        isShown = true
        // 获取WindowManager
        mWindowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        mView = setUpView(mContext!!)

        val params = LayoutParams()
        val monitorStorage = mContext!!.getSharedPreferences("float_monitor_storage", Context.MODE_PRIVATE)

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

        params.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE

        val navHeight = 0
        if (navHeight > 0) {
            val display = mWindowManager!!.getDefaultDisplay()
            val p = Point()
            display.getRealSize(p)
            params.y = -navHeight
            params.x = 0
        } else {
        }
        mWindowManager!!.addView(mView, params)

        // 添加触摸事件
        mView!!.setOnTouchListener(object : View.OnTouchListener {
            private var isTouchDown = false
            private var touchStartX = 0f
            private var touchStartY = 0f
            private var touchStartRawX = 0f
            private var touchStartRawY = 0f
            private var touchStartTime = 0L
            private var lastClickTime = 0L

            private fun onClick() {
                if (System.currentTimeMillis() - lastClickTime < 300) {
                    hidePopupWindow()
                } else {
                    lastClickTime = System.currentTimeMillis()
                }
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
                                    monitorStorage.edit().putInt("x", params.x).putInt("y", params.x).apply()
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
    private var cpuChart: FloatMonitorChartView? = null
    private var cpuFreqText: TextView? = null
    private var gpuChart: FloatMonitorChartView? = null
    private var gpuPanel: View? = null
    private var gpuFreqText: TextView? = null
    private var temperaturePanel: View? = null
    private var temperatureChart: FloatMonitorBatteryView? = null
    private var temperatureText: TextView? = null
    private var batteryLevelText: TextView? = null
    private var chargerView:ImageView? = null
    private var otherInfo: TextView? = null

    private var activityManager: ActivityManager? = null
    private var myHandler = Handler()
    private var batteryUnit = BatteryUtils()
    private val info = ActivityManager.MemoryInfo()

    private var sum = -1
    private var totalMem = 0
    private var availMem = 0
    private var coreCount = -1;
    private var showOtherInfo = false
    private var clusters = ArrayList<Array<String>>()
    private var clustersFreq = ArrayList<String>()

    private fun updateInfo() {
        if (coreCount < 1) {
            coreCount = CpuFrequencyUtil.getCoreCount()
            clusters = CpuFrequencyUtil.getClusterInfo()
        }
        val loads = cpuLoadUtils.cpuLoad
        clustersFreq.clear()
        for (coreIndex in 0 until clusters.size) {
            clustersFreq.add(CpuFrequencyUtil.getCurrentFrequency(coreIndex))
        }
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

        val batteryStatus = batteryUnit.getBatteryTemperature()

        myHandler.post {
            if (showOtherInfo) {
                otherInfo!!.setText("")

                totalMem = (info.totalMem / 1024 / 1024f).toInt()
                availMem = (info.availMem / 1024 / 1024f).toInt()
                val ramInfoText = "DRAM  " + ((totalMem - availMem) * 100 / totalMem).toString() + "%"

                val ramSpannable = SpannableString(ramInfoText);
                val styleSpan  = StyleSpan(Typeface.BOLD);
                ramSpannable.setSpan(ForegroundColorSpan(Color.WHITE), 0, ramInfoText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                ramSpannable.setSpan(styleSpan, 0, ramInfoText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                otherInfo?.append(ramSpannable)
                otherInfo?.append("\n")

                var clusterIndex = 0
                for (cluster in clusters) {
                    if (clusterIndex != 0) {
                        otherInfo?.append("\n")
                    }
                    if(cluster.size > 0) {
                        val title = "#" + cluster[0] + "~" + cluster[cluster.size - 1] + "  "+ subFreqStr(clustersFreq.get(clusterIndex)) + "Mhz";
                        val titleSpannable = SpannableString(title);
                        val styleSpan  = StyleSpan(Typeface.BOLD);
                        titleSpannable.setSpan(ForegroundColorSpan(Color.WHITE), 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        titleSpannable.setSpan(styleSpan, 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        otherInfo?.append(titleSpannable)

                        val otherInfos = StringBuilder("")
                        for (core in cluster) {
                            otherInfos.append("\n")
                            otherInfos.append("CPU")
                            otherInfos.append(core)
                            otherInfos.append("  ")
                            val load = loads.get(core.toInt())
                            if (load != null) {
                                if (load < 10) {
                                    otherInfos.append("0")
                                    otherInfos.append(load.toInt())
                                    otherInfos.append("%")
                                } else {
                                    otherInfos.append(load.toInt())
                                    otherInfos.append("%")
                                }
                            } else {
                                otherInfos.append("×")
                            }
                        }
                        otherInfo?.append(otherInfos.toString())
                    }
                    clusterIndex++
                }
            }

            cpuChart!!.setData(100f, (100 - cpuLoad).toFloat())
            cpuFreqText!!.text = subFreqStr(cpuFreq) + "Mhz"

            gpuFreqText!!.text = gpuFreq
            if (gpuLoad > -1) {
                gpuChart!!.setData(100f, (100f - gpuLoad))
            }

            temperatureChart!!.setData(100f, 100f - batteryStatus.level, batteryStatus.temperature)
            temperatureText!!.setText(batteryStatus.temperature.toString() + "°C")
            batteryLevelText!!.setText(batteryStatus.level.toString() + "%")
            if (batteryStatus.statusText == "2") {
                chargerView!!.visibility = View.VISIBLE
            } else {
                chargerView!!.visibility = View.GONE
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
        if (isShown!! && null != mView) {
            mWindowManager!!.removeView(mView)
            mView = null
            isShown = false
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
            otherInfo?.visibility = if (showOtherInfo) View.GONE else View.VISIBLE
            // it.findViewById<View>(R.id.fw_ram_info).visibility = if (showOtherInfo) View.GONE else View.VISIBLE
            it.findViewById<LinearLayout>(R.id.fw_chart_list).orientation = if (showOtherInfo) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            (mView as LinearLayout).orientation = if (showOtherInfo) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            showOtherInfo = !showOtherInfo
        }

        return view!!
    }

    companion object {
        private var mWindowManager: WindowManager? = null
        public var isShown: Boolean? = false
        @SuppressLint("StaticFieldLeak")
        private var mView: View? = null
    }
}