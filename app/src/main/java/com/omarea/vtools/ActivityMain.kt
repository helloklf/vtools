package com.omarea.vtools

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.content.PermissionChecker
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.omarea.shared.AccessibleServiceHelper
import com.omarea.shared.CrashHandler
import com.omarea.shared.SpfConfig
import com.omarea.shared.Update
import com.omarea.shell.Platform
import com.omarea.shell.units.BackupRestoreUnit
import com.omarea.shell.units.BatteryUnit
import com.omarea.vtools.dialogs.DialogPower
import kotlinx.android.synthetic.main.activity_main.*

class ActivityMain : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var hasRoot = false
    private var globalSPF: SharedPreferences? = null

    private fun setExcludeFromRecents(exclude: Boolean? = null) {
        try {
            val service = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (task in service.appTasks) {
                if (task.taskInfo.id == this.taskId) {
                    val b = if (exclude == null) globalSPF!!.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT, false) else exclude
                    task.setExcludeFromRecents(b)
                }
            }
        } catch (ex: Exception) {
            Log.e("excludeRecent", ex.message)
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
        CrashHandler().init(this)
        startActivityForResult(Intent(this.applicationContext, StartSplashActivity::class.java), 999)
        //setMaxAspect()
        if (globalSPF == null) {
            globalSPF = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT) {
                    setExcludeFromRecents(sharedPreferences.getBoolean(key, false))
                }
            }
            globalSPF!!.registerOnSharedPreferenceChangeListener(listener)
        }
        if (globalSPF!!.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false))
            this.setTheme(R.style.AppTheme_NoActionBarNight)
        /*
        使用壁纸高斯模糊作为窗口背景
        val wallPaper = WallpaperManager.getInstance(this).getDrawable();
        this.getWindow().setBackgroundDrawable(wallPaper);

        this.getWindow().setBackgroundDrawable(BitmapDrawable(resources, rsBlur((wallPaper as BitmapDrawable).bitmap, 25)))
        */
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun rsBlur(source: Bitmap, radius: Int): Bitmap {
        val inputBmp = source
        val renderScript = RenderScript.create(this);

        // Allocate memory for Renderscript to work with
        //(2)
        val input = Allocation.createFromBitmap(renderScript, inputBmp);
        val output = Allocation.createTyped(renderScript, input.getType());
        //(3)
        // Load up an instance of the specific script that we want to use.
        val scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        //(4)
        scriptIntrinsicBlur.setInput(input);
        //(5)
        // Set the blur radius
        scriptIntrinsicBlur.setRadius(radius.toFloat());
        //(6)
        // Start the ScriptIntrinisicBlur
        scriptIntrinsicBlur.forEach(output);
        //(7)
        // Copy the output to the blurred bitmap
        output.copyTo(inputBmp);
        //(8)
        renderScript.destroy();

        return inputBmp;
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (this.isDestroyed) {
            return
        }
        if (requestCode == 999) {
            hasRoot = resultCode == Activity.RESULT_OK
            setExcludeFromRecents()
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
            navigationView.menu.findItem(R.id.nav_battery).isEnabled = BatteryUnit().isSupport

            if (!hasRoot)
                hideRootMenu(navigationView.menu)
            else {
                if (!AccessibleServiceHelper().serviceIsRunning(this) && globalSPF!!.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_SIMPLE, false)) {
                    val intent = Intent(this, MonitorService::class.java)
                    startService(intent)
                }
                try {
                    setHomePage()
                } catch (ex: Exception) {
                    AlertDialog.Builder(this).setTitle(getString(R.string.sorry)).setMessage("启动应用失败\n" + ex.message).setNegativeButton("重试", { _, _ ->
                        setHomePage()
                    }).create().show()
                }
                if (!BackupRestoreUnit.isSupport()) {
                    navigationView.menu.findItem(R.id.nav_img).isEnabled = false
                }
            }
            /*
            val file = File(Environment.getExternalStorageDirectory().absolutePath + "/vtools-error.log")
            if (file.exists()) {
                val error = String(FileInputStream(file).readBytes())
                AlertDialog.Builder(this)
                        .setTitle("异常退出记录")
                        .setMessage("检测到Scene在之前的运行过程中出现错误，错误信息如下：\n\n" + error + "\n\n请截屏错误信息，或将本机存储目录下的vtools-error.log发送给开发者，以便解决问题！")
                        .setNegativeButton(R.string.btn_iknow, {
                            _, _ ->
                        })
                        .setPositiveButton(R.string.btn_delete, {
                            _, _ ->
                            file.delete()
                        })
                        .create()
                        .show()
            }
            */
            /*
            // 在线检查更新：停止维护，不需要了
            if (globalSPF!!.getLong(SpfConfig.GLOBAL_SPF_LAST_UPDATE, 0) + (1000 * 7200) < System.currentTimeMillis()) {
                Update().checkUpdate(this)
                globalSPF!!.edit().putLong(SpfConfig.GLOBAL_SPF_LAST_UPDATE, System.currentTimeMillis()).apply()
            }
            */
        }
    }

    fun checkUseState() {
        if (!(checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE) && checkPermission(Manifest.permission.PACKAGE_USAGE_STATS))) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    }

    private fun checkPermission(permission: String): Boolean =
            PermissionChecker.checkSelfPermission(this@ActivityMain, permission) == PermissionChecker.PERMISSION_GRANTED


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
        }
        return super.onOptionsItemSelected(item)
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
            R.id.nav_applictions -> fragment = FragmentApplistions.createPage()
            R.id.nav_swap -> fragment = FragmentSwap.createPage()
            R.id.nav_battery -> fragment = FragmentBattery.createPage()
            R.id.nav_img -> fragment = FragmentImg.createPage()
            R.id.nav_battery_stats -> fragment = FragmentBatteryStats.createPage()
            R.id.nav_core_control -> fragment = FragmentCpuControl.newInstance()
            R.id.nav_paypal -> {
                fragment = FragmentPay.createPage()
                // Alipay(this).jumpAlipay()
                // startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/duduski")))
            }
            R.id.nav_qq -> {
                val key = "6ffXO4eTZVN0eeKmp-2XClxizwIc7UIu" //""e-XL2In7CgIpeK_sG75s-vAiu7n5DnlS"
                val intent = Intent()
                intent.data = Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D" + key)
                // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    startActivity(intent)
                    return true
                } catch (e: Exception) {
                    // 未安装手Q或安装的版本不支持
                    return false
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
                if (Build.BRAND.toLowerCase() == "meizu" || Build.MANUFACTURER.toLowerCase() == "meizu") {
                    val soc = Platform().getCPUName()
                    if (soc == "sdm845" || soc == "710") {
                        android.app.AlertDialog.Builder(this)
                                .setTitle("暂不支持该设备")
                                .setMessage("根据许多魅族16th/16th Plus用户反馈，使用性能调度模式以后会无法正常开机（原因不详）。为了避免更多用户被坑，该功能现在将直接不再允许Meizu的sdm845、sdm710系列手机使用！")
                                .setPositiveButton(R.string.btn_iknow, { _, _ -> })
                                .create()
                                .show()
                        return false
                    }
                }
                fragment = FragmentConfig.createPage()
            }
            R.id.nav_additional -> fragment = FragmentAddin.createPage()
            R.id.nav_keyevent -> {
                try {
                    val intent = Intent(this, ActivityAccessibilityKeyEventSettings::class.java)
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
        } catch (ex: Exception) {

        }
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        val fragmentManager = supportFragmentManager
        fragmentManager.fragments.clear()
        super.onDestroy()
    }
}
