package com.omarea.krscript.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.omarea.krscript.R
import com.omarea.krscript.config.ActionParamInfo
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.ParamInfoFilter
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.dropLastWhile
import kotlin.collections.get
import kotlin.collections.indices
import kotlin.collections.set
import kotlin.collections.toTypedArray

class LayoutRender {
    private var linearLayout: LinearLayout
    private var context: Context

    constructor(linearLayout: LinearLayout) {
        this.linearLayout = linearLayout
        this.context = linearLayout.context
    }

    fun renderList(actionParamInfos: ArrayList<ActionParamInfo>) {
        for (actionParamInfo in actionParamInfos) {
            val options = getParamOptions(actionParamInfo) // 获取参数的可用选项
            // 下拉框渲染
            if (options != null) {
                val spinner = Spinner(context)
                val selectedIndex = getParamOptionsCurrentIndex(actionParamInfo, options) // 获取当前选中项索引

                spinner.adapter = SimpleAdapter(context, options, R.layout.list_item_text, arrayOf("title"), intArrayOf(R.id.text))
                spinner.isEnabled = !actionParamInfo.readonly

                addToLayout(spinner, actionParamInfo)
                if (selectedIndex > -1 && selectedIndex < options.size) {
                    spinner.setSelection(selectedIndex)
                }
            }
            // 选择框渲染
            else if (actionParamInfo.type == "bool") {
                val checkBox = CheckBox(context)
                checkBox.hint = if (actionParamInfo.title != null) actionParamInfo.title else actionParamInfo.name
                checkBox.isChecked = getCheckState(actionParamInfo, false)
                checkBox.isEnabled = !actionParamInfo.readonly
                addToLayout(checkBox, actionParamInfo)
            }
            // 滑块
            else if (actionParamInfo.type == "seekbar") {
                val seekbar = SeekBarRender(actionParamInfo, context).render()

                addToLayout(seekbar, actionParamInfo)
                seekbar.tag = null
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
                if (actionParamInfo.type == "int" || actionParamInfo.type == "number" && (actionParamInfo.min != Int.MIN_VALUE || actionParamInfo.min != Int.MAX_VALUE)) {
                    editText.hint = "${actionParamInfo.min} ~ ${actionParamInfo.max}"
                }

                addToLayout(editText, actionParamInfo)
            }
        }
    }

    private fun addToLayout(inputView: View, actionParamInfo: ActionParamInfo) {
        inputView.tag = actionParamInfo.name

        val layout = LayoutInflater.from(context).inflate(R.layout.layout_param_row, null)
        if (!actionParamInfo.title.isNullOrEmpty() && actionParamInfo.type != "bool") {
            layout.findViewById<TextView>(R.id.kr_param_title).text = actionParamInfo.title
        } else {
            layout.findViewById<TextView>(R.id.kr_param_title).visibility = View.GONE
        }

        if (!actionParamInfo.desc.isNullOrEmpty()) {
            layout.findViewById<TextView>(R.id.kr_param_desc).text = actionParamInfo.desc
        } else {
            layout.findViewById<TextView>(R.id.kr_param_desc).visibility = View.GONE
        }

        layout.findViewById<FrameLayout>(R.id.kr_param_input).addView(inputView)
        linearLayout.addView(layout)
        (layout.layoutParams as LinearLayout.LayoutParams).topMargin = dp2px(context, 1f)

        (inputView.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER_VERTICAL
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
                if ((actionParamInfo.type == "int" || actionParamInfo.type == "number") && text.isNotEmpty()) {
                    try {
                        val value = text.toInt()
                        if (value < actionParamInfo.min) {
                            throw Exception("[${actionParamInfo.desc}] ${value} < ${actionParamInfo.min} !!!")
                        } else if (value > actionParamInfo.max) {
                            throw Exception("[${actionParamInfo.desc}] ${value} > ${actionParamInfo.max} !!!")
                        }
                    } catch (ex: java.lang.NumberFormatException) {
                    }
                }
                actionParamInfo.value = text
            } else if (view is CheckBox) {
                actionParamInfo.value = if (view.isChecked) "1" else "0"
            } else if (view is SeekBar) {
                val text = (view.progress + actionParamInfo.min).toString()
                actionParamInfo.value = text
            } else if (view is Spinner) {
                val item = view.selectedItem
                if (item is HashMap<*, *>) {
                    val opt = item["item"] as ActionParamInfo.ActionParamOption
                    actionParamInfo.value = opt.value
                } else
                    actionParamInfo.value = item.toString()
            }

            if (actionParamInfo.value.isNullOrEmpty()) {
                if (actionParamInfo.required) {
                    throw Exception("${actionParamInfo.desc} ${actionParamInfo.name}" + context.getString(R.string.do_not_empty))
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
     * 获取当前选中项索引
     * @param ActionParamInfo actionParamInfo 参数信息
     * @param ArrayList<HashMap<String, Any>> options 使用getParamOptions获得的数据（不为空时）
     */
    private fun getParamOptionsCurrentIndex(actionParamInfo: ActionParamInfo, options: ArrayList<HashMap<String, Any>>): Int {
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
     * 获取Param的Options
     */
    private fun getParamOptions(actionParamInfo: ActionParamInfo): ArrayList<HashMap<String, Any>>? {
        val options = ArrayList<HashMap<String, Any>>()
        var shellResult = ""
        if (!actionParamInfo.optionsSh.isEmpty()) {
            shellResult = executeScriptGetResult(context, actionParamInfo.optionsSh)
        }

        if (!(shellResult == "error" || shellResult == "null" || shellResult.isEmpty())) {
            for (item in shellResult.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (item.contains("|")) {
                    val itemSplit = item.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    options.add(object : HashMap<String, Any>() {
                        init {
                            put("title", itemSplit[1])
                            put("item", object : ActionParamInfo.ActionParamOption() {
                                init {
                                    value = itemSplit[0]
                                    desc = itemSplit[1]
                                }
                            })
                        }
                    })
                } else {
                    options.add(object : HashMap<String, Any>() {
                        init {
                            put("title", item)
                            put("item", object : ActionParamInfo.ActionParamOption() {
                                init {
                                    value = item
                                    desc = item
                                }
                            })
                        }
                    })
                }
            }
        } else if (actionParamInfo.options != null) {
            for (option in actionParamInfo.options!!) {
                val opt = HashMap<String, Any>()
                opt.set("title", if (option.desc == null) "" else option.desc!!)
                opt["item"] = option
                options.add(opt)
            }
        } else {
            return null
        }

        return options
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
