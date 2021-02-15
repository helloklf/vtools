package com.omarea.krscript.ui

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.fragment.app.FragmentActivity
import com.omarea.common.model.SelectItem
import com.omarea.krscript.R
import com.omarea.krscript.model.ActionParamInfo

class ActionParamsLayoutRender(private var linearLayout: LinearLayout, activity: FragmentActivity) {
    companion object {
        /**
         * 获取当前选中项索引（单选）
         * @param ActionParamInfo actionParamInfo 参数信息
         * @param ArrayList<HashMap<String, Any>> options 使用getParamOptions获得的数据（不为空时）
         */
        fun getParamOptionsCurrentIndex(actionParamInfo: ActionParamInfo, options: ArrayList<SelectItem>): Int {
            var selectedIndex = -1

            val valList = ArrayList<String>()
            if (actionParamInfo.valueFromShell != null)
                valList.add(actionParamInfo.valueFromShell!!)
            // TODO:这里可能有点争议
            if (actionParamInfo.value != null) {
                valList.add(actionParamInfo.value!!)
            }
            if (valList.size > 0) {
                for (j in valList.indices) {
                    var index = 0
                    for (option in options) {
                        if (option.value == valList[j]) {
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
        fun getParamOptionsSelectedStatus(actionParamInfo: ActionParamInfo, options: ArrayList<SelectItem>): BooleanArray {
            val status = BooleanArray(options.size)
            val values = getParamValues(actionParamInfo)

            for (index in 0 until options.size) {
                val option = options[index]
                status[index] = (values != null && values.contains(option.value))
            }
            return status
        }

        /**
         * 设置列表的选中状态
         * @param ActionParamInfo actionParamInfo 参数信息
         * @param ArrayList<HashMap<String, Any>> options 使用getParamOptions获得的数据（不为空时）
         */
        fun setParamOptionsSelectedStatus(actionParamInfo: ActionParamInfo, options: ArrayList<SelectItem>): ArrayList<SelectItem> {
            val values = getParamValues(actionParamInfo)

            for (index in 0 until options.size) {
                val option = options[index]
                options[index].selected = (values != null && values.contains(option.value))
            }
            return options
        }

        // 获取多选下拉的选中值列表
        fun getParamValues (actionParamInfo: ActionParamInfo): List<String>? {
            val value = if (actionParamInfo.valueFromShell != null) actionParamInfo.valueFromShell else actionParamInfo.value
            val values = value?.split(actionParamInfo.separator)
            return values
        }
    }

    private var context: FragmentActivity = activity

    fun renderList(actionParamInfos: ArrayList<ActionParamInfo>, fileChooser: ParamsFileChooserRender.FileChooserInterface?) {
        for (actionParamInfo in actionParamInfos) {
            val options = actionParamInfo.optionsFromShell
            // 下拉框渲染
            if (options != null && !(actionParamInfo.type == "app" || actionParamInfo.type == "packages")) {
                if (actionParamInfo.multiple) {
                    val view = ParamsMultipleSelect(actionParamInfo, context).render()
                    addToLayout(view, actionParamInfo)
                } else {
                    addToLayout(ParamsSingleSelect(actionParamInfo, context).render(), actionParamInfo)
                }
            }
            // 选择框渲染
            else if (actionParamInfo.type == "bool" || actionParamInfo.type == "checkbox") {
                addToLayout(ParamsCheckbox(actionParamInfo, context).render(), actionParamInfo)
            }
            // 开关渲染
            else if (actionParamInfo.type == "switch") {
                addToLayout(ParamsSwitch(actionParamInfo, context).render(), actionParamInfo)
            }
            // 滑块
            else if (actionParamInfo.type == "seekbar") {
                val layout = ParamsSeekBar(actionParamInfo, context).render()

                addToLayout(layout, actionParamInfo)
            }
            // 文件选择
            else if (actionParamInfo.type == "file" || actionParamInfo.type == "folder") {
                val layout = ParamsFileChooserRender(actionParamInfo, context, fileChooser).render()

                addToLayout(layout, actionParamInfo)
            }
            // 应用选择
            else if (actionParamInfo.type == "app" || actionParamInfo.type == "packages") {
                val layout = ParamsAppChooserRender(actionParamInfo, context).render()

                addToLayout(layout, actionParamInfo)
            }
            // 颜色输入
            else if (actionParamInfo.type == "color") {
                val layout = ParamsColorPicker(actionParamInfo, context).render()

                addToLayout(layout, actionParamInfo)
            }
            // 文本框渲染
            else {
                addToLayout(ParamsEditText(actionParamInfo, context).render(), actionParamInfo)
            }
        }
    }

    // 隐藏label的参数类型
    private val hideLabelTypes = arrayOf("bool", "checkbox", "switch")
    private fun addToLayout(inputView: View, actionParamInfo: ActionParamInfo) {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_row, null)
        if (!actionParamInfo.title.isNullOrEmpty()) {
            layout.findViewById<TextView>(R.id.kr_param_title).text = actionParamInfo.title
        } else {
            layout.findViewById<TextView>(R.id.kr_param_title).visibility = View.GONE
        }

        if ((!actionParamInfo.label.isNullOrEmpty()) && !hideLabelTypes.contains(actionParamInfo.type)) {
            layout.findViewById<TextView>(R.id.kr_param_label).run {
                text = actionParamInfo.label
            }
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
                    item is SelectItem -> {
                        actionParamInfo.value = item.value
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
}
