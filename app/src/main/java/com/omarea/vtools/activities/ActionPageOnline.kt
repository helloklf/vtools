package com.omarea.vtools.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.omarea.common.shared.FilePathResolver
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.WebViewInjector
import com.omarea.krscript.downloader.Downloader
import com.omarea.krscript.ui.ParamsFileChooserRender
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_action_page_online.*
import java.util.*

class ActionPageOnline : ActivityBase() {
    private val progressBarDialog = ProgressBarDialog(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action_page_online)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        setTitle(R.string.app_name)

        // 显示返回按钮
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        loadIntentData()
    }

    private fun hideWindowTitle() {
        if (Build.VERSION.SDK_INT >= 21) {
            val decorView = window.decorView
            val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            decorView.systemUiVisibility = option
            window.statusBarColor = Color.TRANSPARENT
        }
        val actionBar = supportActionBar
        actionBar!!.hide()
    }

    private fun setWindowTitleBar() {
        val window = window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        var flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        if (themeMode.isDarkMode) {
        } else {
            window.statusBarColor = Color.WHITE
            window.navigationBarColor = Color.WHITE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
        }
        getWindow().decorView.systemUiVisibility = flags

        kr_online_root.fitsSystemWindows = true
    }

    private fun loadIntentData() {
        // 读取intent里的参数
        val intent = this.intent
        if (intent.extras != null) {
            val extras = intent.extras
            if (extras != null) {
                if (extras.containsKey("title")) {
                    title = extras.getString("title")!!
                }

                // config、url 都用于设定要打卡的网页
                /*

                when {
                    extras.containsKey("config") -> {
                        initWebview(extras.getString("config"))
                        hideWindowTitle() // 作为网页浏览器时，隐藏标题栏
                    }
                    extras.containsKey("url") -> {
                        initWebview(extras.getString("url"))
                        hideWindowTitle() // 作为网页浏览器时，隐藏标题栏
                    }
                    else -> {
                        setWindowTitleBar()
                    }
                }
                */
                setWindowTitleBar()
                when {
                    extras.containsKey("config") -> initWebview(extras.getString("config")!!)
                    extras.containsKey("url") -> initWebview(extras.getString("url")!!)
                }

                if (extras.containsKey("downloadUrl")) {
                    val downloader = Downloader(this)
                    val url = extras.getString("downloadUrl")!!
                    val taskAliasId = if (extras.containsKey("taskId")) extras.getString("taskId")!! else UUID.randomUUID().toString()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        downloader.saveTaskStatus(taskAliasId, 0)

                        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 2);
                        DialogHelper.helpInfo(this, "", getString(R.string.kr_write_external_storage))
                    } else {
                        val downloadId = downloader.downloadBySystem(url, null, null, taskAliasId)
                        if (downloadId != null) {
                            kr_download_url.text = url
                            val autoClose = extras.containsKey("autoClose") && extras.getBoolean("autoClose")

                            downloader.saveTaskStatus(taskAliasId, 0)
                            watchDownloadProgress(downloadId, autoClose, taskAliasId)
                        } else {
                            downloader.saveTaskStatus(taskAliasId, -1)
                        }
                    }
                }
            }
        }
    }

    private fun initWebview(url: String) {
        kr_online_webview.visibility = View.VISIBLE
        kr_online_webview.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                DialogHelper.animDialog(
                        AlertDialog.Builder(this@ActionPageOnline)
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
                        AlertDialog.Builder(this@ActionPageOnline)
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

        kr_online_webview.webViewClient = object : WebViewClient() {
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

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                try {
                    val requestUrl = request?.url
                    if (requestUrl != null && requestUrl.scheme?.startsWith("http") != true) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl.toString()));
                        startActivity(intent);
                        return true;
                    } else {
                        return super.shouldOverrideUrlLoading(view, request);
                    }
                } catch (e: Exception) {
                    return super.shouldOverrideUrlLoading(view, request);
                }
            }
        }

        kr_online_webview.loadUrl(url)

        WebViewInjector(kr_online_webview,
                object : ParamsFileChooserRender.FileChooserInterface {
                    override fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
                        return chooseFilePath(fileSelectedInterface)
                    }
                }).inject(this, url.startsWith("file:///android_asset"))
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && kr_online_webview.canGoBack()) {
            kr_online_webview.goBack()
            return true
        } else {
            return super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        stopWatchDownloadProgress()
        super.onDestroy()
    }

    private fun stopWatchDownloadProgress() {
        if (progressPolling != null) {
            progressPolling?.cancel()
            progressPolling = null
        }
    }

    var progressPolling: Timer? = null

    /**
     * 监视下载进度
     */
    private fun watchDownloadProgress(downloadId: Long, autoClose: Boolean, taskAliasId: String) {
        kr_download_state.visibility = View.VISIBLE

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)

        kr_download_name_copy.setOnClickListener {
            val myClipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val myClip = ClipData.newPlainText("text", kr_download_name.text.toString())
            myClipboard.setPrimaryClip(myClip)
            Toast.makeText(this@ActionPageOnline, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
        }
        kr_download_url_copy.setOnClickListener {
            val myClipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val myClip = ClipData.newPlainText("text", kr_download_url.text.toString())
            myClipboard.setPrimaryClip(myClip)
            Toast.makeText(this@ActionPageOnline, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
        }

        val handler = Handler(Looper.getMainLooper())
        val downloader = Downloader(this)
        progressPolling = Timer()
        progressPolling?.schedule(object : TimerTask() {
            override fun run() {
                val cursor = downloadManager.query(query)
                var fileName = ""
                var absPath = ""
                if (cursor.moveToFirst()) {
                    val downloadBytesIdx = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalBytesIdx = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val totalBytes = cursor.getLong(totalBytesIdx)
                    val downloadBytes = cursor.getLong(downloadBytesIdx)
                    val ratio = (downloadBytes * 100 / totalBytes).toInt()
                    if (fileName.isEmpty()) {
                        try {
                            val nameColumn = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                            fileName = cursor.getString(nameColumn)
                            absPath = FilePathResolver().getPath(this@ActionPageOnline, Uri.parse(fileName))
                            if (!absPath.isEmpty()) {
                                fileName = absPath
                            }
                        } catch (ex: java.lang.Exception) {
                        }
                    }

                    handler.post {
                        kr_download_name.text = fileName
                        kr_download_progress.progress = ratio
                        kr_download_progress.isIndeterminate = false
                        setTitle(R.string.kr_download_downloading)
                        downloader.saveTaskStatus(taskAliasId, ratio)
                    }

                    if (ratio >= 100) {
                        // 保存下载成功后的路径
                        downloader.saveTaskCompleted(downloadId, absPath)

                        handler.post {
                            setTitle(R.string.kr_download_completed)
                            kr_download_progress.visibility = View.GONE
                            stopWatchDownloadProgress()

                            val result = Intent()
                            result.putExtra("absPath", absPath)
                            setResult(0, result)

                            if (autoClose) {
                                finish()
                            }
                        }
                    }
                }
            }
        }, 200, 500)
    }
}
