package com.omarea.vboot

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Switch
import com.omarea.shared.*
import com.omarea.shared.model.Appinfo
import com.omarea.shell.Platform
import com.omarea.ui.OverScrollListView
import com.omarea.ui.ProgressBarDialog
import com.omarea.ui.SceneModeAdapter
import com.omarea.ui.SearchTextWatcher
import kotlinx.android.synthetic.main.layout_config.*
import java.io.File
import java.util.*


class FragmentConfig : Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog
    private lateinit var spfPowercfg: SharedPreferences
    private lateinit var globalSPF: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var hasSystemApp = false
    private lateinit var applistHelper: AppListHelper
    internal val myHandler: Handler = Handler()
    private var installedList: ArrayList<Appinfo>? = null
    private var packageManager: PackageManager? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_config, container, false)

    override fun onResume() {
        super.onResume()

        val serviceState = AccessibleServiceHelper().serviceIsRunning(context!!)
        btn_config_service_not_active.visibility = if (serviceState) View.GONE else View.VISIBLE
        btn_config_dynamicservice_not_active.visibility = if (!context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, false)) View.VISIBLE else View.GONE
    }

    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        processBarDialog = ProgressBarDialog(context!!)
        applistHelper = AppListHelper(context!!)
        spfPowercfg = context!!.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        editor = spfPowercfg.edit()

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
        btn_config_dynamicservice_not_active.setOnClickListener {
            val intent = Intent(context, ActivityAccessibilitySettings::class.java)
            startActivity(intent)
        }

        configlist_tabhost.setup()

        configlist_tabhost.addTab(configlist_tabhost.newTabSpec("def_tab").setContent(R.id.configlist_tab0).setIndicator("应用场景"))
        configlist_tabhost.addTab(configlist_tabhost.newTabSpec("confg_tab").setContent(R.id.configlist_tab5).setIndicator("设置"))
        configlist_tabhost.currentTab = 0

        config_showSystemApp.isChecked = hasSystemApp
        config_showSystemApp.setOnClickListener {
            hasSystemApp = config_showSystemApp.isChecked
            loadList(true)
        }
        lock_screen_optimize.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_LOCK_SCREEN_OPTIMIZE, false)
        lock_screen_optimize.setOnClickListener {
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_LOCK_SCREEN_OPTIMIZE, (it as Switch).isChecked).commit()
        }
        accu_switch.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_ACCU_SWITCH, false)
        accu_switch.setOnClickListener {
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_ACCU_SWITCH, (it as Switch).isChecked).commit()
        }
        battery_monitor.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, false)
        battery_monitor.setOnClickListener {
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, (it as Switch).isChecked).commit()
        }
        //TODO:
        config_defaultlist.setOnItemClickListener { parent, _, position, id ->
            try {
                val intent = Intent(this.context, AppDetailsActivity::class.java)
                intent.putExtra("app", (parent.adapter.getItem(position) as Appinfo).packageName)
                startActivity(intent)
            } catch (ex: Exception) {
            }
        }

        config_search_box.addTextChangedListener(SearchTextWatcher(Runnable {
            loadList()
        }))

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
        })
        return list
    }

    private fun setListData(dl: ArrayList<Appinfo>?, lv: OverScrollListView) {
        myHandler.post {
            lv.adapter = SceneModeAdapter(context!!, dl!!)
            processBarDialog.hideDialog()
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun loadList(foreceReload: Boolean = false) {
        processBarDialog.showDialog()
        if (packageManager == null) {
            packageManager = context!!.packageManager
        }

        Thread(Runnable {
            if (foreceReload || installedList == null || installedList!!.size == 0) {
                installedList = ArrayList()/*在数组中存放数据*/
                val hasSystemApp = hasSystemApp
                installedList = if (hasSystemApp) applistHelper.getAll() else applistHelper.getUserAppList()
            }

            val keyword = config_search_box.text.toString()
            val search = keyword.isNotEmpty()
            val firstMode = globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, "balance")
            for (i in installedList!!.indices) {
                val item = installedList!![i]
                item.selectState = false
                val packageName = item.packageName.toString()
                if (search && !(packageName.contains(keyword) || item.appName.toString().contains(keyword))) {
                    continue
                }
                val config = spfPowercfg.getString(packageName, firstMode)
                when (config) {
                    "powersave" -> installedList!![i].enabledState = "省电"
                    "performance" -> installedList!![i].enabledState = "性能"
                    "fast" -> installedList!![i].enabledState = "极速"
                    "igoned" -> installedList!![i].enabledState = ""
                    else -> installedList!![i].enabledState = "均衡"
                }
            }
            sortAppList(installedList!!)
            myHandler.post {
                processBarDialog.hideDialog()
                setListData(installedList, config_defaultlist)
            }
        }).start()
    }

    //检查配置文件是否已经安装
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
                        .setSingleChoiceItems(arrayOf(getString(R.string.conservative), getString(R.string.radicalness)), 0, { _, which ->
                            i = which
                        })
                        .setNegativeButton(R.string.btn_confirm, { _, _ ->
                            installConfig(i > 0)
                        }).create().show()
            }
            else ->
                AlertDialog.Builder(context)
                        .setTitle(getString(R.string.not_support_config))
                        .setMessage(String.format(getString(R.string.not_support_config_desc), Consts.POWER_CFG_PATH))
                        .setNegativeButton(getString(R.string.more), { _, _ ->
                            val intent = Intent()
                            //Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                            intent.action = "android.intent.action.VIEW"
                            val content_url = Uri.parse("https://github.com/helloklf/vtools")
                            intent.data = content_url
                            startActivity(intent)
                        })
                        .setPositiveButton(getString(R.string.i_know), { _, _ ->
                        })
                        .create()
                        .show()
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
        } catch (ex: Exception) {
            Snackbar.make(view!!, getString(R.string.config_install_fail) + ex.message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
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
