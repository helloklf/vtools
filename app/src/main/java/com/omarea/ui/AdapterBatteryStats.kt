package com.omarea.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.omarea.shared.ModeList
import com.omarea.shared.model.BatteryAvgStatus
import com.omarea.shared.model.CpuCoreInfo
import com.omarea.vtools.R
import java.util.*

class AdapterBatteryStats(private val context: Context, private val list: ArrayList<BatteryAvgStatus>?) : BaseAdapter() {
    override fun getCount(): Int {
        return list?.size ?: 0
    }

    override fun getItem(position: Int): BatteryAvgStatus {
        return list!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private var packageManager: PackageManager? = null

    private fun loadIcon(convertView: View,packageName: String) {
        convertView.findViewById<TextView>(R.id.itemTitle).text = packageName
        Thread(Runnable {
            try {
                if (packageManager == null) {
                    packageManager = context.packageManager
                }
                val installInfo = packageManager!!.getPackageInfo(packageName.toString(), 0)
                val icon = installInfo.applicationInfo.loadIcon(context.packageManager)
                val appName = if (packageName != "") installInfo.applicationInfo.loadLabel(packageManager) else "未知场景"
                convertView.post {
                    convertView.findViewById<TextView>(R.id.itemTitle).text = appName
                    convertView.findViewById<ImageView>(R.id.itemIcon).setImageDrawable(icon)
                }
            } catch (ex: Exception) {
            } finally {
            }
        }).start()
    }

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.battery_stats_item, null)
        }
        val batteryStats  = getItem(position)
        convertView!!.findViewById<TextView>(R.id.itemModeName).text = ModeList.getModName(batteryStats.mode)
        convertView.findViewById<TextView>(R.id.itemAvgIO).text = batteryStats.io.toString() + "mA/h"
        convertView.findViewById<TextView>(R.id.itemTemperature).text = batteryStats.temperature.toString() + "°C"
        convertView.findViewById<TextView>(R.id.itemCounts).text = "${batteryStats.count}样本，耗电 " + String.format("%.1f", (batteryStats.count * batteryStats.io / 12.0 / 60.0)) + "mAh"
        loadIcon(convertView, batteryStats.packageName)
        return convertView
    }
}