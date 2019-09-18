package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.scene_mode.ModeConfigInstaller
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_addin_online.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

class ActivityAddinOnline : AppCompatActivity() {
    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()

    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeSwitch.switchTheme(this)
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
                if (powercfg.startsWith("#!/") && ModeConfigInstaller().installPowerConfigByText(this, powercfg)) {
                    vtools_online.post {
                        AlertDialog.Builder(this)
                                .setTitle("配置文件已安装")
                                .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                })
                                .setCancelable(false)
                                .create()
                                .show()
                    }
                } else {
                    vtools_online.post {
                        Toast.makeText(applicationContext, "下载配置文件失败或文件无效！", Toast.LENGTH_LONG).show()
                    }
                }
                vtools_online.post {
                    progressBarDialog.hideDialog()
                }
            } catch (ex: Exception) {
                vtools_online.post {
                    progressBarDialog.hideDialog()
                    Toast.makeText(applicationContext, "下载配置文件失败！", Toast.LENGTH_LONG).show()
                }
            }
        }).start()
    }

    private fun downloadPowercfgV2(url: String) {
        val progressBarDialog = ProgressBarDialog(this)
        progressBarDialog.showDialog("正在获取配置，稍等...")
        Thread(Runnable {
            try {
                val myURL = URL(url)
                val conn = myURL.openConnection()
                conn.connect()
                conn.getInputStream()
                val inputStream = conn.getInputStream()
                val buffer = inputStream.readBytes()
                val cacheName = "caches/powercfg_downloaded.zip"
                if (com.omarea.common.shared.FileWrite.writePrivateFile(buffer, cacheName, baseContext)) {
                    val cachePath = com.omarea.common.shared.FileWrite.getPrivateFilePath(baseContext, cacheName)

                    val zipInputStream = ZipInputStream(FileInputStream(File(cachePath)))
                    while (true) {
                        val zipEntry = zipInputStream.nextEntry
                        if (zipEntry == null) {
                            throw java.lang.Exception("下载的文件无效，未从中找到powercfg.sh")
                        } else if (zipEntry.name == "powercfg.sh") {
                            val byteArray = zipInputStream.readBytes()
                            val powercfg = byteArray.toString(Charset.defaultCharset())
                            if (powercfg.startsWith("#!/") && ModeConfigInstaller().installPowerConfigByText(this, powercfg)) {
                                vtools_online.post {
                                    AlertDialog.Builder(this)
                                            .setTitle("配置文件已安装")
                                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                                setResult(Activity.RESULT_OK)
                                                finish()
                                            })
                                            .setCancelable(false)
                                            .create()
                                            .show()
                                }
                            } else {
                                vtools_online.post {
                                    Toast.makeText(applicationContext, "下载配置文件失败或文件无效！", Toast.LENGTH_LONG).show()
                                }
                            }
                            vtools_online.post {
                                progressBarDialog.hideDialog()
                            }
                            break
                        } else {
                            zipInputStream.skip(zipEntry.size)
                        }
                    }
                } else {
                    throw IOException("文件存储失败")
                }
            } catch (ex: Exception) {
                vtools_online.post {
                    progressBarDialog.hideDialog()
                    Toast.makeText(applicationContext, "下载配置文件失败！", Toast.LENGTH_LONG).show()
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
            private fun tryGetPowercfg(view: WebView?, url: String?): Boolean {
                if (url != null && view != null) {
                    // v1
                    // https://github.com/yc9559/cpufreq-interactive-opt/blob/master/vtools-powercfg/20180603/sd_845/powercfg.apk 源码地址
                    // https://github.com/yc9559/cpufreq-interactive-opt/raw/master/vtools-powercfg/20180603/sd_845/powercfg.apk 点击raw指向的链接
                    // https://raw.githubusercontent.com/yc9559/cpufreq-interactive-opt/master/vtools-powercfg/20180603/sd_845/powercfg.apk 然后重定向到具体文件
                    if (url.startsWith("https://github.com/yc9559/cpufreq-interactive-opt/") && url.contains("vtools-powercfg") && url.endsWith("powercfg.apk")) {
                        val configPath = url.substring(url.indexOf("vtools-powercfg"))
                        AlertDialog.Builder(vtools_online.context)
                                .setTitle("可用的配置脚本")
                                .setMessage("在当前页面上检测到可用于动态响应的配置脚本，是否立即将其安装到本地？\n\n配置：$configPath\n\n作者：yc9559\n\n")
                                .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                    val configAbsPath = "https://github.com/yc9559/cpufreq-interactive-opt/raw/master/$configPath"
                                    downloadPowercfg(configAbsPath)
                                })
                                .setCancelable(false)
                                .setNeutralButton(R.string.btn_cancel, { _, _ ->
                                    view.loadUrl(url)
                                })
                                .create()
                                .show()
                    } else if (url.startsWith("https://github.com/yc9559/wipe-v2/releases/download/") && url.endsWith(".zip")) {
                        // v2
                        // https://github.com/yc9559/wipe-v2/releases/download/0.1.190503-dev/sdm625.zip
                        val configPath = url.substring(url.lastIndexOf("/") + 1).replace(".zip", "")
                        AlertDialog.Builder(vtools_online.context)
                                .setTitle("配置安装提示")
                                .setMessage("你刚刚点击的内容，似乎是一个可用于动态响应的配置脚本，是否立即将其安装到本地？\n\n配置：$configPath\n\n作者：yc9559\n\n")
                                .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                    val configAbsPath = url
                                    downloadPowercfgV2(configAbsPath)
                                })
                                .setCancelable(false)
                                .setNeutralButton(R.string.btn_cancel, { _, _ ->
                                    view.loadUrl(url)
                                })
                                .create()
                                .show()
                    } else {
                        view.loadUrl(url)
                    }
                    return true
                }
                return false
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return tryGetPowercfg(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (view != null && request != null) {
                    val url = request.url.toString()
                    return this.tryGetPowercfg(view, url)
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        })
        vtools_online.settings.javaScriptEnabled = true
        vtools_online.settings.setLoadWithOverviewMode(true);
        vtools_online.settings.setUseWideViewPort(true);

        // vtools_online.addJavascriptInterface(VToolsOnlineNative(this, vtools_online), "VToolsNative")
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
