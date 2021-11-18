package com.omarea.ui.power

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.omarea.data.customer.PowerUtilizationCurve.Companion.SAMPLING_INTERVAL
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
            itemAvg = convertView.findViewById(R.id.itemAvgIO)
            itemModeName = convertView.findViewById(R.id.itemModeName)
            itemMax = convertView.findViewById(R.id.itemTemperature)
            itemTimes = convertView.findViewById(R.id.itemCounts)
            itemTitle = convertView.findViewById(R.id.itemTitle)
            itemIcon = convertView.findViewById(R.id.itemIcon)
        }

        return viewHolder
    }

    /*
    private val bgPowersave = ContextCompat.getDrawable(context, R.drawable.powercfg_powersave)?.apply {
        alpha = 128
    }
    private val bgBalance = ContextCompat.getDrawable(context, R.drawable.powercfg_balance)?.apply {
        alpha = 128
    }
    private val bgPerformance = ContextCompat.getDrawable(context, R.drawable.powercfg_performance)?.apply {
        alpha = 128
    }
    private val bgFast = ContextCompat.getDrawable(context, R.drawable.powercfg_fast)?.apply {
        alpha = 128
    }
    private val bgNone = ContextCompat.getDrawable(context, R.drawable.powercfg_none)?.apply {
        alpha = 128
    }
    */
    private val samplingInterval = (SAMPLING_INTERVAL / 1000) // 采样间隔（秒）

    private fun minutes2Str(minutes: Long): String {
        if (minutes >= 1140) {
            return "" + (minutes / 1140) + "d" + ((minutes % 1140) / 60) + "h"
        } else if (minutes > 60) {
            return "" + (minutes / 60) + "h" + (minutes % 60) + "m"
        } else if (minutes == 0L) {
            return "0"
        }
        return "" + minutes + "m"
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val batteryStats = list.get(position)
        holder.apply {
            itemModeName.text = ModeSwitcher.getModName(batteryStats.mode)

            /*
            itemModeName.background = (when (batteryStats.mode) {
                ModeSwitcher.POWERSAVE -> bgPowersave
                ModeSwitcher.BALANCE -> bgBalance
                ModeSwitcher.PERFORMANCE -> bgPerformance
                ModeSwitcher.FAST -> bgFast
                else -> bgNone
            })
            */
            itemModeName.setTextColor(Color.parseColor(when (batteryStats.mode) {
                ModeSwitcher.POWERSAVE -> "#0091D5"
                ModeSwitcher.PERFORMANCE -> "#6ECB00"
                ModeSwitcher.FAST -> "#FF7E00"
                ModeSwitcher.IGONED -> "#888888"
                ModeSwitcher.BALANCE -> "#00B78A"
                else -> "#00B78A"
            }))

            itemAvg.text = String.format ("%dmA, %d°C", abs(batteryStats.io), batteryStats.avgTemperature)
            itemMax.text = String.format ("%d°C", batteryStats.maxTemperature)
            itemTimes.text = minutes2Str(samplingInterval * batteryStats.count / 60)

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
        internal lateinit var itemTitle: TextView
        internal lateinit var itemIcon: ImageView
        internal lateinit var itemAvg: TextView
        internal lateinit var itemMax: TextView
        internal lateinit var itemModeName: TextView
        internal lateinit var itemTimes: TextView
        internal lateinit var packageName: String
    }
}