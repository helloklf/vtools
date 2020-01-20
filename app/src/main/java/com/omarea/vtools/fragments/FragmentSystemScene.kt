package com.omarea.vtools.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.model.Appinfo
import com.omarea.model.TimingTaskInfo
import com.omarea.model.TriggerInfo
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.scene_mode.SceneStandbyMode
import com.omarea.scene_mode.TimingTaskManager
import com.omarea.scene_mode.TriggerManager
import com.omarea.store.SpfConfig
import com.omarea.ui.AppMultipleChoiceAdapter
import com.omarea.ui.SceneTaskItem
import com.omarea.ui.SceneTriggerItem
import com.omarea.ui.TabIconHelper
import com.omarea.utils.AppListHelper
import com.omarea.utils.GetUpTime
import com.omarea.vtools.R
import com.omarea.vtools.activities.ActivityTimingTask
import com.omarea.vtools.activities.ActivityTrigger
import kotlinx.android.synthetic.main.fragment_system_scene.*

class FragmentSystemScene : Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog
    private lateinit var globalSPF: SharedPreferences
    private lateinit var chargeConfig: SharedPreferences
    internal val myHandler: Handler = Handler()
    private var packageManager: PackageManager? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_system_scene, container, false)
    private lateinit var modeSwitcher: ModeSwitcher

    override fun onResume() {
        super.onResume()
        activity!!.title = getString(R.string.menu_system_scene)

        updateCustomList()
    }

    private fun updateCustomList() {
        nextTask = null
        system_scene_task_list.removeAllViews()
        TimingTaskManager(this.context!!).listTask().forEach {
            addCustomTaskItemView(it)
            checkNextTask(it)
        }
        updateNextTaskInfo()

        system_scene_trigger_list.removeAllViews()
        TriggerManager(this.context!!).list().forEach {
            it?.run {
                addCustomTriggerView(it)
            }
        }
    }

    private var nextTask: TimingTaskInfo? = null // 下一个要执行的任务
    private fun checkNextTask(it: TimingTaskInfo) {
        if (it.enabled && (it.expireDate < 1 || it.expireDate > System.currentTimeMillis())) {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (packageManager == null) {
            packageManager = context!!.packageManager
        }

        modeSwitcher = ModeSwitcher()
        processBarDialog = ProgressBarDialog(context!!)
        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        chargeConfig = context!!.getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)

        val tabIconHelper = TabIconHelper(configlist_tabhost, this.activity!!)
        configlist_tabhost.setup()

        tabIconHelper.newTabSpec("系统场景", context!!.getDrawable(R.drawable.tab_security)!!, R.id.blacklist_tab3)
        tabIconHelper.newTabSpec("设置", context!!.getDrawable(R.drawable.tab_settings)!!, R.id.configlist_tab5)
        configlist_tabhost.currentTab = 0
        configlist_tabhost.setOnTabChangedListener { tabId ->
            tabIconHelper.updateHighlight()
        }

        if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_BP, false)) {
            system_scene_bp.visibility = View.VISIBLE
            val limit = chargeConfig.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, SpfConfig.CHARGE_SPF_BP_LEVEL_DEFAULT)
            system_scene_bp_lt.text = (limit - 20).toString() + "%"
            system_scene_bp_gt.text = limit.toString() + "%"
        }

        system_scene_add_task.setOnClickListener {
            val intent = Intent(activity, ActivityTimingTask::class.java)
            startActivity(intent)
        }

        system_scene_add_trigger.setOnClickListener {
            val intent = Intent(activity, ActivityTrigger::class.java)
            startActivity(intent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            system_scene_standby_apps.visibility = View.VISIBLE
            system_scene_standby_apps.setOnClickListener {
                standbyAppConfig()
            }
        } else {
            system_scene_standby_apps.visibility = View.GONE
        }
    }

    // 设置待机模式的应用
    private fun standbyAppConfig() {
        processBarDialog.showDialog()
        val context = context!!
        Thread(Runnable{
            val configFile = context.getSharedPreferences(SceneStandbyMode.configSpfName, Context.MODE_PRIVATE)
            val whiteList = context.resources.getStringArray(R.array.scene_standby_white_list)
            val view = LayoutInflater.from(context).inflate(R.layout.layout_standby_apps, null)
            val apps = AppListHelper(context).getAll().filter {
                !whiteList.contains(it.packageName)
            }.sortedBy {
                it.appType
            }.map {
                it.apply {
                    selectState = configFile.getBoolean(packageName.toString(), it.appType == Appinfo.AppType.USER && !it.updated)
                }
            }

            myHandler.post {
                processBarDialog.hideDialog()
                val listview = view.findViewById<ListView>(R.id.standby_apps)
                val adapter = AppMultipleChoiceAdapter(listview, apps)
                listview.adapter = adapter

                DialogHelper.animDialog(AlertDialog.Builder(context).setCancelable(false)
                        .setPositiveButton(R.string.btn_confirm) { _, _ ->
                            saveStandbyAppConfig(adapter.getAll())
                        }.setNeutralButton(R.string.btn_cancel) { _, _ -> }.setView(view)
                )
            }
        }).start()
    }

    // 保存休眠应用配置
    private fun saveStandbyAppConfig(apps: List<Appinfo>) {
        val configFile = context!!.getSharedPreferences(SceneStandbyMode.configSpfName, Context.MODE_PRIVATE).edit()
        configFile.clear()

        apps.forEach {
            if (it.selectState && it.appType == Appinfo.AppType.SYSTEM) {
                configFile.putBoolean(it.packageName.toString(), true)
            } else if ((!it.selectState) && it.appType == Appinfo.AppType.USER) {
                configFile.putBoolean(it.packageName.toString(), false)
            }
        }

        configFile.apply()
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

    private fun addCustomTriggerView(triggerInfo: TriggerInfo) {
        val itemView = SceneTriggerItem(context!!, triggerInfo)
        itemView.setLayoutParams(LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        itemView.isClickable = true

        system_scene_trigger_list.addView(itemView)

        itemView.setOnClickListener {
            val intent = Intent(activity, ActivityTrigger::class.java)
            intent.putExtra("id", triggerInfo.id)
            startActivity(intent)
        }
        itemView.setOnLongClickListener {
            DialogHelper.animDialog(AlertDialog.Builder(context!!).setTitle("删除该触发器？").setPositiveButton(R.string.btn_confirm) { _, _ ->
                TriggerManager(context!!).removeTrigger(triggerInfo)
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
