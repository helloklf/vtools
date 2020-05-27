package com.omarea.krscript.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.omarea.krscript.R
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.ActionParamInfo
import com.omarea.krscript.model.ParamInfoFilter

class ActionParamsLayoutRender {
    companion object {
        /**
         * 获取当前选中项索引（单选）
         * @param ActionParamInfo actionParamInfo 参数信息
         * @param ArrayList<HashMap<String, Any>> options 使用getParamOptions获得的数据（不为空时）
         */
        fun getParamOptionsCurrentIndex(actionParamInfo: ActionParamInfo, options: ArrayList<HashMap<String, Any>>): Int {
            var selectedIndex = -1
            var index = 0

            val valList = ArrayList<String>()
            if (actionParamInfo.valueFromShell != null)
                valList.add(actionParamInfo.valueFromShell!!)
            if (actionParamInfo.value != null) {
                valList.add(actionParamInfo.value!!)
            }
            if (valList.size > 0) {
                for (j in valList.indices) {
                    for (option in options) {
                        if ((option["item"] as ActionParamInfo.ActionParamOption).value == valList[j]) {
                            selectedIndex = index
                            break
                        }
                        index++
                    }
                    if (selectedIndex > -1)
                        break
                }
            }
            return selectedIndex
        }

        /**
         * 获取当前选中项索引（多选）
         * @param ActionParamInfo actionParamInfo 参数信息
         * @param ArrayList<HashMap<String, Any>> options 使用getParamOptions获得的数据（不为空时）
         */
        fun getParamOptionsSelectedStatus(actionParamInfo: ActionParamInfo, options: ArrayList<HashMap<String, Any>>): BooleanArray {
            val status = BooleanArray(options.size)
            val value = if (actionParamInfo.valueFromShell != null) actionParamInfo.valueFromShell else actionParamInfo.value
            val values = value?.split(if (actionParamInfo.separator.isNotEmpty()) actionParamInfo.separator else "\n")

            for (index in 0 until options.size) {
                val item = options[index]["item"]
                val option = item as ActionParamInfo.ActionParamOption
                status[index] = (values != null && values.contains(option.value))
            }
            return status
        }
    }

    private var linearLayout: LinearLayout
    private var context: Context

    constructor(linearLayout: LinearLayout) {
        this.linearLayout = linearLayout
        this.context = linearLayout.context
    }

    fun renderList(actionParamInfos: ArrayList<ActionParamInfo>, fileChooser: FileChooserRender.FileChooserInterface?) {
        for (actionParamInfo in actionParamInfos) {
            val options = actionParamInfo.optionsFromShell
            // 下拉框渲染
            if (options != null) {
                if (actionParamInfo.multiple) {
                    val view = ParamsMultipleSelect(actionParamInfo, context).render()
                    addToLayout(view, actionParamInfo, false)
                } else {
                    val spinner = Spinner(context)
                    val selectedIndex = getParamOptionsCurrentIndex(actionParamInfo, options) // 获取当前选中项索引

                    spinner.adapter = SimpleAdapter(context, options, R.layout.kr_simple_text_list_item, arrayOf("title"), intArrayOf(R.id.text))
                    spinner.isEnabled = !actionParamInfo.readonly

                    addToLayout(spinner, actionParamInfo)
                    if (selectedIndex > -1 && selectedIndex < options.size) {
                        spinner.setSelection(selectedIndex)
                    }
                }
            }
            // 选择框渲染
            else if (actionParamInfo.type == "bool" || actionParamInfo.type == "checkbox") {
                val checkBox = CheckBox(context)
                checkBox.isChecked = getCheckState(actionParamInfo, false)
                checkBox.isEnabled = !actionParamInfo.readonly
                if (!actionParamInfo.label.isNullOrEmpty()) {
                    checkBox.text = actionParamInfo.label
                }
                addToLayout(checkBox, actionParamInfo)
            }
            // 开关渲染
            else if (actionParamInfo.type == "switch") {
                val switch = Switch(context)
                switch.isChecked = getCheckState(actionParamInfo, false)
                switch.isEnabled = !actionParamInfo.readonly
                if (!actionParamInfo.label.isNullOrEmpty()) {
                    switch.text = actionParamInfo.label
                }
                switch.setPadding(dp2px(context, 8f), 0, 0, 0)
                addToLayout(switch, actionParamInfo)
            }
            // 滑块
            else if (actionParamInfo.type == "seekbar") {
                val layout = SeekBarRender(actionParamInfo, context).render()

                addToLayout(layout, actionParamInfo, false)
            }
            // 文件选择
            else if (actionParamInfo.type == "file") {
                val layout = FileChooserRender(actionParamInfo, context, fileChooser).render()

                addToLayout(layout, actionParamInfo, false)
            }
            // 颜色输入
            else if (actionParamInfo.type == "color") {
                val layout = ParamsColorPicker(actionParamInfo, context).render()

                addToLayout(layout, actionParamInfo, false)
            }
            // 文本框渲染
            else {
                val editText = EditText(context)
                if (actionParamInfo.valueFromShell != null)
                    editText.setText(actionParamInfo.valueFromShell)
                else if (actionParamInfo.value != null)
                    editText.setText(actionParamInfo.value)
                editText.background = ColorDrawable(Color.TRANSPARENT)
                editText.filters = arrayOf(ParamInfoFilter(actionParamInfo))
                editText.isEnabled = !actionParamInfo.readonly
                editText.setPadding(dp2px(context, 8f), 0, dp2px(context, 8f), 0)
                if (actionParamInfo.placeholder.isNotEmpty()) {
                    editText.hint = actionParamInfo.placeholder
                } else if (
                        (actionParamInfo.type == "int" || actionParamInfo.type == "number")
                        &&
                        (actionParamInfo.min != Int.MIN_VALUE || actionParamInfo.max != Int.MAX_VALUE)
                ) {
                    editText.hint = "${actionParamInfo.min} ~ ${actionParamInfo.max}"
                }

                addToLayout(editText, actionParamInfo)
            }
        }
    }

    private fun addToLayout(inputView: View, actionParamInfo: ActionParamInfo, setTag: Boolean = true) {
        if (setTag) {
            inputView.tag = actionParamInfo.name
        }

        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_row, null)
        if (!actionParamInfo.title.isNullOrEmpty()) {
            layout.findViewById<TextView>(R.id.kr_param_title).text = actionParamInfo.title
        } else {
            layout.findViewById<TextView>(R.id.kr_param_title).visibility = View.GONE
        }

        if (!actionParamInfo.label.isNullOrEmpty() && (actionParamInfo.type != "bool" && actionParamInfo.type != "checkbox" && actionParamInfo.type != "switch")) {
            layout.findViewById<TextView>(R.id.kr_param_label).text = actionParamInfo.label
        } else {
            layout.findViewById<TextView>(R.id.kr_param_label).visibility = View.GONE
        }

        if (!actionParamInfo.desc.isNullOrEmpty()) {
            layout.findViewById<TextView>(R.id.kr_param_desc).text = actionParamInfo.desc
        } else {
            layout.findViewById<TextView>(R.id.kr_param_desc).visibility = View.GONE
        }

        layout.findViewById<FrameLayout>(R.id.kr_param_input).addView(inputView)
        linearLayout.addView(layout)
        // (layout.layoutParams as LinearLayout.LayoutParams).topMargin = dp2px(context, 1f)

        (inputView.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER_VERTICAL
    }

    private fun getFieldTips(actionParamInfo: ActionParamInfo): String {
        val tips = StringBuilder()
        if (!actionParamInfo.title.isNullOrEmpty()) {
            tips.append(actionParamInfo.title)
            tips.append(" ")
        }
        if (!actionParamInfo.label.isNullOrEmpty()) {
            tips.append(actionParamInfo.label)
            tips.append(" ")
        }
        tips.append("(")
        tips.append(actionParamInfo.name)
        tips.append(") ")
        return tips.toString()
    }

    /**
     * 读取界面上填入的参数值
     */
    fun readParamsValue(actionParamInfos: ArrayList<ActionParamInfo>): HashMap<String, String> {
        val params = HashMap<String, String>()
        for (actionParamInfo in actionParamInfos) {
            if (actionParamInfo.name == null) {
                continue
            }

            val view = linearLayout.findViewWithTag<View>(actionParamInfo.name)
            if (view is EditText) {
                val text = view.text.toString()
                if (text.isNotEmpty()) {
                    if ((actionParamInfo.type == "int" || actionParamInfo.type == "number")) {
                        try {
                            val value = text.toInt()
                            if (value < actionParamInfo.min) {
                                throw Exception("${getFieldTips(actionParamInfo)} ${value} < ${actionParamInfo.min} !!!")
                            } else if (value > actionParamInfo.max) {
                                throw Exception("${getFieldTips(actionParamInfo)} ${value} > ${actionParamInfo.max} !!!")
                            }
                        } catch (ex: java.lang.NumberFormatException) {
                        }
                    } else if (actionParamInfo.type == "color") {
                        try {
                            Color.parseColor(text)
                        } catch (ex: java.lang.Exception) {
                            throw Exception("" + getFieldTips(actionParamInfo) + "  \n" + context.getString(R.string.kr_invalid_color))
                        }
                    }
                }
                actionParamInfo.value = text
            } else if (view is CheckBox) {
                actionParamInfo.value = if (view.isChecked) "1" else "0"
            } else if (view is Switch) {
                actionParamInfo.value = if (view.isChecked) "1" else "0"
            } else if (view is SeekBar) {
                val text = (view.progress + actionParamInfo.min).toString()
                actionParamInfo.value = text
            } else if (view is TextView) {
                actionParamInfo.value = view.text.toString()
            } else if (view is Spinner) {
                val item = view.selectedItem
                when {
                    item is HashMap<*, *> -> {
                        val opt = item["item"] as ActionParamInfo.ActionParamOption
                        actionParamInfo.value = opt.value
                    }
                    item != null -> actionParamInfo.value = item.toString()
                    else -> actionParamInfo.value = ""
                }
            }

            if (actionParamInfo.value.isNullOrEmpty()) {
                if (actionParamInfo.required) {
                    throw Exception(getFieldTips(actionParamInfo) + context.getString(R.string.do_not_empty))
                } else {
                    params.set(actionParamInfo.name!!, "")
                }
            } else {
                params.set(actionParamInfo.name!!, actionParamInfo.value!!)
            }
        }
        return params
    }

    /**
     * TODO:刷新界面上的参数输入框显示
     */
    fun updateParamsView(actionParamInfos: ArrayList<ActionParamInfo>) {
        for (actionParamInfo in actionParamInfos) {
            if (actionParamInfo.name == null) {
                continue
            }

            val view = linearLayout.findViewWithTag<View>(actionParamInfo.name)
            if (view != null) {
                // TODO:刷新界面显示
            }
        }
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

    /**
     * dp转换成px
     */
    private fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    private fun executeScriptGetResult(context: Context, shellScript: String): String {
        return ScriptEnvironmen.executeResultRoot(context, shellScript);
    }
}
