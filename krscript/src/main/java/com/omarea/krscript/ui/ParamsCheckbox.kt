package com.omarea.krscript.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import com.omarea.krscript.R
import com.omarea.krscript.model.ActionParamInfo

class ParamsCheckbox(private var actionParamInfo: ActionParamInfo, private var context: Context) {
    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_checkbox, null)


        layout.findViewById<CheckBox>(R.id.kr_param_checkbox).run {
            tag = actionParamInfo.name
            isChecked = getCheckState(actionParamInfo, false)
            if (!actionParamInfo.label.isNullOrEmpty()) {
                text = actionParamInfo.label
            }

            setOnClickListener {

            }
        }

        return layout
    }

    /**
     * 获取选中状态
     */
    private fun getCheckState(actionParamInfo: ActionParamInfo, defaultValue: Boolean): Boolean {
        if (actionParamInfo.valueFromShell != null) {
            return actionParamInfo.valueFromShell == "1" || actionParamInfo.valueFromShell!!.toLowerCase() == "true"
        } else if (actionParamInfo.value != null) {
            return actionParamInfo.value == "1" || actionParamInfo.value!!.toLowerCase() == "true"
        }
        return defaultValue
    }
}
