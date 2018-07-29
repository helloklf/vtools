package com.omarea.vtools

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
import com.omarea.shared.*
import com.omarea.shared.model.Appinfo
import com.omarea.shell.KeepShellSync
import com.omarea.shell.Platform
import com.omarea.ui.OverScrollListView
import com.omarea.ui.ProgressBarDialog
import com.omarea.ui.SceneModeAdapter
import com.omarea.ui.SearchTextWatcher
import com.omarea.vaddin.IAppConfigAidlInterface
import kotlinx.android.synthetic.main.layout_config.*
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
    private var firstMode = "balance"
    private var vAddinsInstalled = false
    private var aidlConn: IAppConfigAidlInterface? = null

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.layout_config, container, false)
    private lateinit var modeList: ModeList

    override fun onResume() {
        super.onResume()
        bindService()
        val serviceState = AccessibleServiceHelper().serviceIsRunning(context!!)
        btn_config_service_not_active.visibility = if (serviceState) View.GONE else View.VISIBLE
    }

    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (packageManager == null) {
            packageManager = context!!.packageManager
        }

        modeList = ModeList(context!!)
        processBarDialog = ProgressBarDialog(context!!)
        applistHelper = AppListHelper(context!!)
        spfPowercfg = context!!.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        editor = spfPowercfg.edit()
        firstMode = globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, "balance")
        appConfigStore = AppConfigStore(this.context)

        if (spfPowercfg.all.isEmpty()) {
            initDefaultConfig()
        }
        checkConfig()

        btn_config_service_not_active.setOnClickListener {
            val dialog = ProgressBarDialog(context!!)
            dialog.showDialog("尝试使用ROOT权限开启服务...")
            Thread(Runnable {
                if (!AccessibleServiceHelper().startServiceUseRoot(context!!)) {
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
                        btn_config_service_not_active.visibility = if (AccessibleServiceHelper().serviceIsRunning(context!!)) View.GONE else View.VISIBLE
                    }
                }
            }).start()
        }

        configlist_tabhost.setup()

        configlist_tabhost.addTab(configlist_tabhost.newTabSpec("def_tab").setContent(R.id.configlist_tab0).setIndicator("应用场景"))
        configlist_tabhost.addTab(configlist_tabhost.newTabSpec("tab_3").setContent(R.id.blacklist_tab3).setIndicator(context!!.getString(R.string.autobooster_tab_system_scene)))
        configlist_tabhost.addTab(configlist_tabhost.newTabSpec("confg_tab").setContent(R.id.configlist_tab5).setIndicator("设置"))
        configlist_tabhost.currentTab = 0

        accu_switch.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_ACCU_SWITCH, false)
        accu_switch.setOnClickListener {
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_ACCU_SWITCH, (it as Switch).isChecked).commit()
        }
        battery_monitor.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, false)
        battery_monitor.setOnClickListener {
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, (it as Switch).isChecked).commit()
        }
        config_defaultlist.setOnItemClickListener { parent, view, position, id ->
            try {
                val item = (parent.adapter.getItem(position) as Appinfo)
                val intent = Intent(this.context, AppDetailsActivity::class.java)
                intent.putExtra("app", item.packageName)
                startActivityForResult(intent, REQUEST_APP_CONFIG)
                lastClickRow = view
            } catch (ex: Exception) {
            }
        }
        config_defaultlist.setOnItemLongClickListener { parent, view, position, id ->
            val item = (parent.adapter.getItem(position) as Appinfo)
            var originIndex = 0
            when (spfPowercfg.getString(item.packageName.toString(), firstMode)) {
                ModeList.POWERSAVE -> originIndex = 0
                ModeList.BALANCE -> originIndex = 1
                ModeList.PERFORMANCE -> originIndex = 2
                ModeList.FAST -> originIndex = 3
                else -> originIndex = 4
            }
            var currentMode =originIndex
            AlertDialog.Builder(context)
                    .setTitle(item.appName.toString())
                    .setSingleChoiceItems(arrayOf("省电模式（阅读）", "均衡模式（日常）", "性能模式（游戏）", "极速模式（跑分）", "跟随默认模式"), originIndex, DialogInterface.OnClickListener { dialog, which ->
                        currentMode = which
                    })
                    .setPositiveButton(R.string.btn_confirm, {
                        _, _ ->
                        if (currentMode != originIndex) {
                            when (currentMode) {
                                0 -> spfPowercfg.edit().putString(item.packageName.toString(), ModeList.POWERSAVE).commit()
                                1 -> spfPowercfg.edit().putString(item.packageName.toString(), ModeList.BALANCE).commit()
                                2 -> spfPowercfg.edit().putString(item.packageName.toString(), ModeList.PERFORMANCE).commit()
                                3 -> spfPowercfg.edit().putString(item.packageName.toString(), ModeList.FAST).commit()
                                4 -> spfPowercfg.edit().remove(item.packageName.toString()).commit()
                            }

                            setAppRowDesc(item)
                            (config_defaultlist.adapter as SceneModeAdapter).updateRow(position, view)
                        }
                    })
                    .setNeutralButton(R.string.btn_cancel, null)
                    .create()
                    .show()
            true
        }

        config_search_box.addTextChangedListener(SearchTextWatcher(Runnable {
            loadList()
        }))
        configlist_modes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadList()
            }
        }
        configlist_type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadList()
            }
        }

        loadList()

        val modeValue = globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, "balance")
        when (modeValue) {
            "powersave" -> first_mode.setSelection(0)
            "balance" -> first_mode.setSelection(1)
            "performance" -> first_mode.setSelection(2)
            "fast" -> first_mode.setSelection(3)
            "igoned" -> first_mode.setSelection(4)
        }
        first_mode.onItemSelectedListener = ModeOnItemSelectedListener(globalSPF, Runnable {
            loadList()
        })


        val spfAutoConfig = context!!.getSharedPreferences(SpfConfig.BOOSTER_SPF_CFG_SPF, Context.MODE_PRIVATE)

        bindSPF(auto_switch_network_on_wifi, spfAutoConfig, SpfConfig.WIFI + SpfConfig.ON, false)
        bindSPF(auto_switch_network_on_data, spfAutoConfig, SpfConfig.DATA + SpfConfig.ON, false)
        bindSPF(auto_switch_network_on_nfc, spfAutoConfig, SpfConfig.NFC + SpfConfig.ON, false)
        bindSPF(auto_switch_network_on_gps, spfAutoConfig, SpfConfig.GPS + SpfConfig.ON, false)

        bindSPF(auto_switch_network_off_wifi, spfAutoConfig, SpfConfig.WIFI + SpfConfig.OFF, false)
        bindSPF(auto_switch_network_off_data, spfAutoConfig, SpfConfig.DATA + SpfConfig.OFF, false)
        bindSPF(auto_switch_network_off_nfc, spfAutoConfig, SpfConfig.NFC + SpfConfig.OFF, false)
        bindSPF(auto_switch_network_off_gps, spfAutoConfig, SpfConfig.GPS + SpfConfig.OFF, false)
        config_customer_powercfg.setOnClickListener {
            try {
                val intent = Intent(this.context, ActivityFileSelector::class.java)
                intent.putExtra("extension", "sh")
                startActivityForResult(intent, REQUEST_POWERCFG_FILE)
            } catch (ex: Exception) {
                Toast.makeText(context!!, "启动内置文件选择器失败！", Toast.LENGTH_SHORT).show()
            }
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
    }

    private val REQUEST_POWERCFG_FILE = 1
    private val REQUEST_POWERCFG_ONLINE = 2
    private val REQUEST_APP_CONFIG = 0
    private var lastClickRow: View? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_POWERCFG_FILE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.extras.containsKey("file")) {
                val path = data.extras.getString("file")
                val file = File(path)
                if (file.exists()) {
                    if (file.length() > 200 * 1024) {
                        Toast.makeText(context, "这个文件也太大了，配置脚本大小不能超过200KB！", Toast.LENGTH_LONG).show()
                        return
                    }
                    val lines = file.readLines(Charset.defaultCharset())
                    val configStar = if (lines.size > 0) lines[0] else ""
                    if (configStar.startsWith("#!/") && configStar.endsWith("sh")) {
                        val cmds = StringBuilder("cp '$path' ${Consts.POWER_CFG_PATH}\n")
                        cmds.append("chmod 0755 ${Consts.POWER_CFG_PATH}\n\n")
                        cmds.append("if [[ -f ${Consts.POWER_CFG_PATH} ]]; then \n")
                        cmds.append("chmod 0775 ${Consts.POWER_CFG_PATH};")
                        cmds.append("busybox sed -i 's/^M//g' ${Consts.POWER_CFG_PATH};")
                        cmds.append("fi;")
                        //cmds.append("if [[ -f ${Consts.POWER_CFG_BASE} ]]; then \n")
                        //  cmds.append("chmod 0775 ${Consts.POWER_CFG_BASE};")
                        //  cmds.append("busybox sed -i 's/^M//g' ${Consts.POWER_CFG_BASE};")
                        //cmds.append("fi;")
                        if (KeepShellSync.doCmdSync(cmds.toString()) != "error") {
                            Toast.makeText(context, "动态响应配置脚本已安装！", Toast.LENGTH_SHORT).show()
                            reStartService()
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
                reStartService()
            }
        } else if (requestCode == REQUEST_APP_CONFIG && data != null && displayList != null) {
            try {
                if (resultCode == RESULT_OK) {
                    val adapter = (config_defaultlist.adapter as SceneModeAdapter)
                    var index = -1
                    val packageName = data.extras.getString("app")
                    for (i in 0..displayList!!.size - 1) {
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
                Log.e("update-list", ex.message)
            }
        }
    }

    /**
     * 重启辅助服务
     */
    private fun reStartService() {
        if (AccessibleServiceHelper().serviceIsRunning(context!!)) {
            AlertDialog.Builder(context!!)
                    .setTitle("需要重启辅助服务")
                    .setMessage("请手动重启辅助服务，使配置脚本生效！")
                    .setPositiveButton(R.string.btn_confirm, { _, _ ->

                    })
                    .create()
                    .show()
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun bindSPF(checkBox: Switch, spf: SharedPreferences, prop: String, defValue: Boolean = false) {
        checkBox.isChecked = spf.getBoolean(prop, defValue)
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            spf.edit().putBoolean(prop, isChecked).commit()
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun bindSPF(checkBox: CheckBox, spf: SharedPreferences, prop: String, defValue: Boolean = false) {
        checkBox.isChecked = spf.getBoolean(prop, defValue)
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            spf.edit().putBoolean(prop, isChecked).commit()
        }
    }

    private class ModeOnItemSelectedListener(private var globalSPF: SharedPreferences, private var runnable: Runnable) : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
        }

        @SuppressLint("ApplySharedPref")
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            var mode = "balance"
            when (position) {
                0 -> mode = "powersave"
                1 -> mode = "balance"
                2 -> mode = "performance"
                3 -> mode = "fast"
                4 -> mode = "igoned"
            }
            globalSPF.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, mode).commit()
            runnable.run()
        }

    }

    private fun initDefaultConfig() {
        for (item in resources.getStringArray(R.array.powercfg_igoned)) {
            editor.putString(item, "igoned")
        }
        for (item in resources.getStringArray(R.array.powercfg_fast)) {
            editor.putString(item, "fast")
        }
        for (item in resources.getStringArray(R.array.powercfg_game)) {
            editor.putString(item, "game")
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
                1 -> filterMode = ModeList.POWERSAVE
                2 -> filterMode = ModeList.BALANCE
                3 -> filterMode = ModeList.PERFORMANCE
                4 -> filterMode = ModeList.FAST
                5 -> filterMode = ModeList.IGONED
            }
            displayList = ArrayList()
            for (i in installedList!!.indices) {
                val item = installedList!![i]
                setAppRowDesc(item)
                val packageName = item.packageName.toString()
                if (search && !(packageName.toLowerCase().contains(keyword) || item.appName.toString().toLowerCase().contains(keyword))) {
                    continue
                } else {
                    if (filterMode == "*" || filterMode == spfPowercfg.getString(packageName, firstMode)) {
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
        if (configInfo.aloneLight && configInfo.aloneLightValue > 0) {
            desc.append("亮度：${configInfo.aloneLightValue}  ")
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
        val support = Platform().dynamicSupport(context!!)
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
            File(Consts.POWER_CFG_PATH).exists() -> {
                //TODO：检查是否更新
            }
            support -> {
                var i = 0
                AlertDialog.Builder(context)
                        .setTitle(getString(R.string.first_start_select_config))
                        .setCancelable(false)
                        .setSingleChoiceItems(arrayOf(getString(R.string.conservative), getString(R.string.radicalness), getString(R.string.get_online_config)), 0, { _, which ->
                            i = which
                        })
                        .setNegativeButton(R.string.btn_confirm, { _, _ ->
                            if (i > 1) {
                                getOnlineConfig()
                                return@setNegativeButton
                            }
                            installConfig(i == 1)
                        }).create().show()
            }
            else ->
                AlertDialog.Builder(context)
                        .setTitle(getString(R.string.not_support_config))
                        .setMessage(String.format(getString(R.string.not_support_config_desc), Consts.POWER_CFG_PATH))
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
                        })
                        .create()
                        .show()
        }
    }


    private fun getOnlineConfig() {
        try {
            val intent = Intent(this.context, ActivityAddinOnline::class.java)
            intent.putExtra("url", "https://github.com/yc9559/cpufreq-interactive-opt/tree/master/vtools-powercfg")
            startActivityForResult(intent, REQUEST_POWERCFG_ONLINE)
        } catch (ex: Exception) {
            Toast.makeText(context!!, "启动在线页面失败！", Toast.LENGTH_SHORT).show()
        }
    }

    //安装调频文件
    private fun installConfig(useBigCore: Boolean) {
        if (context == null) return

        if (!Platform().dynamicSupport(context!!)) {
            Snackbar.make(view!!, R.string.not_support_config, Snackbar.LENGTH_LONG).show()
            return
        }

        try {
            ConfigInstaller().installPowerConfig(context!!, "", useBigCore)
            Snackbar.make(view!!, getString(R.string.config_installed), Snackbar.LENGTH_LONG).show()
            reStartService()
        } catch (ex: Exception) {
            Snackbar.make(view!!, getString(R.string.config_install_fail) + ex.message, Snackbar.LENGTH_LONG).show()
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
