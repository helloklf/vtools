package com.omarea.vtools.activities

import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import com.omarea.common.model.SelectItem
import com.omarea.common.shared.FileWrite
import com.omarea.common.ui.DialogItemChooser2
import com.omarea.krscript.executor.ExtractAssets
import com.omarea.library.calculator.GetUpTime
import com.omarea.model.CustomTaskAction
import com.omarea.model.TaskAction
import com.omarea.model.TimingTaskInfo
import com.omarea.scene_mode.TimingTaskManager
import com.omarea.store.TimingTaskStorage
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_timing_task.*
import java.io.File
import java.io.FilenameFilter
import java.net.URLDecoder
import java.util.*
import kotlin.collections.ArrayList

class ActivityTimingTask : ActivityBase() {
    private lateinit var timingTaskInfo: TimingTaskInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_timing_task)
        setBackArrow()

        // 读取或初始化任务模型
        var taskId: String = "SCENE_TASK_" + UUID.randomUUID().toString()
        intent?.run {
            if (hasExtra("taskId")) {
                intent.getStringExtra("taskId")?.run {
                    taskId = this
                }
            }
        }
        val task = TimingTaskStorage(this@ActivityTimingTask).load(taskId)
        timingTaskInfo = if (task == null) TimingTaskInfo(taskId) else task

        // 时间选择
        taks_trigger_time.setOnClickListener {
            TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                taks_trigger_time.setText(String.format(getString(R.string.format_hh_mm), hourOfDay, minute))
                timingTaskInfo.triggerTimeMinutes = hourOfDay * 60 + minute
            }, timingTaskInfo.triggerTimeMinutes / 60, timingTaskInfo.triggerTimeMinutes % 60, true).show()
        }

        // 设定单选关系
        oneOf(task_standby_on, task_standby_off)
        oneOf(task_zen_mode_on, task_zen_mode_off)
        oneOf(task_after_screen_off, task_before_execute_confirm)
        oneOf(task_battery_capacity_require, task_charge_only)

        // 更新选中状态
        updateUI()

        // 勿扰模式
        task_zen_mode.visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) View.VISIBLE else View.GONE
        // 待机模式
        task_standby_mode.visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) View.VISIBLE else View.GONE

        // 自定义动作点击
        task_custom_edit.setOnClickListener {
            customEditClick()
        }
    }

    private fun customEditClick() {
        ExtractAssets(this).extractResources("custom-command")

        val dirPath = FileWrite.getPrivateFilePath(this, "custom-command")
        val dir = File(dirPath)
        if (dir.exists()) {
            val files = dir.listFiles(object : FilenameFilter {
                override fun accept(dir: File?, name: String?): Boolean {
                    return name?.endsWith(".sh") == true
                }
            })
            val fileNames = files?.map {
                SelectItem().apply {
                    val name = URLDecoder.decode(it.name)
                    title = name
                    value = it.absolutePath
                    selected = timingTaskInfo.customTaskActions?.find { it.Name == name } != null
                }
            }?.sortedBy { it.title }
            val selectedItems = ArrayList<SelectItem>()
            timingTaskInfo.customTaskActions?.forEach { item ->
                val name = item.Name
                val r = fileNames?.find { it.title == name }
                if (r != null) {
                    selectedItems.add(r)
                }
            }

            if (fileNames != null && fileNames.size > 0) {
                DialogItemChooser2(themeMode.isDarkMode, ArrayList(fileNames), ArrayList(selectedItems), true, object : DialogItemChooser2.Callback {
                    override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                        timingTaskInfo.customTaskActions = ArrayList(selected.map {
                            CustomTaskAction().apply {
                                Name = it.title
                                Command = "sh '" + it.value + "'"
                            }
                        })
                        updateUI()
                    }
                }).setTitle("选择要执行的命令").show(supportFragmentManager, "custom-action-picker")
            } else {
                Toast.makeText(this, "你还没创建自定义命令", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "你还没创建自定义命令", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI() {
        timingTaskInfo.run {
            system_scene_task_enable.isChecked = enabled && (expireDate < 1 || expireDate > System.currentTimeMillis())

            // 触发时间
            val hourOfDay = triggerTimeMinutes / 60
            val minute = triggerTimeMinutes % 60
            taks_trigger_time.setText(String.format(getString(R.string.format_hh_mm), hourOfDay, minute))

            // 重复周期
            if (expireDate > 0) {
                taks_once.isChecked = true
            } else {
                taks_repeat.isChecked = true
            }

            // 额外条件
            task_after_screen_off.isChecked = afterScreenOff
            task_before_execute_confirm.isChecked = beforeExecuteConfirm
            task_battery_capacity_require.isChecked = batteryCapacityRequire > -0
            task_battery_capacity.text = batteryCapacityRequire.toString()
            task_charge_only.isChecked = chargeOnly

            // 功能动作
            taskActions?.run {
                task_standby_on.isChecked = contains(TaskAction.STANDBY_MODE_ON)
                task_standby_off.isChecked = contains(TaskAction.STANDBY_MODE_OFF)
                task_zen_mode_on.isChecked = contains(TaskAction.ZEN_MODE_ON)
                task_zen_mode_off.isChecked = contains(TaskAction.ZEN_MODE_OFF)
            }

            customTaskActions?.run {
                val str = this.map { it.Name }.toTypedArray().joinToString("\n\n").trim()
                task_custom_actions.text = str
            }
        }
    }

    private fun oneOf(radioButton1: CompoundButton, radioButton2: CompoundButton) {
        radioButton1.setOnClickListener {
            if ((it as CompoundButton).isChecked && it.tag == true) {
                it.tag = false
                it.isChecked = false
            } else {
                it.tag = it.isChecked
                radioButton2.tag = false
                radioButton2.isChecked = false
            }
        }
        radioButton2.setOnClickListener {
            if ((it as CompoundButton).isChecked && it.tag == true) {
                it.tag = false
                it.isChecked = false
            } else {
                it.tag = it.isChecked
                radioButton1.tag = false
                radioButton1.isChecked = false
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.save, menu)
        return true
    }

    //右上角菜单
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                saveConfigAndFinish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // 保存并关闭界面
    private fun saveConfigAndFinish() {
        timingTaskInfo.enabled = system_scene_task_enable.isChecked
        timingTaskInfo.expireDate = if (taks_repeat.isChecked) 0 else (GetUpTime(timingTaskInfo.triggerTimeMinutes).nextGetUpTime)
        timingTaskInfo.afterScreenOff = task_after_screen_off.isChecked
        timingTaskInfo.beforeExecuteConfirm = task_before_execute_confirm.isChecked
        timingTaskInfo.chargeOnly = task_charge_only.isChecked
        timingTaskInfo.batteryCapacityRequire = if (task_battery_capacity_require.isChecked) (task_battery_capacity.text).toString().toInt() else 0
        timingTaskInfo.taskActions = ArrayList<TaskAction>().apply {
            task_standby_on.isChecked && add(TaskAction.STANDBY_MODE_ON)
            task_standby_off.isChecked && add(TaskAction.STANDBY_MODE_OFF)
            task_zen_mode_on.isChecked && add(TaskAction.ZEN_MODE_ON)
            task_zen_mode_off.isChecked && add(TaskAction.ZEN_MODE_OFF)
        }
        // timingTaskInfo.taskId = taskId

        TimingTaskManager(this).setTaskAndSave(timingTaskInfo)

        finish()
    }

    override fun onPause() {
        super.onPause()
    }
}