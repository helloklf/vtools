package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.omarea.Scene
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.OverScrollListView
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.model.AppInfo
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.SceneConfigStore
import com.omarea.store.SpfConfig
import com.omarea.ui.SceneModeAdapter
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.utils.AppListHelper
import com.omarea.vaddin.IAppConfigAidlInterface
import com.omarea.vtools.R
import com.omarea.vtools.dialogs.DialogAppOrientation
import com.omarea.vtools.dialogs.DialogAppPowerConfig
import kotlinx.android.synthetic.main.activity_app_config2.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList


class ActivityAppConfig2 : ActivityBase() {
    private lateinit var processBarDialog: ProgressBarDialog
    private lateinit var spfPowercfg: SharedPreferences
    private lateinit var globalSPF: SharedPreferences
    private lateinit var applistHelper: AppListHelper
    private var installedList: ArrayList<AppInfo>? = null
    private var displayList: ArrayList<AppInfo>? = null
    private lateinit var sceneConfigStore: SceneConfigStore
    private var aidlConn: IAppConfigAidlInterface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_config2)

        setBackArrow()
        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        this.onViewCreated()
    }
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
            if (context?.packageManager?.getPackageInfo("com.omarea.vaddin", 0) == null) {
                return
            }
        } catch (ex: Exception) {
            return
        }
        if (aidlConn != null) {
            return
        }
        try {
            val intent = Intent()
            //绑定服务端的service
            intent.action = "com.omarea.vaddin.ConfigUpdateService"
            //新版本（5.0后）必须显式intent启动 绑定服务
            intent.setComponent(ComponentName("com.omarea.vaddin", "com.omarea.vaddin.ConfigUpdateService"))
            //绑定的时候服务端自动创建
            if (!bindService(intent, conn, Context.BIND_AUTO_CREATE)) {
                throw Exception("")
            }
        } catch (ex: Exception) {
            Toast.makeText(this.context, "连接到“Scene-高级设定”插件失败，请不要阻止插件自启动！", Toast.LENGTH_LONG).show()
        }
    }

    private lateinit var modeSwitcher: ModeSwitcher

    private fun onViewCreated() {
        modeSwitcher = ModeSwitcher()
        processBarDialog = ProgressBarDialog(this)
        applistHelper = AppListHelper(this)
        spfPowercfg = getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
        globalSPF = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        sceneConfigStore = SceneConfigStore(this.context)

        if (spfPowercfg.all.isEmpty()) {
            initDefaultConfig()
        }


        scene_app_list.setOnItemClickListener { parent, view2, position, _ ->
            try {
                val item = (parent.adapter.getItem(position) as AppInfo)
                val intent = Intent(this.context, ActivityAppDetails::class.java)
                intent.putExtra("app", item.packageName)
                startActivityForResult(intent, REQUEST_APP_CONFIG)
                lastClickRow = view2
            } catch (ex: Exception) {
            }
        }

        // 动态响应检测
        val dynamicControl = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)

        if (dynamicControl) {
            scene_app_list.setOnItemLongClickListener { parent, view, position, id ->
                val item = (parent.adapter.getItem(position) as AppInfo)
                val app = item.packageName.toString()
                DialogAppPowerConfig(this,
                        spfPowercfg.getString(app, ""),
                        object : DialogAppPowerConfig.IResultCallback {
                            override fun onChange(mode: String?) {
                                spfPowercfg.edit().run {
                                    if (mode.isNullOrEmpty()) {
                                        remove(app)
                                    } else {
                                        putString(app, mode)
                                    }
                                }.apply()

                                setAppRowDesc(item)
                                (parent.adapter as SceneModeAdapter).updateRow(position, view)
                                notifyService(app, "" + mode)
                            }
                        }).show()
                true
            }
        } else {
            scene_app_list.setOnItemLongClickListener { _, _, _, _ ->
                DialogHelper.helpInfo(this, "", "请先回到功能列表，进入 [性能配置] 功能，开启 [动态响应] 功能")
                true
            }
        }

        config_search_box.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                loadList()
                return@setOnEditorActionListener true
            }
            false
        }

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
    }

    private val REQUEST_APP_CONFIG = 0
    private var lastClickRow: View? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_APP_CONFIG && data != null && displayList != null) {
            try {
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    val adapter = (scene_app_list.adapter as SceneModeAdapter)
                    var index = -1
                    val packageName = data.extras!!.getString("app")
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
                    (scene_app_list.adapter as SceneModeAdapter?)?.run {
                        updateRow(index, lastClickRow!!)
                    }
                    //loadList(false)
                }
            } catch (ex: Exception) {
                Log.e("update-list", "" + ex.message)
            }
        }
    }

    // 通知辅助服务配置变化
    private fun notifyService(app: String, mode: String) {
        if (AccessibleServiceHelper().serviceRunning(this)) {
            val intent = Intent(getString(R.string.scene_appchange_action))
            intent.putExtra("app", app)
            intent.putExtra("mode", mode)
            sendBroadcast(intent)
        }
    }

    private fun bindSPF(checkBox: CompoundButton, spf: SharedPreferences, prop: String, defValue: Boolean = false) {
        checkBox.isChecked = spf.getBoolean(prop, defValue)
        checkBox.setOnClickListener { view ->
            spf.edit().putBoolean(prop, (view as CompoundButton).isChecked).apply()
            if (AccessibleServiceHelper().serviceRunning(this)) {
                sendBroadcast(Intent(getString(R.string.scene_service_config_change_action)))
            }
        }
    }

    private fun initDefaultConfig() {
        for (item in resources.getStringArray(R.array.powercfg_igoned)) {
            spfPowercfg.edit().putString(item, ModeSwitcher.IGONED).apply()
        }
        for (item in resources.getStringArray(R.array.powercfg_fast)) {
            spfPowercfg.edit().putString(item, ModeSwitcher.FAST).apply()
        }
        for (item in resources.getStringArray(R.array.powercfg_game)) {
            spfPowercfg.edit().putString(item, ModeSwitcher.PERFORMANCE).apply()
        }
    }

    private fun sortAppList(list: ArrayList<AppInfo>): ArrayList<AppInfo> {
        list.sortWith { l, r ->
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
        }
        return list
    }

    private fun setListData(dl: ArrayList<AppInfo>?, lv: OverScrollListView) {
        Scene.post {
            lv.adapter = SceneModeAdapter(
                    this,
                    dl!!,
                    globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, ModeSwitcher.DEFAULT)!!
            )
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
            if (config_search_box == null) {
                Scene.post {
                    processBarDialog.hideDialog()
                }
                return@Runnable
            }
            val keyword = config_search_box.text.toString().toLowerCase(Locale.getDefault())
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
                if (search && !(packageName.toLowerCase(Locale.getDefault()).contains(keyword) || item.appName.toString().toLowerCase(Locale.getDefault()).contains(keyword))) {
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
            Scene.post {
                processBarDialog.hideDialog()
                setListData(displayList, scene_app_list)
            }
            onLoading = false
        }).start()
    }

    private fun setAppRowDesc(item: AppInfo) {
        item.selected = false
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
        if (configInfo.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            DialogAppOrientation.Transform(this).getName(configInfo.screenOrientation).run {
                if (isNotEmpty()) {
                    desc.append(this)
                    desc.append("  ")
                }
            }
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
            unbindService(conn)
            aidlConn = null
        }
        processBarDialog.hideDialog()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        bindService()
        title = getString(R.string.menu_scene_mode)
    }
}
