package com.omarea.scene_mode

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.omarea.model.TimingTaskInfo
import com.omarea.store.TimingTaskStorage
import com.omarea.utils.GetUpTime

public class TimingTaskManager(private var context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val taskListConfig = context.getSharedPreferences("scene_task_list", Context.MODE_PRIVATE)

    private fun getPendingIntent(timingTaskInfo: TimingTaskInfo): PendingIntent {
        val taskId = timingTaskInfo.taskId
        val taskIntent = Intent(context.applicationContext, TimingTaskReceiver::class.java)
        taskIntent.putExtra("taskId", taskId)
        taskIntent.setAction(taskId)
        val pendingIntent = PendingIntent.getBroadcast(context.applicationContext, 0, taskIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        return pendingIntent
    }

    public fun setTask(timingTaskInfo: TimingTaskInfo) {
        TimingTaskStorage(context).save(timingTaskInfo)

        val taskId = timingTaskInfo.taskId

        // 如果任务启用了，立即添加到队列
        if (timingTaskInfo.enabled) {
            val delay = GetUpTime(timingTaskInfo.triggerTimeMinutes).minutes.toLong() * 60 * 1000 // 下次执行
            val period = timingTaskInfo.periodMillis.toLong() // 重复周期

            val pendingIntent = getPendingIntent(timingTaskInfo)
            if (period > 0) {
                setRepeating(pendingIntent, delay, period)
            } else {
                setExact(pendingIntent, delay)
            }
        } else {
            cancelTask(timingTaskInfo)
        }

        taskListConfig.edit().putBoolean(taskId, timingTaskInfo.enabled).apply()
    }

    public fun listTask(): ArrayList<TimingTaskInfo> {
        val taskList = ArrayList<TimingTaskInfo>()
        val storage = TimingTaskStorage(context)
        taskListConfig.all.keys.map {
            storage.load(it)?.run {
                taskList.add(this)
            }
        }
        return taskList
    }

    public fun cancelTask(timingTaskInfo: TimingTaskInfo) {
        val pendingIntent = getPendingIntent(timingTaskInfo)
        cancelTask(pendingIntent)
    }

    public fun removeTask(timingTaskInfo: TimingTaskInfo) {
        cancelTask(timingTaskInfo)
        val storage = TimingTaskStorage(context)
        storage.remove(timingTaskInfo.taskId)
    }

    /**
     * 设置精准时间的任务
     */
    public fun setExact(pendingIntent: PendingIntent, delay: Long): TimingTaskManager {
        // val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, pendingIntent)
        }
        // alarmManager.cancel(pendingIntent)
        return this
    }

    /**
     * 设置重复任务
     *
     */
    public fun setRepeating(pendingIntent: PendingIntent, delay: Long, period: Long): TimingTaskManager {
        // val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, period, pendingIntent)
        // alarmManager.cancel(pendingIntent)
        return this
    }

    /**
     *
     */
    public fun cancelTask(pendingIntent: PendingIntent): TimingTaskManager {
        alarmManager.cancel(pendingIntent)
        return this
    }

    /**
     * 获取下个任务信息
     */
    public fun getNextAlarmClock(): AlarmManager.AlarmClockInfo? {
         return alarmManager.getNextAlarmClock()
    }
}