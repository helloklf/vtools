package com.omarea.vtools.fragments

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
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
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.BatteryHistoryStore
import com.omarea.store.SceneConfigStore
import com.omarea.store.SpfConfig
import com.omarea.ui.SceneModeAdapter
import com.omarea.ui.SearchTextWatcher
import com.omarea.ui.TabIconHelper
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.utils.AppListHelper
import com.omarea.vaddin.IAppConfigAidlInterface
import com.omarea.vtools.R
import com.omarea.vtools.activities.ActivityAdvSettings
import com.omarea.vtools.activities.ActivityAppDetails
import kotlinx.android.synthetic.main.fragment_app_config.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class FragmentAppConfig : androidx.fragment.app.Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog
    private lateinit var spfPowercfg: SharedPreferences
    private lateinit var globalSPF: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var applistHelper: AppListHelper
    internal val myHandler: Handler = Handler()
    private var installedList: ArrayList<Appinfo>? = null
    private var displayList: ArrayList<Appinfo>? = null
    private var packageManager: PackageManager? = null
    private lateinit var sceneConfigStore: SceneConfigStore
    private var firstMode = ModeSwitcher.DEFAULT
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_app_config, container, false)
    private lateinit var modeSwitcher: ModeSwitcher

    override fun onResume() {
        super.onResume()
        activity!!.title = getString(R.string.menu_scene_mode)

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
        sceneConfigStore = SceneConfigStore(this.context)

        if (spfPowercfg.all.isEmpty()) {
            initDefaultConfig()
        }

        btn_config_service_not_active.setOnClickListener {
            startService()
        }

        val tabIconHelper = TabIconHelper(configlist_tabhost, this.activity!!)
        configlist_tabhost.setup()

        tabIconHelper.newTabSpec("APP场景", context!!.getDrawable(R.drawable.tab_app)!!, R.id.configlist_tab0)
        tabIconHelper.newTabSpec("设置", context!!.getDrawable(R.drawable.tab_settings)!!, R.id.configlist_tab5)
        configlist_tabhost.currentTab = 0
        configlist_tabhost.setOnTabChangedListener { tabId ->
            tabIconHelper.updateHighlight()
        }

        battery_monitor.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, false)
        battery_monitor.setOnClickListener {
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, (it as Switch).isChecked).commit()
            BatteryHistoryStore(context!!).clearData()
            Toast.makeText(context!!, "已自动清空耗电统计数据，以便于重新采集！", Toast.LENGTH_SHORT).show()
            reStartService()
        }
        scene_app_list.setOnItemClickListener { parent, view2, position, id ->
            try {
                val item = (parent.adapter.getItem(position) as Appinfo)
                val intent = Intent(this.context, ActivityAppDetails::class.java)
                intent.putExtra("app", item.packageName)
                startActivityForResult(intent, REQUEST_APP_CONFIG)
                lastClickRow = view2
            } catch (ex: Exception) {
            }
        }

        // 动态响应检测
        val dynamicControl = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)
        first_mode.isEnabled = dynamicControl
        if (dynamicControl) {
            dynamic_control_opts.visibility = View.VISIBLE
            scene_app_list.setOnItemLongClickListener { parent, view, position, id ->
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
                                originIndex
                        ) { dialog, which ->
                            currentMode = which
                        }
                        .setPositiveButton(R.string.btn_confirm) { _, _ ->
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
                                (scene_app_list.adapter as SceneModeAdapter).updateRow(position, view)
                                notifyService(item.packageName.toString(), modeName)
                            }
                        }
                        .setNeutralButton(R.string.btn_cancel, null))
                true
            }
        } else {
            dynamic_control_opts.visibility = View.GONE
            scene_app_list.setOnItemLongClickListener { _, _, _, _ ->
                DialogHelper.helpInfo(context!!, "", "请先回到功能列表，进入 [性能配置] 功能，开启 [性能调节] 功能")
                true
            }
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

        bindSPF(dynamic_lock_mode, globalSPF, SpfConfig.GLOBAL_SPF_LOCK_MODE, false)
        bindSPF(settings_autoinstall, globalSPF, SpfConfig.GLOBAL_SPF_AUTO_INSTALL, false)

        config_adv.setOnClickListener {
            try {
                val intent = Intent(this.context, ActivityAdvSettings::class.java)
                startActivity(intent)
            } catch (ex: Exception) {
            }
        }
        scene_notify.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, true)
        scene_notify.setOnClickListener {
            val checked = (it as Switch).isChecked
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, checked).commit()
            reStartService()
        }
    }

    private val REQUEST_APP_CONFIG = 0
    private var lastClickRow: View? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (this.isDetached) {
            return
        }
        if (requestCode == REQUEST_APP_CONFIG && data != null && displayList != null) {
            try {
                if (resultCode == RESULT_OK) {
                    val adapter = (scene_app_list.adapter as SceneModeAdapter)
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
                    (scene_app_list.adapter as SceneModeAdapter).updateRow(index, lastClickRow!!)
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
            if (globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, ModeSwitcher.DEFAULT) != mode) {
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
                setListData(displayList, scene_app_list)
            }
            onLoading = false
        }).start()
    }

    private fun setAppRowDesc(item: Appinfo) {
        item.selectState = false
        val packageName = item.packageName.toString()
        item.enabledState = spfPowercfg.getString(packageName, "")
        val configInfo = sceneConfigStore.getAppConfig(packageName)
        item.sceneConfigInfo = configInfo
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
        if (configInfo.freeze) {
            desc.append("自动冻结  ")
        }
        if (configInfo.gpsOn) {
            desc.append("打开GPS  ")
        }
        if (configInfo.dpi >= 96) {
            desc.append("DIP " + configInfo.dpi + "  ")
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

    override fun onDestroy() {
        if (aidlConn != null) {
            context!!.unbindService(conn)
            aidlConn = null
        }
        processBarDialog.hideDialog()
        super.onDestroy()
    }

    companion object {
        fun createPage(): androidx.fragment.app.Fragment {
            val fragment = FragmentAppConfig()
            return fragment
        }
    }
}
