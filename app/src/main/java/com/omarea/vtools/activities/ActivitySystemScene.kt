package com.omarea.vtools.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.library.calculator.GetUpTime
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
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_system_scene.*

class ActivitySystemScene : ActivityBase() {
    private lateinit var processBarDialog: ProgressBarDialog
    private lateinit var globalSPF: SharedPreferences
    private lateinit var chargeConfig: SharedPreferences
    internal val myHandler: Handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_scene)

        setBackArrow()
        onViewCreated()
    }

    private lateinit var modeSwitcher: ModeSwitcher

    override fun onResume() {
        super.onResume()
        title = getString(R.string.menu_system_scene)

        updateCustomList()
    }

    private fun updateCustomList() {
        nextTask = null
        system_scene_task_list.removeAllViews()
        TimingTaskManager(context).listTask().forEach {
            addCustomTaskItemView(it)
            checkNextTask(it)
        }
        updateNextTaskInfo()

        system_scene_trigger_list.removeAllViews()
        TriggerManager(context).list().forEach {
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

    private fun onViewCreated() {
        modeSwitcher = ModeSwitcher()
        processBarDialog = ProgressBarDialog(context)
        globalSPF = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        chargeConfig = getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)

        val tabIconHelper = TabIconHelper(configlist_tabhost, this)
        configlist_tabhost.setup()

        tabIconHelper.newTabSpec("系统场景", getDrawable(R.drawable.tab_security)!!, R.id.blacklist_tab3)
        tabIconHelper.newTabSpec("设置", getDrawable(R.drawable.tab_settings)!!, R.id.configlist_tab5)
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
            val intent = Intent(this, ActivityTimingTask::class.java)
            startActivity(intent)
        }

        system_scene_add_trigger.setOnClickListener {
            val intent = Intent(this, ActivityTrigger::class.java)
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
        Thread(Runnable {
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
        val configFile = getSharedPreferences(SceneStandbyMode.configSpfName, Context.MODE_PRIVATE).edit()
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
        val sceneTaskItem = SceneTaskItem(context, timingTaskInfo)
        sceneTaskItem.setLayoutParams(LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        sceneTaskItem.isClickable = true
        return sceneTaskItem
    }

    private fun addCustomTaskItemView(timingTaskInfo: TimingTaskInfo) {
        val sceneTaskItem = buildCustomTaskItemView(timingTaskInfo)

        system_scene_task_list.addView(sceneTaskItem)
        sceneTaskItem.setOnClickListener {
            val intent = Intent(this, ActivityTimingTask::class.java)
            intent.putExtra("taskId", timingTaskInfo.taskId)
            startActivity(intent)
        }
        sceneTaskItem.setOnLongClickListener {
            DialogHelper.animDialog(AlertDialog.Builder(context).setTitle("删除该任务？").setPositiveButton(R.string.btn_confirm) { _, _ ->
                TimingTaskManager(context).removeTask(timingTaskInfo)
                updateCustomList()
            }.setNeutralButton(R.string.btn_cancel) { _, _ ->
            })
            true
        }
    }

    private fun addCustomTriggerView(triggerInfo: TriggerInfo) {
        val itemView = SceneTriggerItem(context, triggerInfo)
        itemView.setLayoutParams(LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        itemView.isClickable = true

        system_scene_trigger_list.addView(itemView)

        itemView.setOnClickListener {
            val intent = Intent(this, ActivityTrigger::class.java)
            intent.putExtra("id", triggerInfo.id)
            startActivity(intent)
        }
        itemView.setOnLongClickListener {
            DialogHelper.animDialog(AlertDialog.Builder(context).setTitle("删除该触发器？").setPositiveButton(R.string.btn_confirm) { _, _ ->
                TriggerManager(context).removeTrigger(triggerInfo)
                updateCustomList()
            }.setNeutralButton(R.string.btn_cancel) { _, _ ->
            })
            true
        }
    }

    override fun onDestroy() {
        processBarDialog.hideDialog()
        super.onDestroy()
    }
}
