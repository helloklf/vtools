package com.omarea.krscript.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.SimpleAdapter
import android.widget.Spinner
import com.omarea.krscript.R
import com.omarea.krscript.model.ActionParamInfo

class ParamsSpinner(private var actionParamInfo: ActionParamInfo, private var context: Context) {
    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_spinner, null)


        layout.findViewById<Spinner>(R.id.kr_param_spinner).run {
            tag = actionParamInfo.name
            val options = actionParamInfo.optionsFromShell!!
            val selectedIndex = ActionParamsLayoutRender.getParamOptionsCurrentIndex(actionParamInfo, options) // 获取当前选中项索引

            adapter = SimpleAdapter(context, options, R.layout.kr_spinner_default, arrayOf("title"), intArrayOf(R.id.text)).apply {
                setDropDownViewResource(R.layout.kr_spinner_dropdown)
            }
            isEnabled = !actionParamInfo.readonly

            if (selectedIndex > -1 && selectedIndex < options.size) {
                setSelection(selectedIndex)
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
