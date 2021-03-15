package com.omarea.vtools.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import com.omarea.common.ui.AdapterAppChooser
import com.omarea.common.ui.DialogAppChooser
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.library.calculator.GetUpTime
import com.omarea.model.AppInfo
import com.omarea.model.TimingTaskInfo
import com.omarea.model.TriggerInfo
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.scene_mode.SceneStandbyMode
import com.omarea.scene_mode.TimingTaskManager
import com.omarea.scene_mode.TriggerManager
import com.omarea.store.SpfConfig
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
        processBarDialog = ProgressBarDialog(this)
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

        system_scene_command.setOnClickListener {
            val intent = Intent(this, ActivityCustomCommand::class.java)
            startActivity(intent)
        }
    }

    // 设置待机模式的应用
    private fun standbyAppConfig() {
        processBarDialog.showDialog()
        Thread {
            val configFile = context.getSharedPreferences(SceneStandbyMode.configSpfName, Context.MODE_PRIVATE)
            val whiteList = context.resources.getStringArray(R.array.scene_standby_white_list)
            val options = ArrayList(AppListHelper(context).getAll().filter {
                !whiteList.contains(it.packageName)
            }.sortedBy {
                it.appType
            }.map {
                it.apply {
                    selected = configFile.getBoolean(packageName.toString(), it.appType == AppInfo.AppType.USER && !it.updated)
                }
            })

            myHandler.post {
                processBarDialog.hideDialog()

                DialogAppChooser(themeMode.isDarkMode, ArrayList(options), true, object : DialogAppChooser.Callback {
                    override fun onConfirm(apps: List<AdapterAppChooser.AppInfo>) {
                        val items = apps.map { it.packageName }
                        options.forEach {
                            it.selected = items.contains(it.packageName)
                        }
                        saveStandbyAppConfig(options)
                    }
                }).show(supportFragmentManager, "standby_apps")
            }
        }.start()
    }

    // 保存休眠应用配置
    private fun saveStandbyAppConfig(apps: List<AppInfo>) {
        val configFile = getSharedPreferences(SceneStandbyMode.configSpfName, Context.MODE_PRIVATE).edit()
        configFile.clear()

        apps.forEach {
            if (it.selected && it.appType == AppInfo.AppType.SYSTEM) {
                configFile.putBoolean(it.packageName.toString(), true)
            } else if ((!it.selected) && it.appType == AppInfo.AppType.USER) {
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
            DialogHelper.confirm(this, "删除该任务？", "", {
                TimingTaskManager(context).removeTask(timingTaskInfo)
                updateCustomList()
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
            DialogHelper.confirm(this, "删除该触发器？", "", {
                TriggerManager(context).removeTrigger(triggerInfo)
                updateCustomList()
            })
            true
        }
    }

    override fun onDestroy() {
        processBarDialog.hideDialog()
        super.onDestroy()
    }
}
