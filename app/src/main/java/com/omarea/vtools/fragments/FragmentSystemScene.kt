package com.omarea.vtools.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Switch
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.model.TimingTaskInfo
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.scene_mode.TimingTaskManager
import com.omarea.store.SceneConfigStore
import com.omarea.store.SpfConfig
import com.omarea.ui.SceneTaskItem
import com.omarea.ui.TabIconHelper
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.utils.AppListHelper
import com.omarea.utils.GetUpTime
import com.omarea.vtools.R
import com.omarea.vtools.activities.ActivityTimingTask
import kotlinx.android.synthetic.main.fragment_system_scene.*


class FragmentSystemScene : Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog
    private lateinit var spfPowercfg: SharedPreferences
    private lateinit var globalSPF: SharedPreferences
    private lateinit var chargeConfig: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var applistHelper: AppListHelper
    internal val myHandler: Handler = Handler()
    private var packageManager: PackageManager? = null
    private lateinit var sceneConfigStore: SceneConfigStore
    private var firstMode = ModeSwitcher.DEFAULT

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_system_scene, container, false)
    private lateinit var modeSwitcher: ModeSwitcher

    override fun onResume() {
        super.onResume()
        activity!!.title = getString(R.string.menu_system_scene)
        val serviceState = AccessibleServiceHelper().serviceRunning(context!!)
        btn_config_service_not_active.visibility = if (serviceState) View.GONE else View.VISIBLE

        updateCustomList()
    }

    private fun updateCustomList() {
        nextTask = null
        system_scene_task_list.removeAllViews()
        TimingTaskManager(this.context!!).listTask().map {
            addCustomTaskItemView(it)
            checkNextTask(it)
        }
        updateNextTaskInfo()
    }

    private var nextTask: TimingTaskInfo? = null // 下一个要执行的任务
    private fun checkNextTask(it: TimingTaskInfo) {
        if (it.enabled) {
            if (nextTask == null || GetUpTime(it.triggerTimeMinutes).minutes < GetUpTime(nextTask!!.triggerTimeMinutes).minutes) {
                nextTask = it
            }
        }
    }

    private fun updateNextTaskInfo() {
        system_scene_next_content.removeAllViews()
        if (nextTask != null) {
            system_scene_next_content.addView(buildCustomTaskItemView(nextTask!!))
        }
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
        chargeConfig = context!!.getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
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

        tabIconHelper.newTabSpec("系统场景", context!!.getDrawable(R.drawable.tab_security)!!, R.id.blacklist_tab3)
        tabIconHelper.newTabSpec("设置", context!!.getDrawable(R.drawable.tab_settings)!!, R.id.configlist_tab5)
        configlist_tabhost.currentTab = 0
        configlist_tabhost.setOnTabChangedListener { tabId ->
            tabIconHelper.updateHighlight()
        }

        val modeValue = globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, "balance")


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

        if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_BP, false)) {
            system_scene_bp.visibility = View.VISIBLE
            val limit = chargeConfig.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, SpfConfig.CHARGE_SPF_BP_LEVEL_DEFAULT)
            system_scene_bp_lt.text = (limit - 20).toString() + "%"
            system_scene_bp_gt.text = limit.toString() + "%"
        }

        system_scene_add.setOnClickListener {
            val intent = Intent(activity, ActivityTimingTask::class.java)
            startActivity(intent)
        }
    }

    private fun buildCustomTaskItemView(timingTaskInfo: TimingTaskInfo): SceneTaskItem {
        val sceneTaskItem = SceneTaskItem(context!!, timingTaskInfo)
        sceneTaskItem.setLayoutParams(LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        sceneTaskItem.isClickable = true
        return sceneTaskItem
    }

    private fun addCustomTaskItemView(timingTaskInfo: TimingTaskInfo) {
        val sceneTaskItem = buildCustomTaskItemView(timingTaskInfo)

        system_scene_task_list.addView(sceneTaskItem)
        sceneTaskItem.setOnClickListener {
            val intent = Intent(activity, ActivityTimingTask::class.java)
            intent.putExtra("taskId", timingTaskInfo.taskId)
            startActivity(intent)
        }
        sceneTaskItem.setOnLongClickListener {
            DialogHelper.animDialog(AlertDialog.Builder(context!!).setTitle("删除该任务？").setPositiveButton(R.string.btn_confirm) { _, _ ->
                TimingTaskManager(context!!).removeTask(timingTaskInfo)
                updateCustomList()
            }.setNeutralButton(R.string.btn_cancel) { _, _ ->
            })
            true
        }
    }

    private fun formateTime(time: Long): String {
        Log.d(">>>>time", "" + time)
        val days = time / (24 * 3600)
        val hours = time % (24 * 3600) / 3600
        val minutes = time % 3600 / 60
        val seconds = time % 60

        return "${days}天 ${hours}时 ${minutes}分 ${seconds}秒"
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

    override fun onDestroy() {
        processBarDialog.hideDialog()
        super.onDestroy()
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentSystemScene()
            return fragment
        }
    }
}
