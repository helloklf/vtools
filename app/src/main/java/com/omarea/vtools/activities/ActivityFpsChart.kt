package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.Toast
import com.omarea.Scene
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.library.basic.AppInfoLoader
import com.omarea.library.calculator.Flags
import com.omarea.library.shell.PlatformUtils
import com.omarea.store.FpsWatchStore
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.vtools.R
import com.omarea.vtools.popup.FloatFpsWatch
import kotlinx.android.synthetic.main.activity_addin_online.*
import org.json.JSONArray
import org.json.JSONObject

class ActivityFpsChart : ActivityBase() {
    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fps_chart)

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

        vtools_online.loadUrl("file:///android_asset/fps-chart/index.html")
        // vtools_online.loadUrl("https://vtools.oss-cn-beijing.aliyuncs.com/Scene-Online/fps-chart/index.html")
        val context = this@ActivityFpsChart
        val progressBarDialog = ProgressBarDialog(context)

        // 处理alert、confirm
        vtools_online.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                DialogHelper.animDialog(
                        AlertDialog.Builder(context)
                                .setMessage(message)
                                .setPositiveButton(R.string.btn_confirm, { _, _ -> })
                                .setOnDismissListener {
                                    result?.confirm()
                                }
                                .create()
                )?.setCancelable(false)
                return true // super.onJsAlert(view, url, message, result)
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                DialogHelper.animDialog(
                        AlertDialog.Builder(context)
                                .setMessage(message)
                                .setPositiveButton(R.string.btn_confirm) { _, _ ->
                                    result?.confirm()
                                }
                                .setNeutralButton(R.string.btn_cancel) { _, _ ->
                                    result?.cancel()
                                }
                                .create()
                )?.setCancelable(false)
                return true // super.onJsConfirm(view, url, message, result)
            }
        }

        // 处理loading、文件下载
        vtools_online.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBarDialog.hideDialog()
                view?.run {
                    setTitle(this.title)
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBarDialog.showDialog(getString(R.string.please_wait))
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        })

        vtools_online.settings.javaScriptEnabled = true
        vtools_online.settings.setLoadWithOverviewMode(true);
        vtools_online.settings.setUseWideViewPort(true);
        val fpsWatchStore = FpsWatchStore(this)

        val appInfoLoader = AppInfoLoader(context)
        vtools_online.addJavascriptInterface(object {
            private var pmInstance: PackageManager? = null
            protected val pm: PackageManager
                get() {
                    if (pmInstance == null) {
                        pmInstance = context.packageManager
                    }
                    return pmInstance!!
                }

            private fun loadAppBasicInfo(packageName: String): AppInfoLoader.AppBasicInfo {
                var icon: Drawable? = null
                var name = packageName
                try {
                    val installInfo = pm.getPackageInfo(packageName, 0)
                    name = "" + installInfo.applicationInfo.loadLabel(pm)
                    icon = installInfo.applicationInfo.loadIcon(pm)
                    // saveCache(packageName, icon)
                } catch (ex: Exception) {
                } finally {
                }

                return AppInfoLoader.AppBasicInfo(name, icon)
            }

            @JavascriptInterface
            public fun toggleFpsToolbar(show: Boolean) {
                Scene.post {
                    if (show) {
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

            @JavascriptInterface
            public fun getFpsToolbarState(): String {
                return FloatFpsWatch.show.toString()
            }

            @JavascriptInterface
            public fun deleteSession(sessionId: Long) {
                fpsWatchStore.deleteSession(sessionId);
            }

            @JavascriptInterface
            public fun getDeviceInfo(): String {
                val obj = JSONObject()
                obj.put("soc", PlatformUtils().getCPUName())
                obj.put("model", Build.MODEL)
                obj.put("sdk", Build.VERSION.SDK_INT)

                return obj.toString(2)
            }

            @JavascriptInterface
            public fun getSessions(): String {
                val sessions = fpsWatchStore.sessions()
                val obj = JSONArray()
                sessions.forEach {
                    obj.put(JSONObject().apply {
                        put("sessionId", it.sessionId)
                        put("packageName", it.packageName)
                        put("beginTime", it.beginTime)
                        put("appName", loadAppBasicInfo(it.packageName).appName)
                    })
                }
                return obj.toString(2)
            }

            @JavascriptInterface
            public fun getSessionData(sessionId: Long): String {
                return JSONObject().apply {
                    put("fps", JSONArray().apply {
                        fpsWatchStore.sessionFpsData(sessionId).forEach {
                            put(it)
                        }
                    })
                    put("temperature", JSONArray().apply {
                        fpsWatchStore.sessionTemperatureData(sessionId).forEach {
                            put(it)
                        }
                    })
                    put("min", fpsWatchStore.sessionMinFps(sessionId))
                    put("max", fpsWatchStore.sessionMaxFps(sessionId))
                    put("avg", fpsWatchStore.sessionAvgFps(sessionId))
                }.toString(2)
            }

            @JavascriptInterface
            public fun setStatusBarColor(colorStr: String): Boolean {
                try {
                    val color = Color.parseColor(colorStr)
                    vtools_online.post {
                        window.statusBarColor = color
                        if (Build.VERSION.SDK_INT >= 23) {
                            if (Color.red(color) > 180 && Color.green(color) > 180 && Color.blue(color) > 180) {
                                window.decorView.systemUiVisibility = Flags(window.decorView.systemUiVisibility).addFlag(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
                            } else {
                                window.decorView.systemUiVisibility = Flags(window.decorView.systemUiVisibility).removeFlag(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
                            }
                        }
                    }
                    return true
                } catch (ex: java.lang.Exception) {
                    return false
                }
            }

            @JavascriptInterface
            public fun setNavigationBarColor(colorStr: String): Boolean {
                try {
                    val color = Color.parseColor(colorStr)
                    vtools_online.post {
                        window.navigationBarColor = color
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (Color.red(color) > 180 && Color.green(color) > 180 && Color.blue(color) > 180) {
                                window.decorView.systemUiVisibility = Flags(window.decorView.systemUiVisibility).addFlag(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
                            } else {
                                window.decorView.systemUiVisibility = Flags(window.decorView.systemUiVisibility).removeFlag(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
                            }
                        }
                    }

                    return true
                } catch (ex: java.lang.Exception) {
                    return false
                }
            }

            @JavascriptInterface
            public fun showToast(str: String) {
                try {
                    vtools_online.post {
                        Toast.makeText(context, str, Toast.LENGTH_LONG).show()
                    }
                } catch (ex: java.lang.Exception) {
                }
            }
        }, "SceneJS")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && vtools_online.canGoBack()) {
            vtools_online.goBack()
            return true
        } else {
            return super.onKeyDown(keyCode, event)
        }
    }
    override fun onDestroy() {
        vtools_online.clearCache(true)
        vtools_online.removeAllViews()
        vtools_online.destroy()
        super.onDestroy()
    }

    public override fun onPause() {
        super.onPause()
    }
}
