package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.omarea.charger_booster.ServiceBattery
import com.omarea.common.shared.MagiskExtend
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.KernelProrp
import com.omarea.common.shell.RootFile
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ThemeMode
import com.omarea.permissions.CheckRootStatus
import com.omarea.scene_mode.ModeConfigInstaller
import com.omarea.shell_utils.BackupRestoreUtils
import com.omarea.shell_utils.BatteryUtils
import com.omarea.store.SpfConfig
import com.omarea.utils.Update
import com.omarea.vtools.R
import com.omarea.vtools.dialogs.DialogPower
import com.omarea.vtools.fragments.*
import com.omarea.vtools.popup.FloatMonitor
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference

class ActivityMain : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var globalSPF: SharedPreferences? = null
    private lateinit var themeMode: ThemeMode

    private fun setExcludeFromRecents(exclude: Boolean? = null) {
        try {
            val service = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (task in service.appTasks) {
                if (task.taskInfo.id == this.taskId) {
                    val b = exclude ?: globalSPF!!.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT, false)
                    task.setExcludeFromRecents(b)
                }
            }
        } catch (ex: Exception) {
            Log.e("excludeRecent", "" + ex.message)
        }
    }

    private class ServiceCreateThread(context: Context) : Runnable {
        private var context: WeakReference<Context> = WeakReference(context)
        override fun run() {
            //判断是否开启了充电加速和充电保护，如果开启了，自动启动后台服务
            val chargeConfig = context.get()!!.getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
            if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false) || chargeConfig!!.getBoolean(SpfConfig.CHARGE_SPF_BP, false)) {
                try {
                    val intent = Intent(context.get()!!, ServiceBattery::class.java)
                    context.get()!!.startService(intent)
                } catch (ex: Exception) {
                    Log.e("startChargeService", "" + ex.message)
                }
            }
        }

    }

    private class ConfigInstallerThread : Thread() {
        override fun run() {
            super.run()
            ModeConfigInstaller().configCodeVerify()
        }

    }

    private class ThermalCheckThread(private var context: Context) : Thread() {
        override fun run() {
            super.run()

            if (MagiskExtend.magiskSupported() && KernelProrp.getProp("${MagiskExtend.MAGISK_PATH}system/vendor/etc/thermal-engine.current.ini") != "") {
                if (RootFile.list("/data/thermal/config").size > 0) {
                    KeepShellPublic.doCmdSync(
                            "rm -rf /data/thermal 2> /dev/null\n" +
                                    "mkdir -p /data/thermal/config 2> /dev/null\n" +
                                    "chattr +i /data/thermal/config 2> /dev/null")
                } else if (RootFile.list("/data/vendor/thermal/config").size > 0) {
                    KeepShellPublic.doCmdSync(
                            "rm -rf /data/vendor/thermal 2> /dev/null\n" +
                                    "mkdir -p /data/vendor/thermal/config 2> /dev/null\n" +
                                    "chattr +i /data/vendor/thermal/config 2> /dev/null")
                } else {
                    return
                }
                DialogHelper.helpInfo(context,"", "检测到系统自动创建了温控副本，这会导致在附加功能中切换的温控失效。\n\nScene已自动将副本删除，但可能需要重启手机才能解决问题")
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        /*
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .detectAll()
                .build());
        */

        if (globalSPF == null) {
            globalSPF = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        }

        themeMode = ThemeSwitch.switchTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // AppShortcutManager(this.applicationContext).removeMenu()
        // checkUseState()
        /*
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                val item = supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1)
                title = item.name
            } else {
                title = getString(R.string.app_name)
            }
        }
        */

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.menu.findItem(R.id.nav_battery).isEnabled = BatteryUtils().isSupport

        if (CheckRootStatus.lastCheckResult) {
            try {
                setHomePage()
            } catch (ex: Exception) {
                DialogHelper.animDialog(AlertDialog.Builder(this).setTitle(getString(R.string.sorry))
                        .setMessage("启动应用失败\n" + ex.message).setNegativeButton(getString(R.string.btn_retry)) { _, _ ->
                            setHomePage()
                        })
            }
            if (!BackupRestoreUtils.isSupport()) {
                navigationView.menu.findItem(R.id.nav_img).isEnabled = false
            }
            ConfigInstallerThread().start()
            ServiceCreateThread(this).run()
            ThermalCheckThread(this).run()
        } else {
            try {
                setNotRootPage()
            } catch (ex: java.lang.Exception) {
            }
            hideRootMenu(navigationView.menu)
        }

    }

    override fun onResume() {
        super.onResume()

        // 如果距离上次检查更新超过 24 小时
        if (globalSPF!!.getLong(SpfConfig.GLOBAL_SPF_LAST_UPDATE, 0) + (3600 * 24 * 1000) < System.currentTimeMillis()) {
            Update().checkUpdate(this)
            globalSPF!!.edit().putLong(SpfConfig.GLOBAL_SPF_LAST_UPDATE, System.currentTimeMillis()).apply()
        }
    }

    private fun setHomePage() {
        val fragmentManager = supportFragmentManager
        fragmentManager.fragments.clear()
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.main_content, FragmentHome())
        // transaction.addToBackStack(getString(R.string.app_name))
        transaction.commit()
    }

    private fun setNotRootPage() {
        val fragmentManager = supportFragmentManager
        fragmentManager.fragments.clear()
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.main_content, FragmentNotRoot())
        transaction.commit()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    }

    //返回键事件
    override fun onBackPressed() {
        try {
            val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
            when {
                drawer.isDrawerOpen(GravityCompat.START) -> drawer.closeDrawer(GravityCompat.START)
                supportFragmentManager.backStackEntryCount > 0 -> {
                    supportFragmentManager.popBackStack()
                }
                else -> {
                    setExcludeFromRecents(true)
                    super.onBackPressed()
                    this.finishActivity(0)
                }
            }
        } catch (ex: Exception) {
            ex.stackTrace
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    //右上角菜单
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> startActivity(Intent(this.applicationContext, ActivitySceneOtherSettings::class.java))
            R.id.action_power -> DialogPower(this).showPowerMenu()
            R.id.action_graph -> {
                if (FloatMonitor.isShown == true) {
                    FloatMonitor(this).hidePopupWindow()
                    return false
                }
                if (Build.VERSION.SDK_INT >= 23) {
                    if (Settings.canDrawOverlays(this)) {
                        showFloatMonitor()
                    } else {
                        //若没有权限，提示获取
                        //val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        //startActivity(intent);
                        val intent = Intent()
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                        intent.data = Uri.fromParts("package", this.packageName, null)
                        Toast.makeText(applicationContext, getString(R.string.permission_float), Toast.LENGTH_LONG).show();
                    }
                } else {
                    showFloatMonitor()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showFloatMonitor() {
        DialogHelper.animDialog(AlertDialog.Builder(this)
                .setMessage(getString(R.string.float_monitor_tips))
                .setPositiveButton(R.string.btn_confirm) { _, _ ->
                    FloatMonitor(this).showPopupWindow()
                }
                .setNegativeButton(R.string.btn_cancel) { _, _ ->
                })
    }

    //导航菜单选中
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(R.animator.fragment_enter, R.animator.fragment_exit)
        var fragment: Fragment? = null

        //以下代码用于去除阴影
        if (Build.VERSION.SDK_INT >= 21)
            supportActionBar!!.elevation = 0f

        when (id) {
            R.id.nav_home -> fragment = FragmentHome()
            R.id.nav_freeze -> fragment = FragmentFreeze.createPage()
            R.id.nav_applictions -> fragment = FragmentApplistions.createPage()
            R.id.nav_swap -> fragment = FragmentSwap.createPage()
            R.id.nav_battery -> fragment = FragmentBattery.createPage()
            R.id.nav_img -> fragment = FragmentImg.createPage()
            R.id.nav_battery_stats -> fragment = FragmentBatteryStats.createPage()
            R.id.nav_core_control -> fragment = FragmentCpuControl.newInstance()
            R.id.nav_paypal -> fragment = FragmentPay.createPage()
            R.id.nav_qq -> {
                val key = "6ffXO4eTZVN0eeKmp-2XClxizwIc7UIu" //""e-XL2In7CgIpeK_sG75s-vAiu7n5DnlS"
                val intent = Intent()
                intent.data = Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D" + key)
                // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return try {
                    startActivity(intent)
                    true
                } catch (e: Exception) {
                    // 未安装手Q或安装的版本不支持
                    false
                }
            }
            R.id.nav_share -> {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_link))
                sendIntent.type = "text/plain"
                startActivity(sendIntent)
            }
            R.id.nav_profile -> {
                fragment = FragmentConfig.createPage()
            }
            R.id.nav_app_magisk -> {
                fragment = FragmentMagisk.createPage()
            }
            R.id.nav_additional -> fragment = FragmentAddin.createPage(themeMode)
            R.id.nav_keyevent -> {
                try {
                    val intent = Intent(this, AccessibilityKeySettings::class.java)
                    startActivity(intent)
                } catch (ex: Exception) {
                }
            }
        }

        if (fragment != null) {
            transaction.disallowAddToBackStack()
            transaction.replace(R.id.main_content, fragment)
            //transaction.addToBackStack(item.title.toString());
            transaction.commit()
            title = item.title
            //item.isChecked = true
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return false
    }

    private fun hideRootMenu(menu: Menu) {
        try {
            menu.findItem(R.id.nav_applictions).isEnabled = false
            menu.findItem(R.id.nav_swap).isEnabled = false
            menu.findItem(R.id.nav_core_control).isEnabled = false
            menu.findItem(R.id.nav_battery).isEnabled = false
            menu.findItem(R.id.nav_img).isEnabled = false
            menu.findItem(R.id.nav_profile).isEnabled = false
            menu.findItem(R.id.nav_additional).isEnabled = false
            menu.findItem(R.id.nav_app_magisk).isEnabled = false
            menu.findItem(R.id.nav_freeze).isEnabled = false
        } catch (ex: Exception) {
        }
    }

    public override fun onPause() {
        super.onPause()
        if (!CheckRootStatus.lastCheckResult) {
            finish()
        }
    }

    override fun onDestroy() {
        val fragmentManager = supportFragmentManager
        fragmentManager.fragments.clear()
        super.onDestroy()
    }
}
