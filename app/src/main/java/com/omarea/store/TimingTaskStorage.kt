package com.omarea.store

import android.content.Context
import com.omarea.model.TimingTaskInfo

class TimingTaskStorage(private val context: Context) : ObjectStorage<TimingTaskInfo>(context) {
    override public fun load(configFile: String): TimingTaskInfo? {
        return super.load(configFile)
    }

    override public fun save(obj: TimingTaskInfo?, configFile: String): Boolean {
        return super.save(obj, configFile)
    }
}
