package com.omarea.vtools.activities

import android.Manifest
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
import android.util.TypedValue
import android.view.View
import com.omarea.permissions.BusyboxInstaller2
import com.omarea.permissions.CheckRootStatus
import com.omarea.permissions.WriteSettings
import com.omarea.store.SpfConfig
import com.omarea.utils.CrashHandler
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_start_splash.*
import java.lang.ref.WeakReference

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
        checkRoot(CheckFileWirte(this), CheckRootFail(this))
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

    private class CheckFileWirte(context: ActivityStartSplash) : Runnable {
        private var context: WeakReference<ActivityStartSplash>;
        override fun run() {
            context.get()!!.start_state_text.text = "检查并获取必需权限……"
            context.get()!!.hasRoot = true

            context.get()!!.checkFileWrite(InstallBusybox(context.get()!!))
        }

        init {
            this.context = WeakReference(context)
        }
    }

    private class InstallBusybox(context: ActivityStartSplash) : Runnable {
        private var context: WeakReference<ActivityStartSplash> = WeakReference(context)
        override fun run() {
            val installer2 = BusyboxInstaller2(context.get()!!)
            context.get()!!.start_state_text.text = "初始化Busybox……"
            if (!installer2.busyboxInstalled()) {
                val handler = Handler()
                Thread(Runnable {
                    if (installer2.installPrivateBusybox()) {
                        handler.post {
                            BusyboxInstalled(context.get()!!).run()
                        }
                    } else {
                        handler.post {
                            context.get()!!.start_state_text.text = "请通过其它方式安装Busybox……"
                        }
                    }
                }).start()
            } else {
                BusyboxInstalled(context.get()!!).run()
            }
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

    /**
     * 启动完成
     */
    private fun startToFinish() {
        start_state_text.text = "启动完成！"

        val intent = Intent(this.applicationContext, ActivityMain::class.java)
        startActivity(intent)
        finish()
    }
}