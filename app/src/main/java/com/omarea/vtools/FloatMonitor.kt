package com.omarea.vtools

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.TextView
import android.widget.Toast
import com.omarea.shell.cpucontrol.CpuFrequencyUtils
import com.omarea.shell.cpucontrol.CpuLoadUtils
import com.omarea.shell.cpucontrol.GpuUtils
import com.omarea.ui.CpuChatView
import com.omarea.ui.FloatMonitorChatView
import java.util.*
import kotlin.collections.HashMap

class FloatMonitor(context: Context) {
    private var mContext: Context? = context
    private var timer: Timer? = null
    private var startMonitorTime = 0L
    private var gpuLoadAvg = HashMap<Int, Int>()
    private var postionMode = 1
    private var cpuLoadUtils = CpuLoadUtils()
    private var postionModes = arrayOf(
        Gravity.START or Gravity.TOP,
        Gravity.TOP or Gravity.CENTER_HORIZONTAL,
        Gravity.TOP or Gravity.END,
        Gravity.END or Gravity.BOTTOM,
        Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
        Gravity.BOTTOM or Gravity.START
    )

    /**
     * 显示弹出框
     * @param context
     */
    fun showPopupWindow(postion: Int = postionMode) {
        if (isShown!!) {
            return
        }
        startMonitorTime = System.currentTimeMillis()

        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(mContext)) {
            Toast.makeText(mContext, "未授予“显示悬浮窗/在应用上层显示”权限", Toast.LENGTH_LONG).show()
            return
        }

        isShown = true
        // 获取WindowManager
        mWindowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        mView = setUpView(mContext!!)

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

        //val flags = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        // | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        // 如果设置了WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE，弹出的View收不到Back键的事件
        //params.flags = flags
        // 不设置这个弹出框的透明遮罩显示为黑色
        params.format = PixelFormat.TRANSLUCENT
        // FLAG_NOT_TOUCH_MODAL不阻塞事件传递到后面的窗口
        // 设置 FLAG_NOT_FOCUSABLE 悬浮窗口较小时，后面的应用图标由不可长按变为可长按
        // 不设置这个flag的话，home页的划屏会有问题

        params.width = LayoutParams.WRAP_CONTENT
        params.height = LayoutParams.WRAP_CONTENT

        params.gravity = postionModes.get(postion % 6)

        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        // WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        // LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_NOT_FOCUSABLE or

        val navHeight = 0 // (getNavBarHeight(mContext!!))
        if (navHeight > 0) {
            val display = mWindowManager!!.getDefaultDisplay()
            val p = Point()
            display.getRealSize(p)
            params.y = -navHeight
            params.x = 0
        } else {
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
    private var cpuChat: CpuChatView? = null
    private var cpuFreqView: TextView? = null
    private var gpuChat: FloatMonitorChatView? = null
    private var gpuFreqView: TextView? = null
    private var ramChat: FloatMonitorChatView? = null
    private var ramUseView: TextView? = null
    private var activityManager: ActivityManager? = null
    private var myHandler = Handler()
    val info = ActivityManager.MemoryInfo()

    var sum = -1
    var totalMem = 0
    var availMem = 0
    private fun getGpuLoadAvg(): Int {
        try {
            var total = 0L
            var items = 0L
            for (item in gpuLoadAvg) {
                total += (item.key * item.value)
                items += item.value
            }
            return (total / items).toInt()
        } catch (ex: java.lang.Exception) {
        }
        return 0
    }

    private fun updateInfo() {
        val cpuFreq = CpuFrequencyUtils.getCurrentFrequency()
        val gpuFreq = GpuUtils.getGpuFreq() + "Mhz"
        val gpuLoad = GpuUtils.getGpuLoad()

        activityManager!!.getMemoryInfo(info)
        if (sum < 0) {
            totalMem = (info.totalMem / 1024 / 1024f).toInt()
            availMem = (info.availMem / 1024 / 1024f).toInt()
        }
        myHandler.post {
            if (sum < 0) {
                ramUseView!!.text = "${((totalMem - availMem) * 100 / totalMem)}% (${totalMem / 1024 + 1}GB)"
                ramChat!!.setData(totalMem.toFloat(), availMem.toFloat())
                sum = 5
            } else {
                sum--
            }

            var load = cpuLoadUtils.cpuLoadSum
            if (load < 0) {
                load = 0.toDouble();
            }
            cpuChat!!.setData(100f, (100 - load).toFloat())
            cpuFreqView!!.text = subFreqStr(cpuFreq.toString()) + "Mhz"

            gpuFreqView!!.text = gpuFreq
            if (gpuLoad > -1) {
                gpuChat!!.setData(100f, (100f - gpuLoad))
                if (gpuLoadAvg.containsKey(gpuLoad)) {
                    gpuLoadAvg[gpuLoad]!!.plus(1)
                } else {
                    gpuLoadAvg.put(gpuLoad, 1)
                }
            }
            val layoutParams = view!!.layoutParams
            view!!.layoutParams = layoutParams
        }
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
        try {
            val loads = cpuLoadUtils!!.cpuLoad
            if (loads != null && loads.containsKey(-1)) {
                val time = String.format("%.1f", System.currentTimeMillis() - startMonitorTime / 1000 / 60.0)
                val timeMinute = String.format("%.1f", time)
                val gpuLoadAvg = getGpuLoadAvg()
                Toast.makeText(mContext, "${timeMinute}分钟，\n平均负载\n\nCPU: ${loads.get(-1)!!.toInt()}%\nGPU: ${gpuLoadAvg}%", Toast.LENGTH_LONG).show()
            }
            gpuLoadAvg.clear()
        } catch (ex: java.lang.Exception) {

        }
        if (isShown!! && null != mView) {
            mWindowManager!!.removeView(mView)
            mView = null
            isShown = false
        }
    }

    @SuppressLint("ApplySharedPref", "ClickableViewAccessibility")
    private fun setUpView(context: Context): View {
        view = LayoutInflater.from(context).inflate(R.layout.fw_monitor, null)
        cpuChat = view!!.findViewById(R.id.fw_cpu_load)
        gpuChat = view!!.findViewById(R.id.fw_gpu_load)
        ramChat = view!!.findViewById(R.id.fw_ram_load)

        cpuFreqView = view!!.findViewById(R.id.fw_cpu_freq)
        gpuFreqView = view!!.findViewById(R.id.fw_gpu_freq)
        ramUseView = view!!.findViewById(R.id.fw_ram_use)

        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

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
        return view!!
    }

    companion object {
        private var mWindowManager: WindowManager? = null
        public var isShown: Boolean? = false
        @SuppressLint("StaticFieldLeak")
        private var mView: View? = null
    }
}