package com.omarea.vtools

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.omarea.shared.model.AppConfigInfo
import com.omarea.shared.*
import com.omarea.shell.KeepShellPublic
import com.omarea.shell.NoticeListing
import com.omarea.shell.Platform
import com.omarea.shell.WriteSettings
import com.omarea.ui.IntInputFilter
import com.omarea.vaddin.IAppConfigAidlInterface
import com.omarea.xposed.XposedCheck
import kotlinx.android.synthetic.main.activity_app_details.*
import org.json.JSONObject
import java.io.File
import java.util.*


class AppDetailsActivity : AppCompatActivity() {
    var app = ""
    lateinit var policyControl: PolicyControl
    lateinit var appConfigInfo: AppConfigInfo
    private var dynamicCpu: Boolean = false
    private var _result = RESULT_CANCELED
    private var vAddinsInstalled = false
    private var aidlConn: IAppConfigAidlInterface? = null

    fun getAddinVersion(): Int {
        var code = 0
        try {
            val manager = getPackageManager()
            val info = manager.getPackageInfo("com.omarea.vaddin", 0)
            code = info.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return code
    }

    fun getAddinMinimumVersion(): Int {
        return resources.getInteger(R.integer.addin_minimum_version)
    }

    private var conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            aidlConn = IAppConfigAidlInterface.Stub.asInterface(service)
            updateXposedConfigFromAddin()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            aidlConn = null
        }
    }

    private fun updateXposedConfigFromAddin() {
        if (aidlConn != null) {
            try {
                if (getAddinMinimumVersion() > getAddinVersion()) {
                    // TODO:自动安装
                    Toast.makeText(applicationContext, getString(R.string.scene_addin_version_toolow), Toast.LENGTH_SHORT).show()
                    if (aidlConn != null) {
                        unbindService(conn)
                        aidlConn = null
                    }
                    installVAddin()
                } else {
                    val configJson = aidlConn!!.getAppConfig(app)
                    val config = JSONObject(configJson)
                    for (key in config.keys()) {
                        when (key) {
                            "dpi" -> {
                                appConfigInfo.dpi = config.getInt(key)
                            }
                            "excludeRecent" -> {
                                appConfigInfo.excludeRecent = config.getBoolean(key)
                            }
                            "smoothScroll" -> {
                                appConfigInfo.smoothScroll = config.getBoolean(key)
                            }
                        }
                    }
                    app_details_scrollopt.isChecked = appConfigInfo.smoothScroll
                    app_details_excludetask.isChecked = appConfigInfo.excludeRecent
                    if (appConfigInfo.dpi >= 96) {
                        app_details_dpi.text = appConfigInfo.dpi.toString()
                    } else {
                        app_details_dpi.text = "默认"
                    }
                    app_details_hide_su.isChecked = aidlConn!!.getBooleanValue("com.android.systemui_hide_su", false)
                    app_details_webview_debug.isChecked = aidlConn!!.getBooleanValue("android_webdebug", false)
                    app_details_service_running.isChecked = aidlConn!!.getBooleanValue("android_dis_service_foreground", false)
                }
            } catch (ex: Exception) {
                Toast.makeText(applicationContext, getString(R.string.scene_addin_sync_fail), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 安装插件
     */
    private fun installVAddin() {
        val addin = "addin/xposed-addin.apk"
        // 解压应用内部集成的插件文件
        val addinPath = FileWrite.writePrivateFile(assets, addin, "addin/xposed-addin.apk", this)

        // 如果应用内部集成的插件文件获取失败
        if (addinPath == null) {
            Toast.makeText(applicationContext, getString(R.string.scene_addin_miss), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            // 判断应用内部集成的插件文件是否和应用版本匹配（不匹配则取消安装）
            if (packageManager.getPackageArchiveInfo(addinPath, PackageManager.GET_ACTIVITIES).versionCode < getAddinMinimumVersion()) {
                Toast.makeText(applicationContext, getString(R.string.scene_inner_addin_invalid), Toast.LENGTH_SHORT).show()
                return
            }
        } catch (ex: Exception) {
            // 异常
            Toast.makeText(applicationContext, getString(R.string.scene_addin_install_fail), Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(applicationContext, getString(R.string.scene_addin_installing), Toast.LENGTH_SHORT).show()
        //使用ROOT权限安装插件
        val installResult = KeepShellPublic.doCmdSync("pm install -r '$addinPath'")
        // 如果使用ROOT权限自动安装成功（再次检查Xposed状态）
        if (installResult !== "error" && installResult.contains("Success") && getAddinVersion() == getAddinMinimumVersion()) {
            Toast.makeText(applicationContext, getString(R.string.scene_addin_installed), Toast.LENGTH_SHORT).show()
            checkXposedState(false)
        } else {
            // 让用户手动安装
            try {
                val apk = FileWrite.writeFile(assets, addin, true)

                val intent = Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(File((if (apk != null) apk else addinPath))), "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (ex: Exception) {
                Toast.makeText(applicationContext, getString(R.string.scene_addin_install_fail), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindService() {
        tryUnBindAddin()
        try {
            val intent = Intent();
            //绑定服务端的service
            intent.setAction("com.omarea.vaddin.ConfigUpdateService");
            //新版本（5.0后）必须显式intent启动 绑定服务
            intent.setComponent(ComponentName("com.omarea.vaddin", "com.omarea.vaddin.ConfigUpdateService"));
            //绑定的时候服务端自动创建
            if (bindService(intent, conn, Context.BIND_AUTO_CREATE)) {
            } else {
                throw Exception("")
            }
        } catch (ex: Exception) {
            Toast.makeText(applicationContext, "连接到“Scene-高级设定”插件失败，请不要阻止插件自启动！", Toast.LENGTH_LONG).show()
        }
    }

    private fun tryUnBindAddin() {
        try {
            if (aidlConn != null) {
                unbindService(conn)
                aidlConn = null
            }
        } catch (ex: Exception) {

        }
    }

    /**
     * 检查Xposed状态
     */
    private fun checkXposedState(autoUpdate: Boolean = true) {
        var allowXposedConfig = XposedCheck.xposedIsRunning()
        app_details_vaddins_notactive.visibility = if (allowXposedConfig) View.GONE else View.VISIBLE
        try {
            vAddinsInstalled = packageManager.getPackageInfo("com.omarea.vaddin", 0) != null
            allowXposedConfig = allowXposedConfig && vAddinsInstalled
        } catch (ex: Exception) {
            vAddinsInstalled = false
        }
        app_details_vaddins_notinstall.setOnClickListener {
            installVAddin()
        }
        if (vAddinsInstalled && getAddinVersion() < getAddinMinimumVersion()) {
            // 版本过低（更新插件）
            if (autoUpdate) {
                installVAddin()
            }
        } else if (vAddinsInstalled) {
            // 已安装（获取配置）
            app_details_vaddins_notinstall.visibility = View.GONE
            if (aidlConn == null) {
                bindService()
            } else {
                updateXposedConfigFromAddin()
            }
        } else {
            // 未安装（显示未安装）
            app_details_vaddins_notinstall.visibility = View.VISIBLE
        }
        app_details_vaddins_notactive.visibility = if (XposedCheck.xposedIsRunning()) View.GONE else View.VISIBLE
        app_details_dpi.isEnabled = allowXposedConfig
        app_details_excludetask.isEnabled = allowXposedConfig
        app_details_scrollopt.isEnabled = allowXposedConfig
        app_details_hide_su.isEnabled = allowXposedConfig
        app_details_webview_debug.isEnabled = allowXposedConfig
        app_details_service_running.isEnabled = allowXposedConfig
    }


    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        val spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (spf.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false)) {
            this.setTheme(R.style.AppTheme_NoActionBarNight)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_details)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        app = this.intent.extras.getString("app")

        if (app == "android" || app == "com.android.systemui" || app == "com.android.webview" || app == "mokee.platform" || app == "com.miui.rom") {
            app_details_permission.visibility = View.GONE
            app_details_auto.visibility = View.GONE
            app_details_assist.visibility = View.GONE
            app_details_version.visibility = View.GONE
        }

        policyControl = PolicyControl(contentResolver)

        dynamicCpu = (Platform().dynamicSupport(this) || File(Consts.POWER_CFG_PATH).exists())

        app_details_dynamic.setOnClickListener {
            if (!dynamicCpu) {
                Snackbar.make(it, getString(R.string.dynamic_config_notinstalled), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val modeList = ModeList(this)
            val powercfg = getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
            val currentMode = powercfg.getString(app, getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, ModeList.BALANCE))
            var index = 0
            when (currentMode) {
                ModeList.POWERSAVE -> index = 0
                ModeList.BALANCE -> index = 1
                ModeList.PERFORMANCE -> index = 2
                ModeList.FAST -> index = 3
                ModeList.IGONED -> index = 4
            }
            var selectedIndex = index
            AlertDialog.Builder(this)
                    .setTitle(getString(R.string.perf_opt))
                    .setSingleChoiceItems(R.array.powercfg_modes2, index, DialogInterface.OnClickListener { dialog, which ->
                        selectedIndex = which
                    })
                    .setPositiveButton(R.string.btn_confirm, DialogInterface.OnClickListener { dialog, which ->
                        if (index != selectedIndex) {
                            var modeName = ModeList.BALANCE
                            when (selectedIndex) {
                                0 -> modeName = ModeList.POWERSAVE
                                1 -> modeName = ModeList.BALANCE
                                2 -> modeName = ModeList.PERFORMANCE
                                3 -> modeName = ModeList.FAST
                                4 -> modeName = ""
                            }
                            if (modeName.isEmpty()) {
                                powercfg.edit().remove(app).commit()
                            } else {
                                powercfg.edit().putString(app, modeName).commit()
                            }
                            app_details_dynamic.text = ModeList.getModName(modeName)
                            _result = RESULT_OK
                            notifyService(app, modeName)
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
                r = KeepShellPublic.doCmdSync("pm grant $app android.permission.SYSTEM_ALERT_WINDOW")
            } else {
                r = KeepShellPublic.doCmdSync("pm revoke $app android.permission.SYSTEM_ALERT_WINDOW")
            }
            if (r == "error") {
                (it as Switch).isChecked = !isChecked
                Toast.makeText(applicationContext, getString(R.string.permission_modify_fail), Toast.LENGTH_SHORT).show()
            }
        }
        app_details_usagedata.setOnClickListener {
            val isChecked = (it as Switch).isChecked
            var r = ""
            if (isChecked) {
                r = KeepShellPublic.doCmdSync("pm grant $app android.permission.PACKAGE_USAGE_STATS")
            } else {
                r = KeepShellPublic.doCmdSync("pm revoke $app android.permission.PACKAGE_USAGE_STATS")
            }
            if (r == "error") {
                (it as Switch).isChecked = !isChecked
                Toast.makeText(applicationContext, getString(R.string.permission_modify_fail), Toast.LENGTH_SHORT).show()
            }
        }
        app_details_modifysettings.setOnClickListener {
            val isChecked = (it as Switch).isChecked
            var r = ""
            if (isChecked) {
                r = KeepShellPublic.doCmdSync("pm grant $app android.permission.WRITE_SECURE_SETTINGS")
            } else {
                r = KeepShellPublic.doCmdSync("pm revoke $app android.permission.WRITE_SECURE_SETTINGS")
            }
            if (r == "error") {
                (it as Switch).isChecked = !isChecked
                Toast.makeText(applicationContext, getString(R.string.permission_modify_fail), Toast.LENGTH_SHORT).show()
            }
        }

        //immersive.preconfirms=*

        app_details_hidenav.setOnClickListener {
            if (!WriteSettings().getPermission(this)) {
                WriteSettings().setPermission(this)
                Toast.makeText(applicationContext, getString(R.string.scene_need_write_sys_settings), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(applicationContext, getString(R.string.scene_need_write_sys_settings), Toast.LENGTH_SHORT).show()
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
                KeepShellPublic.doCmdSync("dumpsys deviceidle whitelist +$app")
            } else {
                KeepShellPublic.doCmdSync("dumpsys deviceidle whitelist -$app")
            }
        }
        app_details_icon.setOnClickListener {
            try {
                saveConfig()
                val intent = getPackageManager().getLaunchIntentForPackage(app)
                startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(applicationContext, getString(R.string.start_app_fail), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(applicationContext, getString(R.string.scene_need_notic_listing), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(applicationContext, getString(R.string.scene_need_write_sys_settings), Toast.LENGTH_SHORT).show()
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
        if (appConfigInfo.dpi >= 96) {
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
        if (XposedCheck.xposedIsRunning()) {
            if (appConfigInfo.dpi >= 96) {
                app_details_dpi.text = appConfigInfo.dpi.toString()
            } else {
                app_details_dpi.text = "默认"
            }
            app_details_dpi.setOnClickListener {
                var dialog: AlertDialog? = null
                val view = layoutInflater.inflate(R.layout.dpi_input, null)
                val inputDpi = view.findViewById<EditText>(R.id.input_dpi)
                inputDpi.setFilters(arrayOf(IntInputFilter()));
                view.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
                    val dpiText = inputDpi.text.toString()
                    if (dpiText.isEmpty()) {
                        appConfigInfo.dpi = 0
                        return@setOnClickListener
                    } else {
                        try {
                            val dpi = dpiText.toInt()
                            if (dpi < 96 && dpi != 0) {
                                Toast.makeText(applicationContext, "DPI的值必须大于96", Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }
                            appConfigInfo.dpi = dpi
                            if (dpi == 0) {
                                app_details_dpi.text = "默认"
                            } else
                                app_details_dpi.text = dpi.toString()
                        } catch (ex: Exception) {

                        }
                    }
                    if (dialog != null) {
                        dialog!!.dismiss()
                    }
                }
                view.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                    if (dialog != null) {
                        dialog!!.dismiss()
                    }
                }
                dialog = AlertDialog.Builder(this)
                        .setTitle("请输入DPI")
                        .setView(view)
                        .create()
                dialog.show()
            }
        }
    }


    // 通知辅助服务配置变化
    private fun notifyService(app: String, mode: String) {
        if (AccessibleServiceHelper().serviceIsRunning(this)) {
            val intent = Intent(this.getString(R.string.scene_appchange_action))
            intent.putExtra("app", app)
            intent.putExtra("mode", mode)
            sendBroadcast(intent)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.save, menu)
        return true
    }

    //右上角菜单
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                saveConfigAndFinish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        checkXposedState()
        val powercfg = getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
        val spfGlobal = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        dynamicCpu = (Platform().dynamicSupport(this) || File(Consts.POWER_CFG_PATH).exists())

        var packageInfo: PackageInfo? = null
        try {
            packageInfo = packageManager.getPackageInfo(app, 0)
        } catch (ex: Exception) {
            Toast.makeText(applicationContext, "所选的应用已被卸载！", Toast.LENGTH_SHORT).show()
        }
        if (packageInfo == null) {
            finish()
            return
        }
        val applicationInfo = packageInfo.applicationInfo
        app_details_name.text = applicationInfo.loadLabel(packageManager)
        app_details_packagename.text = packageInfo.packageName
        app_details_path.text = applicationInfo.sourceDir
        app_details_icon.setImageDrawable(applicationInfo.loadIcon(packageManager))
        app_details_versionname.text = packageInfo.versionName
        app_details_versioncode.text = packageInfo.versionCode.toString()
        app_details_time.text = Date(packageInfo.lastUpdateTime).toLocaleString()
        app_details_light.isEnabled = WriteSettings().getPermission(this)
        Thread(Runnable {
            var size = getTotalSizeOfFilesInDir(File(applicationInfo.sourceDir).parentFile)
            try {
                if (applicationInfo.dataDir != null) {
                    // size += getTotalSizeOfFilesInDir(File(applicationInfo.dataDir))
                }
                val dataFile = File("${Consts.SDCardDir}/Android/data/$app")
                val obbFile = File("${Consts.SDCardDir}/Android/obb/$app")
                val sdFile = File("${Consts.SDCardDir}/$app")
                if (dataFile.exists()) {
                    size += getTotalSizeOfFilesInDir(dataFile)
                }
                if (obbFile.exists()) {
                    size += getTotalSizeOfFilesInDir(obbFile)
                }
                if (sdFile.exists()) {
                    size += getTotalSizeOfFilesInDir(sdFile)
                }
            } catch (ex: Exception) { }
            val dumpR = KeepShellPublic.doCmdSync("dumpsys meminfo --S --package $app | grep TOTAL")
            var memSize = 0
            if (dumpR.isEmpty() || dumpR == "error") {

            } else {
                val mem = KeepShellPublic.doCmdSync("dumpsys meminfo --package $app | grep TOTAL").split("\n", ignoreCase = true)
                for (rowIndex in 0 until mem.size) {
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
            // 由于这是个异步操作，执行完时界面可能已经被销毁，因此需要做这个判断
            if (app_details_size != null) {
                app_details_size.post {
                    try {
                        app_details_size.text = String.format("%.2f", size / 1024.0 / 1024) + "MB"
                        app_details_ram.text = String.format("%.2f", memSize / 1024.0) + "MB"
                    } catch (ex: Exception) {
                    }
                }
            }
        }).start()

        val firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, ModeList.BALANCE))
        app_details_dynamic.text = ModeList.getModName(powercfg.getString(app, firstMode))
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
            val disDoze = KeepShellPublic.doCmdSync("dumpsys deviceidle whitelist | grep $app")
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
        app_details_gps.isChecked = appConfigInfo.gpsOn
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            saveConfigAndFinish()
        }
        return false
    }

    private fun saveConfigAndFinish() {
        saveConfig()
        this.finish()
    }

    private fun saveConfig() {
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
        if (aidlConn != null) {
            aidlConn!!.updateAppConfig(app, appConfigInfo.dpi, appConfigInfo.excludeRecent, appConfigInfo.smoothScroll)
            aidlConn!!.setBooleanValue("com.android.systemui_hide_su", app_details_hide_su.isChecked)
            aidlConn!!.setBooleanValue("android_webdebug", app_details_webview_debug.isChecked)
            aidlConn!!.setBooleanValue("android_dis_service_foreground", app_details_service_running.isChecked)
        } else {
        }
        if (!AppConfigStore(this).setAppConfig(appConfigInfo)) {
            Toast.makeText(applicationContext, getString(R.string.config_save_fail), Toast.LENGTH_LONG).show()
        }
    }

    override fun finish() {
        super.finish()
        tryUnBindAddin()
    }

    override fun onPause() {
        super.onPause()
    }
}
