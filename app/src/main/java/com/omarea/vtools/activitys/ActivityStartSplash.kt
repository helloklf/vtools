package com.omarea.vtools.activitys

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.PermissionChecker
import android.util.Log
import android.util.TypedValue
import android.view.View
import com.omarea.shared.ConfigInstaller
import com.omarea.shared.SpfConfig
import com.omarea.shell.Busybox
import com.omarea.shell.CheckRootStatus
import com.omarea.shell.WriteSettings
import com.omarea.vtools.R
import com.omarea.vtools.services.ServiceBattery
import kotlinx.android.synthetic.main.activity_start_splash.*
import java.lang.ref.WeakReference

class ActivityStartSplash : Activity() {
    private var globalSPF: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (globalSPF == null) {
            globalSPF = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        }
        if (globalSPF!!.getInt(SpfConfig.GLOBAL_SPF_THEME, 1) != 8) {
            ThemeSwitch.switchTheme(this)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_splash)

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
            val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE // or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(option);
            //设置状态栏颜色为透明
            getWindow().setStatusBarColor(Color.TRANSPARENT)
        }

        //checkFileWrite()
        val config = getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
        if (config.getBoolean(SpfConfig.GLOBAL_SPF_CONTRACT, false) != true) {
            start_contract.visibility = View.VISIBLE
            start_logo.visibility = View.GONE
            contract_confirm.setOnClickListener {
                start_contract.visibility = View.GONE
                start_logo.visibility = View.VISIBLE
                checkRoot(CheckRootSuccess(this), CheckRootFail(this))
                config.edit().putBoolean(SpfConfig.GLOBAL_SPF_CONTRACT, true).apply()
            }
            contract_exit.setOnClickListener {
                val intent = Intent()
                intent.setAction(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                startActivity(intent)
            }
        } else {
            start_contract.visibility = View.GONE
            start_logo.visibility = View.VISIBLE
            checkRoot(CheckRootSuccess(this), CheckRootFail(this))
        }
    }

    fun getColorAccent(): Int {
        val typedValue = TypedValue()
        this.theme.resolveAttribute(R.attr.colorAccent, typedValue, true)
        return typedValue.data
    }

    private class CheckRootFail(context: ActivityStartSplash) : Runnable {
        private var context: WeakReference<ActivityStartSplash>;
        override fun run() {
            context.get()!!.next()
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
            ConfigInstallerThread(context.get()!!.applicationContext).start()
            context.get()!!.next()
        }

        init {
            this.context = WeakReference(context)
        }
    }

    private class ConfigInstallerThread(context: Context) : Thread() {
        private var context: WeakReference<Context>;
        override fun run() {
            super.run()
            ConfigInstaller().configCodeVerify()
        }

        init {
            this.context = WeakReference(context)
        }
    }

    private class ServiceCreateThread(context: Context) : Runnable {
        private var context: WeakReference<Context>;
        override fun run() {
            //判断是否开启了充电加速和充电保护，如果开启了，自动启动后台服务
            val chargeConfig = context.get()!!.getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
            if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false) || chargeConfig!!.getBoolean(SpfConfig.CHARGE_SPF_BP, false)) {
                try {
                    val intent = Intent(context.get()!!, ServiceBattery::class.java)
                    context.get()!!.startService(intent)
                } catch (ex: Exception) {
                    Log.e("startChargeService", ex.message)
                }
            }
        }

        init {
            this.context = WeakReference(context)
        }
    }

    private fun checkPermission(permission: String): Boolean =
            PermissionChecker.checkSelfPermission(this.applicationContext, permission) == PermissionChecker.PERMISSION_GRANTED


    //检查权限 主要是文件读写权限
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
    @SuppressLint("ApplySharedPref", "CommitPrefEdits")
    private fun checkRoot(next: Runnable, skip: Runnable) {
        val globalConfig = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        val disableSeLinux = globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)
        CheckRootStatus(this, next, disableSeLinux).forceGetRoot()
    }

    private fun next() {
        start_state_text.text = "启动完成！"
        setResult(if (hasRoot) RESULT_OK else RESULT_CANCELED)
        ServiceCreateThread(this.applicationContext).run()
        finish()
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }
}