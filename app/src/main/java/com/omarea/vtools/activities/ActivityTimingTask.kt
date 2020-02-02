package com.omarea.vtools.activities

import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import com.omarea.model.TaskAction
import com.omarea.model.TimingTaskInfo
import com.omarea.scene_mode.TimingTaskManager
import com.omarea.store.TimingTaskStorage
import com.omarea.utils.GetUpTime
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_timing_task.*
import java.util.*
import kotlin.collections.ArrayList

class ActivityTimingTask : AppCompatActivity() {
    private lateinit var timingTaskInfo: TimingTaskInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeSwitch.switchTheme(this)

        setContentView(R.layout.activity_timing_task)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        // setTitle(R.string.app_name)

        // 显示返回按钮
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

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
        oneOf(task_airplane_mode_on, task_airplane_mode_off)
        oneOf(task_wifi_on, task_wifi_off)
        oneOf(task_gps_on, task_gps_off)
        oneOf(task_gprs_on, task_gprs_off)
        oneOf(task_zen_mode_on, task_zen_mode_off)
        oneOf(task_after_screen_off, task_before_execute_confirm)
        oneOf(task_battery_capacity_require, task_charge_only)
        oneOf(task_power_off, task_power_reboot)

        oneOf(task_compile_speed, task_compile_everything)

        // 更新选中状态
        updateUI()

        // dex2oat
        task_compile.visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) View.VISIBLE else View.GONE
        // 勿扰模式
        task_zen_mode.visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) View.VISIBLE else View.GONE
        // 待机模式
        task_standby_mode.visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) View.VISIBLE else View.GONE
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
            task_battery_capacity_require.isChecked = batteryCapacityRequire >- 0
            task_battery_capacity.text = batteryCapacityRequire.toString()
            task_charge_only.isChecked = chargeOnly

            // 功能动作
            taskActions?.run {
                task_standby_on.isChecked = contains(TaskAction.STANDBY_MODE_ON)
                task_standby_off.isChecked = contains(TaskAction.STANDBY_MODE_OFF)
                task_airplane_mode_on.isChecked = contains(TaskAction.AIRPLANE_MODE_ON)
                task_airplane_mode_off.isChecked = contains(TaskAction.AIRPLANE_MODE_OFF)
                task_wifi_on.isChecked = contains(TaskAction.WIFI_ON)
                task_wifi_off.isChecked = contains(TaskAction.WIFI_OFF)
                task_gps_on.isChecked = contains(TaskAction.GPS_ON)
                task_gps_off.isChecked = contains(TaskAction.GPS_OFF)
                task_gprs_on.isChecked = contains(TaskAction.GPRS_ON)
                task_gprs_off.isChecked = contains(TaskAction.GPRS_OFF)
                task_zen_mode_on.isChecked = contains(TaskAction.ZEN_MODE_ON)
                task_zen_mode_off.isChecked = contains(TaskAction.ZEN_MODE_OFF)
                task_fstrim.isChecked = contains(TaskAction.FSTRIM)
                task_power_off.isChecked = contains(TaskAction.POWER_OFF)
                task_power_reboot.isChecked = contains(TaskAction.POWER_REBOOT)
                task_compile_speed.isChecked = contains(TaskAction.COMPILE_SPEED)
                task_compile_everything.isChecked = contains(TaskAction.COMPILE_EVERYTHING)
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
                radioButton2.tag  = false
                radioButton2.isChecked = false
            }
        }
        radioButton2.setOnClickListener {
            if ((it as CompoundButton).isChecked && it.tag == true) {
                it.tag = false
                it.isChecked = false
            } else {
                it.tag = it.isChecked
                radioButton1.tag  = false
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
        timingTaskInfo.batteryCapacityRequire = if(task_battery_capacity_require.isChecked) (task_battery_capacity.text).toString().toInt() else 0
        timingTaskInfo.taskActions = ArrayList<TaskAction>().apply {
            task_standby_on.isChecked && add(TaskAction.STANDBY_MODE_ON)
            task_standby_off.isChecked && add(TaskAction.STANDBY_MODE_OFF)
            task_airplane_mode_on.isChecked && add(TaskAction.AIRPLANE_MODE_ON)
            task_airplane_mode_off.isChecked && add(TaskAction.AIRPLANE_MODE_OFF)
            task_wifi_on.isChecked && add(TaskAction.WIFI_ON)
            task_wifi_off.isChecked && add(TaskAction.WIFI_OFF)
            task_gps_on.isChecked && add(TaskAction.GPS_ON)
            task_gps_off.isChecked && add(TaskAction.GPS_OFF)
            task_gprs_on.isChecked && add(TaskAction.GPRS_ON)
            task_gprs_off.isChecked && add(TaskAction.GPRS_OFF)
            task_zen_mode_on.isChecked && add(TaskAction.ZEN_MODE_ON)
            task_zen_mode_off.isChecked && add(TaskAction.ZEN_MODE_OFF)
            task_fstrim.isChecked && add(TaskAction.FSTRIM)
            task_compile_speed.isChecked && add(TaskAction.COMPILE_SPEED)
            task_compile_everything.isChecked && add(TaskAction.COMPILE_EVERYTHING)

            // 关机和重启动作放在最后
            task_power_off.isChecked && add(TaskAction.POWER_OFF)
            task_power_reboot.isChecked && add(TaskAction.POWER_REBOOT)
        }
        // timingTaskInfo.taskId = taskId

        TimingTaskManager(this).setTaskAndSave(timingTaskInfo)

        finish()
    }

    override fun onPause() {
        super.onPause()
    }
}