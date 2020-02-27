package com.omarea.vtools.fragments

import android.content.Context
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.omarea.store.BatteryHistoryStore
import com.omarea.store.SpfConfig
import com.omarea.ui.AdapterBatteryStats
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.fragment_battery_stats.*
import java.util.*
import kotlin.math.abs


class FragmentBatteryStats : Fragment() {
    private lateinit var storage: BatteryHistoryStore
    private var timer: Timer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_battery_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        battery_stats_delete.setOnClickListener {
            BatteryHistoryStore(context!!).clearData()
            Toast.makeText(context!!, "统计记录已清理", Toast.LENGTH_SHORT).show()
            loadData()
        }
        storage = BatteryHistoryStore(context!!)
    }

    override fun onResume() {
        super.onResume()
        if (isDetached) {
            return
        }

        loadData()
        activity!!.title = getString(R.string.menu_battery_stats)
        timer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    updateMaxState()
                }
            }, 0, 200)
        }
    }

    override fun onPause() {
        timer?.cancel()
        timer = null
        super.onPause()
    }

    private val hander = Handler()
    private fun updateMaxState() {
        // 峰值设置
        val maxInput = abs(storage.getMaxIO(BatteryManager.BATTERY_STATUS_CHARGING))
        val maxOutput = abs(storage.getMinIO(BatteryManager.BATTERY_STATUS_DISCHARGING))
        val maxTemperature = abs(storage.getMaxTemperature())
        var batteryInputMax = 10000
        var batteryOutputMax = 3000
        var batteryTemperatureMax = 60

        if (maxInput > batteryInputMax) {
            batteryInputMax = maxInput
        }
        if (maxOutput > batteryOutputMax) {
            batteryOutputMax = maxOutput
        }
        if (maxTemperature > batteryTemperatureMax) {
            batteryTemperatureMax = maxTemperature
        }

        hander.post {
            try {
                battery_max_output.setData(batteryOutputMax.toFloat(), batteryOutputMax - maxOutput.toFloat())
                battery_max_output_text.text = maxOutput.toString() + " mA"
                battery_max_intput.setData(batteryInputMax.toFloat(), batteryInputMax - maxInput.toFloat())
                battery_max_intput_text.text = maxInput.toString() + " mA"
                if (maxTemperature < 0) {
                    battery_max_temperature.setData(batteryTemperatureMax.toFloat(), batteryTemperatureMax.toFloat())
                } else {
                    battery_max_temperature.setData(batteryTemperatureMax.toFloat(), batteryTemperatureMax - maxTemperature.toFloat())
                }
                battery_max_temperature_text.text = maxTemperature.toString() + "°C"
            } catch (ex: Exception) {
                timer?.cancel()
                timer = null
            }
        }
    }

    private fun loadData() {
        val storage = BatteryHistoryStore(context!!)
        val data = storage.getAvgData(BatteryManager.BATTERY_STATUS_DISCHARGING)

        val accuMode = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).getBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, false)
        val sampleTime = if (accuMode) 2 else 6
        battery_stats.adapter = AdapterBatteryStats(context!!, (data.filter {
            // 仅显示运行时间超过2分钟的应用数据，避免误差过大
            (it.count * sampleTime) > 120
        }), sampleTime)
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentBatteryStats()
            return fragment
        }
    }
}
