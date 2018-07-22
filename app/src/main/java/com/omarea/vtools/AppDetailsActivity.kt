package com.omarea.vtools

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.KeyEvent
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import com.omarea.shared.*
import com.omarea.shell.KeepShellSync
import com.omarea.shell.NoticeListing
import com.omarea.shell.Platform
import com.omarea.shell.WriteSettings
import kotlinx.android.synthetic.main.activity_app_details.*
import java.io.File
import java.util.*


class AppDetailsActivity : AppCompatActivity() {
    var app = ""
    lateinit var policyControl: PolicyControl
    lateinit var appConfigInfo: AppConfigStore.AppConfigInfo
    private var dynamicCpu: Boolean = false
    private var _result = RESULT_CANCELED

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        val spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (spf.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false)) {
            this.setTheme(R.style.AppTheme_NoActionBarNight)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_details)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        app = this.intent.extras.getString("app")
        policyControl = PolicyControl(contentResolver)

        dynamicCpu = (Platform().dynamicSupport(this) || File(Consts.POWER_CFG_PATH).exists())

        app_details_dynamic.setOnClickListener {
            if (!dynamicCpu) {
                Snackbar.make(it, "未安装模式配置脚本，无法使用动态响应！", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val modeList = ModeList(this)
            val powercfg = getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
            val currentMode = powercfg.getString(app, getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, modeList.BALANCE))
            var index = 0
            when (currentMode) {
                modeList.POWERSAVE -> index = 0
                modeList.BALANCE -> index = 1
                modeList.PERFORMANCE -> index = 2
                modeList.FAST -> index = 3
                modeList.IGONED -> index = 4
            }
            var selectedIndex = index
            AlertDialog.Builder(this)
                    .setTitle("性能调节")
                    .setSingleChoiceItems(R.array.powercfg_modes, index, DialogInterface.OnClickListener { dialog, which ->
                        selectedIndex = which
                    })
                    .setPositiveButton(R.string.btn_confirm, DialogInterface.OnClickListener { dialog, which ->
                        if (index != selectedIndex) {
                            var modeName = modeList.BALANCE
                            when (selectedIndex) {
                                0 -> modeName = modeList.POWERSAVE
                                1 -> modeName = modeList.BALANCE
                                2 -> modeName = modeList.PERFORMANCE
                                3 -> modeName = modeList.FAST
                                4 -> modeName = modeList.IGONED
                            }
                            powercfg.edit().putString(app, modeName).commit()
                            app_details_dynamic.text = modeList.getModName(modeName)
                            _result = RESULT_OK
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, DialogInterface.OnClickListener { dialog, which -> })
                    .create()
                    .show()
        }
        app_details_floatwindow.setOnClickListener {
            val isChecked = (it as Switch).isChecked
            var r = ""
            if (isChecked) {
                r = KeepShellSync.doCmdSync("pm grant $app android.permission.SYSTEM_ALERT_WINDOW")
            } else {
                r = KeepShellSync.doCmdSync("pm revoke $app android.permission.SYSTEM_ALERT_WINDOW")
            }
            if (r == "error") {
                (it as Switch).isChecked = !isChecked
                Toast.makeText(this, "修改权限失败！", Toast.LENGTH_SHORT).show()
            }
        }
        app_details_usagedata.setOnClickListener {
            val isChecked = (it as Switch).isChecked
            var r = ""
            if (isChecked) {
                r = KeepShellSync.doCmdSync("pm grant $app android.permission.PACKAGE_USAGE_STATS")
            } else {
                r = KeepShellSync.doCmdSync("pm revoke $app android.permission.PACKAGE_USAGE_STATS")
            }
            if (r == "error") {
                (it as Switch).isChecked = !isChecked
                Toast.makeText(this, "修改权限失败！", Toast.LENGTH_SHORT).show()
            }
        }
        app_details_modifysettings.setOnClickListener {
            val isChecked = (it as Switch).isChecked
            var r = ""
            if (isChecked) {
                r = KeepShellSync.doCmdSync("pm grant $app android.permission.WRITE_SECURE_SETTINGS")
            } else {
                r = KeepShellSync.doCmdSync("pm revoke $app android.permission.WRITE_SECURE_SETTINGS")
            }
            if (r == "error") {
                (it as Switch).isChecked = !isChecked
                Toast.makeText(this, "修改权限失败！", Toast.LENGTH_SHORT).show()
            }
        }

        //immersive.preconfirms=*

        app_details_hidenav.setOnClickListener {
            if (!WriteSettings().getPermission(this)) {
                WriteSettings().setPermission(this)
                Toast.makeText(this, "请先授权允许工具箱“修改系统设置”！", Toast.LENGTH_SHORT).show()
                (it as Switch).isChecked = !(it as Switch).isChecked
                return@setOnClickListener
            }
            val isSelected = (it as Switch).isChecked
            if (isSelected && app_details_hidestatus.isChecked) {
                policyControl.hideAll(app)
            } else if (isSelected) {
                policyControl.hideNavBar(app)
            } else {
                policyControl.showNavBar(app)
            }
        }
        app_details_hidestatus.setOnClickListener {
            if (!WriteSettings().getPermission(this)) {
                WriteSettings().setPermission(this)
                Toast.makeText(this, "请先授权允许工具箱“修改系统设置”！", Toast.LENGTH_SHORT).show()
                (it as Switch).isChecked = !(it as Switch).isChecked
                return@setOnClickListener
            }
            val isSelected = (it as Switch).isChecked
            if (isSelected && app_details_hidenav.isChecked) {
                policyControl.hideAll(app)
            } else if (isSelected) {
                policyControl.hideStatusBar(app)
            } else {
                policyControl.showStatusBar(app)
            }
        }

        app_details_disdoze.setOnClickListener {
            if ((it as Switch).isChecked) {
                KeepShellSync.doCmdSync("dumpsys deviceidle whitelist +$app")
            } else {
                KeepShellSync.doCmdSync("dumpsys deviceidle whitelist -$app")
            }
        }
        app_details_icon.setOnClickListener {
            try {
                val intent = getPackageManager().getLaunchIntentForPackage(app)
                startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(this, "启动应用失败！", Toast.LENGTH_SHORT).show()
            }
        }

        appConfigInfo = AppConfigStore(this).getAppConfig(app)

        app_details_hidebtn.setOnClickListener {
            appConfigInfo.disButton = (it as Switch).isChecked
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            app_details_hidenotice.isEnabled = false
        } else {
            app_details_hidenotice.setOnClickListener {
                if (!NoticeListing().getPermission(this)) {
                    NoticeListing().setPermission(this)
                    Toast.makeText(this, "请先授权允许工具箱“通知使用权限”！", Toast.LENGTH_SHORT).show()
                    (it as Switch).isChecked = !it.isChecked
                    return@setOnClickListener
                }
                appConfigInfo.disNotice = (it as Switch).isChecked
            }
        }
        app_details_disbackground.setOnClickListener {
            appConfigInfo.disBackgroundRun = (it as Switch).isChecked
        }
        app_details_aloowlight.setOnClickListener {
            if (!WriteSettings().getPermission(this)) {
                WriteSettings().setPermission(this)
                Toast.makeText(this, "请先授权允许工具箱“修改系统设置”！", Toast.LENGTH_SHORT).show()
                (it as CheckBox).isChecked = false
                return@setOnClickListener
            }
            appConfigInfo.aloneLight = (it as CheckBox).isChecked
        }
        app_details_light.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            var mode = -1;
            var screenBrightness = 100;
            var onMove = false

            fun getScreenMode(): Int {
                try {
                    mode = Settings.System.getInt(getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE)
                } catch (e: Settings.SettingNotFoundException) {
                    e.printStackTrace()
                }
                return mode
            }

            fun getScreenBrightness() {
                try {
                    screenBrightness = Settings.System.getInt(getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS)
                } catch (e: Settings.SettingNotFoundException) {
                    e.printStackTrace()
                }
            }

            fun setScreenMode() {
                try {
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
                    getContentResolver().notifyChange(Settings.System.getUriFor("screen_brightness_mode"), null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            fun setScreenBrightness() {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, screenBrightness)
                getContentResolver().notifyChange(Settings.System.getUriFor("screen_brightness_mode"), null)
            }


            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                onMove = false
                setScreenMode()
                setScreenBrightness()
                if (seekBar != null) {
                    if (seekBar.progress < 20) {
                        seekBar.progress = 20
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                getScreenMode()
                getScreenBrightness()
                try {
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
                    val uri = Settings.System.getUriFor("screen_brightness_mode")
                    getContentResolver().notifyChange(uri, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onMove = true
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!onMove || progress < 10) {
                    return
                }
                appConfigInfo.aloneLightValue = progress
                try {
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, progress)
                    val uri = Settings.System.getUriFor("screen_brightness")
                    getContentResolver().notifyChange(uri, null)
                } catch (ex: Exception) {

                }
            }
        })
        // TODO: 输入DPI
        if (appConfigInfo.dpi > -1) {
            app_details_dpi.text = appConfigInfo.dpi.toString()
        }
        app_details_excludetask.setOnClickListener {
            appConfigInfo.excludeRecent = (it as Switch).isChecked
        }
        app_details_scrollopt.setOnClickListener {
            appConfigInfo.smoothScroll = (it as Switch).isChecked
        }
        app_details_gps.setOnClickListener {
            appConfigInfo.gpsOn = (it as Switch).isChecked
        }
    }

    private fun getTotalSizeOfFilesInDir(file: File): Long {
        if (!file.exists()) {
            return 0
        }
        if (file.isFile)
            return file.length()
        val children = file.listFiles()
        var total: Long = 0
        if (children != null)
            for (child in children)
                total += getTotalSizeOfFilesInDir(child)
        return total
    }

    private fun checkPermission(permissionName: String, app: String): Boolean {
        val pm = packageManager;
        val permission = (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission(permissionName, app));
        if (permission) {
            return true
        } else {
            return false
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()

        val modeList = ModeList(this)
        val powercfg = getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
        val spfGlobal = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        dynamicCpu = (Platform().dynamicSupport(this) || File(Consts.POWER_CFG_PATH).exists())

        val packageInfo = packageManager.getPackageInfo(app, 0)
        val applicationInfo = packageInfo.applicationInfo
        app_details_name.text = applicationInfo.loadLabel(packageManager)
        app_details_packagename.text = packageInfo.packageName
        app_details_path.text = applicationInfo.sourceDir
        app_details_icon.setImageDrawable(applicationInfo.loadIcon(packageManager))
        app_details_versionname.text = packageInfo.versionName
        app_details_versioncode.text = packageInfo.versionCode.toString()
        app_details_time.text = Date(packageInfo.lastUpdateTime).toLocaleString()
        Thread(Runnable {
            var size = getTotalSizeOfFilesInDir(File(applicationInfo.sourceDir).parentFile)
            size += getTotalSizeOfFilesInDir(File(applicationInfo.dataDir))
            val dumpR = KeepShellSync.doCmdSync("dumpsys meminfo --S --package $app | grep TOTAL")
            var memSize = 0
            if (dumpR.isEmpty() || dumpR == "error") {

            } else {
                val mem = KeepShellSync.doCmdSync("dumpsys meminfo --package $app | grep TOTAL").split("\n", ignoreCase = true)
                for (rowIndex in 0..mem.size - 1) {
                    if (rowIndex % 2 == 0) {
                        //TOTAL    17651    11740      672        0    18832    16424     2407
                        val row = mem[rowIndex].trim().split("    ", ignoreCase = true)
                        try {
                            memSize += row[1].toInt()
                        } catch (ex: Exception) {
                        }
                        Log.d("", row.toString())
                    }
                }
            }
            app_details_size.post {
                app_details_size.text = String.format("%.2f", size / 1024.0 / 1024) + "MB"
                app_details_ram.text = String.format("%.2f", memSize / 1024.0) + "MB"
            }
        }).start()

        val firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, modeList.BALANCE))
        app_details_dynamic.text = modeList.getModName(powercfg.getString(app, firstMode))
        app_details_floatwindow.isChecked = checkPermission("android.permission.SYSTEM_ALERT_WINDOW", app)
        app_details_usagedata.isChecked = checkPermission("android.permission.PACKAGE_USAGE_STATS", app)
        app_details_modifysettings.isChecked = checkPermission("android.permission.WRITE_SECURE_SETTINGS", app)

        if (policyControl.isFullScreen(app)) {
            app_details_hidenav.isChecked = true
            app_details_hidestatus.isChecked = true
        } else {
            app_details_hidenav.isChecked = policyControl.isHideNavbarOnly(app)
            app_details_hidestatus.isChecked = policyControl.isHideStatusOnly(app)
        }


        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            app_details_disdoze.isEnabled = true
            val disDoze = KeepShellSync.doCmdSync("dumpsys deviceidle whitelist | grep $app")
            if (disDoze.contains(app)) {
                app_details_disdoze.isChecked = true
            }
        }

        app_details_hidebtn.isChecked = appConfigInfo.disButton
        app_details_hidenotice.isChecked = appConfigInfo.disNotice
        app_details_disbackground.isChecked = appConfigInfo.disBackgroundRun
        app_details_aloowlight.isChecked = appConfigInfo.aloneLight
        if (appConfigInfo.aloneLightValue > 0) {
            app_details_light.setProgress(appConfigInfo.aloneLightValue)
        }
        if (appConfigInfo.dpi > -1) {
            app_details_dpi.text = appConfigInfo.dpi.toString()
        }
        app_details_excludetask.isChecked = appConfigInfo.excludeRecent
        app_details_scrollopt.isChecked = appConfigInfo.smoothScroll
        app_details_gps.isChecked = appConfigInfo.gpsOn
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val originConfig = AppConfigStore(this).getAppConfig(appConfigInfo.packageName)
            if (
                    appConfigInfo.aloneLight != originConfig.aloneLight ||
                    appConfigInfo.aloneLightValue != originConfig.aloneLightValue ||
                    appConfigInfo.disNotice != originConfig.disNotice ||
                    appConfigInfo.disButton != originConfig.disButton ||
                    appConfigInfo.disBackgroundRun != originConfig.disBackgroundRun ||
                    appConfigInfo.gpsOn != originConfig.gpsOn ||
                    appConfigInfo.dpi != originConfig.dpi ||
                    appConfigInfo.excludeRecent != originConfig.excludeRecent ||
                    appConfigInfo.smoothScroll != originConfig.smoothScroll
            ) {
                setResult(RESULT_OK, this.intent)
            } else {
                setResult(_result, this.intent)
            }
            if (!AppConfigStore(this).setAppConfig(appConfigInfo)) {
                Toast.makeText(this, getString(R.string.config_save_fail), Toast.LENGTH_LONG).show()
            }
            this.finishAndRemoveTask()
        }
        return true
    }

    override fun finish() {
        super.finish()
    }

    override fun onPause() {
        super.onPause()
        finishAndRemoveTask()
    }
}
