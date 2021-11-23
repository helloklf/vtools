package com.omarea.vtools.dialogs

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.omarea.common.ui.DialogHelper
import com.omarea.data.GlobalStatus
import com.omarea.store.ChargeSpeedStore
import com.omarea.store.SpfConfig
import com.omarea.vtools.R
import java.util.*

class DialogElectricityUnit {
    fun showDialog(context: Context) {
        val globalSPF = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        var currentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val defaultUnit = if (Build.MANUFACTURER.toUpperCase() == "XIAOMI") {
            SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT
        } else {
            if (GlobalStatus.batteryStatus == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                if (currentNow > 20000) {
                    -1000
                } else if (currentNow < -20000) {
                    1000
                } else if (currentNow > 0) {
                    -1
                } else {
                    1
                }
            } else if (GlobalStatus.batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                if (currentNow > 20000) {
                    1000
                } else if (currentNow < -20000) {
                    -1000
                } else if (currentNow > 0) {
                    1
                } else {
                    -1
                }
            } else {
                SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT
            }
        }
        var unit = globalSPF.getInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, defaultUnit)
        val origin = unit
        var alertDialog: DialogHelper.DialogWrap? = null
        val dialog = LayoutInflater.from(context).inflate(R.layout.dialog_electricity_unit, null)
        val electricity_adj_unit = dialog.findViewById<TextView>(R.id.electricity_adj_unit)
        val electricity_adj_sample = dialog.findViewById<TextView>(R.id.electricity_adj_sample)

        dialog.findViewById<ImageButton>(R.id.electricity_adj_minus).setOnClickListener {
            if (unit == -1) {
                unit = -10
            } else if (unit == 1) {
                unit = -1
            } else if (unit > 0) {
                unit /= 10
            } else if (unit > -1000 * 1000 * 100) {
                unit *= 10
            }
            electricity_adj_unit.setText(unit.toString())
            val currentMA = currentNow / unit
            electricity_adj_sample.setText((if (currentMA >= 0) "+" else "") + currentMA + "mA")
        }
        dialog.findViewById<ImageButton>(R.id.electricity_adj_plus).setOnClickListener {
            if (unit == -1) {
                unit = 1
            } else if (unit < 0) {
                unit /= 10
            } else if (unit < 1000 * 1000 * 100) {
                unit *= 10
            }
            electricity_adj_unit.setText(unit.toString())
            val currentMA = currentNow / unit
            electricity_adj_sample.setText((if (currentMA >= 0) "+" else "") + currentMA + "mA")
        }
        dialog.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            globalSPF.edit().putInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, unit).apply()
            alertDialog?.dismiss()
        }
        electricity_adj_unit.setText(unit.toString())
        val handler = Handler(Looper.getMainLooper())
        val timer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    handler.post {
                        currentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                        try {
                            val currentMA = currentNow / unit
                            electricity_adj_sample.setText((if (currentMA >= 0) "+" else "") + currentMA + "mA")
                        } catch (ex: Exception) {
                        }
                    }
                }
            }, 10, 1000)
        }

        alertDialog = DialogHelper.customDialog(context, dialog, false).setOnDismissListener {
            if (origin != unit) {
                ChargeSpeedStore(context).clearAll()
            }
            timer.cancel()
        }
    }
}
