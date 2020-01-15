package com.omarea.scene_mode

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context

class TimingTask(private var context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 设置精准时间的任务
     */
    private fun setExact(pendingIntent: PendingIntent, delay: Long) {
        // val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, pendingIntent)
        alarmManager.cancel(pendingIntent)
    }

    /**
     * 设置重复任务
     *
     */
    private fun setRepeating(pendingIntent: PendingIntent, delay: Long, period: Long) {
        // val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, period, pendingIntent)
        alarmManager.cancel(pendingIntent)
    }

    /**
     *
     */
    private fun cancelTask(pendingIntent: PendingIntent) {
        alarmManager.cancel(pendingIntent)
    }
}