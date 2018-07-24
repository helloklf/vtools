package com.omarea.vboot

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.omarea.scripts.VToolsOnlineNative
import com.omarea.shared.ConfigInstaller
import com.omarea.shared.FileWrite
import com.omarea.shared.SpfConfig
import com.omarea.ui.ProgressBarDialog
import kotlinx.android.synthetic.main.activity_addin_online.*
import java.net.URL
import java.nio.charset.Charset


class ActivityAddinOnline : AppCompatActivity() {
    private lateinit var spf: SharedPreferences

    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()

    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (spf.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false))
            this.setTheme(R.style.AppTheme_NoActionBarNight)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addin_online)
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
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (vtools_online.canGoBack()) {
            vtools_online.goBack()
            return true;
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun downloadPowercfg(url: String) {
        val progressBarDialog = ProgressBarDialog(this)
        progressBarDialog.showDialog("正在获取配置，稍等...")
        Thread(Runnable {
            try {
                val myURL = URL(url)
                val conn = myURL.openConnection()
                conn.connect()
                conn.getInputStream()
                val reader = conn.getInputStream().bufferedReader(Charset.forName("UTF-8"))
                val powercfg = reader.readText()
                if (powercfg.startsWith("#!/") && ConfigInstaller().installPowerConfig(this, powercfg)) {
                    vtools_online.post {
                        AlertDialog.Builder(this)
                                .setTitle("配置文件已安装")
                                .setPositiveButton(R.string.btn_confirm, {
                                    _,_ ->
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                })
                                .setCancelable(false)
                                .create()
                                .show()
                    }
                } else {
                    vtools_online.post {
                        Toast.makeText(this, "下载配置文件失败或文件无效！", Toast.LENGTH_LONG).show()
                    }
                }
                vtools_online.post {
                    progressBarDialog.hideDialog()
                }
            } catch (ex: Exception) {
                vtools_online.post {
                    progressBarDialog.hideDialog()
                    Toast.makeText(this, "下载配置文件失败！", Toast.LENGTH_LONG).show()
                }
            }
        }).start()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onStart() {
        super.onStart()

        if (this.intent.extras != null) {
            val extraData = intent.extras
            if (extraData.containsKey("url")) {
                vtools_online.loadUrl(extraData.getString("url"))
            } else {
                vtools_online.loadUrl("https://helloklf.github.io/vtools-online.html#/scripts")
            }
        } else {
            vtools_online.loadUrl("https://helloklf.github.io/vtools-online.html#/scripts")
        }
        //vtools_online.setWebChromeClient(object : WebChromeClient() { })
        vtools_online.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (view != null && request != null) {
                    val url = request.url.toString()

                    // https://github.com/yc9559/cpufreq-interactive-opt/blob/master/vtools-powercfg/20180603/sd_845/powercfg.apk 源码地址
                    // https://github.com/yc9559/cpufreq-interactive-opt/raw/master/vtools-powercfg/20180603/sd_845/powercfg.apk 点击raw指向的链接
                    // https://raw.githubusercontent.com/yc9559/cpufreq-interactive-opt/master/vtools-powercfg/20180603/sd_845/powercfg.apk 然后重定向到具体文件
                    if (url.startsWith("https://github.com/yc9559/cpufreq-interactive-opt/") && url.contains("vtools-powercfg") && url.endsWith("powercfg.apk")) {
                        val configPath = url.substring(url.indexOf("vtools-powercfg"))
                        AlertDialog.Builder(vtools_online.context)
                                .setTitle("可用的配置脚本")
                                .setMessage("在当前页面上检测到可用于动态响应的配置脚本，是否立即将其安装到本地？\n\n配置名称：$configPath")
                                .setPositiveButton(R.string.btn_confirm, {
                                    _, _ ->
                                    val configAbsPath = "https://github.com/yc9559/cpufreq-interactive-opt/raw/master/$configPath"
                                    // view.loadUrl(configAbsPath)
                                    downloadPowercfg(configAbsPath)
                                })
                                .setCancelable(false)
                                .setNeutralButton(R.string.btn_cancel, {
                                    _, _ ->
                                    view.loadUrl(request.url.toString())
                                })
                                .create()
                                .show()
                    } else {
                        view.loadUrl(request.url.toString())
                    }
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        })
        //vtools_online.setWebViewClient(WebViewClient())
        vtools_online.settings.javaScriptEnabled = true
        vtools_online.settings.setLoadWithOverviewMode(true);
        vtools_online.settings.setUseWideViewPort(true);
        //vtools_online.loadUrl("http://192.168.2.144/")

        //vtools_online.loadUrl("file:///android_asset/index.html")
        //vtools_online.loadUrl("http://192.168.2.118:8080/#/scripts")
        vtools_online.addJavascriptInterface(VToolsOnlineNative(this, vtools_online), "VToolsNative")
    }

    public override fun onPause() {
        super.onPause()
    }
}
