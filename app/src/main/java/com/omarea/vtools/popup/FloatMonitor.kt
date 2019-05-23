package com.omarea.vtools.popup

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.TextView
import android.widget.Toast
import com.omarea.shell.cpucontrol.CpuFrequencyUtils
import com.omarea.shell.cpucontrol.CpuLoadUtils
import com.omarea.shell.cpucontrol.GpuUtils
import com.omarea.shell.units.BatteryUnit
import com.omarea.ui.CpuChartView
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
                    when(event.action) {
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
                            if (System.currentTimeMillis() - touchStartTime < 300) {
                                if (Math.abs(event.rawX - touchStartRawX) < 15 && Math.abs(event.rawY - touchStartRawY) < 15) {
                                    onClick()
                                } else {
                                    monitorStorage.edit().putInt("x", params.x).putInt("y", params.x).apply()
                                }
                            }
                            isTouchDown = false
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
    private var cpuChart: CpuChartView? = null
    private var cpuFreqText: TextView? = null
    private var gpuChart: FloatMonitorChartView? = null
    private var gpuPanel: View? = null
    private var gpuFreqText: TextView? = null
    private var ramChart: FloatMonitorChartView? = null
    private var ramUseText: TextView? = null
    private var temperaturePanel: View? = null
    private var temperatureChart: FloatMonitorBatteryView? = null
    private var temperatureText: TextView? = null
    private var batteryLevelText: TextView? = null

    private var activityManager: ActivityManager? = null
    private var myHandler = Handler()
    private var batteryUnit = BatteryUnit()
    val info = ActivityManager.MemoryInfo()

    var sum = -1
    var totalMem = 0
    var availMem = 0

    private fun updateInfo() {
        val cpuFreq = CpuFrequencyUtils.getCurrentFrequency()
        val gpuFreq = GpuUtils.getGpuFreq() + "Mhz"
        val gpuLoad = GpuUtils.getGpuLoad()

        activityManager!!.getMemoryInfo(info)

        var cpuLoad = cpuLoadUtils.cpuLoadSum
        if (cpuLoad < 0) {
            cpuLoad = 0.toDouble();
        }

        val batteryStatus = batteryUnit.getBatteryTemperature()

        myHandler.post {
            /*
            // 内存使用显示似乎不重要，去掉吧
            if (sum < 0) {
                totalMem = (info.totalMem / 1024 / 1024f).toInt()
                availMem = (info.availMem / 1024 / 1024f).toInt()
                ramUseText!!.text = "${((totalMem - availMem) * 100 / totalMem)}% (${totalMem / 1024 + 1}GB)"
                ramChart!!.setData(totalMem.toFloat(), availMem.toFloat())
                sum = 5
            } else {
                sum--
            }
            */

            cpuChart!!.setData(100f, (100 - cpuLoad).toFloat())
            cpuFreqText!!.text = subFreqStr(cpuFreq.toString()) + "Mhz"

            gpuFreqText!!.text = gpuFreq
            if (gpuLoad > -1) {
                gpuChart!!.setData(100f, (100f - gpuLoad))
            }

            /*
            var value = batteryStatus.temperature - 10
            if (value > 45) {
                value = 45.0f
            } else if (value < 0) {
                value = 0.0f
            }
            temperatureChart!!.setData(45f, 45f - value)
            */
            temperatureChart!!.setData(100f, 100f - batteryStatus.level)
            temperatureText!!.setText(batteryStatus.temperature.toString() + "°C")
            batteryLevelText!!.setText(batteryStatus.level.toString() + "%")

            if (batteryStatus.temperature >= 54) {
                temperatureText!!.setTextColor(Color.rgb(255, 15, 0))
            } else if (batteryStatus.temperature >= 49) {
                temperatureText!!.setTextColor(mContext!!.resources.getColor(R.color.color_load_veryhight))
            } else if (batteryStatus.temperature >= 44) {
                temperatureText!!.setTextColor(mContext!!.resources.getColor(R.color.color_load_hight))
            } else if (batteryStatus.temperature > 34) {
                temperatureText!!.setTextColor(mContext!!.resources.getColor(R.color.color_load_mid))
            } else {
                temperatureText!!.setTextColor(mContext!!.resources.getColor(R.color.color_load_low))
            }

            val layoutParams = view!!.layoutParams
            view!!.layoutParams = layoutParams
        }
        /*
        view!!.setOnClickListener {
            view!!.visibility = View.GONE
            myHandler.postDelayed({
                if (mView != null) {
                    view!!.visibility = View.VISIBLE
                }
            }, 2000)
            true
        }
        // val opt = view!!.findViewById(R.id.fw_monitor_opt)
        view!!.setOnLongClickListener {
            hidePopupWindow()
            true
        }
        */
    }

    private fun startTimer() {
        stopTimer()
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                updateInfo()
            }
        }, 0, 1000)
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
        ramChart = view!!.findViewById(R.id.fw_ram_load)
        temperatureChart = view!!.findViewById(R.id.fw_battery_chart)

        cpuFreqText = view!!.findViewById(R.id.fw_cpu_freq)
        gpuFreqText = view!!.findViewById(R.id.fw_gpu_freq)
        ramUseText = view!!.findViewById(R.id.fw_ram_use)
        temperatureText = view!!.findViewById(R.id.fw_battery_temp)
        batteryLevelText = view!!.findViewById<TextView>(R.id.fw_battery_level)

        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return view!!
    }

    companion object {
        private var mWindowManager: WindowManager? = null
        public var isShown: Boolean? = false
        @SuppressLint("StaticFieldLeak")
        private var mView: View? = null
    }
}