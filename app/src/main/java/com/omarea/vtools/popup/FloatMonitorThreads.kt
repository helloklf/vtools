package com.omarea.vtools.popup

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.omarea.data.GlobalStatus
import com.omarea.library.shell.ProcessUtils2
import com.omarea.vtools.R
import java.util.*

class FloatMonitorThreads(private val mContext: Context) {
    companion object {
        private var timer: Timer? = null
        var mView: View? = null
        val show: Boolean
            get() {
                return mView != null
            }
    }

    private var view: View = LayoutInflater.from(mContext).inflate(R.layout.fw_threads, null)
    private var textView: TextView = view.findViewById(R.id.fw_logs)
    private val processUtils = ProcessUtils2()
    private val handle = Handler(Looper.getMainLooper())

    val supported: Boolean
        get () {
            return processUtils.supported(mContext)
        }

    private var params: WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        height = WindowManager.LayoutParams.MATCH_PARENT
        width = WindowManager.LayoutParams.MATCH_PARENT
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        // 类型
        type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        if (mContext is AccessibilityService && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//6.0+
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        format = PixelFormat.TRANSLUCENT
        x = 0
        y = 0

        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private val wm = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var lastApp = "";
    private var lastPid = -1;

    private val pid:Int
        get() {
            val app = GlobalStatus.lastPackageName
            if (app!=lastApp || lastPid < 1) {
                if (app.isNotEmpty()) {
                    lastApp = app;
                    lastPid = processUtils.getAppMainProcess(app)
                }
            }
            return lastPid;
        }

    // 更新数据
    private fun updateData() {
        val pid = this.pid
        if (pid > 0) {
            val top15 = processUtils.getThreadLoads(pid)
            val text = top15.joinToString("\n",
                "$lastApp [$lastPid]\nTop15, Sorted by %CPU\n") {
                "${it.cpuLoad}% [${it.tid}] ${it.name}"
            }
            // val text = lastApp + "\n" + processUtils.getThreads(pid)
            handle.post {
                textView.text = text
            }
        } else {
            handle.post {
                textView.text = "未能获取当前应用进程，请检查辅助服务是否激活！"
            }
        }
    }

    private fun stopUpdate() {
        if (timer != null) {
            timer?.cancel()
            timer = null
        }
    }

    private fun startTimer() {
        this.stopUpdate()
        if (timer == null) {
            timer = Timer()
            timer!!.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    updateData()
                }
            }, 0, 3000)
        }
    }

    fun hidePopupWindow() {
        this.stopUpdate()
        mView?.run {
            // 获取WindowManager
            val mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            mWindowManager.removeViewImmediate(mView)
            mView = null
        }
    }
    public fun showPopupWindow() {
        if (!show) {
            wm.addView(view, params)
            mView = view
            startTimer()
        }
    }
}