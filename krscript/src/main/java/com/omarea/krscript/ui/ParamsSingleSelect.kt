package com.omarea.krscript.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.omarea.common.model.SelectItem
import com.omarea.common.ui.DialogItemChooser
import com.omarea.krscript.R
import com.omarea.krscript.model.ActionParamInfo

class ParamsSingleSelect(
        private var actionParamInfo: ActionParamInfo,
        private var context: FragmentActivity
) {

    private val systemUiVisibility = context.window?.decorView?.systemUiVisibility
    private var darkMode = systemUiVisibility != null && (systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) == 0

    val options = actionParamInfo.optionsFromShell!!
    var selectedIndex = ActionParamsLayoutRender.getParamOptionsCurrentIndex(actionParamInfo, options) // 获取当前选中项索引

    private fun updateValueView(valueView: TextView, textView: TextView) {
        if (selectedIndex > -1 && selectedIndex < options.size) {
            valueView.text = options[(selectedIndex)].value
            textView.text = options[(selectedIndex)].title
        } else {
            valueView.text = ""
            textView.text = ""
        }
    }

    fun render(): View {
        if (options.size > 5) {
            val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_single_select, null)
            val textView = layout.findViewById<TextView>(R.id.kr_param_single_select)
            val valueView = layout.findViewById<TextView>(R.id.kr_param_value).apply {
                tag = actionParamInfo.name
                updateValueView(this, textView)
            }
            textView.run {
                setOnClickListener {
                    openSingleSelectDialog(valueView, textView)
                }
            }

            return layout
        } else {
            val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_spinner, null)

            // TODO:设置Spinner默认不选中任何项
            layout.findViewById<Spinner>(R.id.kr_param_spinner).run {
                tag = actionParamInfo.name

                adapter = ArrayAdapter(context, R.layout.kr_spinner_default, R.id.text, options).apply {
                    setDropDownViewResource(R.layout.kr_spinner_dropdown)
                }
                isEnabled = !actionParamInfo.readonly

                if (selectedIndex > -1 && selectedIndex < options.size) {
                    setSelection(selectedIndex)
                }
            }

            return layout
        }
    }

    private fun openSingleSelectDialog(valueView: TextView, textView: TextView) {
        DialogItemChooser(darkMode, ArrayList(options.mapIndexed{index, item->
            SelectItem().apply {
                title = item.title
                selected = index == selectedIndex
            }
        }), false, object : DialogItemChooser.Callback {
            override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                selectedIndex = status.indexOf(true)
                updateValueView(valueView, textView)
            }
        }).show(context.supportFragmentManager, "params-single-select")
    }
}
