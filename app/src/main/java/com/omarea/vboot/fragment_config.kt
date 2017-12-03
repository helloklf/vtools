package com.omarea.vboot

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView.OnItemClickListener
import android.widget.CheckBox
import android.widget.ListView
import com.omarea.shared.*
import com.omarea.shell.DynamicConfig
import com.omarea.shell.Platform
import com.omarea.ui.list_adapter
import com.omarea.units.AppListHelper
import kotlinx.android.synthetic.main.layout_config.*
import java.io.File
import java.util.*


class fragment_config : Fragment() {
    internal var cmdshellTools: cmd_shellTools? = null
    internal var thisview: main? = null

    lateinit internal var spfPowercfg: SharedPreferences
    lateinit internal var editor: SharedPreferences.Editor
    internal var HasSystemApp = false
    lateinit var applistHelper: AppListHelper

    internal val myHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }

    private var defaultList: ArrayList<HashMap<String, Any>>? = null
    private var gameList: ArrayList<HashMap<String, Any>>? = null
    private var powersaveList: ArrayList<HashMap<String, Any>>? = null
    private var fastList: ArrayList<HashMap<String, Any>>? = null
    private var ignoredList: ArrayList<HashMap<String, Any>>? = null
    private var installedList: ArrayList<HashMap<String, Any>>? = null

    internal var packageManager: PackageManager? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.layout_config, container, false)
    }

    override fun onResume() {
        val serviceState = ServiceHelper.serviceIsRunning(context)
        btn_config_service_not_active.visibility = if (serviceState) View.GONE else View.VISIBLE
        btn_config_dynamicservice_not_active.visibility = if (!context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, false)) View.VISIBLE else View.GONE

        super.onResume()
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        applistHelper = AppListHelper(context)
        spfPowercfg = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
        editor = spfPowercfg.edit()

        if (spfPowercfg.all.size == 0) {
            initDefaultConfig()
        }
        checkConfig();

        btn_config_service_not_active.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        btn_config_dynamicservice_not_active.setOnClickListener {
            val intent = Intent(thisview, accessibility_settings::class.java)
            startActivity(intent)
        }

        configlist_tabhost.setup()

        configlist_tabhost.addTab(configlist_tabhost.newTabSpec("def_tab").setContent(R.id.configlist_tab0).setIndicator("均衡"))
        configlist_tabhost.addTab(configlist_tabhost.newTabSpec("game_tab").setContent(R.id.configlist_tab1).setIndicator("性能"))
        configlist_tabhost.addTab(configlist_tabhost.newTabSpec("power_tab").setContent(R.id.configlist_tab2).setIndicator("省电"))
        configlist_tabhost.addTab(configlist_tabhost.newTabSpec("fast_tab").setContent(R.id.configlist_tab3).setIndicator("极速"))
        configlist_tabhost.addTab(configlist_tabhost.newTabSpec("fast_tab").setContent(R.id.configlist_tab4).setIndicator("忽略"))
        configlist_tabhost.addTab(configlist_tabhost.newTabSpec("confg_tab").setContent(R.id.configlist_tab5).setIndicator("设置"))
        configlist_tabhost.currentTab = 0
        configlist_tabhost.setOnTabChangedListener { config_addtodefaultlist.visibility = if (configlist_tabhost.currentTab > 4) View.GONE else View.VISIBLE }

        config_addtodefaultlist.setOnClickListener {
            when (configlist_tabhost.currentTab) {
                0 -> {
                    val builder = AlertDialog.Builder(thisview)
                    val items = arrayOf("添加选中到 -> 性能模式", "添加选中到 -> 省电模式", "添加选中到 -> 极速模式", "添加选中到 -> 忽略列表")
                    val configses = arrayOf(Configs.Game, Configs.PowerSave, Configs.Fast, Configs.Ignored)
                    builder.setItems(items) { dialog, which ->
                        val listadapter = config_defaultlist.adapter as list_adapter
                        AddToList(defaultList!!, listadapter.states, configses[which])
                    }
                    builder.setIcon(R.drawable.ic_menu_profile).setTitle("设置配置模式").create().show()
                }
                1 -> {
                    val builder = AlertDialog.Builder(thisview)
                    val items = arrayOf("添加选中到 -> 均衡模式", "添加选中到 -> 省电模式", "添加选中到 -> 极速模式", "添加选中到 -> 忽略列表")
                    val configses = arrayOf(Configs.Default, Configs.PowerSave, Configs.Fast, Configs.Ignored)
                    builder.setItems(items) { dialog, which ->
                        val listadapter = config_gamelist.adapter as list_adapter
                        AddToList(gameList!!, listadapter.states, configses[which])
                    }
                    builder.setIcon(R.drawable.ic_menu_profile).setTitle("设置配置模式").create().show()
                }
                2 -> {
                    val builder = AlertDialog.Builder(thisview)
                    val items = arrayOf("添加选中到 -> 均衡模式", "添加选中到 -> 性能模式", "添加选中到 -> 极速模式", "添加选中到 -> 忽略列表")
                    val configses = arrayOf(Configs.Default, Configs.Game, Configs.Fast, Configs.Ignored)
                    builder.setItems(items) { dialog, which ->
                        val listadapter = config_powersavelist.adapter as list_adapter
                        AddToList(powersaveList!!, listadapter.states, configses[which])
                    }
                    builder.setIcon(R.drawable.ic_menu_profile).setTitle("设置配置模式").create().show()
                }
                3 -> {
                    val builder = AlertDialog.Builder(thisview)
                    val items = arrayOf("添加选中到 -> 均衡模式", "添加选中到 -> 性能模式", "添加选中到 -> 省电模式", "添加选中到 -> 忽略列表")
                    val configses = arrayOf(Configs.Default, Configs.Game, Configs.PowerSave, Configs.Ignored)
                    builder.setItems(items) { dialog, which ->
                        val listadapter = config_fastlist.adapter as list_adapter
                        AddToList(fastList!!, listadapter.states, configses[which])
                    }
                    builder.setIcon(R.drawable.ic_menu_profile).setTitle("设置配置模式").create().show()
                }
                4 -> {
                    val builder = AlertDialog.Builder(thisview)
                    val items = arrayOf("添加选中到 -> 均衡模式", "添加选中到 -> 性能模式", "添加选中到 -> 省电模式", "添加选中到 -> 极速模式")
                    val configses = arrayOf(Configs.Default, Configs.Game, Configs.PowerSave, Configs.Fast)
                    builder.setItems(items) { dialog, which ->
                        val listadapter = config_ignoredlist.adapter as list_adapter
                        AddToList(ignoredList!!, listadapter.states, configses[which])
                    }
                    builder.setIcon(R.drawable.ic_menu_profile).setTitle("设置配置模式").create().show()
                }
            }
        }

        config_showSystemApp.isChecked = HasSystemApp
        config_showSystemApp.setOnClickListener {
            HasSystemApp = config_showSystemApp.isChecked
            LoadList(true)
        }

        config_defaultlist.onItemClickListener = OnItemClickListener { parent, current, position, id ->
            val select_state = current.findViewById(R.id.select_state) as CheckBox
            if (select_state != null)
                select_state.isChecked = !select_state.isChecked
            defaultList!![position].put("select_state", !select_state.isChecked)
        }

        config_gamelist.onItemClickListener = OnItemClickListener { parent, current, position, id ->
            val select_state = current.findViewById(R.id.select_state) as CheckBox
            if (select_state != null)
                select_state.isChecked = !select_state.isChecked
            gameList!![position].put("select_state", !select_state.isChecked)
        }

        config_powersavelist.onItemClickListener = OnItemClickListener { parent, current, position, id ->
            val select_state = current.findViewById(R.id.select_state) as CheckBox
            if (select_state != null)
                select_state.isChecked = !select_state.isChecked
            powersaveList!![position].put("select_state", !select_state.isChecked)
        }

        config_fastlist.onItemClickListener = OnItemClickListener { parent, current, position, id ->
            val select_state = current.findViewById(R.id.select_state) as CheckBox
            if (select_state != null)
                select_state.isChecked = !select_state.isChecked
            fastList!![position].put("select_state", !select_state.isChecked)
        }

        config_ignoredlist.onItemClickListener = OnItemClickListener { parent, current, position, id ->
            val select_state = current.findViewById(R.id.select_state) as CheckBox
            if (select_state != null)
                select_state.isChecked = !select_state.isChecked
            ignoredList!![position].put("select_state", !select_state.isChecked)
        }

        config_search_box.setOnEditorActionListener({
            tv, actionId, key ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                LoadList()
            }
            false
        })
        LoadList()
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
    private fun sortAppList(list: ArrayList<HashMap<String, Any>>): ArrayList<HashMap<String, Any>> {
        list.sortWith(Comparator { l, r ->
            val les = l["enabled_state"].toString()
            val res = r["enabled_state"].toString()
            when {
                les < res -> -1
                les > res -> 1
                else -> {
                    val lp = l["packageName"].toString()
                    val rp = r["packageName"].toString()
                    when {
                        lp < rp -> -1
                        lp > rp -> 1
                        else -> 0
                    }
                }
            }
        })
        return list
    }

    internal fun SetListData(dl: ArrayList<HashMap<String, Any>>?, lv: ListView) {
        myHandler.post {
            lv.adapter = list_adapter(context, dl)
            thisview!!.progressBar.visibility = View.GONE
        }
    }

    internal fun LoadList(foreceReload: Boolean = false) {
        thisview!!.progressBar.visibility = View.VISIBLE
        if (packageManager == null) {
            packageManager = thisview!!.packageManager
        }

        Thread(Runnable {
            if (foreceReload || installedList == null || installedList!!.size == 0) {
                installedList = ArrayList()/*在数组中存放数据*/
                val hasSystemApp = HasSystemApp
                installedList = if (hasSystemApp) applistHelper.getAll() else applistHelper.getUserAppList()
                sortAppList(installedList!!)
            }
            defaultList = ArrayList()
            gameList = ArrayList()
            powersaveList = ArrayList()
            fastList = ArrayList()
            ignoredList = ArrayList()

            val keyword = config_search_box.text.toString()
            val search = keyword.length > 0
            for (i in installedList!!.indices) {
                val item = installedList!![i]
                if (item.containsKey("select_state")) {
                    item.remove("select_state")
                }
                item.put("select_state", false)
                val packageName = item["packageName"].toString()
                if (search && !(packageName.contains(keyword) || item["name"].toString().contains(keyword))) {
                    continue
                }
                val config = spfPowercfg.getString(packageName.toLowerCase(), "default")
                when (config) {
                    "powersave" -> powersaveList!!.add(installedList!![i])
                    "game" -> gameList!!.add(installedList!![i])
                    "fast" -> fastList!!.add(installedList!![i])
                    "igoned" -> ignoredList!!.add(installedList!![i])
                    else -> defaultList!!.add(installedList!![i])
                }
            }
            myHandler.post {
                thisview!!.progressBar.visibility = View.GONE
                SetListData(defaultList, config_defaultlist)
                SetListData(gameList, config_gamelist)
                SetListData(powersaveList, config_powersavelist)
                SetListData(fastList, config_fastlist)
                SetListData(ignoredList, config_ignoredlist)
            }
        }).start()
    }

    //检查配置文件是否已经安装
    fun checkConfig() {
        var support = DynamicConfig().DynamicSupport(context)
        if (support) {
            config_cfg_select.visibility = View.VISIBLE
            config_cfg_select_0.setOnClickListener {
                InstallConfig(false)
            }
            config_cfg_select_1.setOnClickListener {
                InstallConfig(true)
            }
        }
        if (File("/data/powercfg.sh").exists()) {
            //TODO：检查是否更新
        } else if (support) {
            var i = 0;
            AlertDialog.Builder(context)
                    .setTitle("首次使用，先选择配置偏好")
                    .setCancelable(false)
                    .setSingleChoiceItems(arrayOf("保守 - 更加省电", "激进 - 性能优先"), 0, { dialog, which ->
                        i = which
                    })
                    .setNegativeButton("确定", { dialog, which ->
                        InstallConfig(i > 0)
                    }).create().show()
        } else {
            AlertDialog.Builder(context).setTitle("未找到可用的模式配置文件").setMessage("尽管应用没有为您的设备专门适配此功能。但你仍然可以自己创建配置文件（powercfg.sh）并复制打/data/powercfg.sh。详情可以咨询开发者，").setNegativeButton("知道了", { dialog, which ->
            }).create().show()
        }
    }

    //安装调频文件
    internal fun InstallConfig(useBigCore: Boolean) {
        if (context == null) return

        if (!DynamicConfig().DynamicSupport(context!!)) {
            Snackbar.make(view!!, "未找到对应到当前SOC的调频配置文件！", Snackbar.LENGTH_LONG).show()
            return
        }

        try {
            val ass = context!!.assets
            val cpuName = Platform().GetCPUName()
            val cpuNumber = cpuName.replace("msm", "")

            if (useBigCore) {
                AppShared.WriteFile(ass, cpuName + "/init.qcom.post_boot-bigcore.sh", "init.qcom.post_boot.sh")
                AppShared.WriteFile(ass, cpuName + "/powercfg-bigcore.sh", "powercfg.sh")
            } else {
                AppShared.WriteFile(ass, cpuName + "/init.qcom.post_boot-default.sh", "init.qcom.post_boot.sh")
                AppShared.WriteFile(ass, cpuName + "/powercfg-default.sh", "powercfg.sh")
            }

            val cmd = StringBuilder().append(Consts.InstallConfig).append(Consts.ExecuteConfig).append(Consts.ToggleDefaultMode)
                    .toString().replace("cpuNumber", cpuNumber)
            cmdshellTools!!.DoCmdSync(cmd)

            //ToggleConfig(Configs.Default)

            Snackbar.make(view!!, "配置安装成功！", Snackbar.LENGTH_LONG).show()
        } catch (ex: Exception) {
            Snackbar.make(view!!, "安装配置文件失败!\n" + ex.message, Snackbar.LENGTH_LONG).show()
        }
    }

    /**
     * 从当前列表中获取已选中的应用，添加到指定模式
     *
     * @param list     当前列表
     * @param postions 各个序号的选中状态
     * @param config   指定的新模式
     */
    internal fun AddToList(list: ArrayList<HashMap<String, Any>>, postions: HashMap<Int, Boolean>, config: Configs) {
        for (position in postions.keys) {
            if (postions[position] == true) {
                val item = list[position]
                when (config) {
                    fragment_config.Configs.Default -> editor.putString(item["packageName"].toString().toLowerCase(), "default")
                    fragment_config.Configs.Game -> editor.putString(item["packageName"].toString().toLowerCase(), "game")
                    fragment_config.Configs.PowerSave -> editor.putString(item["packageName"].toString().toLowerCase(), "powersave")
                    fragment_config.Configs.Fast -> editor.putString(item["packageName"].toString().toLowerCase(), "fast")
                    fragment_config.Configs.Ignored -> editor.putString(item["packageName"].toString().toLowerCase(), "igoned")
                }
            }
        }
        editor.commit()
        try {
            LoadList()
        } catch (ex: Exception) {
        }
    }

    internal enum class Configs {
        Default,
        Game,
        PowerSave,
        Fast,
        Ignored
    }

    companion object {

        fun Create(thisView: main, cmdshellTools: cmd_shellTools): Fragment {
            val fragment = fragment_config()
            fragment.cmdshellTools = cmdshellTools
            fragment.thisview = thisView
            return fragment
        }
    }
}
