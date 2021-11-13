package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Checkable
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.omarea.common.ui.DialogHelper
import com.omarea.data.EventBus
import com.omarea.data.EventType
import com.omarea.library.permissions.NotificationListener
import com.omarea.library.shell.CGroupMemoryUtlis
import com.omarea.model.SceneConfigInfo
import com.omarea.permissions.WriteSettings
import com.omarea.scene_mode.ImmersivePolicyControl
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.scene_mode.SceneMode
import com.omarea.store.SceneConfigStore
import com.omarea.store.SpfConfig
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.vtools.R
import com.omarea.vtools.dialogs.DialogAppBoostPolicy
import com.omarea.vtools.dialogs.DialogAppCGroupMem
import com.omarea.vtools.dialogs.DialogAppOrientation
import com.omarea.vtools.dialogs.DialogAppPowerConfig
import kotlinx.android.synthetic.main.activity_app_details.*

class ActivityAppDetails : ActivityBase() {
    var app = ""
    lateinit var immersivePolicyControl: ImmersivePolicyControl
    lateinit var sceneConfigInfo: SceneConfigInfo
    private var dynamicCpu: Boolean = false
    private var _result = RESULT_CANCELED
    private lateinit var sceneBlackList: SharedPreferences
    private lateinit var spfGlobal: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_details)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        // setTitle(R.string.app_name)

        // 显示返回按钮
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { _ ->
            // finish()
            saveConfigAndFinish()
        }

        spfGlobal = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

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

        if (app == "android" || app == "com.android.systemui" || app == "com.android.webview" || app == "mokee.platform" || app == "com.miui.rom") {
            app_details_perf.visibility = View.GONE
            app_details_auto.visibility = View.GONE
            app_details_assist.visibility = View.GONE
            app_details_freeze.isEnabled = false
            scene_mode_config.visibility = View.GONE
            scene_mode_allow.visibility = View.GONE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            app_details_assist.visibility = View.GONE
        }

        // 场景模式白名单开关
        sceneBlackList = getSharedPreferences(SpfConfig.SCENE_BLACK_LIST, Context.MODE_PRIVATE);
        scene_mode_allow.setOnClickListener {
            val checked = (it as Checkable).isChecked
            scene_mode_config.visibility = if (checked) View.VISIBLE else View.GONE
            if (checked) {
                sceneBlackList.edit().remove(app).apply()
            } else {
                sceneBlackList.edit().putBoolean(app, true).apply()
            }
        }

        immersivePolicyControl = ImmersivePolicyControl(contentResolver)

        dynamicCpu = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)

        app_details_dynamic.setOnClickListener {
            if (!dynamicCpu) {
                DialogHelper.helpInfo(this, "", "请先回到功能列表，进入 [性能配置] 功能，开启 [动态响应] 功能")
                return@setOnClickListener
            }

            val spfPowercfg = getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)

            DialogAppPowerConfig(this,
                    spfPowercfg.getString(app, ""),
                    object : DialogAppPowerConfig.IResultCallback {
                        override fun onChange(mode: String?) {
                            spfPowercfg.edit().run {
                                if (mode.isNullOrEmpty()) {
                                    remove(app)
                                } else {
                                    putString(app, mode)
                                }
                            }.apply()

                            (it as TextView).text = ModeSwitcher.getModName("" + mode)
                            _result = RESULT_OK
                            notifyService(app, "" + mode)
                        }
                    }).show()
        }

        app_details_cgroup_mem.setOnClickListener {
            val utlis = CGroupMemoryUtlis(this)
            if (!utlis.isSupported) {
                DialogHelper.helpInfo(this, "", "抱歉，您的内核不支持该功能特性~")
                return@setOnClickListener
            }
            DialogAppCGroupMem(this, sceneConfigInfo.fgCGroupMem, object : DialogAppCGroupMem.IResultCallback {
                override fun onChange(group: String?, name: String?) {
                    sceneConfigInfo.fgCGroupMem = group
                    (it as TextView).text = name
                    _result = RESULT_OK
                }
            }).show()
        }


        app_details_cgroup_mem2.setOnClickListener {
            val utlis = CGroupMemoryUtlis(this)
            if (!utlis.isSupported) {
                DialogHelper.helpInfo(this, "", "抱歉，您的内核不支持该功能特性~")
                return@setOnClickListener
            }

            DialogAppCGroupMem(this, sceneConfigInfo.bgCGroupMem, object : DialogAppCGroupMem.IResultCallback {
                override fun onChange(group: String?, name: String?) {
                    sceneConfigInfo.bgCGroupMem = group
                    (it as TextView).text = name
                    _result = RESULT_OK
                }
            }).show()
        }

        app_details_boost_mem.setOnClickListener {
            DialogAppBoostPolicy(this, sceneConfigInfo.dynamicBoostMem, object : DialogAppBoostPolicy.IResultCallback {
                override fun onChange(enabled: Boolean) {
                    sceneConfigInfo.dynamicBoostMem = enabled
                    (it as TextView).text = if (enabled) "已启用" else "未启用"
                    _result = RESULT_OK
                }
            }).show()
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
                startActivity(getPackageManager().getLaunchIntentForPackage(app))
            } catch (ex: Exception) {
                Toast.makeText(applicationContext, getString(R.string.start_app_fail), Toast.LENGTH_SHORT).show()
            }
        }

        sceneConfigInfo = SceneConfigStore(this).getAppConfig(app)

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
                sceneConfigInfo.disNotice = (it as Switch).isChecked
            }
        }
        scene_orientation.setOnClickListener {
            DialogAppOrientation(this, sceneConfigInfo.screenOrientation, object : DialogAppOrientation.IResultCallback {
                override fun onChange(value: Int, name: String?) {
                    sceneConfigInfo.screenOrientation = value
                    (it as TextView).text = "" + name
                }
            }).show()
        }
        app_details_aloowlight.setOnClickListener {
            if (!WriteSettings().getPermission(this)) {
                WriteSettings().setPermission(this)
                Toast.makeText(applicationContext, getString(R.string.scene_need_write_sys_settings), Toast.LENGTH_SHORT).show()
                (it as Switch).isChecked = false
                return@setOnClickListener
            }
            sceneConfigInfo.aloneLight = (it as Switch).isChecked
        }
        app_details_gps.setOnClickListener {
            sceneConfigInfo.gpsOn = (it as Switch).isChecked
        }

        app_details_freeze.setOnClickListener {
            sceneConfigInfo.freeze = (it as Switch).isChecked
            if (!sceneConfigInfo.freeze) {
                SceneMode.unfreezeApp(sceneConfigInfo.packageName)
            }
        }

        app_monitor.setOnClickListener {
            sceneConfigInfo.showMonitor = (it as Switch).isChecked
        }
    }

    // 通知辅助服务配置变化
    private fun notifyService(app: String, mode: String? = null) {
        if (AccessibleServiceHelper().serviceRunning(this)) {
            EventBus.publish(EventType.SCENE_APP_CONFIG, HashMap<String, Any>().apply {
                put("app", app)
                if (mode != null) {
                    put("mode", mode)
                }
            })
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
        val powercfg = getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)

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

        val firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, "")
        app_details_dynamic.text = ModeSwitcher.getModName(powercfg.getString(app, firstMode)!!)

        app_details_cgroup_mem.text = DialogAppCGroupMem.Transform(this).getName(sceneConfigInfo.fgCGroupMem)
        app_details_cgroup_mem2.text = DialogAppCGroupMem.Transform(this).getName(sceneConfigInfo.bgCGroupMem)
        app_details_boost_mem.text = if (sceneConfigInfo.dynamicBoostMem) "已启用" else "未启用"

        if (immersivePolicyControl.isFullScreen(app)) {
            app_details_hidenav.isChecked = true
            app_details_hidestatus.isChecked = true
        } else {
            app_details_hidenav.isChecked = immersivePolicyControl.isHideNavbarOnly(app)
            app_details_hidestatus.isChecked = immersivePolicyControl.isHideStatusOnly(app)
        }

        app_details_hidenotice.isChecked = sceneConfigInfo.disNotice
        app_details_aloowlight.isChecked = sceneConfigInfo.aloneLight
        app_details_gps.isChecked = sceneConfigInfo.gpsOn
        app_details_freeze.isChecked = sceneConfigInfo.freeze
        app_monitor.isChecked = sceneConfigInfo.showMonitor

        scene_mode_allow.isChecked = !sceneBlackList.contains(app)
        scene_mode_config.visibility = if (scene_mode_config.visibility == View.VISIBLE && scene_mode_allow.isChecked) View.VISIBLE else View.GONE

        val screenOrientation = sceneConfigInfo.screenOrientation
        scene_orientation.text = DialogAppOrientation.Transform(this).getName(screenOrientation)
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
        val originConfig = SceneConfigStore(this).getAppConfig(sceneConfigInfo.packageName)

        if (
                sceneConfigInfo.screenOrientation != originConfig.screenOrientation ||
                sceneConfigInfo.aloneLight != originConfig.aloneLight ||
                sceneConfigInfo.disNotice != originConfig.disNotice ||
                sceneConfigInfo.disButton != originConfig.disButton ||
                sceneConfigInfo.gpsOn != originConfig.gpsOn ||
                sceneConfigInfo.freeze != originConfig.freeze ||
                sceneConfigInfo.fgCGroupMem != originConfig.fgCGroupMem ||
                sceneConfigInfo.bgCGroupMem != originConfig.bgCGroupMem ||
                sceneConfigInfo.dynamicBoostMem != originConfig.dynamicBoostMem ||
                sceneConfigInfo.showMonitor != originConfig.showMonitor
        ) {
            setResult(RESULT_OK, this.intent)
        } else {
            setResult(_result, this.intent)
        }
        if (!SceneConfigStore(this).setAppConfig(sceneConfigInfo)) {
            Toast.makeText(applicationContext, getString(R.string.config_save_fail), Toast.LENGTH_LONG).show()
        } else {
            if (sceneConfigInfo.fgCGroupMem != originConfig.fgCGroupMem ||
                    sceneConfigInfo.bgCGroupMem != originConfig.bgCGroupMem ||
                    sceneConfigInfo.dynamicBoostMem != originConfig.dynamicBoostMem) {
                notifyService(app)
            }

            if (sceneConfigInfo.freeze != originConfig.freeze) {
                if (sceneConfigInfo.freeze) {
                    SceneMode.getCurrentInstance()?.setFreezeAppLeaveTime(sceneConfigInfo.packageName)
                }
            }
        }
    }

    override fun finish() {
        super.finish()
    }

    override fun onPause() {
        super.onPause()
    }
}
