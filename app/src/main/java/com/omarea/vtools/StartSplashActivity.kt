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
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.PermissionChecker
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import com.omarea.shared.ConfigInstaller
import com.omarea.shared.Consts
import com.omarea.shared.SpfConfig
import com.omarea.shell.Busybox
import com.omarea.shell.CheckRootStatus
import com.omarea.shell.KeepShellSync
import com.omarea.shell.WriteSettings
import com.omarea.shell.units.BackupRestoreUnit
import com.omarea.shell.units.BatteryUnit
import com.omarea.ui.ProgressBarDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_start_splash.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class StartSplashActivity : Activity() {
    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreen_content.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        actionBar?.show()
        fullscreen_content_controls.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = View.OnTouchListener { _, _ ->
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_start_splash)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        mVisible = true

        // Set up the user interaction to manually show or hide the system UI.
        fullscreen_content.setOnClickListener { toggle() }

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        dummy_button.setOnTouchListener(mDelayHideTouchListener)


        //checkFileWrite()
        checkRoot(Runnable {
            start_state_text.text = "检查并获取必需权限..."
            hasRoot = true
            checkFileWrite(Runnable{
                start_state_text.text = "检查Busybox是否安装..."
                Busybox(this).forceInstall(Runnable {
                    configInstallerThread = Thread(Runnable {
                        ConfigInstaller().configCodeVerify(this)
                    })
                    configInstallerThread!!.start()
                    next()
                })
            })
        }, Runnable {
            next()
        })
    }

    private fun checkPermission(permission: String): Boolean =
            PermissionChecker.checkSelfPermission(this.applicationContext, permission) == PermissionChecker.PERMISSION_GRANTED


    private var configInstallerThread: Thread? = null
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
                        CheckRootStatus(this, next, skip, false).forceGetRoot()
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
                        KeepShellSync.doCmdSync(Consts.DisableSELinux + "\n sleep 10; \n")
                        myHandler.post {
                            next.run()
                        }
                        globalConfig.edit()
                                .putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, true)
                                .remove(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE_CHECKING)
                                .commit()
                    }).start()
                }
            }, skip, false).forceGetRoot()
        } else {
            val disableSeLinux = globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, true)
            CheckRootStatus(this, next, skip, disableSeLinux).forceGetRoot()
        }
    }

    private fun next() {
        start_state_text.text = "启动完成！"
        //判断是否开启了充电加速和充电保护，如果开启了，自动启动后台服务
        val chargeConfig = getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
        if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false) || chargeConfig!!.getBoolean(SpfConfig.CHARGE_SPF_BP, false)) {
            try {
                val intent = Intent(this.applicationContext, ServiceBattery::class.java)
                this.applicationContext.startService(intent)
            } catch (ex: Exception) {
                Log.e("startChargeService", ex.message)
            }
        }
        setResult(if (hasRoot) RESULT_OK else RESULT_CANCELED)
        finish()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            if (getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
                            .getBoolean(SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT, false)) {
                //this.finishAndRemoveTask()
            }
            if (this.configInstallerThread != null && !this.configInstallerThread!!.isInterrupted) {
                this.configInstallerThread!!.destroy()
            }
        } catch (ex: Exception) {
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        delayedHide(100)
    }

    private fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        actionBar?.hide()
        fullscreen_content_controls.visibility = View.GONE
        mVisible = false

        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        fullscreen_content.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300
    }
}
