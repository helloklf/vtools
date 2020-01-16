package com.omarea.scene_mode

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.SystemClock

public class TimingTask(private var context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 设置精准时间的任务
     */
    public fun setExact(pendingIntent: PendingIntent, delay: Long): TimingTask {
        // val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + delay, pendingIntent)
        // alarmManager.cancel(pendingIntent)
        return this
    }

    /**
     * 设置重复任务
     *
     */
    public fun setRepeating(pendingIntent: PendingIntent, delay: Long, period: Long): TimingTask {
        // val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + delay, period, pendingIntent)
        // alarmManager.cancel(pendingIntent)
        return this
    }

    /**
     *
     */
    public fun cancelTask(pendingIntent: PendingIntent): TimingTask {
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