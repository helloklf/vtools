package com.omarea.vtools.popup

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.shell.KeepShellPublic
import com.omarea.data.EventBus
import com.omarea.data.EventType
import com.omarea.data.GlobalStatus
import com.omarea.data.IEventReceiver
import com.omarea.library.shell.FpsUtils
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.FpsWatchStore
import com.omarea.vtools.R
import java.util.*

public class FloatFpsWatch(private val mContext: Context) {
    private var startMonitorTime = 0L
    private val fpsWatchStore = FpsWatchStore(mContext)
    private var sessionId = 0L
    private var sessionApp: String? = null

    /**
     * dp转换成px
     */
    private fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 显示弹出框
     * @param context
     */
    fun showPopupWindow(): Boolean {
        if (show!!) {
            return true
        }
        startMonitorTime = System.currentTimeMillis()

        if (!(mContext is AccessibilityService)) {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(mContext)) {
                Toast.makeText(mContext, mContext.getString(R.string.permission_float), Toast.LENGTH_LONG).show()
                return false
            }
        }

        show = true
        mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        mView = setUpView(mContext)

        val params = LayoutParams()

        // 类型
        params.type = LayoutParams.TYPE_SYSTEM_ALERT

        // 优先使用辅助服务叠加层（如果是辅助服务Context）
        if (mContext is AccessibilityService) {
            params.type = LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//6.0+
                params.type = LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                params.type = LayoutParams.TYPE_SYSTEM_ALERT
            }
        }
        params.format = PixelFormat.TRANSLUCENT

        params.width = LayoutParams.WRAP_CONTENT
        params.height = LayoutParams.WRAP_CONTENT

        params.gravity = Gravity.TOP or Gravity.RIGHT
        // params.x = 0
        params.y = dp2px(mContext, 55f)

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
        EventBus.subscribe(appWatch)

        return true
    }

    private val appWatch = object : IEventReceiver{
        override fun eventFilter(eventType: EventType): Boolean {
            return (eventType == EventType.APP_SWITCH || eventType == EventType.SCREEN_OFF || eventType == EventType.SCREEN_ON)
        }

        override fun onReceive(eventType: EventType, data: HashMap<String, Any>?) {
            if (sessionId > 0) {
                if ((eventType == EventType.SCREEN_OFF || eventType == EventType.SCREEN_ON) || (GlobalStatus.lastPackageName != sessionApp)) {
                    sessionId = -1
                    myHandler.post {
                        if (eventType == EventType.SCREEN_OFF) {
                            Toast.makeText(mContext, "屏幕显示状态变化，帧率录制结束", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(mContext, "前台应用发生变化，帧率录制结束", Toast.LENGTH_SHORT).show()
                        }
                        recordBtn?.run {
                            setImageResource(R.drawable.play)
                            view?.alpha = 1f
                        }
                    }
                }
            }
        }

        override val isAsync: Boolean
            get() = false

        override fun onSubscribe() {

        }

        override fun onUnsubscribe() {

        }
    }

    private fun stopTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    private var view: View? = null
    private var fpsText: TextView? = null
    private var recordBtn:ImageButton? = null

    private var myHandler = Handler(Looper.getMainLooper())

    private val fpsUtils = FpsUtils(KeepShellPublic.getInstance("fps-recorder", true))

    private fun updateInfo() {
        val fps = fpsUtils.fps
        if (sessionId > 0) {
            fpsWatchStore.addHistory(
                sessionId,
                fps,
                GlobalStatus.temperatureCurrent,
                ModeSwitcher.DEFAULT
            )
        }

        myHandler.post {
            fpsText?.text = if (fps >= 100) fps.toInt().toString() else String.format("%.1f", fps)
        }
    }

    private fun startTimer() {
        stopTimer()

        timer = Timer()
        timer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                updateInfo()
            }
        }, 0, 1000)
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
        sessionId = -1
        EventBus.unsubscribe(appWatch)
    }

    @SuppressLint("ApplySharedPref", "ClickableViewAccessibility")
    private fun setUpView(context: Context): View {
        view = LayoutInflater.from(context).inflate(R.layout.fw_fps_watch, null)
        fpsText = view!!.findViewById(R.id.fw_fps)
        recordBtn = view!!.findViewById(R.id.fw_action)
        view?.setOnClickListener {
            recordBtn?.run {
                if (sessionId > 0) {
                    sessionId = -1
                    setImageResource(R.drawable.play)
                    view?.alpha = 1f
                    Toast.makeText(mContext, "帧率录制结束！", Toast.LENGTH_SHORT).show()
                } else {
                    val app = if (GlobalStatus.lastPackageName.isNullOrEmpty()) "android" else GlobalStatus.lastPackageName
                    sessionId = fpsWatchStore.createSession(app)
                    sessionApp = GlobalStatus.lastPackageName
                    setImageResource(R.drawable.stop)
                    view?.alpha = 0.6f
                    Toast.makeText(mContext, "帧率录制开始，请勿使用游戏工具箱或离开当前应用！", Toast.LENGTH_LONG).show()
                }
            }
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