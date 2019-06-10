package com.omarea.krscript.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.omarea.krscript.model.ActionInfo
import com.omarea.krscript.model.ConfigItemBase
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.executor.SimpleShellExecutor
import com.omarea.krscript.model.SwitchInfo
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.OverScrollListView
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.R
import com.omarea.krscript.config.ActionParamInfo
import java.util.*
import kotlin.collections.HashMap

class ActionListView : OverScrollListView {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    private val progressBarDialog: ProgressBarDialog

    init {
        this.progressBarDialog = ProgressBarDialog(context)
    }

    fun setListData(actionInfos: ArrayList<ConfigItemBase>?) {
        if (actionInfos != null) {
            this.overScrollMode = ListView.OVER_SCROLL_ALWAYS
            this.adapter = ActionListAdapter(actionInfos)
            this.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                val item = parent.adapter.getItem(position)
                if (item is ActionInfo) {
                    onActionClick(item, Runnable {
                        post { (adapter as ActionListAdapter).update(position, this) }
                    })
                } else if (item is SwitchInfo) {
                    onSwitchClick(item, Runnable {
                        post { (adapter as ActionListAdapter).update(position, this) }
                    })
                }
            }
        }
    }

    /**
     * 当switch项被点击
     */
    private fun onSwitchClick(switchInfo: SwitchInfo, onExit: Runnable) {
        val toValue = !switchInfo.selected
        if (switchInfo.confirm) {
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle(switchInfo.title)
                    .setMessage(switchInfo.desc)
                    .setPositiveButton("执行", { _, _ ->
                        switchExecute(switchInfo, toValue, onExit)
                    })
                    .setNegativeButton("取消", { _, _ ->
                    }))
        } else {
            switchExecute(switchInfo, toValue, onExit)
        }
    }

    /**
     * 执行switch的操作
     */
    private fun switchExecute(switchInfo: SwitchInfo, toValue: Boolean, onExit: Runnable) {
        val script = switchInfo.setState ?: return

        var startPath: String? = null
        if (switchInfo.start != null) {
            startPath = switchInfo.start
        }

        SimpleShellExecutor(context).execute(switchInfo, script, startPath, onExit, object : java.util.HashMap<String, String>() {
            init {
                put("state", if (toValue) "1" else "0")
            }
        })
    }

    /**
     * 列表项点击时（如果需要确认界面，则显示确认界面，否则直接准备执行）
     */
    private fun onActionClick(action: ActionInfo, onExit: Runnable) {
        if (action.confirm) {
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle(action.title)
                    .setMessage(action.desc)
                    .setPositiveButton("执行") { _, _ -> actionExecute(action, onExit) }
                    .setNegativeButton("取消") { _, _ -> })
        } else {
            actionExecute(action, onExit)
        }
    }

    /**
     * action执行参数界面
     */
    private fun actionExecute(action: ActionInfo, onExit: Runnable) {
        val script = action.script ?: return

        var startPath: String? = null
        if (action.start != null) {
            startPath = action.start
        }
        if (action.params != null) {
            val actionParamInfos = action.params!!
            if (actionParamInfos.size > 0) {
                val layoutInflater = LayoutInflater.from(context)
                val view = layoutInflater.inflate(R.layout.dialog_params, null)
                val linearLayout = view.findViewById<LinearLayout>(R.id.params_list)

                val handler = Handler()
                val finalStartPath = startPath
                progressBarDialog.showDialog("正在读取数据...")
                Thread(Runnable {
                    for (actionParamInfo in actionParamInfos) {
                        if (actionParamInfo.valueShell != null) {
                            actionParamInfo.valueFromShell = executeScriptGetResult(context, actionParamInfo.valueShell)
                        }
                    }
                    handler.post {
                        progressBarDialog.hideDialog()
                        for (actionParamInfo in actionParamInfos) {
                            val options = getParamOptions(actionParamInfo) // 获取参数的可用选项
                            // 下拉框渲染
                            if (options != null) {
                                val spinner = Spinner(context)
                                val selectedIndex = getParamOptionsCurrentIndex(actionParamInfo, options) // 获取当前选中项索引

                                spinner.adapter = SimpleAdapter(context, options, R.layout.list_item_text, arrayOf("title"), intArrayOf(R.id.text))
                                spinner.tag = actionParamInfo.name

                                if (!actionParamInfo.desc.isNullOrEmpty()) {
                                    val textView = TextView(context)
                                    textView.text = actionParamInfo.desc
                                    linearLayout.addView(textView)
                                    val lp = textView.layoutParams as LinearLayout.LayoutParams
                                    lp.setMargins(0, 10, 0, 0)
                                    textView.layoutParams = lp
                                    linearLayout.addView(spinner)
                                    val lp2 = spinner.layoutParams as LinearLayout.LayoutParams
                                    lp2.setMargins(0, 4, 0, 20)
                                    spinner.layoutParams = lp2
                                } else {
                                    linearLayout.addView(spinner)
                                    val lp2 = spinner.layoutParams as LinearLayout.LayoutParams
                                    lp2.setMargins(0, 10, 0, 20)
                                    spinner.layoutParams = lp2
                                }
                                if (selectedIndex > -1 && selectedIndex < options.size) {
                                    spinner.setSelection(selectedIndex)
                                }
                            }
                            // 选择框渲染
                            else if (actionParamInfo.type == "bool") {
                                val checkBox = CheckBox(context)
                                checkBox.hint = if (actionParamInfo.desc != null) actionParamInfo.desc else actionParamInfo.name
                                checkBox.isChecked = getCheckState(actionParamInfo, false)
                                checkBox.tag = actionParamInfo.name
                                linearLayout.addView(checkBox)
                                val lp = checkBox.layoutParams as LinearLayout.LayoutParams
                                lp.setMargins(0, 10, 0, 20)
                                checkBox.layoutParams = lp
                            }
                            // 文本框渲染
                            else {
                                val editText = EditText(context)
                                if (actionParamInfo.desc != null) {
                                    editText.hint = actionParamInfo.desc
                                } else {
                                    editText.hint = actionParamInfo.name
                                }
                                if (actionParamInfo.valueFromShell != null)
                                    editText.setText(actionParamInfo.valueFromShell)
                                else if (actionParamInfo.value != null)
                                    editText.setText(actionParamInfo.value)
                                editText.filters = arrayOf(ActionParamInfo.ParamInfoFilter(actionParamInfo))
                                editText.tag = actionParamInfo.name
                                linearLayout.addView(editText)
                                val lp = editText.layoutParams as LinearLayout.LayoutParams
                                lp.setMargins(0, 10, 0, 20)
                                editText.layoutParams = lp
                            }
                        }
                        DialogHelper.animDialog(AlertDialog.Builder(context)
                                .setTitle(action.title)
                                .setView(view)
                                .setPositiveButton(R.string.btn_confirm) { _, _ -> actionExecute(action, script, finalStartPath, onExit, readParamsValue(actionParamInfos, linearLayout)) })
                    }
                }).start()

                return
            }
        }
        actionExecute(action, script, startPath, onExit, null)
    }

    /**
     * 获取Param的Options
     */
    private fun getParamOptions(actionParamInfo: ActionParamInfo):ArrayList<HashMap<String, Any>>? {
        val options = ArrayList<HashMap<String, Any>>()
        var shellResult = ""
        if (!actionParamInfo.optionsSh.isNullOrEmpty()) {
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
        }
        else if (actionParamInfo.options != null) {
            for (option in actionParamInfo.options) {
                val opt = HashMap<String, Any>()
                opt["title"] = option.desc
                opt["item"] = option
                options.add(opt)
            }
        }
        else {
            return null
        }

        return options
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
            valList.add(actionParamInfo.valueFromShell)
        if (actionParamInfo.value != null) {
            valList.add(actionParamInfo.value)
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
     * 获取选中状态
     */
    private fun getCheckState(actionParamInfo: ActionParamInfo, defaultValue: Boolean): Boolean {
        if (actionParamInfo.valueFromShell != null) {
            return actionParamInfo.valueFromShell == "1" || actionParamInfo.valueFromShell.toLowerCase() == "true"
        } else if (actionParamInfo.value != null) {
            return actionParamInfo.value == "1" || actionParamInfo.value.toLowerCase() == "true"
        }
        return defaultValue
    }

    /**
     * 读取界面上填入的参数值
     */
    private fun readParamsValue(actionParamInfos: ArrayList<ActionParamInfo>, linearLayout: LinearLayout): HashMap<String, String> {
        val params = HashMap<String, String>()
        for (actionParamInfo in actionParamInfos) {
            val view = linearLayout.findViewWithTag<View>(actionParamInfo.name)
            if (view is EditText) {
                actionParamInfo.value = view.text.toString()
            } else if (view is CheckBox) {
                actionParamInfo.value = if (view.isChecked) "1" else "0"
            } else if (view is Spinner) {
                val item = view.selectedItem
                if (item is HashMap<*, *>) {
                    val opt = item["item"] as ActionParamInfo.ActionParamOption
                    actionParamInfo.value = opt.value
                } else
                    actionParamInfo.value = item.toString()
            }
            params[actionParamInfo.name] = actionParamInfo.value
        }
        return params
    }

    private fun executeScriptGetResult(context: Context, shellScript: String): String {
        return ScriptEnvironmen.executeResultRoot(context, shellScript);
    }

    private fun actionExecute(configItem: ConfigItemBase, script: String, startPath: String?, onExit: Runnable, params: HashMap<String, String>?) {
        SimpleShellExecutor(context).execute(configItem, script, startPath, onExit, params)
    }
}
