package com.omarea.vboot

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.WindowManager
import android.widget.Switch
import android.widget.Toast
import com.omarea.shared.ModeList
import com.omarea.shared.SpfConfig
import com.omarea.shell.KeepShellSync
import kotlinx.android.synthetic.main.activity_app_details.*
import java.io.File
import java.util.*

class AppDetailsActivity : AppCompatActivity() {
    var app = ""

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        val spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (spf.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false))
            this.setTheme(R.style.AppTheme_NoActionBarNight)
        else {
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_details)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        app = this.intent.extras.getString("app")

        app_details_dynamic.setOnClickListener {
            val modeList = ModeList(this)
            val powercfg = getSharedPreferences(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, Context.MODE_PRIVATE)
            val currentMode = powercfg.getString(app, modeList.BALANCE)
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
                    .setSingleChoiceItems(R.array.powercfg_modes, 0, DialogInterface.OnClickListener { dialog, which ->
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
            val isSelected = (it as Switch).isChecked
            val policyControl = Settings.Global.getString(contentResolver, "policy_control").split("\n")
            var hideNav = ""
            var hideFull = ""
            var hideStatus = ""
            for (item in policyControl) {
                if (item.startsWith("immersive.full")) {
                    hideFull = item
                } else if (item.startsWith("immersive.navigation")) {
                    hideNav = item
                } else if (item.startsWith("immersive.status")) {
                    hideStatus = item
                }
            }
            if (isSelected) {
                if (hideNav.isEmpty()) {
                    hideNav = "immersive.navigation=$app"
                } else {
                    if (hideNav.contains(",-$app")) {
                        hideNav.replace(",-$app", "")
                    }
                    if (hideNav.contains("-$app")) {
                        hideNav.replace("-$app", "")
                    }
                    hideNav = hideNav + ",$app"
                }
            } else if (!hideNav.isEmpty()) {
                if (hideNav.contains("+$app")) {
                    hideNav.replace("+$app", "")
                }
                if (hideNav.contains(",+$app")) {
                    hideNav.replace(",+$app", "")
                }
                if (hideNav.contains(",$app")) {
                    hideNav.replace(",$app", "")
                }
                if (hideNav.contains(app)) {
                    hideNav.replace(app, "")
                }
            }
            val stringBuild = StringBuilder()
            if (!hideFull.isEmpty()) {
                stringBuild.append(hideFull)
            }
            if (!hideNav.isEmpty()) {
                if (!stringBuild.isEmpty()) {
                    stringBuild.append(":")
                }
                stringBuild.append(hideNav)
            }
            if (!hideStatus.isEmpty()) {
                if (!stringBuild.isEmpty()) {
                    stringBuild.append(":")
                }
                stringBuild.append(hideStatus)
            }
            Settings.Global.putString(contentResolver, "policy_control", stringBuild.toString().trim())
        }
        app_details_hidestatus.setOnClickListener {
            val isSelected = (it as Switch).isChecked
            val policyControl = Settings.Global.getString(contentResolver, "policy_control").split("\n")
            var hideNav = ""
            var hideFull = ""
            var hideStatus = ""
            for (item in policyControl) {
                if (item.startsWith("immersive.full")) {
                    hideFull = item
                } else if (item.startsWith("immersive.navigation")) {
                    hideNav = item
                } else if (item.startsWith("immersive.status")) {
                    hideStatus = item
                }
            }
            if (isSelected) {
                if (hideStatus.isEmpty()) {
                    hideStatus = "immersive.status=$app"
                } else {
                    if (hideStatus.contains(",-$app")) {
                        hideStatus.replace(",-$app", "")
                    }
                    if (hideStatus.contains("-$app")) {
                        hideStatus.replace("-$app", "")
                    }
                    hideStatus = hideStatus + ",$app"
                }
            } else if (!hideStatus.isEmpty()) {
                if (hideStatus.contains("+$app")) {
                    hideStatus.replace("+$app", "")
                }
                if (hideStatus.contains(",+$app")) {
                    hideStatus.replace(",+$app", "")
                }
                if (hideStatus.contains(",$app")) {
                    hideStatus.replace(",$app", "")
                }
                if (hideStatus.contains(app)) {
                    hideStatus.replace(app, "")
                }
            }
            val stringBuild = StringBuilder()
            if (!hideFull.isEmpty()) {
                stringBuild.append(hideFull)
            }
            if (!hideNav.isEmpty()) {
                if (!stringBuild.isEmpty()) {
                    stringBuild.append(":")
                }
                stringBuild.append(hideNav)
            }
            if (!hideStatus.isEmpty()) {
                if (!stringBuild.isEmpty()) {
                    stringBuild.append(":")
                }
                stringBuild.append(hideStatus)
            }
            Settings.Global.putString(contentResolver, "policy_control", stringBuild.toString().trim())
        }
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

        val powercfg = getSharedPreferences(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, Context.MODE_PRIVATE)
        val spfGlobal = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        val packageInfo = packageManager.getPackageInfo(app, 0)
        val applicationInfo = packageInfo.applicationInfo
        app_details_name.text = applicationInfo.loadLabel(packageManager)
        app_details_packagename.text = packageInfo.packageName
        app_details_path.text = applicationInfo.sourceDir
        app_details_icon.setImageDrawable(applicationInfo.loadIcon(packageManager))
        app_details_versionname.text = packageInfo.versionName
        app_details_versioncode.text = packageInfo.versionCode.toString()
        app_details_time.text = Date(packageInfo.lastUpdateTime).toLocaleString()
        app_details_size.text = String.format("%.2f", File(applicationInfo.publicSourceDir).length() / 1024.0 / 1024) + "MB"
        val mem = KeepShellSync.doCmdSync("dumpsys meminfo $app | grep Realtime")
        if (mem.contains("Realtime:")) {
            app_details_ram.text = mem.substring(mem.lastIndexOf(":") + 1)
        }

        app_details_dynamic.text = ModeList(this).getModName(powercfg.getString(app, spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, ModeList(this).BALANCE)))
        app_details_floatwindow.isChecked = checkPermission("android.permission.SYSTEM_ALERT_WINDOW", app)
        app_details_usagedata.isChecked = checkPermission("android.permission.PACKAGE_USAGE_STATS", app)
        app_details_modifysettings.isChecked = checkPermission("android.permission.WRITE_SECURE_SETTINGS", app)

        val policyControl = Settings.Global.getString(contentResolver, "policy_control").split(":")
        for (item in policyControl) {
            if (item.startsWith("immersive.full")) {
                if (item.contains(app) && !item.contains("-$app")) {
                    app_details_hidenav.isChecked = true
                    app_details_hidestatus.isChecked = true
                }
            } else if (item.startsWith("immersive.navigation")) {
                if (item.contains(app) && !item.contains("-$app")) {
                    app_details_hidenav.isChecked = true
                }
            } else if (item.startsWith("immersive.status")) {
                if (item.contains(app) && !item.contains("-$app")) {
                    app_details_hidestatus.isChecked = true
                }
            }
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            app_details_disdoze.isEnabled = true
            val disDoze = KeepShellSync.doCmdSync("dumpsys deviceidle whitelist | grep $app")
            if (disDoze != "error" && !disDoze.isEmpty()) {
                app_details_disdoze.isChecked = true
            }
        }
    }
}
