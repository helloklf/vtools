package com.omarea.vtools.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.Switch
import android.widget.Toast
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.OverScrollListView
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.model.Appinfo
import com.omarea.scene_mode.ModeConfigInstaller
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.AppConfigStore
import com.omarea.store.BatteryHistoryStore
import com.omarea.store.SpfConfig
import com.omarea.ui.SceneModeAdapter
import com.omarea.ui.SearchTextWatcher
import com.omarea.ui.TabIconHelper
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.utils.AppListHelper
import com.omarea.vaddin.IAppConfigAidlInterface
import com.omarea.vtools.R
import com.omarea.vtools.activities.ActivityAddinOnline
import com.omarea.vtools.activities.ActivityAdvSettings
import com.omarea.vtools.activities.ActivityAppDetails
import com.omarea.vtools.activities.ActivityFileSelector
import kotlinx.android.synthetic.main.fragment_config.*
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList


class FragmentConfig : Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog
    private lateinit var spfPowercfg: SharedPreferences
    private lateinit var globalSPF: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var applistHelper: AppListHelper
    internal val myHandler: Handler = Handler()
    private var installedList: ArrayList<Appinfo>? = null
    private var displayList: ArrayList<Appinfo>? = null
    private var packageManager: PackageManager? = null
    private lateinit var appConfigStore: AppConfigStore
    private var firstMode = ModeSwitcher.DEFAULT
    private var aidlConn: IAppConfigAidlInterface? = null
    private val configInstaller = ModeConfigInstaller()

    private var conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            aidlConn = IAppConfigAidlInterface.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            aidlConn = null
        }
    }

    private fun bindService() {
        try {
            if (packageManager!!.getPackageInfo("com.omarea.vaddin", 0) == null) {
                return
            }
        } catch (ex: Exception) {
            return
        }
        if (aidlConn != null) {
            return
        }
        try {
            val intent = Intent();
            //绑定服务端的service
            intent.setAction("com.omarea.vaddin.ConfigUpdateService");
            //新版本（5.0后）必须显式intent启动 绑定服务
            intent.setComponent(ComponentName("com.omarea.vaddin", "com.omarea.vaddin.ConfigUpdateService"));
            //绑定的时候服务端自动创建
            if (context!!.bindService(intent, conn, Context.BIND_AUTO_CREATE)) {
            } else {
                throw Exception("")
            }
        } catch (ex: Exception) {
            Toast.makeText(this.context, "连接到“Scene-高级设定”插件失败，请不要阻止插件自启动！", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_config, container, false)
    private lateinit var modeSwitcher: ModeSwitcher

    override fun onResume() {
        super.onResume()
        bindService()
        val serviceState = AccessibleServiceHelper().serviceRunning(context!!)
        btn_config_service_not_active.visibility = if (serviceState) View.GONE else View.VISIBLE
    }

    private fun startService() {
        val dialog = ProgressBarDialog(context!!)
        dialog.showDialog("尝试使用ROOT权限开启服务...")
        Thread(Runnable {
            if (!AccessibleServiceHelper().startSceneModeService(context!!)) {
                try {
                    myHandler.post {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    myHandler.post {
                        dialog.hideDialog()
                    }
                }
            } else {
                myHandler.post {
                    dialog.hideDialog()
                    btn_config_service_not_active.visibility = if (AccessibleServiceHelper().serviceRunning(context!!)) View.GONE else View.VISIBLE
                }
            }
        }).start()
    }

    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (packageManager == null) {
            packageManager = context!!.packageManager
        }

        modeSwitcher = ModeSwitcher()
        processBarDialog = ProgressBarDialog(context!!)
        applistHelper = AppListHelper(context!!)
        spfPowercfg = context!!.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        editor = spfPowercfg.edit()
        firstMode = globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, ModeSwitcher.DEFAULT)!!
        appConfigStore = AppConfigStore(this.context)

        if (spfPowercfg.all.isEmpty()) {
            initDefaultConfig()
        }
        checkConfig()

        btn_config_service_not_active.setOnClickListener {
            startService()
        }

        val tabIconHelper = TabIconHelper(configlist_tabhost, this.activity!!)
        configlist_tabhost.setup()

        tabIconHelper.newTabSpec("APP场景", context!!.getDrawable(R.drawable.tab_app)!!, R.id.configlist_tab0)
        tabIconHelper.newTabSpec("系统场景", context!!.getDrawable(R.drawable.tab_security)!!, R.id.blacklist_tab3)
        tabIconHelper.newTabSpec("设置", context!!.getDrawable(R.drawable.tab_settings)!!, R.id.configlist_tab5)
        configlist_tabhost.currentTab = 0
        configlist_tabhost.setOnTabChangedListener({ tabId ->
            tabIconHelper.updateHighlight()
        })

        battery_monitor.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, false)
        battery_monitor.setOnClickListener {
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, (it as Switch).isChecked).commit()
            BatteryHistoryStore(context!!).clearData()
            Toast.makeText(context!!, "已自动清空耗电统计数据，以便于重新采集！", Toast.LENGTH_SHORT).show()
            reStartService()
        }
        config_defaultlist.setOnItemClickListener { parent, view2, position, id ->
            try {
                val item = (parent.adapter.getItem(position) as Appinfo)
                val intent = Intent(this.context, ActivityAppDetails::class.java)
                intent.putExtra("app", item.packageName)
                startActivityForResult(intent, REQUEST_APP_CONFIG)
                lastClickRow = view2
            } catch (ex: Exception) {
            }
        }
        config_defaultlist.setOnItemLongClickListener { parent, view, position, id ->
            val item = (parent.adapter.getItem(position) as Appinfo)
            var originIndex = 0
            when (spfPowercfg.getString(item.packageName.toString(), firstMode)) {
                ModeSwitcher.POWERSAVE -> originIndex = 0
                ModeSwitcher.BALANCE -> originIndex = 1
                ModeSwitcher.PERFORMANCE -> originIndex = 2
                ModeSwitcher.FAST -> originIndex = 3
                ModeSwitcher.IGONED -> originIndex = 5
                else -> originIndex = 4
            }
            var currentMode = originIndex
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle(item.appName.toString())
                    .setSingleChoiceItems(
                            R.array.powercfg_modes2,
                            originIndex,
                            { dialog, which ->
                                currentMode = which
                            })
                    .setPositiveButton(R.string.btn_confirm, { _, _ ->
                        if (currentMode != originIndex) {
                            var modeName = ""
                            when (currentMode) {
                                0 -> modeName = ModeSwitcher.POWERSAVE
                                1 -> modeName = ModeSwitcher.BALANCE
                                2 -> modeName = ModeSwitcher.PERFORMANCE
                                3 -> modeName = ModeSwitcher.FAST
                                4 -> modeName = ""
                                5 -> modeName = ModeSwitcher.IGONED
                            }

                            if (modeName.isEmpty()) {
                                spfPowercfg.edit().remove(item.packageName.toString()).commit()
                            } else {
                                spfPowercfg.edit().putString(item.packageName.toString(), modeName).commit()
                            }

                            setAppRowDesc(item)
                            (config_defaultlist.adapter as SceneModeAdapter).updateRow(position, view)
                            notifyService(item.packageName.toString(), modeName)
                        }
                    })
                    .setNeutralButton(R.string.btn_cancel, null))
            true
        }

        config_search_box.addTextChangedListener(SearchTextWatcher(Runnable {
            loadList()
        }))
        configlist_modes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadList()
            }
        }
        configlist_type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadList()
            }
        }
        dynamic_control.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, true)
        dynamic_control.setOnClickListener {
            val value = (it as Switch).isChecked
            if (value && !configInstaller.configInstalled()) {
                dynamic_control.isChecked = false
                Toast.makeText(context, "你需要先安装配置脚本", Toast.LENGTH_SHORT).show()
            } else {
                globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, value).commit()
                reStartService()
            }
        }

        loadList()

        val modeValue = globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, "balance")
        when (modeValue) {
            ModeSwitcher.POWERSAVE -> first_mode.setSelection(0)
            ModeSwitcher.BALANCE -> first_mode.setSelection(1)
            ModeSwitcher.PERFORMANCE -> first_mode.setSelection(2)
            ModeSwitcher.FAST -> first_mode.setSelection(3)
            ModeSwitcher.IGONED -> first_mode.setSelection(4)
        }
        first_mode.onItemSelectedListener = ModeOnItemSelectedListener(globalSPF, Runnable {
            reStartService()
            loadList()
        })


        val spfAutoConfig = context!!.getSharedPreferences(SpfConfig.BOOSTER_SPF_CFG_SPF, Context.MODE_PRIVATE)

        bindSPF(auto_switch_network_on_wifi, spfAutoConfig, SpfConfig.WIFI + SpfConfig.ON, false)
        bindSPF(auto_switch_network_on_data, spfAutoConfig, SpfConfig.DATA + SpfConfig.ON, false)
        bindSPF(auto_switch_network_on_nfc, spfAutoConfig, SpfConfig.NFC + SpfConfig.ON, false)
        bindSPF(auto_switch_network_on_gps, spfAutoConfig, SpfConfig.GPS + SpfConfig.ON, false)
        bindSPF(auto_switch_forcedoze_off, spfAutoConfig, SpfConfig.FORCEDOZE + SpfConfig.OFF, false)
        bindSPF(auto_switch_powersave_off, spfAutoConfig, SpfConfig.POWERSAVE + SpfConfig.OFF, false)

        bindSPF(auto_switch_network_off_wifi, spfAutoConfig, SpfConfig.WIFI + SpfConfig.OFF, false)
        bindSPF(auto_switch_network_off_data, spfAutoConfig, SpfConfig.DATA + SpfConfig.OFF, false)
        bindSPF(auto_switch_network_off_nfc, spfAutoConfig, SpfConfig.NFC + SpfConfig.OFF, false)
        bindSPF(auto_switch_network_off_gps, spfAutoConfig, SpfConfig.GPS + SpfConfig.OFF, false)
        bindSPF(auto_switch_forcedoze_on, spfAutoConfig, SpfConfig.FORCEDOZE + SpfConfig.ON, false)
        bindSPF(auto_switch_powersave_on, spfAutoConfig, SpfConfig.POWERSAVE + SpfConfig.ON, false)

        bindSPF(dynamic_lock_mode, globalSPF, SpfConfig.GLOBAL_SPF_LOCK_MODE, false)
        bindSPF(settings_autoinstall, globalSPF, SpfConfig.GLOBAL_SPF_AUTO_INSTALL, false)
        config_customer_powercfg.setOnClickListener {
            chooseLocalConfig()
        }
        config_customer_powercfg_online.setOnClickListener {
            getOnlineConfig()
        }

        config_adv.setOnClickListener {
            try {
                val intent = Intent(this.context, ActivityAdvSettings::class.java)
                startActivity(intent)
            } catch (ex: Exception) {
            }
        }
        scene_notify.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, true)
        scene_notify.setOnClickListener({
            val checked = (it as Switch).isChecked
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, checked).commit()
            reStartService()
        })
    }

    private val REQUEST_POWERCFG_FILE = 1
    private val REQUEST_POWERCFG_ONLINE = 2
    private val REQUEST_APP_CONFIG = 0
    private var lastClickRow: View? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (this.isDetached) {
            return
        }
        if (requestCode == REQUEST_POWERCFG_FILE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                if (data.extras == null || !data.extras.containsKey("file")) {
                    return
                }
                val path = data.extras.getString("file")
                val file = File(path)
                if (file.exists()) {
                    if (file.length() > 200 * 1024) {
                        Toast.makeText(context, "这个文件也太大了，配置脚本大小不能超过200KB！", Toast.LENGTH_LONG).show()
                        return
                    }
                    val lines = file.readText(Charset.defaultCharset()).replace("\r", "")
                    val configStar = lines.split("\n").firstOrNull()
                    if (configStar != null && configStar.startsWith("#!/") && configStar.endsWith("sh")) {
                        if (configInstaller.installPowerConfigByText(context!!, lines)) {
                            configInstalled()
                        } else {
                            Toast.makeText(context, "由于某些原因，安装配置脚本失败，请重试！", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "这似乎是个无效的脚本文件！", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context!!, "所选的文件没找到！", Toast.LENGTH_LONG).show()
                }
            }
            return
        } else if (requestCode == REQUEST_POWERCFG_ONLINE) {
            if (resultCode == Activity.RESULT_OK) {
                configInstalled()
            }
        } else if (requestCode == REQUEST_APP_CONFIG && data != null && displayList != null) {
            try {
                if (resultCode == RESULT_OK) {
                    val adapter = (config_defaultlist.adapter as SceneModeAdapter)
                    var index = -1
                    val packageName = data.extras.getString("app")
                    for (i in 0 until displayList!!.size) {
                        if (displayList!![i].packageName == packageName) {
                            index = i
                        }
                    }
                    if (index < 0) {
                        return
                    }
                    val item = adapter.getItem(index)
                    setAppRowDesc(item)
                    (config_defaultlist.adapter as SceneModeAdapter).updateRow(index, lastClickRow!!)
                    //loadList(false)
                }
            } catch (ex: Exception) {
                Log.e("update-list", "" + ex.message)
            }
        }
    }

    // 通知辅助服务配置变化
    private fun notifyService(app: String, mode: String) {
        if (AccessibleServiceHelper().serviceRunning(context!!)) {
            val intent = Intent(context!!.getString(R.string.scene_appchange_action))
            intent.putExtra("app", app)
            intent.putExtra("mode", mode)
            context!!.sendBroadcast(intent)
        }
    }

    /**
     * 重启辅助服务
     */
    private fun reStartService() {
        if (AccessibleServiceHelper().serviceRunning(context!!)) {
            context!!.sendBroadcast(Intent(context!!.getString(R.string.scene_change_action)))
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun bindSPF(checkBox: Switch, spf: SharedPreferences, prop: String, defValue: Boolean = false, restartService: Boolean = false) {
        checkBox.isChecked = spf.getBoolean(prop, defValue)
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            spf.edit().putBoolean(prop, isChecked).commit()
        }
        if (restartService) {
            reStartService()
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun bindSPF(checkBox: CheckBox, spf: SharedPreferences, prop: String, defValue: Boolean = false, restartService: Boolean = false) {
        checkBox.isChecked = spf.getBoolean(prop, defValue)
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            spf.edit().putBoolean(prop, isChecked).commit()
        }
        if (restartService) {
            reStartService()
        }
    }

    private class ModeOnItemSelectedListener(private var globalSPF: SharedPreferences, private var runnable: Runnable) : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
        }

        @SuppressLint("ApplySharedPref")
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            var mode = ModeSwitcher.DEFAULT
            when (position) {
                0 -> mode = ModeSwitcher.POWERSAVE
                1 -> mode = ModeSwitcher.BALANCE
                2 -> mode = ModeSwitcher.PERFORMANCE
                3 -> mode = ModeSwitcher.FAST
                4 -> mode = ModeSwitcher.IGONED
            }
            if (globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, "") != mode) {
                globalSPF.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, mode).commit()
                runnable.run()
            }
        }
    }

    private fun initDefaultConfig() {
        for (item in resources.getStringArray(R.array.powercfg_igoned)) {
            editor.putString(item, ModeSwitcher.IGONED)
        }
        for (item in resources.getStringArray(R.array.powercfg_fast)) {
            editor.putString(item, ModeSwitcher.FAST)
        }
        for (item in resources.getStringArray(R.array.powercfg_game)) {
            editor.putString(item, ModeSwitcher.PERFORMANCE)
        }
        editor.commit()
    }

    private fun sortAppList(list: ArrayList<Appinfo>): ArrayList<Appinfo> {
        list.sortWith(Comparator { l, r ->
            try {
                val les = l.enabledState.toString()
                val res = r.enabledState.toString()
                when {
                    les < res -> -1
                    les > res -> 1
                    else -> {
                        val lp = l.packageName.toString()
                        val rp = r.packageName.toString()
                        when {
                            lp < rp -> -1
                            lp > rp -> 1
                            else -> 0
                        }
                    }
                }
            } catch (ex: Exception) {
                0
            }
        })
        return list
    }

    private fun setListData(dl: ArrayList<Appinfo>?, lv: OverScrollListView) {
        myHandler.post {
            lv.adapter = SceneModeAdapter(context!!, dl!!)
            processBarDialog.hideDialog()
        }
    }

    private var onLoading = false
    @SuppressLint("ApplySharedPref")
    private fun loadList(foreceReload: Boolean = false) {
        if (this.isDetached) {
            return
        }
        if (onLoading) {
            return
        }
        processBarDialog.showDialog()

        Thread(Runnable {
            onLoading = true
            if (foreceReload || installedList == null || installedList!!.size == 0) {
                installedList = ArrayList()/*在数组中存放数据*/
                installedList = applistHelper.getAll()
            }
            if (config_search_box == null) {
                myHandler.post {
                    processBarDialog.hideDialog()
                }
                return@Runnable
            }
            val keyword = config_search_box.text.toString().toLowerCase()
            val search = keyword.isNotEmpty()
            var filterMode = ""
            var filterAppType = ""
            when (configlist_type.selectedItemPosition) {
                0 -> filterAppType = "/data"
                1 -> filterAppType = "/system"
                2 -> filterAppType = "*"
            }
            when (configlist_modes.selectedItemPosition) {
                0 -> filterMode = "*"
                1 -> filterMode = ModeSwitcher.POWERSAVE
                2 -> filterMode = ModeSwitcher.BALANCE
                3 -> filterMode = ModeSwitcher.PERFORMANCE
                4 -> filterMode = ModeSwitcher.FAST
                5 -> filterMode = ""
                6 -> filterMode = ModeSwitcher.IGONED
            }
            displayList = ArrayList()
            for (i in installedList!!.indices) {
                val item = installedList!![i]
                setAppRowDesc(item)
                val packageName = item.packageName.toString()
                if (search && !(packageName.toLowerCase().contains(keyword) || item.appName.toString().toLowerCase().contains(keyword))) {
                    continue
                } else {
                    if (filterMode == "*" || filterMode == spfPowercfg.getString(packageName, "")) {
                        if (filterAppType == "*" || item.path.startsWith(filterAppType)) {
                            displayList!!.add(item)
                        }
                    }
                }
            }
            sortAppList(displayList!!)
            myHandler.post {
                processBarDialog.hideDialog()
                setListData(displayList, config_defaultlist)
            }
            onLoading = false
        }).start()
    }

    private fun setAppRowDesc(item: Appinfo) {
        item.selectState = false
        val packageName = item.packageName.toString()
        item.enabledState = spfPowercfg.getString(packageName, "")
        val configInfo = appConfigStore.getAppConfig(packageName)
        item.appConfigInfo = configInfo
        val desc = StringBuilder()
        if (configInfo.aloneLight) {
            desc.append("独立亮度 ")
        }
        if (configInfo.disNotice) {
            desc.append("屏蔽通知  ")
        }
        if (configInfo.disButton) {
            desc.append("屏蔽按键  ")
        }
        if (configInfo.disBackgroundRun) {
            desc.append("阻止后台 ")
        }
        if (aidlConn != null) {
            try {
                val configJson = aidlConn!!.getAppConfig(configInfo.packageName)
                val config = JSONObject(configJson)
                for (key in config.keys()) {
                    when (key) {
                        "dpi" -> {
                            configInfo.dpi = config.getInt(key)
                        }
                        "excludeRecent" -> {
                            configInfo.excludeRecent = config.getBoolean(key)
                        }
                        "smoothScroll" -> {
                            configInfo.smoothScroll = config.getBoolean(key)
                        }
                    }
                }
            } catch (ex: Exception) {

            }
        }
        if (configInfo.dpi > 0) {
            desc.append("DPI:${configInfo.dpi}  ")
        }
        if (configInfo.excludeRecent) {
            desc.append("隐藏后台  ")
        }
        if (configInfo.smoothScroll) {
            desc.append("滚动优化  ")
        }
        item.desc = desc.toString()
    }

    //检查配置脚本是否已经安装
    private fun checkConfig() {
        val support = configInstaller.dynamicSupport(context!!)
        if (support) {
            config_cfg_select.visibility = View.VISIBLE
            config_cfg_select_0.setOnClickListener {
                installConfig(false)
            }
            config_cfg_select_1.setOnClickListener {
                installConfig(true)
            }
        }
        when {
            configInstaller.configInstalled() -> {
                //TODO：检查是否更新
            }
            support -> {
                var i = 0
                DialogHelper.animDialog(AlertDialog.Builder(context)
                        .setTitle(getString(R.string.first_start_select_config))
                        .setCancelable(false)
                        .setSingleChoiceItems(
                                arrayOf(
                                        getString(R.string.conservative),
                                        getString(R.string.radicalness),
                                        getString(R.string.get_online_config),
                                        getString(R.string.choose_local_config),
                                        getString(R.string.skipnow)
                                ), 0, { _, which ->
                            i = which
                        })
                        .setNegativeButton(R.string.btn_confirm, { _, _ ->
                            if (i == 4) {
                                // 跳过配置安装时，关闭动态响应
                                globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, false).apply()
                            } else if (i == 3) {
                                chooseLocalConfig()
                            } else if (i == 2) {
                                getOnlineConfig()
                            } else if (i == 1) {
                                installConfig(true)
                            } else if (i == 0) {
                                installConfig(false)
                            }
                        }))
            }
            else -> {
                DialogHelper.animDialog(AlertDialog.Builder(context)
                        .setTitle(getString(R.string.not_support_config))
                        .setMessage(R.string.not_support_config_desc)
                        .setPositiveButton(getString(R.string.get_online_config), { _, _ ->
                            getOnlineConfig()
                        })
                        .setNegativeButton(getString(R.string.more), { _, _ ->
                            val intent = Intent()
                            //Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                            intent.action = "android.intent.action.VIEW"
                            val content_url = Uri.parse("https://github.com/helloklf/vtools")
                            intent.data = content_url
                            startActivity(intent)
                        }))
            }
        }
    }

    private fun chooseLocalConfig() {
        try {
            val intent = Intent(this.context, ActivityFileSelector::class.java)
            intent.putExtra("extension", "sh")
            startActivityForResult(intent, REQUEST_POWERCFG_FILE)
        } catch (ex: Exception) {
            Toast.makeText(context!!, "启动内置文件选择器失败！", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getOnlineConfig() {
        var i = 0
        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setTitle(getString(R.string.config_online_options))
                .setCancelable(true)
                .setSingleChoiceItems(
                        arrayOf(
                                getString(R.string.online_config_v1),
                                getString(R.string.online_config_v2)
                        ), 0, { _, which ->
                    i = which
                })
                .setNegativeButton(R.string.btn_confirm, { _, _ ->
                    if (i == 0) {
                        getOnlineConfigV1()
                    } else if (i == 1) {
                        getOnlineConfigV2()
                    }
                }))
    }

    private fun getOnlineConfigV1() {
        try {
            val intent = Intent(this.context, ActivityAddinOnline::class.java)
            intent.putExtra("url", "https://github.com/yc9559/cpufreq-interactive-opt/tree/master/vtools-powercfg")
            startActivityForResult(intent, REQUEST_POWERCFG_ONLINE)
        } catch (ex: Exception) {
            Toast.makeText(context!!, "启动在线页面失败！", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getOnlineConfigV2() {
        try {
            val intent = Intent(this.context, ActivityAddinOnline::class.java)
            intent.putExtra("url", "https://github.com/yc9559/wipe-v2/releases")
            startActivityForResult(intent, REQUEST_POWERCFG_ONLINE)
        } catch (ex: Exception) {
            Toast.makeText(context!!, "启动在线页面失败！", Toast.LENGTH_SHORT).show()
        }
    }

    //安装调频文件
    private fun installConfig(useBigCore: Boolean) {
        if (context == null) return

        if (!configInstaller.dynamicSupport(context!!)) {
            Snackbar.make(view!!, R.string.not_support_config, Snackbar.LENGTH_LONG).show()
            return
        }

        configInstaller.installPowerConfig(context!!, "", useBigCore)
        configInstalled()
    }

    private fun configInstalled() {
        if (globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, true)) {
            Snackbar.make(view!!, getString(R.string.config_installed), Snackbar.LENGTH_LONG).show()
            reStartService()
        } else {
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setMessage("配置脚本已安装，是否开启动态响应？")
                    .setPositiveButton(R.string.btn_confirm, { _, _ ->
                        globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, true).apply()
                        dynamic_control.isChecked = true
                        reStartService()
                    })
                    .setNegativeButton(R.string.btn_cancel, { _, _ ->
                    }))
        }
    }

    override fun onDestroy() {
        if (aidlConn != null) {
            context!!.unbindService(conn)
            aidlConn = null
        }
        processBarDialog.hideDialog()
        super.onDestroy()
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentConfig()
            return fragment
        }
    }
}
