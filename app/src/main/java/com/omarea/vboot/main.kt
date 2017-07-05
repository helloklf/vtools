package com.omarea.vboot

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
    var fileSelectType: FileSelectType = FileSelectType.BootSave


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
            Snackbar.make(fab, application.getString(R.string.error_busybox), Snackbar.LENGTH_LONG).show()
            return
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

        if (DynamicConfig().DynamicSupport(ConfigInfo.getConfigInfo().CPUName)) {
            nav_profile.isVisible = true
        }


        fab.setOnClickListener { view ->
            fab.visibility = View.GONE//禁用系统切换按钮
            onToggleSys = true
            cmdshellTools.ToggleSystem()//切换系统
            Snackbar.make(view, "正在切换系统，请稍等...", Snackbar.LENGTH_SHORT).setAction("Action", null).show()
            navigationView.visibility = View.GONE
            toolbar.visibility = View.GONE
            findViewById(R.id.main_content).visibility = View.GONE
        }

        InitApp()
    }

    fun setfileSelectType(ft: FileSelectType) {
        this.fileSelectType = ft;
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode == Activity.RESULT_OK) {//是否选择，没选择就不会继续

            val uri = data.data//得到uri，后面就是将uri转化成file的过程。
            var img_path = ""

            var file: File? = null

            val proj = arrayOf(MediaStore.Images.Media.DATA)
            val actualimagecursor = thisview.contentResolver.query(uri, proj, null, null, null)

            if (actualimagecursor != null) {
                val actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                actualimagecursor.moveToFirst()
                img_path = actualimagecursor.getString(actual_image_column_index)
                file = File(img_path)
            } else {
                val uriString = uri.toString()
                var a = arrayOfNulls<String>(2)
                //判断文件是否在sd卡中
                if (uriString.indexOf(Environment.getExternalStorageDirectory().toString()) != -1) {
                    //对Uri进行切割
                    a = uriString.split(Environment.getExternalStorageDirectory().toString().toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    //获取到file
                    file = File(Environment.getExternalStorageDirectory(), a[1])
                    img_path = file.absolutePath
                } else if (uriString.indexOf(Environment.getDataDirectory().toString()) != -1) { //判断文件是否在手机内存中
                    //对Uri进行切割
                    a = uriString.split(Environment.getDataDirectory().toString().toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    //获取到file
                    file = File(Environment.getDataDirectory(), a[1])
                    img_path = file.absolutePath
                }

            }

            if (img_path == "" && file == null) {
                Snackbar.make(fab, application.getString(R.string.error_unsupport_uri), Snackbar.LENGTH_SHORT).show()
                return
            }

            val fileName = file!!.toString()
            when (fileSelectType) {
                FileSelectType.BootSave -> {
                }
                FileSelectType.BootFlash -> {
                    if (!fileName.toLowerCase().endsWith(".img")) {
                        Snackbar.make(fab, application.getString(R.string.error_not_img), Snackbar.LENGTH_LONG).show()
                        return
                    }
                    cmdshellTools.FlashBoot(fileName)
                }
                FileSelectType.RecSave -> {
                }
                FileSelectType.RecFlash -> {
                    if (!fileName.toLowerCase().endsWith(".img")) {
                        Snackbar.make(fab, application.getString(R.string.error_not_img), Snackbar.LENGTH_LONG).show()
                        return
                    }
                    cmdshellTools.FlashRecovery(fileName)
                }
                else -> {
                    Snackbar.make(fab, application.getString(R.string.unknow_action), Snackbar.LENGTH_SHORT).show()
                }
            }
        }
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
