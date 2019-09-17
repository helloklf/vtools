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
import com.omarea.model.BatteryAvgStatus
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.SpfConfig
import com.omarea.vtools.R
import java.util.*

class AdapterBatteryStats(private val context: Context, private val list: ArrayList<BatteryAvgStatus>?) : BaseAdapter() {
    private var accuMode: Boolean = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).getBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, false)
    private var timerRate: Int
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

    private fun loadIcon(convertView: View, packageName: String) {
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
        convertView.findViewById<TextView>(R.id.itemAvgIO).text = batteryStats.io.toString() + "mA"
        convertView.findViewById<TextView>(R.id.itemTemperature).text = batteryStats.temperature.toString() + "°C"
        val time = (batteryStats.count * timerRate / 60.0).toInt()
        val total = batteryStats.count * batteryStats.io * timerRate / 3600.0
        convertView.findViewById<TextView>(R.id.itemCounts).text = "约${time}分钟,耗电" + String.format("%.1f", total) + "mAh"
        loadIcon(convertView, batteryStats.packageName)
        return convertView
    }

    init {
        timerRate = if (accuMode) 2 else 10
    }
}