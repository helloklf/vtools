package com.omarea.vboot

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.PermissionChecker
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.omarea.shared.ConfigInstaller
import com.omarea.shared.Consts
import com.omarea.shared.CrashHandler
import com.omarea.shared.SpfConfig
import com.omarea.shared.helper.NotifyHelper
import com.omarea.shell.Busybox
import com.omarea.shell.CheckRootStatus
import com.omarea.shell.SuDo
import com.omarea.shell.units.BatteryUnit
import com.omarea.ui.AppShortcutManager
import com.omarea.ui.ProgressBarDialog
import com.omarea.vboot.dialogs.DialogPower
import kotlinx.android.synthetic.main.activity_main.*

class ActivityMain : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit internal var thisview: AppCompatActivity
    private var hasRoot = false
    private var globalSPF: SharedPreferences? = null
    private var myHandler = Handler()

    private fun setExcludeFromRecents(exclude:Boolean? = null) {
        try {
            val service = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (task in service.appTasks) {
                if (task.taskInfo.id == this.taskId) {
                    val b = if (exclude == null) globalSPF!!.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT, false) else exclude
                    task.setExcludeFromRecents(b)
                }
            }
        } catch (ex: Exception) {
            ex.stackTrace
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (globalSPF == null) {
            globalSPF = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
            val listener = SharedPreferences.OnSharedPreferenceChangeListener {
                sharedPreferences, key ->
                if (key == SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT) {
                    setExcludeFromRecents(sharedPreferences.getBoolean(key, false))
                }
            }
            globalSPF!!.registerOnSharedPreferenceChangeListener(listener)
        }
        if (globalSPF!!.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false))
            this.setTheme(R.style.AppTheme_NoActionBarNight)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val crashHandler = CrashHandler.instance
        crashHandler.init(this)

        thisview = this
        //checkFileWrite()
        checkRoot(Runnable {
            hasRoot = true
            checkBusybox()
        }, Runnable {
            next()
        })
        setExcludeFromRecents()
        AppShortcutManager(thisview).removeMenu()
        //checkUseState()

        val intent = Intent(this, ActivityAddinOnline::class.java)
        //startActivity(intent)
    }

    fun checkUseState() {
        if (!(checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE) && checkPermission(Manifest.permission.PACKAGE_USAGE_STATS))) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        }
    }

    private fun getModName(mode:String) : String {
        when(mode) {
            "powersave" ->      return "省电模式"
            "performance" ->      return "性能模式"
            "fast" ->      return "极速模式"
            "balance" ->   return "均衡模式"
            else ->         return "未知模式"
        }
    }

    private fun getAppName(packageName: String): String{
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0);
            return  packageInfo.applicationInfo.loadLabel(packageManager).toString()
        } catch (ex : Exception) {
            return  packageName;
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun quickSwitchMode(){
        val parameterValue = this.intent.getStringExtra("packageName");
        if(packageName == null || parameterValue.isEmpty()) {
            return
        }
        val spfPowercfg = getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
        val mode = spfPowercfg.getString(parameterValue, "balance");
        var index = 0;

        when(mode) {
            "powersave" ->      index = 0
            "balance" ->   index = 1
            "performance" ->      index = 2
            "fast" ->      index = 3
            else ->         index = 1
        }

        AlertDialog.Builder(this)
                .setPositiveButton(R.string.btn_confirm, { dialog, which ->
                    try {
                        var selectedMode = ""
                        when(index) {
                            0 -> selectedMode = "powersave"
                            1 -> selectedMode = "balance"
                            2 -> selectedMode = "performance"
                            3 -> selectedMode = "fast"
                            4 -> selectedMode = "igoned"
                        }
                        spfPowercfg.edit().putString(parameterValue, selectedMode).commit()
                        SuDo(this).execCmd(String.format(Consts.ToggleMode, selectedMode));
                        NotifyHelper(this).notify("${getModName(selectedMode)} -> $parameterValue" , parameterValue)

                        val intent = getPackageManager().getLaunchIntentForPackage(parameterValue);
                        startActivity(intent);
                    } catch (ex : Exception) {

                    }
                })
                .setNegativeButton(R.string.btn_cancel, { dialog, which ->

                })
                .setNeutralButton("", { dialog, which ->

                })
                .setTitle(getAppName(parameterValue))
                .setSingleChoiceItems(arrayOf("省电模式", "均衡模式", "游戏模式", "极速模式", "加入“忽略列表”"),index, { dialog, which ->
                    index = which;
                })
                .create()
                .show()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        try {
            setHomePage()
        } catch (ex: Exception) {
            AlertDialog.Builder(this).setTitle(getString(R.string.sorry)).setMessage("启动应用失败\n" + ex.message).setNegativeButton("重试", { _, _ ->
                setHomePage()
            }).create().show()
        }
    }

    private fun setHomePage() {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        val fragment = FragmentHome()
        transaction.replace(R.id.main_content, fragment)
        transaction.commit()
    }

    private fun next() {
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
        navigationView.menu.findItem(R.id.nav_battery).isEnabled = BatteryUnit().isSupport

        if (!hasRoot)
            hideRootMenu(navigationView.menu);
    }

    private fun checkBusybox() {
        ConfigInstaller().configCodeVerify(this)
        Busybox(this).forceInstall(Runnable {
            next()
        })
    }

    @SuppressLint("ApplySharedPref", "CommitPrefEdits")
    private fun checkRoot(next: Runnable, skip: Runnable) {
        val globalConfig = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (globalConfig.contains(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE_CHECKING)) {
            AlertDialog.Builder(this)
                    .setTitle("兼容性问题")
                    .setMessage("检测到你的设备在上次“兼容性检测”过程中断，“自动SELinux宽容模式”将不会被开启！\n\n因此，有些功能可能无法使用！")
                    .setPositiveButton(R.string.btn_confirm, DialogInterface.OnClickListener { dialog, which ->
                        globalConfig.edit()
                                .putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE_CHECKING, false)
                                .remove(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE_CHECKING)
                                .commit()
                        CheckRootStatus(this, next, skip, false).forceGetRoot()
                    })
                    .setCancelable(false)
                    .create()
                    .show()
            return
        }
        if(!globalConfig.contains(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE)) {
            CheckRootStatus(this, Runnable {
                myHandler.post {
                    globalConfig.edit().putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE_CHECKING, true)
                            .commit()
                    val dialog = ProgressBarDialog(this)
                    dialog.showDialog("兼容性检测，稍等10几秒...")
                    Thread(Runnable {
                        SuDo(this).execCmdSync(Consts.DisableSELinux + "\n sleep 10; \n")
                        myHandler.post {
                            dialog.hideDialog()
                            next.run()
                        }
                        globalConfig.edit()
                                .putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, true)
                                .remove(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE_CHECKING)
                                .commit()
                    }).start()
                }
            }, skip, false).forceGetRoot()
        } else{
            val disableSeLinux = globalConfig.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, true)
            CheckRootStatus(this, next, skip, disableSeLinux).forceGetRoot()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

    }

    override fun onStart() {
        super.onStart()
        if (this.intent.extras != null && this.intent.extras.containsKey("packageName")) {
            quickSwitchMode()
            this.intent.extras.clear()
        }
    }
    override fun onResume() {
        super.onResume()
    }
    override fun onRestart() {
        super.onRestart()
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

    override fun onDestroy() {
        try {
            if (getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
                    .getBoolean(SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT, false)) {
                this.finishAndRemoveTask()
            }
        } catch (ex: Exception) {
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    //右上角菜单
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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
            R.id.nav_booster -> fragment = FragmentBooster.createPage()
            R.id.nav_applictions -> fragment = FragmentApplistions.createPage()
            R.id.nav_swap -> fragment = FragmentSwap.createPage()
            R.id.nav_battery -> fragment = FragmentBattery.createPage()
            R.id.nav_img -> fragment = FragmentImg.createPage(this)
            R.id.nav_core_control -> fragment = FragmentCpuControl.newInstance()
            R.id.nav_whitelist -> fragment = FragmentWhitelist.createPage()
            R.id.nav_share -> {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_TEXT, application.getString(R.string.share_link))
                sendIntent.type = "text/plain"
                startActivity(sendIntent)
            }
            R.id.nav_feedback -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(application.getString(R.string.feedback_link))))
            R.id.nav_profile -> fragment = FragmentConfig.createPage()
            R.id.nav_additional -> fragment = FragmentAddin.createPage()
            R.id.nav_reward -> fragment = FragmentReward.createPage()
            R.id.nav_xposed -> {
                //fragment = FragmentXposed.Create()
                try {
                    startActivity(Intent().setComponent(ComponentName("com.omarea.vaddin", "com.omarea.vaddin.MainActivity")))
                } catch (e: Exception) {
                    AlertDialog.Builder(this).setTitle("Fail！")
                            .setMessage( getString(R.string.xposed_cannot_openaddin))
                            .setPositiveButton(R.string.btn_confirm, DialogInterface.OnClickListener { dialog, which ->  })
                            .create()
                            .show()
                }
            }
        }

        if (fragment != null) {
            //transaction.disallowAddToBackStack()
            transaction.replace(R.id.main_content, fragment)
            transaction.commit()
            title = item.title
            //item.isChecked = true
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return false
    }

    private fun hideRootMenu(menu: Menu) {
        try {
            menu.findItem(R.id.nav_booster).isEnabled = false
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
}
