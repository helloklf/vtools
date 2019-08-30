package com.omarea.vtools.dialogs

import android.app.AlertDialog
import android.content.Context
import android.support.v4.app.Fragment
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.omarea.common.ui.DialogHelper
import com.omarea.store.SpfConfig
import com.omarea.vtools.R

class DialogElectricityUnit {
    fun showDialog(fragment: Fragment, sampleValue: Long) {
        val globalSPF = fragment.context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        var alertDialog: AlertDialog? = null
        val dialog = fragment.layoutInflater.inflate(R.layout.dialog_electricity_unit, null)
        val electricity_adj_unit = dialog.findViewById<TextView>(R.id.electricity_adj_unit)
        var unit = globalSPF.getInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT)
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
            electricity_adj_sample.setText((sampleValue / unit).toString() + "mA")
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
            electricity_adj_sample.setText((sampleValue / unit).toString() + "mA")
        }
        dialog.findViewById<Button>(R.id.electricity_adj_applay).setOnClickListener {
            globalSPF.edit().putInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, unit).apply()
            alertDialog?.dismiss()
        }
        electricity_adj_unit.setText(unit.toString())
        electricity_adj_sample.setText((sampleValue / unit).toString() + "mA")
        alertDialog = DialogHelper.animDialog(AlertDialog.Builder(fragment.context).setView(dialog))
    }
}
