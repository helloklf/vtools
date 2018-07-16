package com.omarea.vboot

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.omarea.shared.ModeList
import com.omarea.shared.SpfConfig
import com.omarea.shell.KeepShellSync
import kotlinx.android.synthetic.main.activity_app_details.*
import java.io.File
import java.util.*

class AppDetailsActivity : AppCompatActivity {
    constructor() {

    }

    constructor(appPackageName: String) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (spf.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false))
            this.setTheme(R.style.AppTheme_NoActionBarNight)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_details)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
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

        val app = "com.tencent.tim"
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
    }
}
