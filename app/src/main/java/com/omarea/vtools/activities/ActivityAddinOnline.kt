package com.omarea.vtools.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.Toast
import com.omarea.common.shared.FilePathResolver
import com.omarea.common.shared.FileWrite
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.WebViewInjector
import com.omarea.krscript.ui.ParamsFileChooserRender
import com.omarea.library.calculator.Flags
import com.omarea.scene_mode.CpuConfigInstaller
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_addin_online.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

class ActivityAddinOnline : ActivityBase() {
    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
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


        if (this.intent.extras != null) {
            val extraData = intent.extras
            if (extraData?.containsKey("url") == true) {
                vtools_online.loadUrl(extraData.getString("url")!!)
            } else {
                vtools_online.loadUrl("https://helloklf.github.io/vtools-online.html#/scripts")
            }
        } else {
            vtools_online.loadUrl("https://helloklf.github.io/vtools-online.html#/scripts")
        }
        val context = this@ActivityAddinOnline
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

            private fun tryGetPowercfg(view: WebView?, url: String?): Boolean {
                if (url != null && view != null) {
                    // v1
                    // https://github.com/yc9559/cpufreq-interactive-opt/blob/master/vtools-powercfg/20180603/sd_845/powercfg.apk 源码地址
                    // https://github.com/yc9559/cpufreq-interactive-opt/raw/master/vtools-powercfg/20180603/sd_845/powercfg.apk 点击raw指向的链接
                    // https://raw.githubusercontent.com/yc9559/cpufreq-interactive-opt/master/vtools-powercfg/20180603/sd_845/powercfg.apk 然后重定向到具体文件
                    if (url.startsWith("https://github.com/yc9559/cpufreq-interactive-opt/") && url.contains("vtools-powercfg") && url.endsWith("powercfg.apk")) {
                        val configPath = url.substring(url.indexOf("vtools-powercfg"))
                        DialogHelper.animDialog(AlertDialog.Builder(vtools_online.context)
                                .setTitle("可用的配置脚本")
                                .setMessage("在当前页面上检测到可用于性能调节的配置脚本，是否立即将其安装到本地？\n\n配置：$configPath\n\n作者：yc9559\n\n")
                                .setPositiveButton(R.string.btn_confirm) { _, _ ->
                                    val configAbsPath = "https://github.com/yc9559/cpufreq-interactive-opt/raw/master/$configPath"
                                    downloadPowercfg(configAbsPath)
                                }
                                .setNeutralButton(R.string.btn_cancel) { _, _ ->
                                    view.loadUrl(url)
                                })?.setCancelable(false)
                    } else if (url.startsWith("https://github.com/yc9559/wipe-v2/releases/download/") && url.endsWith(".zip")) {
                        // v2
                        // https://github.com/yc9559/wipe-v2/releases/download/0.1.190503-dev/sdm625.zip
                        val configPath = url.substring(url.lastIndexOf("/") + 1).replace(".zip", "")
                        DialogHelper.animDialog(AlertDialog.Builder(vtools_online.context)
                                .setTitle("配置安装提示")
                                .setMessage("你刚刚点击的内容，似乎是一个可用于性能调节的配置脚本，是否立即将其安装到本地？\n\n配置：$configPath\n\n作者：yc9559\n\n")
                                .setPositiveButton(R.string.btn_confirm) { _, _ ->
                                    val configAbsPath = url
                                    downloadPowercfgV2(configAbsPath)
                                }
                                .setNeutralButton(R.string.btn_cancel) { _, _ ->
                                    view.loadUrl(url)
                                })?.setCancelable(false)
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

        val url = vtools_online.url
        if (url != null) {
            if (url.startsWith("https://vtools.oss-cn-beijing.aliyuncs.com/") || url.startsWith("https://vtools.omarea.com/")) {
                // 添加kr-script for web
                WebViewInjector(vtools_online,
                        object : ParamsFileChooserRender.FileChooserInterface {
                            override fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
                                return chooseFilePath(fileSelectedInterface)
                            }
                        }).inject(this, false)
            }
        }
        vtools_online.addJavascriptInterface(object {
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
        }, "SceneUI")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && vtools_online.canGoBack()) {
            vtools_online.goBack()
            return true
        } else {
            return super.onKeyDown(keyCode, event)
        }
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
                if (powercfg.startsWith("#!/") && CpuConfigInstaller().installCustomConfig(this, powercfg, ModeSwitcher.SOURCE_SCENE_ONLINE)) {
                    vtools_online.post {
                        DialogHelper.animDialog(AlertDialog.Builder(this)
                                .setTitle("配置文件已安装")
                                .setPositiveButton(R.string.btn_confirm) { _, _ ->
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                }).setCancelable(false)
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
                if (FileWrite.writePrivateFile(buffer, cacheName, baseContext)) {
                    val cachePath = FileWrite.getPrivateFilePath(baseContext, cacheName)

                    val zipInputStream = ZipInputStream(FileInputStream(File(cachePath)))
                    while (true) {
                        val zipEntry = zipInputStream.nextEntry
                        if (zipEntry == null) {
                            throw java.lang.Exception("下载的文件无效，未从中找到powercfg.sh")
                        } else if (zipEntry.name == "powercfg.sh") {
                            val byteArray = zipInputStream.readBytes()
                            val powercfg = byteArray.toString(Charset.defaultCharset())
                            if (powercfg.startsWith("#!/") && CpuConfigInstaller().installCustomConfig(this, powercfg, ModeSwitcher.SOURCE_SCENE_ONLINE)) {
                                vtools_online.post {
                                    DialogHelper.animDialog(AlertDialog.Builder(this)
                                            .setTitle("配置文件已安装")
                                            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                                                setResult(Activity.RESULT_OK)
                                                finish()
                                            }).setCancelable(false)
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

    private var fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface? = null
    private val ACTION_FILE_PATH_CHOOSER = 65400
    private fun chooseFilePath(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 2);
            Toast.makeText(this, getString(R.string.kr_write_external_storage), Toast.LENGTH_LONG).show()
            return false
        } else {
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*")
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, ACTION_FILE_PATH_CHOOSER);
                this.fileSelectedInterface = fileSelectedInterface
                return true;
            } catch (ex: java.lang.Exception) {
                return false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTION_FILE_PATH_CHOOSER) {
            val result = if (data == null || resultCode != Activity.RESULT_OK) null else data.data
            if (fileSelectedInterface != null) {
                if (result != null) {
                    val absPath = getPath(result)
                    fileSelectedInterface?.onFileSelected(absPath)
                } else {
                    fileSelectedInterface?.onFileSelected(null)
                }
            }
            this.fileSelectedInterface = null
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getPath(uri: Uri): String? {
        try {
            return FilePathResolver().getPath(this, uri)
        } catch (ex: java.lang.Exception) {
            return null
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
