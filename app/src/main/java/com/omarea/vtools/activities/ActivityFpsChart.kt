package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.omarea.Scene
import com.omarea.library.basic.AppInfoLoader
import com.omarea.library.calculator.Flags
import com.omarea.library.shell.PlatformUtils
import com.omarea.model.FpsWatchSession
import com.omarea.store.FpsWatchStore
import com.omarea.ui.fps.AdapterSessions
import com.omarea.vtools.R
import com.omarea.vtools.popup.FloatFpsWatch
import kotlinx.android.synthetic.main.activity_fps_chart.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat

class ActivityFpsChart : ActivityBase(), AdapterSessions.OnItemClickListener {
    private lateinit var fpsWatchStore: FpsWatchStore
    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fps_chart)
        setBackArrow()

        fpsWatchStore = FpsWatchStore(this)

        /*
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        */

        chart_platform.text = PlatformUtils().getCPUName()
        chart_phone.text = Build.MODEL
        chart_os.text = when (Build.VERSION.SDK_INT) {
            31 -> "Android 12"
            30 -> "Android 11"
            29 -> "Android 10"
            28 -> "Android 9"
            27 -> "Android 8.1"
            26 -> "Android 8.0"
            25 -> "Android 7.0"
            24 -> "Android 7.0"
            23 -> "Android 6.0"
            22 -> "Android 5.1"
            21 -> "Android 5.0"
            else -> "SDK(" + Build.VERSION.SDK_INT + ")"
        }

        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        chart_sessions.layoutManager = linearLayoutManager
        val appInfoLoader = AppInfoLoader(context)
        GlobalScope.launch(Dispatchers.Main) {
            val sessions = fpsWatchStore.sessions()
            sessions.forEach {
                val appInfo = appInfoLoader.loadAppBasicInfo(it.packageName).await()
                it.appName = appInfo.appName
                it.appIcon = appInfo.icon
            }
            if (sessions.size > 0) {
                chart_sessions_empty?.visibility = View.GONE
                chart_sessions?.adapter = AdapterSessions(context, sessions).apply {
                    setOnItemClickListener(this@ActivityFpsChart)
                    setOnItemDeleteClickListener(object : AdapterSessions.OnItemClickListener {
                        override fun onItemClick(view: View, position: Int) {
                            onSessionDeleteClick(position)
                        }
                    })
                }
            } else {
                chart_sessions_empty?.visibility = View.VISIBLE
            }
        }

        chart_add.setOnClickListener {
            if (FloatFpsWatch.show != true) {
                FloatFpsWatch(context).showPopupWindow()
                /*
                val serviceState = AccessibleServiceHelper().serviceRunning(context)
                if (serviceState) {
                    FloatFpsWatch(context).showPopupWindow()
                } else {
                    Scene.toast("请在系统设置里激活[Scene - 场景模式]辅助服务", Toast.LENGTH_SHORT)
                }
                */
            } else {
                FloatFpsWatch(context).hidePopupWindow()
            }
        }
    }

    // 删除会话
    private fun onSessionDeleteClick(position: Int) {
        val adapter = (chart_sessions.adapter as AdapterSessions)
        val item = adapter.getItem(position)

        fpsWatchStore.deleteSession(item.sessionId)
        // TODO:重新加载会话列表
        adapter.notifyItemRemoved(position)
        // adapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onItemClick(view: View, position: Int) {
        val item = (chart_sessions.adapter as AdapterSessions).getItem(position)
        val sessionId = item.sessionId
        val fpsData = fpsWatchStore.sessionFpsData(sessionId)
        val tData  = fpsWatchStore.sessionTemperatureData(sessionId)
        val smoothRatio = fpsData.filter { it >= 45 }.size * 100.0 / fpsData.size
        val feverRatio = tData.filter { it > 46 }.size * 100.0 / tData.size

        chart_fps_max.text = String.format("%.1f", fpsWatchStore.sessionMaxFps(sessionId))
        chart_fps_min.text = String.format("%.1f", fpsWatchStore.sessionMinFps(sessionId))
        chart_fps_avg.text = String.format("%.1f", fpsWatchStore.sessionAvgFps(sessionId))
        chart_smooth_ratio.text = String.format("%.1f%%", smoothRatio)
        chart_fever_ratio.text = String.format("%.1f%%", feverRatio)
        chart_temp_max.text = String.format("%.1f", tData.maxOrNull())
        chart_session_name.text = item.appName
        chart_session_time.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(item.beginTime)
        chart_session.setSessionId(sessionId)
    }
}
