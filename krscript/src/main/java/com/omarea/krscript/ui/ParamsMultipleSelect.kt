package com.omarea.krscript.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.omarea.common.model.SelectItem
import com.omarea.common.ui.DialogItemChooser
import com.omarea.krscript.R
import com.omarea.krscript.model.ActionParamInfo

class ParamsMultipleSelect(private val actionParamInfo: ActionParamInfo, private val context: FragmentActivity) {
    private var options: ArrayList<SelectItem>? = null
    private var status = booleanArrayOf()
    private var labels: Array<String?> = arrayOf()
    private var values: Array<String?> = arrayOf()

    fun render(): View {
        options = actionParamInfo.optionsFromShell
        options?.run {
            labels = map { it.title }.toTypedArray()
            values = map { it.value }.toTypedArray()
            status = ActionParamsLayoutRender.getParamOptionsSelectedStatus(actionParamInfo, this)
        }

        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_multiple_select, null)
        val textView = layout.findViewById<TextView>(R.id.kr_param_label_text)
        val valueView = layout.findViewById<TextView>(R.id.kr_param_value_text)
        val countView = layout.findViewById<TextView>(R.id.kr_param_count_text)

        valueView.tag = actionParamInfo.name

        setView(textView, valueView, countView)

        textView.setOnClickListener {
            openDialog(textView, valueView, countView)
        }

        return layout
    }

    private fun setView(textView: TextView, valueView: TextView, countView: TextView) {
        val resultValues = ArrayList<String?>()
        val resultLables = ArrayList<String?>()
        var count = 0
        for (index in status.indices) {
            if (status[index]) {
                values[index]?.run {
                    resultValues.add(this)
                }
                labels[index]?.run {
                    resultLables.add(this)
                }
                count++
            }
        }
        val resultValueStr = "" + resultValues.joinToString(actionParamInfo.separator)
        val resultLabelStr = if (resultLables.size > 0) "" + resultLables.joinToString("，") else ""

        textView.text = resultLabelStr
        valueView.text = resultValueStr
        countView.text = count.toString()
    }

    private fun openDialog(textView: TextView, valueView: TextView, countView: TextView) {
        options?.run {
            val items = ArrayList<SelectItem>()
            for (i in labels.indices) {
                items.add(SelectItem().apply {
                    title = "" + labels[i]
                    selected = status[i]
                })
            }
            // TODO:深色模式、浅色模式
            DialogItemChooser(true, ArrayList(items), true, object : DialogItemChooser.Callback {
                override fun onConfirm(selected: List<SelectItem>, result: BooleanArray) {
                    result.forEachIndexed { index, value ->
                        status[index] = value
                    }
                    setView(textView, valueView, countView)
                }
            }).show(context.supportFragmentManager, "params-multi-select")
        }
        /*
        options?.run {
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.kr_please_select))
                    .setMultiChoiceItems(labels, status) { _, index, isChecked ->
                        status[index] = isChecked
                    }
                    .setNeutralButton(R.string.btn_cancel) { _, _ -> }
                    .setPositiveButton(R.string.btn_confirm) { _, _ ->
                        setView(textView, valueView, countView)
                    })
        }
        */
    }
}
