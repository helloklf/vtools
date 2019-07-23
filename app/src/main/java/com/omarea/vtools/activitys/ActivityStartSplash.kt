package com.omarea.vtools.activitys

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.PermissionChecker
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import com.omarea.shared.CrashHandler
import com.omarea.shared.SpfConfig
import com.omarea.shell.Busybox
import com.omarea.shell.CheckRootStatus
import com.omarea.shell.WriteSettings
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_start_splash.*
import java.lang.ref.WeakReference
import java.util.*

class ActivityStartSplash : Activity() {
    private var globalSPF: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CheckRootStatus.lastCheckResult) {
            if (isTaskRoot) {
                val intent = Intent(this.applicationContext, ActivityMain::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(intent)
                // overridePendingTransition(0, 0)
            }
            finish()
            return
        }

        CrashHandler().init(this.applicationContext)

        if (globalSPF == null) {
            globalSPF = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        }
        if (globalSPF!!.getInt(SpfConfig.GLOBAL_SPF_THEME, 1) != 8) {
            ThemeSwitch.switchTheme(this)
        }

        setContentView(R.layout.activity_start_splash)
        updateThemeStyle()

        if (getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE).getBoolean(SpfConfig.GLOBAL_SPF_CONTRACT, false) != true) {
            initContractAction()
        } else {
            checkPermissions()
        }
    }

    /**
     * 协议 同意与否
     */
    private fun initContractAction() {
        start_contract.visibility = View.VISIBLE
        start_logo.visibility = View.GONE
        // 协议同意
        contract_confirm.setOnClickListener {
            getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE).edit().putBoolean(SpfConfig.GLOBAL_SPF_CONTRACT, true).apply()
            checkPermissions()
        }
        // 协议不同意
        contract_exit.setOnClickListener {
            // val intent = Intent()
            // intent.setAction(Intent.ACTION_MAIN)
            // intent.addCategory(Intent.CATEGORY_HOME)
            // startActivity(intent)
            finish()
        }
    }

    /**
     * 界面主题样式调整
     */
    private fun updateThemeStyle() {
        if (globalSPF!!.getInt(SpfConfig.GLOBAL_SPF_THEME, 1) == 8) {
            splash_root.setBackgroundColor(Color.argb(255, 0, 0, 0))
            getWindow().setNavigationBarColor(Color.argb(255, 0, 0, 0))
        } else {
            getWindow().setNavigationBarColor(getColorAccent())
        }

        //  得到当前界面的装饰视图
        if (Build.VERSION.SDK_INT >= 21) {
            val decorView = getWindow().getDecorView();
            //让应用主题内容占用系统状态栏的空间,注意:下面两个参数必须一起使用 stable 牢固的
            val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            decorView.setSystemUiVisibility(option);
            //设置状态栏颜色为透明
            getWindow().setStatusBarColor(Color.TRANSPARENT)
        }
    }

    private fun getColorAccent(): Int {
        val typedValue = TypedValue()
        this.theme.resolveAttribute(R.attr.colorAccent, typedValue, true)
        return typedValue.data
    }

    /**
     * 开始检查必需权限
     */
    private fun checkPermissions() {
        start_contract.visibility = View.GONE
        start_logo.visibility = View.VISIBLE
        checkRoot(CheckRootSuccess(this), CheckRootFail(this))
    }

    /**
     * 获取root权限失败
     */
    private class CheckRootFail(context: ActivityStartSplash) : Runnable {
        private var context: WeakReference<ActivityStartSplash>;
        override fun run() {
            context.get()!!.startToFinish()
        }

        init {
            this.context = WeakReference(context)
        }
    }

    private class CheckRootSuccess(context: ActivityStartSplash) : Runnable {
        private var context: WeakReference<ActivityStartSplash>;
        override fun run() {
            context.get()!!.start_state_text.text = "检查并获取必需权限..."
            context.get()!!.hasRoot = true

            context.get()!!.checkFileWrite(CheckFileWriteSuccess(context.get()!!))
        }

        init {
            this.context = WeakReference(context)
        }
    }

    private class CheckFileWriteSuccess(context: ActivityStartSplash) : Runnable {
        private var context: WeakReference<ActivityStartSplash>;
        override fun run() {
            context.get()!!.start_state_text.text = "检查Busybox是否安装..."
            Busybox(context.get()!!).forceInstall(BusyboxInstalled(context.get()!!))
        }

        init {
            this.context = WeakReference(context)
        }
    }

    private class BusyboxInstalled(context: ActivityStartSplash) : Runnable {
        private var context: WeakReference<ActivityStartSplash>;
        override fun run() {
            context.get()!!.startToFinish()
        }

        init {
            this.context = WeakReference(context)
        }
    }

    private fun checkPermission(permission: String): Boolean = PermissionChecker.checkSelfPermission(this.applicationContext, permission) == PermissionChecker.PERMISSION_GRANTED

    /**
     * 检查权限 主要是文件读写权限
     */
    private fun checkFileWrite(next: Runnable) {
        Thread(Runnable {
            CheckRootStatus.grantPermission(this)
            if (!(checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE) && checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ActivityCompat.requestPermissions(
                            this@ActivityStartSplash,
                            arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                                    Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Manifest.permission.WAKE_LOCK
                            ),
                            0x11
                    )
                } else {
                    ActivityCompat.requestPermissions(
                            this@ActivityStartSplash,
                            arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                                    Manifest.permission.WAKE_LOCK
                            ),
                            0x11
                    )
                }
            }
            myHandler.post {
                val writeSettings = WriteSettings()
                if (!writeSettings.getPermission(applicationContext)) {
                    writeSettings.setPermission(applicationContext)
                }
                next.run()
            }
        }).start()
    }

    private var hasRoot = false
    private var myHandler = Handler()

    private fun checkRoot(next: Runnable, skip: Runnable) {
        val globalConfig = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        val disableSeLinux = globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)
        CheckRootStatus(this, next, disableSeLinux, Runnable {
            startToFinish()
        }).forceGetRoot()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (splash_adview.visibility == View.VISIBLE) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun setTimer() {
        val timer = Timer(true)
        var timeOut = 3
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                timeOut -= 1
                myHandler.post {
                    if (timeOut < 1) {
                        btn_skip_ad.visibility = View.VISIBLE
                        startToFinish()
                    } else {

                    }
                }
            }
        }, 0, 1000L)
    }

    private fun downloadAd() {
        try {
            val uri = Uri.parse(
                    getSharedPreferences(SpfConfig.AD_CONFIG, Context.MODE_PRIVATE).getString(
                            SpfConfig.AD_CONFIG_A_LINK,
                            getString(R.string.promote_link)))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (ex: Exception) {
            startToFinish()
        }
    }

    /**
     * 启动完成
     */
    private fun startToFinish() {
        val adConfig = getSharedPreferences(SpfConfig.AD_CONFIG, Context.MODE_PRIVATE)
        val hideAd = adConfig.getBoolean(SpfConfig.AD_CONFIG_HIDE_A, false)
        val adUrl = adConfig.getString(SpfConfig.AD_CONFIG_A_LINK, "")!!

        if (!hideAd && adUrl.isNotEmpty() && splash_adview.visibility == View.GONE) {
            splash_adview.visibility = View.VISIBLE
            splash_ad_download.setOnClickListener {
                downloadAd()
            }
            splash_ad_image.setOnClickListener {
                downloadAd()
            }
            btn_skip_ad.setOnClickListener {
                startToFinish()
            }
            setTimer()
        } else {
            start_state_text.text = "启动完成！"

            val intent = Intent(this.applicationContext, ActivityMain::class.java)
            startActivity(intent)
            finish()
        }
    }
}