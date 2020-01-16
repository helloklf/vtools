package com.omarea.vtools.activities

import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.omarea.model.TaskAction
import com.omarea.model.TimingTaskInfo
import com.omarea.scene_mode.TimingTask
import com.omarea.scene_mode.TimingTaskReceiver
import com.omarea.store.SpfConfig
import com.omarea.store.TimingTaskStorage
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_timing_task.*

class ActivityTimingTask : AppCompatActivity() {
    private lateinit var taskConfig: SharedPreferences
    private lateinit var timingTaskInfo: TimingTaskInfo
    private var taskId = "scene_task_1"

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

        taskConfig = getSharedPreferences("", Context.MODE_PRIVATE)

        taks_trigger_time.setOnClickListener {
            TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                taks_trigger_time.setText(String.format(getString(R.string.format_hh_mm), hourOfDay, minute))
                timingTaskInfo.triggerTime = hourOfDay * 60 + minute
            }, timingTaskInfo.triggerTime / 60, timingTaskInfo.triggerTime % 60, true).show()
        }

        val task = TimingTaskStorage(this).load(taskId)
        timingTaskInfo = if (task == null) TimingTaskInfo() else task
        updateUI()
    }

    private fun updateUI() {
        timingTaskInfo.run {
            // 触发时间
            val hourOfDay = triggerTime / 60
            val minute = triggerTime % 60
            taks_trigger_time.setText(String.format(getString(R.string.format_hh_mm), hourOfDay, minute))

            // 重复周期
            if (period < 1) {
                taks_once.isChecked = true
            } else {
                taks_repeat.isChecked = true
            }

            // 额外条件
            task_after_screen_off.isChecked = afterScreenOff
            task_before_execute_confirm.isChecked = beforeExecuteConfirm
            task_battery_capacity_require.isChecked = batteryCapacityRequire >- 0
            task_battery_capacity.text = batteryCapacityRequire.toString()

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
                task_fstrim.isChecked = contains(TaskAction.FSTRIM)
                task_compile_speed.isChecked = contains(TaskAction.COMPILE_SPEED)
                task_compile_everything.isChecked = contains(TaskAction.COMPILE_EVERYTHING)
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
        timingTaskInfo.period = if (taks_repeat.isChecked) (24 * 3600) else -1
        timingTaskInfo.afterScreenOff = task_after_screen_off.isChecked
        timingTaskInfo.beforeExecuteConfirm = task_before_execute_confirm.isChecked
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
            task_fstrim.isChecked && add(TaskAction.FSTRIM)
            task_compile_speed.isChecked && add(TaskAction.COMPILE_SPEED)
            task_compile_everything.isChecked && add(TaskAction.COMPILE_EVERYTHING)
        }
        TimingTaskStorage(this).save(timingTaskInfo, taskId)

        val taskIntent = Intent(this.applicationContext, TimingTaskReceiver::class.java)
        taskIntent.putExtra("taskId", taskId)
        val pendingIntent = PendingIntent.getBroadcast(this.applicationContext, 0, taskIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        TimingTask(this).cancelTask(pendingIntent).setExact(pendingIntent, 10000)
        finish()
    }

    override fun onPause() {
        super.onPause()
    }
}