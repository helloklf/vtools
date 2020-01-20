package com.omarea.scene_mode

import android.content.Context
import com.omarea.model.TriggerInfo
import com.omarea.store.TriggerStorage

class TriggerManager(private var context: Context) {
    private val triggerListConfig = context.getSharedPreferences("scene_trigger_list", Context.MODE_PRIVATE)

    fun setTriggerAndSave(triggerInfo: TriggerInfo) {
        val events = StringBuilder()
        triggerInfo.events?.forEach {
            events.append(it.name)
            events.append(",")
        }

        triggerListConfig.edit().putString(triggerInfo.id, events.toString()).apply()
        TriggerStorage(context).save(triggerInfo)
    }

    fun removeTrigger(triggerInfo: TriggerInfo) {
        triggerListConfig.edit().remove(triggerInfo.id).apply()
        TriggerStorage(context).remove(triggerInfo.id)
    }

    fun list(): List<TriggerInfo?> {
        val storage = TriggerStorage(context)
        return triggerListConfig.all.keys.map {
            storage.load(it)
        }.filter { it != null }.toList()
    }
}