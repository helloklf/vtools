package com.omarea.vtools.activities

import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.omarea.data.GlobalStatus
import com.omarea.library.device.BatteryCapacity
import com.omarea.library.shell.BatteryUtils
import com.omarea.store.BatteryHistoryStore
import com.omarea.ui.AdapterBatteryStats
import com.omarea.vtools.R
import com.omarea.vtools.dialogs.DialogElectricityUnit
import kotlinx.android.synthetic.main.activity_power_utilization.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs

class ActivityPowerUtilization : ActivityBase() {
    private lateinit var storage: BatteryHistoryStore
    private var timer: Timer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_power_utilization)

        setBackArrow()
        storage = BatteryHistoryStore(context)

        electricity_adj_unit.setOnClickListener {
            DialogElectricityUnit().showDialog(this)
        }
        more_charge.setOnClickListener {
            val intent = Intent(context, ActivityCharge::class.java)
            startActivity(intent)
        }
        GlobalScope.launch(Dispatchers.Main) {
            if (BatteryUtils().qcSettingSupport() || batteryUtils.bpSettingSupport()) {
                charge_controller.visibility = View.VISIBLE
                charge_controller.setOnClickListener {
                    val intent = Intent(context, ActivityChargeController::class.java)
                    startActivity(intent)
                }
            }
        }
        battery_stats.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.VERTICAL
            isSmoothScrollbarEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        title = getString(R.string.menu_power_utilization)
        timer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    updateUI()
                }
            }, 0, 10000)
        }
    }

    override fun onPause() {
        timer?.cancel()
        timer = null
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.delete, menu)
        return true
    }

    //右上角菜单
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> {
                BatteryHistoryStore(context).clearData()
                Toast.makeText(context, "统计记录已清理", Toast.LENGTH_SHORT).show()
                updateUI()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private var batteryUtils = BatteryUtils()
    private val hander = Handler(Looper.getMainLooper())
    private fun updateUI() {
        val level = GlobalStatus.batteryCapacity
        val temp = GlobalStatus.updateBatteryTemperature()
        val kernelCapacity = batteryUtils.getKernelCapacity(level)
        val batteryMAH = BatteryCapacity().getBatteryCapacity(this).toInt().toString() + "mAh" + "   "
        val voltage = GlobalStatus.batteryVoltage

        val data = storage.getAvgData(BatteryManager.BATTERY_STATUS_DISCHARGING)
        val sampleTime = 6

        hander.post {
            battery_stats.adapter = AdapterBatteryStats(context, (data.filter {
                // 仅显示运行时间超过2分钟的应用数据，避免误差过大
                (it.count * sampleTime) > 120
            }), sampleTime)

            view_time.invalidate()

            if (kernelCapacity > -1) {
                val str = "$kernelCapacity%"
                val ss = SpannableString(str)
                if (str.contains(".")) {
                    val small = AbsoluteSizeSpan((battrystatus_level.textSize * 0.3).toInt(), false)
                    ss.setSpan(small, str.indexOf("."), str.lastIndexOf("%"), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    val medium = AbsoluteSizeSpan((battrystatus_level.textSize * 0.5).toInt(), false)
                    ss.setSpan(medium, str.indexOf("%"), str.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                battrystatus_level.text = ss
            } else {
                battrystatus_level.text = "" + level + "%"
            }

            battery_status.text = getString(R.string.battery_temperature) + temp + "°C\n" + getString(R.string.battery_voltage) + voltage + "v\n" + (when (GlobalStatus.batteryStatus) {
                BatteryManager.BATTERY_STATUS_DISCHARGING -> {
                    getString(R.string.battery_status_discharging)
                }
                BatteryManager.BATTERY_STATUS_CHARGING -> {
                    getString(R.string.battery_status_charging)
                }
                BatteryManager.BATTERY_STATUS_FULL -> {
                    getString(R.string.battery_status_full)
                }
                BatteryManager.BATTERY_STATUS_UNKNOWN -> {
                    getString(R.string.battery_status_unknown)
                }
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> {
                    getString(R.string.battery_status_not_charging)
                }
                else -> getString(R.string.battery_status_unknown)
            }) + "\n" + batteryMAH
            battery_capacity_chart.setData(100f, 100f - level, temp.toFloat())
        }
    }

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
}
