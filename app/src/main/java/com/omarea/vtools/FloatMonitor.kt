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
import com.omarea.shared.model.CpuCoreInfo
import com.omarea.shell.cpucontrol.CpuFrequencyUtils
import com.omarea.shell.cpucontrol.GpuUtils
import com.omarea.ui.CpuChatView
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

/**
 * 弹窗辅助类
 *
 * @ClassName WindowUtils
 */
class FloatMonitor (context: Context) {
    private var mContext: Context? = context
    private var timer: Timer? = null

    /**
     * 显示弹出框
     *
     * @param context
     */
    fun showPopupWindow() {
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

        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
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

    private var coreCount = -1;

    private var view: View? = null
    private var minFreqs = HashMap<Int, String>()
    private var maxFreqs = HashMap<Int, String>()
    private var cpuChat : CpuChatView? = null
    private var cpuFreqView: TextView? = null
    private var gpuChat : CpuChatView? = null
    private var gpuFreqView: TextView? = null
    private var ramChat : CpuChatView? = null
    private var ramUseView: TextView? = null
    private var activityManager:ActivityManager? = null
    private var myHandler = Handler()

    fun format1(value: Double): String {
        var bd = BigDecimal(value)
        bd = bd.setScale(1, RoundingMode.HALF_UP)
        return bd.toString()
    }
    private fun updateInfo() {
        if (coreCount < 1) {
            coreCount = CpuFrequencyUtils.getCoreCount()
        }
        val cores = ArrayList<CpuCoreInfo>()
        val loads = CpuFrequencyUtils.getCpuLoad()
        val gpuFreq = GpuUtils.getGpuFreq() + "Mhz"
        val gpuLoad = GpuUtils.getGpuLoad()
        for (coreIndex in 0 until coreCount) {
            val core = CpuCoreInfo()

            core.currentFreq = CpuFrequencyUtils.getCurrentFrequency("cpu$coreIndex")
            if (!maxFreqs.containsKey(coreIndex) || (core.currentFreq != "" && maxFreqs.get(coreIndex).isNullOrEmpty())) {
                maxFreqs.put(coreIndex, CpuFrequencyUtils.getCurrentMaxFrequency("cpu" + coreIndex))
            }
            core.maxFreq = maxFreqs.get(coreIndex)

            if (!minFreqs.containsKey(coreIndex) || (core.currentFreq != "" && minFreqs.get(coreIndex).isNullOrEmpty())) {
                minFreqs.put(coreIndex, CpuFrequencyUtils.getCurrentMinFrequency("cpu" + coreIndex))
            }
            core.minFreq = minFreqs.get(coreIndex)

            if (loads.containsKey(coreIndex)) {
                core.loadRatio = loads.get(coreIndex)!!
            }
            cores.add(core)
        }
        var cpuFreq = ""
        cores.forEach { item ->
            run {
                if (item.currentFreq > cpuFreq) {
                    cpuFreq = item.currentFreq
                }
            }
        }

        val info = ActivityManager.MemoryInfo()
        activityManager!!.getMemoryInfo(info)
        val totalMem = (info.totalMem / 1024 / 1024f).toInt()
        val availMem = (info.availMem / 1024 / 1024f).toInt()
        myHandler.post {
            ramUseView!!.text = "${((totalMem - availMem) * 100 / totalMem)}% (${totalMem / 1024 + 1}GB)"
            ramChat!!.setData(totalMem.toFloat(), availMem.toFloat())

            if (loads.containsKey(-1)) {
                cpuChat!!.setData(100.toFloat(), (100 - loads.get(-1)!!.toInt()).toFloat())
                cpuFreqView!!.text = subFreqStr(cpuFreq) + "Mhz"
            }
            if (gpuLoad > -1) {
                gpuChat!!.setData(100.toFloat(), (100 - gpuLoad).toFloat())
                gpuFreqView!!.text = gpuFreq
            }
            val layoutParams = view!!.layoutParams
            view!!.layoutParams = layoutParams
        }
    }

    private fun startTimer () {
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

        view!!.setOnTouchListener { v, event ->
            view!!.visibility = View.GONE
            myHandler.postDelayed({
                view!!.visibility = View.VISIBLE
            }, 5000)
            false
        }
        view!!.setOnLongClickListener {
            hidePopupWindow()
            false
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