package com.omarea.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.omarea.library.basic.AppInfoLoader
import com.omarea.model.BatteryAvgStatus
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.vtools.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.abs

class AdapterBatteryStats(private var context: Context, private var list: List<BatteryAvgStatus>, private var timerRate: Int) : BaseAdapter() {
    private var appInfoLoader: AppInfoLoader

    init {
        this.appInfoLoader = AppInfoLoader(context)
    }

    override fun getCount(): Int {
        return list.size ?: 0
    }

    override fun getItem(position: Int): BatteryAvgStatus {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private var packageManager: PackageManager? = null

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.list_item_battery_record, null)
        }
        val batteryStats = getItem(position)
        val modeView = convertView!!.findViewById<TextView>(R.id.itemModeName)
        modeView.text = ModeSwitcher.getModName(batteryStats.mode)

        when (batteryStats.mode) {
            ModeSwitcher.POWERSAVE -> {
                modeView.setTextColor(Color.parseColor("#0091D5"))
            }
            ModeSwitcher.PERFORMANCE -> {
                modeView.setTextColor(Color.parseColor("#6ECB00"))
            }
            ModeSwitcher.FAST -> {
                modeView.setTextColor(Color.parseColor("#FF7E00"))
            }
            ModeSwitcher.IGONED -> {
                modeView.setTextColor(Color.parseColor("#888888"))
            }
            ModeSwitcher.BALANCE -> {
                modeView.setTextColor(Color.parseColor("#00B78A"))
            }
            else -> {
                modeView.setTextColor(Color.parseColor("#00B78A"))
            }
        }
        convertView.findViewById<TextView>(R.id.itemAvgIO).text = (abs(batteryStats.io)).toString() + "mA"
        convertView.findViewById<TextView>(R.id.itemTemperature).text = "Avg:${batteryStats.avgTemperature}°C Max:${batteryStats.maxTemperature}°C"
        val time = (batteryStats.count * timerRate / 60.0).toInt()
        val total = batteryStats.count * batteryStats.io * timerRate / 3600.0
        convertView.findViewById<TextView>(R.id.itemCounts).text = "🕓 ${time}分钟"

        GlobalScope.launch(Dispatchers.Main) {
            val icon = appInfoLoader.loadAppBasicInfo(batteryStats.packageName).await()

            convertView.findViewById<TextView>(R.id.itemTitle).text = icon.appName
            convertView.findViewById<ImageView>(R.id.itemIcon).setImageDrawable(icon.icon)
        }

        return convertView
    }
}