package com.omarea.scene_mode

import android.content.Context
import com.omarea.library.shell.CGroupMemoryUtlis
import java.util.*
import kotlin.collections.ArrayList

class CGroupManager(private val context: Context) {
    private val cGroupMemoryUtlis = CGroupMemoryUtlis(context)

    class ProcessHistory {
        companion object {
            val STATE_TOP = 1
            val STATE_BACKGROUND = 2
            val STATE_LIMITED = 3
            val STATE_RECLAIMED = 4
        }

        constructor(packageName: String, state: Int) {
            this.packageName = packageName
            this.state = state
        }

        var packageName: String = ""
        var leaveTime: Long = System.currentTimeMillis()
        var state = STATE_TOP
    }

    private val history = ArrayList<ProcessHistory>()

    private var timer: Timer? = null

    private val periodTickInterval = 10000L
    private val ticks = 6
    private val periodTime = periodTickInterval * ticks
    fun startPolling() {
        if (timer == null) {
            timer = Timer().apply {
                var period = 0
                schedule(object : TimerTask() {
                    override fun run() {
                        period += 1
                        period %= ticks
                        onTick(period)
                    }
                }, periodTickInterval, periodTickInterval)
            }
        }
    }

    fun cancelPolling() {
        timer?.run {
            timer?.cancel()
            timer = null
        }
    }

    private fun onTick(period: Int) {
        val currentTime = System.currentTimeMillis()
        if (period == 0) {
            val targetGroup = "scene_cache"
            val toRecycle = history.filter {
                it.state == ProcessHistory.STATE_LIMITED && (currentTime - it.leaveTime) >= periodTime
            }

            if (toRecycle.isNotEmpty()) {
                val cGroupMemoryUtlis = CGroupMemoryUtlis(context)
                toRecycle.forEach {
                    cGroupMemoryUtlis.setGroup(it.packageName, targetGroup)
                    it.state = ProcessHistory.STATE_RECLAIMED
                }
            }
        } else {
            val targetGroup = "scene_bg"
            val toRecycle = history.filter {
                it.state == ProcessHistory.STATE_BACKGROUND && (currentTime - it.leaveTime) >= periodTickInterval
            }

            if (toRecycle.isNotEmpty()) {
                toRecycle.forEach {
                    cGroupMemoryUtlis.setGroup(it.packageName, targetGroup)
                    it.state = ProcessHistory.STATE_LIMITED
                }
            }
        }
    }

    public fun onAppLeave(packageName: String) {
        var record = history.find { it.packageName == packageName }
        val time = System.currentTimeMillis()
        if (record != null) {
            record.state = ProcessHistory.STATE_BACKGROUND
            record.leaveTime = time
        } else {
            record = ProcessHistory(packageName, ProcessHistory.STATE_BACKGROUND).apply {
                leaveTime = time
            }
            history.add(record)
        }
    }

    public fun onAppEnter(packageName: String) {
        var record = history.find { it.packageName == packageName }
        if (record != null) {
            if (record.state != ProcessHistory.STATE_TOP || record.state != ProcessHistory.STATE_BACKGROUND) {
                cGroupMemoryUtlis.setGroup(record.packageName, "")
            }
            record.state = ProcessHistory.STATE_TOP
            record.leaveTime = System.currentTimeMillis()
        } else {
            record = ProcessHistory(packageName, ProcessHistory.STATE_TOP)
            history.add(record)
        }

        startPolling()
    }
}