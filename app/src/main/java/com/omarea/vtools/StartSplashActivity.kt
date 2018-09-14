package com.omarea.vtools

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.PermissionChecker
import android.support.v7.app.AlertDialog
import android.util.Log
import com.omarea.shared.CommonCmds
import com.omarea.shared.ConfigInstaller
import com.omarea.shared.SpfConfig
import com.omarea.shell.Busybox
import com.omarea.shell.CheckRootStatus
import com.omarea.shell.KeepShellPublic
import com.omarea.shell.WriteSettings
import kotlinx.android.synthetic.main.activity_start_splash.*
import java.lang.ref.WeakReference

class StartSplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_start_splash)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        //checkFileWrite()
        checkRoot(CheckRootSuccess(this), CheckRootFail(this))
    }

    private class CheckRootFail(context:StartSplashActivity) : Runnable {
        private var context:WeakReference<StartSplashActivity>;
        override fun run() {
            context.get()!!.next()
        }
        init {
            this.context = WeakReference(context)
        }
    }

    private class CheckRootSuccess(context:StartSplashActivity) : Runnable {
        private var context:WeakReference<StartSplashActivity>;
        override fun run() {
            context.get()!!.start_state_text.text = "检查并获取必需权限..."
            context.get()!!.hasRoot = true

            context.get()!!.checkFileWrite(CheckFileWriteSuccess(context.get()!!))
        }
        init {
            this.context = WeakReference(context)
        }
    }

    private class CheckFileWriteSuccess(context:StartSplashActivity) : Runnable {
        private var context:WeakReference<StartSplashActivity>;
        override fun run() {
            context.get()!!.start_state_text.text = "检查Busybox是否安装..."
            Busybox(context.get()!!).forceInstall(BusyboxInstalled(context.get()!!))
        }
        init {
            this.context = WeakReference(context)
        }
    }

    private class BusyboxInstalled(context:StartSplashActivity) : Runnable {
        private var context:WeakReference<StartSplashActivity>;
        override fun run() {
            ConfigInstallerThread(context.get()!!.applicationContext).start()
            context.get()!!.next()
        }
        init {
            this.context = WeakReference(context)
        }
    }

    private class ConfigInstallerThread(context: Context): Thread() {
        private var context:WeakReference<Context>;
        override fun run() {
            super.run()
            ConfigInstaller().configCodeVerify(context.get()!!)
        }
        init {
            this.context = WeakReference(context)
        }
    }

    private class ServiceCreateThread(context: Context): Runnable {
        private var context:WeakReference<Context>;
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
                            this@StartSplashActivity,
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
                            this@StartSplashActivity,
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
        if (globalConfig.contains(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE_CHECKING)) {
            AlertDialog.Builder(this)
                    .setTitle("兼容性问题")
                    .setMessage("检测到你的设备在上次“兼容性检测”过程中断，“自动SELinux宽容模式”将不会被开启！\n\n因此，有些功能可能无法使用！")
                    .setPositiveButton(R.string.btn_confirm, DialogInterface.OnClickListener { dialog, which ->
                        globalConfig.edit()
                                .putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE_CHECKING, false)
                                .remove(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE_CHECKING)
                                .commit()
                        CheckRootStatus(this, next, false).forceGetRoot()
                    })
                    .setCancelable(false)
                    .create()
                    .show()
            return
        }
        if (!globalConfig.contains(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE)) {
            CheckRootStatus(this, Runnable {
                myHandler.post {
                    globalConfig.edit().putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE_CHECKING, true)
                            .commit()
                    start_state_text.text = "兼容性检测，稍等10秒..."
                    Thread(Runnable {
                        KeepShellPublic.doCmdSync(CommonCmds.DisableSELinux + "\n sleep 10; \n")
                        myHandler.post {
                            next.run()
                        }
                        globalConfig.edit()
                                .putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)
                                .remove(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE_CHECKING)
                                .commit()
                    }).start()
                }
            }, false).forceGetRoot()
        } else {
            val disableSeLinux = globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)
            CheckRootStatus(this, next, disableSeLinux).forceGetRoot()
        }
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