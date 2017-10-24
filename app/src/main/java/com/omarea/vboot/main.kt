package com.omarea.vboot

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import java.io.File

class main : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit internal var thisview: AppCompatActivity
    lateinit internal var fab: FloatingActionButton
    lateinit internal var cmdshellTools: cmd_shellTools
    internal var onToggleSys: Boolean = false
    lateinit internal var progressBar: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        val crashHandler = CrashHandler.instance
        crashHandler.init(this)
        //传入参数必须为Activity，否则AlertDialog将不显示。

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        val fragment = fragment_home()
        transaction.replace(R.id.main_content, fragment)
        transaction.commit()

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        fab = findViewById(R.id.fab) as FloatingActionButton

        thisview = this
        progressBar = findViewById(R.id.shell_on_execute) as ProgressBar
        cmdshellTools = cmd_shellTools(this, progressBar)

        if (!CheckRootStatus().IsRoot()) {
            Snackbar.make(fab, application.getString(R.string.error_root), Snackbar.LENGTH_LONG).show()
            return
        }

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
                    { a,b->
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

        //自动启动后台服务
        if(ConfigInfo.getConfigInfo().QcMode || ConfigInfo.getConfigInfo().BatteryProtection){
            try{
                val intent = Intent( this.applicationContext, BatteryService::class.java)
                this.applicationContext.startService(intent)
            } catch (ex:Exception){
            }
        }

        if (cmdshellTools.IsDualSystem())
            fab.visibility = View.VISIBLE
        else
            fab.visibility = View.GONE

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)
        val menu = navigationView.menu
        val nav_vboot = menu.findItem(R.id.nav_vboot)
        if (cmdshellTools.IsDualSystem())
            nav_vboot.isVisible = true
        val nav_profile = menu.findItem(R.id.nav_profile)

        if (DynamicConfig().DynamicSupport(this)) {
            nav_profile.isVisible = true
        }


        fab.setOnClickListener { view ->
            fab.visibility = View.GONE//禁用系统切换按钮
            onToggleSys = true
            cmdshellTools.ToggleSystem()//切换系统
            Snackbar.make(view, "正在切换系统，请稍等...", Snackbar.LENGTH_SHORT).setAction("Action", null).show()
            navigationView.visibility = View.GONE
            toolbar.visibility = View.GONE
            findViewById(R.id.main_content)!!.visibility = View.GONE
        }

        InitApp()
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

    private fun InitApp() {
        Thread(Runnable {
            if (!(CheckSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) && CheckSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
                ActivityCompat.requestPermissions(this@main, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Manifest.permission.WAKE_LOCK), 0x11)
            }

            cmdshellTools.InstallShellTools()
        }).start()
    }

    override fun onBackPressed() {
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else if (onToggleSys) {
            return
        }
        else if(supportFragmentManager.backStackEntryCount>0){
            supportFragmentManager.popBackStack();
        }
        else {
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

        fab.visibility = View.GONE

        if (id == R.id.nav_home) {
            fragment = fragment_home()
            if (cmdshellTools.IsDualSystem())
                fab.visibility = View.VISIBLE
        } else if (id == R.id.nav_vboot) {
            val rom2 = File("/MainData")
            if (rom2.exists()) {
                Snackbar.make(fab, "请在系统一下进行操作！", Snackbar.LENGTH_SHORT).show()
            } else {
                fragment = fragment_vboot.Create(this, cmdshellTools)
            }
        } else if (id == R.id.nav_booster) {
            fragment = fragment_booster.Create(this, cmdshellTools)
        } else if (id == R.id.nav_applictions) {
            fragment = fragment_applistions.Create(this, cmdshellTools)
        } else if(id==R.id.nav_swap){
            fragment = fragment_swap.Create(this, cmdshellTools)
        } else if(id==R.id.nav_battery){
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
        ConfigInfo.getConfigInfo().saveChange()

        super.onPause()
    }

    override fun onDestroy() {
        ConfigInfo.getConfigInfo().saveChange()

        super.onDestroy()
    }
}
