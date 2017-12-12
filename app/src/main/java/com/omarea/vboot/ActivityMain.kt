package com.omarea.vboot

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.PermissionChecker
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import com.omarea.shared.*
import com.omarea.shell.Busybox
import com.omarea.shell.CheckRootStatus
import com.omarea.shell.units.BusyboxInstallerUnit
import com.omarea.vboot.dialogs.DialogPower
import kotlinx.android.synthetic.main.activity_main.*

class ActivityMain : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit internal var thisview: AppCompatActivity
    lateinit internal var cmdshellTools: cmd_shellTools
    lateinit internal var progressBar: ProgressBar

    internal var myHandler: android.os.Handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val crashHandler = CrashHandler.instance
        crashHandler.init(this)

        thisview = this
        progressBar = findViewById(R.id.shell_on_execute) as ProgressBar
        cmdshellTools = cmd_shellTools(this, progressBar)

        checkRoot(Runnable {
            checkBusybox()
        })
    }

    private fun next() {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        val fragment = FragmentHome()
        transaction.replace(R.id.main_content, fragment)
        transaction.commit()

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        //判断是否开启了充电加速和充电保护，如果开启了，自动启动后台服务
        val chargeConfig = getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
        if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false) || chargeConfig!!.getBoolean(SpfConfig.CHARGE_SPF_BP, false)) {
            try {
                val intent = Intent(this.applicationContext, ServiceBattery::class.java)
                this.applicationContext.startService(intent)
            } catch (ex: Exception) {
            }
        }

        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)

        checkFileWrite()
    }

    private fun checkBusybox() {
        Busybox(this).forceInstall(Runnable {
            next()
        })
    }

    private fun checkRoot(next: Runnable) {
        CheckRootStatus(progressBar, this, Runnable {
            next.run()
        }).forceGetRoot()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

    }

    private fun checkPermission(permission: String): Boolean =
            PermissionChecker.checkSelfPermission(this@ActivityMain, permission) == PermissionChecker.PERMISSION_GRANTED

    //检查权限 主要是文件读写权限
    private fun checkFileWrite() {
        Thread(Runnable {
            if (!(checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE) && checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
                ActivityCompat.requestPermissions(
                        this@ActivityMain,
                        arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                                Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Manifest.permission.WAKE_LOCK
                        ),
                        0x11
                )
            }
        }).start()
    }

    //返回键事件
    override fun onBackPressed() {
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        when {
            drawer.isDrawerOpen(GravityCompat.START) -> drawer.closeDrawer(GravityCompat.START)
            supportFragmentManager.backStackEntryCount > 0 -> supportFragmentManager.popBackStack()
            else -> super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    //右上角菜单
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when ( item.itemId) {
            R.id.action_settings -> startActivity(Intent(thisview, ActivityAccessibilitySettings::class.java))
            R.id.action_power -> DialogPower(thisview).showPowerMenu()
        }
        return super.onOptionsItemSelected(item)
    }

    //导航菜单选中
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val transaction = supportFragmentManager.beginTransaction()
        var fragment: Fragment? = null

        //以下代码用于去除阴影
        if (Build.VERSION.SDK_INT >= 21)
            supportActionBar!!.elevation = 0f

        when (id) {
            R.id.nav_home -> fragment = FragmentHome()
            R.id.nav_booster -> fragment = FragmentBooster.createPage(this)
            R.id.nav_applictions -> fragment = FragmentApplistions.createPage(this)
            R.id.nav_swap -> fragment = FragmentSwap.createPage(this, cmdshellTools)
            R.id.nav_tasks -> fragment = FragmentTasks.Create(this, cmdshellTools)
            R.id.nav_battery -> fragment = FragmentBattery.createPage(cmdshellTools)
            R.id.nav_img -> fragment = FragmentImg.createPage(this, cmdshellTools)
            R.id.nav_share -> {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_TEXT, application.getString(R.string.share_link))
                sendIntent.type = "text/plain"
                startActivity(sendIntent)
            }
            R.id.nav_feedback -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(application.getString(R.string.feedback_link))))
            R.id.nav_profile -> fragment = FragmentConfig.createPage(this, cmdshellTools)
            R.id.nav_additional -> fragment = FragmentAddin.createPage(this)
            R.id.nav_xposed -> fragment = FragmentXposed.Create(this)
        }

        if (fragment != null) {
            transaction.disallowAddToBackStack()
            transaction.replace(R.id.main_content, fragment)
            transaction.commit()
            title = item.title
            item.isChecked = true
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return false
    }

    public override fun onPause() {
        super.onPause()
    }
}
