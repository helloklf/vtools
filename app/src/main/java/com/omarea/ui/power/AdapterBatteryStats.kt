package com.omarea.ui.power

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.omarea.library.basic.AppInfoLoader
import com.omarea.model.BatteryAvgStatus
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.vtools.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.abs

class AdapterBatteryStats(
        private var context: Context,
        private var list: List<BatteryAvgStatus>) : RecyclerView.Adapter<AdapterBatteryStats.ViewHolder>()
{
    private var appInfoLoader: AppInfoLoader = AppInfoLoader(context)

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val convertView = LayoutInflater.from(context).inflate(R.layout.list_item_battery_record, null)
        val viewHolder = ViewHolder(convertView).apply {
            itemAvgIO = convertView.findViewById(R.id.itemAvgIO)
            itemModeName = convertView.findViewById(R.id.itemModeName)
            itemTemperature = convertView.findViewById(R.id.itemTemperature)
            itemCounts = convertView.findViewById(R.id.itemCounts)
            itemTitle = convertView.findViewById(R.id.itemTitle)
            itemIcon = convertView.findViewById(R.id.itemIcon)
        }

        return viewHolder
    }

    private val bgPowersave = ContextCompat.getDrawable(context, R.drawable.powercfg_powersave)
    private val bgBalance = ContextCompat.getDrawable(context, R.drawable.powercfg_balance)
    private val bgPerformance = ContextCompat.getDrawable(context, R.drawable.powercfg_performance)
    private val bgFast = ContextCompat.getDrawable(context, R.drawable.powercfg_fast)
    private val bgNone = ContextCompat.getDrawable(context, R.drawable.powercfg_none)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val batteryStats = list.get(position)
        holder.apply {
            itemModeName.text = ModeSwitcher.getModName(batteryStats.mode)

            itemModeName.background = (when (batteryStats.mode) {
                ModeSwitcher.POWERSAVE -> bgPowersave
                ModeSwitcher.BALANCE -> bgBalance
                ModeSwitcher.PERFORMANCE -> bgPerformance
                ModeSwitcher.FAST -> bgFast
                else -> bgNone
            })

            itemAvgIO.text = String.format ("Avg: %dmA", abs(batteryStats.io))
            itemTemperature.text = String.format ("Avg: %d°C  Max: %d°C", batteryStats.avgTemperature, batteryStats.maxTemperature)
            itemCounts.text = "*" + batteryStats.count + ""

            val app = batteryStats.packageName
            packageName = app

            GlobalScope.launch(Dispatchers.Main) {
                val icon = appInfoLoader.loadAppBasicInfo(app).await()
                if (packageName == app) {
                    itemTitle.text = icon.appName
                    itemIcon.setImageDrawable(icon.icon)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        internal lateinit var itemAvgIO: TextView
        internal lateinit var itemModeName: TextView
        internal lateinit var itemCounts: TextView
        internal lateinit var itemTemperature: TextView
        internal lateinit var itemTitle: TextView
        internal lateinit var itemIcon: ImageView
        internal lateinit var packageName: String
    }
}