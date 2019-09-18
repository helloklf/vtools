package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.model.AppConfigInfo
import com.omarea.permissions.NotificationListener
import com.omarea.permissions.WriteSettings
import com.omarea.scene_mode.FreezeAppShortcutHelper
import com.omarea.scene_mode.ImmersivePolicyControl
import com.omarea.scene_mode.ModeConfigInstaller
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.AppConfigStore
import com.omarea.store.SpfConfig
import com.omarea.ui.IntInputFilter
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.vaddin.IAppConfigAidlInterface
import com.omarea.vtools.R
import com.omarea.xposed.XposedCheck
import kotlinx.android.synthetic.main.activity_app_details.*
import org.json.JSONObject
import java.io.File

class ActivityAppDetails : AppCompatActivity() {
    var app = ""
    lateinit var immersivePolicyControl: ImmersivePolicyControl
    lateinit var appConfigInfo: AppConfigInfo
    private var dynamicCpu: Boolean = false
    private var _result = RESULT_CANCELED
    private var vAddinsInstalled = false
    private var aidlConn: IAppConfigAidlInterface? = null
    private val configInstaller = ModeConfigInstaller()

    private var needKeyCapture = false

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
        val addinPath = com.omarea.common.shared.FileWrite.writePrivateFile(assets, addin, "addin/xposed-addin.apk", this)

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
        if (installResult !== "error" && installResult.contains("Success") && getAddinVersion() >= getAddinMinimumVersion()) {
            Toast.makeText(applicationContext, getString(R.string.scene_addin_installed), Toast.LENGTH_SHORT).show()
            checkXposedState(false)
        } else {
            // 让用户手动安装
            try {
                val apk = FileWrite.writeFile(applicationContext, addin, true)

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
        ThemeSwitch.switchTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_details)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        // setTitle(R.string.app_name)

        // 显示返回按钮
        getSupportActionBar()!!.setHomeButtonEnabled(true);
        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener({ _ ->
            // finish()
            saveConfigAndFinish()
        });

        val intent = this.intent
        if (intent == null) {
            setResult(_result, this.intent)
            finish()
            return
        }
        val extras = this.intent.extras
        if (extras == null || !extras.containsKey("app")) {
            setResult(_result, this.intent)
            finish()
            return
        }

        app = extras.getString("app")!!
        needKeyCapture = AppConfigStore(this.applicationContext).needKeyCapture()

        if (app == "android" || app == "com.android.systemui" || app == "com.android.webview" || app == "mokee.platform" || app == "com.miui.rom") {
            app_details_auto.visibility = View.GONE
            app_details_assist.visibility = View.GONE
            app_details_freeze.isEnabled = false
        }

        if ((packageManager.getPackageInfo(app, 0).applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || packageManager.getLaunchIntentForPackage(app) == null) {
            app_freez_config.visibility = View.GONE
        }

        immersivePolicyControl = ImmersivePolicyControl(contentResolver)

        dynamicCpu = configInstaller.dynamicSupport(this.applicationContext) || configInstaller.configInstalled()

        app_details_dynamic.setOnClickListener {
            if (!dynamicCpu) {
                Snackbar.make(it, getString(R.string.scene_config_notinstalled), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val modeList = ModeSwitcher()
            val powercfg = getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
            val currentMode = powercfg.getString(app, getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, ModeSwitcher.BALANCE))
            var index = 0
            when (currentMode) {
                ModeSwitcher.POWERSAVE -> index = 0
                ModeSwitcher.BALANCE -> index = 1
                ModeSwitcher.PERFORMANCE -> index = 2
                ModeSwitcher.FAST -> index = 3
                ModeSwitcher.IGONED -> index = 5
                else -> index = 4
            }
            var selectedIndex = index
            val dialog = AlertDialog.Builder(this)
                    .setTitle(getString(R.string.perf_opt))
                    .setSingleChoiceItems(R.array.powercfg_modes2, index, { dialog, which ->
                        selectedIndex = which
                    })
                    .setPositiveButton(R.string.btn_confirm, { dialog, which ->
                        if (index != selectedIndex) {
                            var modeName = ModeSwitcher.BALANCE
                            when (selectedIndex) {
                                0 -> modeName = ModeSwitcher.POWERSAVE
                                1 -> modeName = ModeSwitcher.BALANCE
                                2 -> modeName = ModeSwitcher.PERFORMANCE
                                3 -> modeName = ModeSwitcher.FAST
                                5 -> modeName = ModeSwitcher.IGONED
                                4 -> modeName = ""
                            }
                            if (modeName.isEmpty()) {
                                powercfg.edit().remove(app).commit()
                            } else {
                                powercfg.edit().putString(app, modeName).commit()
                            }
                            app_details_dynamic.text = ModeSwitcher.getModName(modeName)
                            _result = RESULT_OK
                            notifyService(app, modeName)
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, DialogInterface.OnClickListener { dialog, which -> })
                    .create()
            DialogHelper.animDialog(dialog)
        }

        app_details_hidenav.setOnClickListener {
            if (!WriteSettings().getPermission(this)) {
                WriteSettings().setPermission(this)
                Toast.makeText(applicationContext, getString(R.string.scene_need_write_sys_settings), Toast.LENGTH_SHORT).show()
                (it as Switch).isChecked = !(it as Switch).isChecked
                return@setOnClickListener
            }
            val isSelected = (it as Switch).isChecked
            if (isSelected && app_details_hidestatus.isChecked) {
                immersivePolicyControl.hideAll(app)
            } else if (isSelected) {
                immersivePolicyControl.hideNavBar(app)
            } else {
                immersivePolicyControl.showNavBar(app)
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
                immersivePolicyControl.hideAll(app)
            } else if (isSelected) {
                immersivePolicyControl.hideStatusBar(app)
            } else {
                immersivePolicyControl.showStatusBar(app)
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
            val isChecked = (it as Switch).isChecked
            appConfigInfo.disButton = isChecked
            if (isChecked && !needKeyCapture) {
                saveConfig()
                sendBroadcast(Intent(getString(R.string.scene_key_capture_change_action)))
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            app_details_hidenotice.isEnabled = false
        } else {
            app_details_hidenotice.setOnClickListener {
                if (!NotificationListener().getPermission(this)) {
                    NotificationListener().setPermission(this)
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
                (it as Switch).isChecked = false
                return@setOnClickListener
            }
            appConfigInfo.aloneLight = (it as Switch).isChecked
        }
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

        app_details_freeze.setOnClickListener {
            val value = (it as Switch).isChecked
            if (value) {
                if (FreezeAppShortcutHelper().createShortcut(this.applicationContext, appConfigInfo.packageName)) {
                    appConfigInfo.freeze = true
                    KeepShellPublic.doCmdSync("pm disable " + appConfigInfo.packageName)
                    Toast.makeText(this, "当前应用已禁用，但你可以通过刚刚添加的快捷方式启动^_^", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "创建启动快捷方式失败，没有它你没法启动被冻结的应用，所以你不能打开这个功能...", Toast.LENGTH_LONG).show()
                    it.isChecked = false
                }
            } else {
                appConfigInfo.freeze = value
            }
        }

        if (XposedCheck.xposedIsRunning()) {
            if (appConfigInfo.dpi >= 96) {
                app_details_dpi.text = appConfigInfo.dpi.toString()
            } else {
                app_details_dpi.text = "默认"
            }
            app_details_dpi.setOnClickListener {
                var dialog: AlertDialog? = null
                val view = layoutInflater.inflate(R.layout.dialog_dpi_input, null)
                val inputDpi = view.findViewById<EditText>(R.id.input_dpi)
                inputDpi.setFilters(arrayOf(IntInputFilter()));
                if (appConfigInfo.dpi >= 96) {
                    inputDpi.setText(appConfigInfo.dpi.toString())
                }
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
                dialog.window!!.setWindowAnimations(R.style.windowAnim)
                dialog.show()
            }
        }
    }


    // 通知辅助服务配置变化
    private fun notifyService(app: String, mode: String) {
        if (AccessibleServiceHelper().serviceRunning(this)) {
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

        dynamicCpu = configInstaller.dynamicSupport(this.applicationContext) || configInstaller.configInstalled()

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
        app_details_icon.setImageDrawable(applicationInfo.loadIcon(packageManager))

        val firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, ModeSwitcher.BALANCE))
        app_details_dynamic.text = ModeSwitcher.getModName(powercfg.getString(app, firstMode))

        if (immersivePolicyControl.isFullScreen(app)) {
            app_details_hidenav.isChecked = true
            app_details_hidestatus.isChecked = true
        } else {
            app_details_hidenav.isChecked = immersivePolicyControl.isHideNavbarOnly(app)
            app_details_hidestatus.isChecked = immersivePolicyControl.isHideStatusOnly(app)
        }

        app_details_hidebtn.isChecked = appConfigInfo.disButton
        app_details_hidenotice.isChecked = appConfigInfo.disNotice
        app_details_disbackground.isChecked = appConfigInfo.disBackgroundRun
        app_details_aloowlight.isChecked = appConfigInfo.aloneLight
        app_details_gps.isChecked = appConfigInfo.gpsOn
        app_details_freeze.isChecked = appConfigInfo.freeze
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
                appConfigInfo.disNotice != originConfig.disNotice ||
                appConfigInfo.disButton != originConfig.disButton ||
                appConfigInfo.disBackgroundRun != originConfig.disBackgroundRun ||
                appConfigInfo.gpsOn != originConfig.gpsOn ||
                appConfigInfo.dpi != originConfig.dpi ||
                appConfigInfo.excludeRecent != originConfig.excludeRecent ||
                appConfigInfo.smoothScroll != originConfig.smoothScroll ||
                appConfigInfo.freeze != originConfig.freeze
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
