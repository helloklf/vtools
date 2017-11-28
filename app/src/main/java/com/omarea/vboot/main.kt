package com.omarea.vboot

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
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
import com.omarea.shell.DynamicConfig
import com.omarea.shell.units.BusyboxInstallerUnit
import java.io.File

class main : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit internal var thisview: AppCompatActivity
    lateinit internal var cmdshellTools: cmd_shellTools
    internal var onToggleSys: Boolean = false
    lateinit internal var progressBar: ProgressBar

    internal var myHandler: android.os.Handler = object : android.os.Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val crashHandler = CrashHandler.instance
        crashHandler.init(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        thisview = this
        progressBar = findViewById(R.id.shell_on_execute) as ProgressBar
        cmdshellTools = cmd_shellTools(this, progressBar)

        progressBar.visibility = View.VISIBLE
        checkRoot(Runnable {
            checkBusybox()
        })
    }

    private fun checkBusybox() {
        if (!Busybox().IsBusyboxInstalled()) {
            //Snackbar.make(fab, application.getString(R.string.error_busybox), Snackbar.LENGTH_LONG).show()
            AlertDialog.Builder(this)
                .setTitle("安装Busybox吗？")
                .setMessage("你的手机似乎没有安装busybox，这会导致微工具箱无法使用，是否要立即安装（需要修改System）？")
                .setNegativeButton(
                        "取消",
                        { dialog, which ->
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                )
                .setPositiveButton(
                        "确定",
                        { a, b ->
                            AppShared.WriteFile(thisview.assets, "busybox.zip", "busybox")
                            val cmd = StringBuilder("cp /sdcard/Android/data/com.omarea.vboot/busybox /cache/busybox\nchmod 0777 /cache/busybox\n")
                            cmd.append(Consts.MountSystemRW2)
                            cmd.append("cp /cache/busybox /system/xbin/busybox\n/cache/busybox chmod 0777 /system/xbin/busybox\n")
                            cmdshellTools.DoCmdSync(cmd.toString())
                            //android.os.Process.killProcess(android.os.Process.myPid());
                        }
                )
                .setCancelable(false)
                .create().show()
            //return
        }
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        val fragment = fragment_home()
        transaction.replace(R.id.main_content, fragment)
        transaction.commit()

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        //判断是否开启了充电加速和充电保护，如果开启了，自动启动后台服务
        var chargeConfig = getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
        if (chargeConfig!!.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false) || chargeConfig!!.getBoolean(SpfConfig.CHARGE_SPF_BP, false)) {
            try {
                val intent = Intent(this.applicationContext, BatteryService::class.java)
                this.applicationContext.startService(intent)
            } catch (ex: Exception) {
            }
        }

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)
        val menu = navigationView.menu

        checkFileWrite()
    }

    private fun checkRoot(next: Runnable) {
        var completed = false
        Thread {
            if (!CheckRootStatus().IsRoot()) {
                completed = true
                myHandler.post {
                    progressBar.visibility = View.GONE
                    var alert = android.support.v7.app.AlertDialog.Builder(this)
                    alert.setCancelable(false)
                    alert.setTitle(R.string.error_root)
                    alert.setNegativeButton(R.string.btn_refresh, { a, b ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            this.recreate()
                        } else {
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
                    })
                    alert.setNeutralButton(R.string.btn_exit, { a, b ->
                        android.os.Process.killProcess(android.os.Process.myPid())
                    })
                    alert.create().show()
                }
            } else {
                completed = true
                myHandler.post {
                    progressBar.visibility = View.GONE
                    next.run()
                }
            }
        }.start()
        myHandler.postDelayed({
            if (!completed) {
                progressBar.visibility = View.GONE
                var alert = android.support.v7.app.AlertDialog.Builder(this)
                alert.setCancelable(false)
                alert.setTitle(R.string.error_root)
                alert.setMessage(R.string.error_su_timeout)
                alert.setNegativeButton(R.string.btn_refresh, { a, b ->
                    this.recreate()
                })
                alert.setNeutralButton(R.string.btn_exit, { a, b ->
                    android.os.Process.killProcess(android.os.Process.myPid())
                })
                alert.create().show()
            }
        }, 10000)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

    }

    /*
    public boolean selfPermissionGranted(String permission)
    {
        // For Android < Android M, self permissions are always granted.
        boolean result = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (targetSdkVersion >= Build.VERSION_CODES.M)
            {
                // targetSdkVersion >= Android M, we can
                // use Context#checkSelfPermission
                result = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
            }
            else
            {
                // targetSdkVersion < Android M, we have to use PermissionChecker
                result = PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED;
            }
        }
        return result;
    }
    */

    internal fun CheckSelfPermission(permission: String): Boolean {
        return PermissionChecker.checkSelfPermission(this@main, Manifest.permission.READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED
    }

    //检查权限 主要是文件读写权限
    private fun checkFileWrite() {
        Thread(Runnable {
            if (!(CheckSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) && CheckSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
                ActivityCompat.requestPermissions(this@main, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Manifest.permission.WAKE_LOCK), 0x11)
            }

            BusyboxInstallerUnit().InstallShellTools()
        }).start()
    }

    //返回键事件
    override fun onBackPressed() {
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else if (onToggleSys) {
            return
        } else if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack();
        } else {
            //supportFragmentManager.fragments.clear()
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    //右上角菜单
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.action_graph -> {
                try {
                    var intent = this.packageManager.getLaunchIntentForPackage("com.omarea.kernel");
                    //intent.putExtra("section", "modules")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {

                }
            }
            R.id.action_settings -> {
                val intent = Intent(thisview, accessibility_settings::class.java)
                startActivity(intent)
            }
            R.id.action_hotreboot -> {
                cmdshellTools.DoCmd(Consts.RebootHot)
            }
            R.id.action_reboot -> {
                cmdshellTools.DoCmd(Consts.Reboot)
            }
            R.id.action_reboot_rec -> {
                cmdshellTools.DoCmd(Consts.RebootRecovery)
            }
            R.id.action_reboot_bl -> {
                cmdshellTools.DoCmd(Consts.RebootBootloader)
            }
            R.id.action_reboot9008 -> {
                cmdshellTools.DoCmd(Consts.RebootOEM_EDL)
            }
            R.id.action_shutdown -> {
                cmdshellTools.DoCmd(Consts.RebootShutdown)
            }
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

        if (id == R.id.nav_home) {
            fragment = fragment_home()
        } else if (id == R.id.nav_booster) {
            fragment = fragment_booster.Create(this, cmdshellTools)
        } else if (id == R.id.nav_applictions) {
            fragment = fragment_applistions.Create(this, cmdshellTools)
        } else if (id == R.id.nav_swap) {
            fragment = fragment_swap.Create(this, cmdshellTools)
        } else if (id == R.id.nav_tasks) {
            fragment = fragment_tasks.Create(this, cmdshellTools)
        } else if (id == R.id.nav_battery) {
            fragment = fragment_battery.Create(this, cmdshellTools)
        } else if (id == R.id.nav_img) {
            fragment = fragment_img.Create(this, cmdshellTools)
        } else if (id == R.id.nav_share) {
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, application.getString(R.string.share_link))
            sendIntent.type = "text/plain"
            startActivity(sendIntent)
        } else if (id == R.id.nav_feedback) {
            val url = application.getString(R.string.feedback_link)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            /*
            String url = "http://jq.qq.com/?_wv=1027&k=2F5EVSu"; // web address
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
            */
        } else if (id == R.id.nav_profile) {
            fragment = fragment_config.Create(this, cmdshellTools)
        } else if (id == R.id.nav_additional) {
            fragment = fragment_addin.Create(this, cmdshellTools)
        } else if (id == R.id.nav_help) {
            fragment = fragment_helpinfo()
        } else if (id == R.id.nav_xposed) {
            fragment = fragment_xposed.Create(this, cmdshellTools)
        }

        if (fragment != null) {
            transaction.disallowAddToBackStack()
            transaction.replace(R.id.main_content, fragment)
            transaction.commit()
            title = item.title
            item.isChecked = true
        }


        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return false
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        //System.exit(0)
        super.onDestroy()
    }
}
