package com.omarea.krscript.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.OverScrollListView
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.R
import com.omarea.krscript.config.ActionParamInfo
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.executor.SimpleShellExecutor
import com.omarea.krscript.model.*

class ActionListView : OverScrollListView {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    private val progressBarDialog: ProgressBarDialog
    private var actionShortClickHandler: ActionShortClickHandler? = null

    init {
        this.progressBarDialog = ProgressBarDialog(context)
    }

    fun setListData(
            actionInfos: ArrayList<ConfigItemBase>?,
            actionShortClickHandler: ActionShortClickHandler? = null,
            actionLongClickHandler: ActionLongClickHandler? = null) {
        if (actionInfos != null) {
            this.actionShortClickHandler = actionShortClickHandler
            this.overScrollMode = ListView.OVER_SCROLL_ALWAYS
            this.adapter = ActionListAdapter(actionInfos)
            this.onItemClickListener = OnItemClickListener { parent, _, position, _ ->
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
            this.setOnItemLongClickListener { parent, view, position, id ->
                if (actionLongClickHandler != null) {
                    val item = parent.adapter.getItem(position)
                    if (item is ActionInfo) {
                        actionLongClickHandler.addToFavorites(item)
                    } else if (item is SwitchInfo) {
                        actionLongClickHandler.addToFavorites(item)
                    }
                    true
                } else {
                    false
                }
            }
        }
    }

    fun triggerAction(id: String, onCompleted: Runnable): Boolean {
        val actionListAdapter = (adapter as ActionListAdapter)
        val position = actionListAdapter.getPositionById(id)
        if (position < 0) {
            return false
        } else {
            val item = actionListAdapter.getItem(position)
            if (item is ActionInfo) {
                onActionClick(item, Runnable {
                    post {
                        (adapter as ActionListAdapter).update(position, this)
                        onCompleted.run()
                    }
                })
            } else if (item is SwitchInfo) {
                onSwitchClick(item, Runnable {
                    post {
                        (adapter as ActionListAdapter).update(position, this)
                        onCompleted.run()
                    }
                })
            }
            return true
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
                    .setPositiveButton(context.getString(R.string.btn_execute), { _, _ ->
                        switchExecute(switchInfo, toValue, onExit)
                    })
                    .setNegativeButton(context.getString(R.string.btn_cancel), { _, _ ->
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

        actionExecute(switchInfo, script, onExit, object : java.util.HashMap<String, String>() {
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
                    .setPositiveButton(context.getString(R.string.btn_execute)) { _, _ ->
                        actionExecute(action, onExit)
                    }
                    .setNegativeButton(context.getString(R.string.btn_cancel)) { _, _ -> })
        } else {
            actionExecute(action, onExit)
        }
    }


    /**
     * action执行参数界面
     */
    private fun actionExecute(action: ActionInfo, onExit: Runnable) {
        val script = action.script ?: return

        if (action.params != null) {
            val actionParamInfos = action.params!!
            if (actionParamInfos.size > 0) {
                val layoutInflater = LayoutInflater.from(context)
                val linearLayout = layoutInflater.inflate(R.layout.kr_params_list, null) as LinearLayout

                val handler = Handler()
                progressBarDialog.showDialog(context.getString(R.string.onloading))
                Thread(Runnable {
                    for (actionParamInfo in actionParamInfos) {
                        handler.post {
                            progressBarDialog.showDialog(context.getString(R.string.kr_param_load) + if (!actionParamInfo.label.isNullOrEmpty()) actionParamInfo.label else actionParamInfo.name)
                        }
                        if (actionParamInfo.valueShell != null) {
                            actionParamInfo.valueFromShell = executeScriptGetResult(context, actionParamInfo.valueShell!!)
                        }
                        handler.post {
                            progressBarDialog.showDialog(context.getString(R.string.kr_param_options_load) + if (!actionParamInfo.label.isNullOrEmpty()) actionParamInfo.label else actionParamInfo.name)
                        }
                        actionParamInfo.optionsFromShell = getParamOptions(actionParamInfo) // 获取参数的可用选项
                    }
                    handler.post {
                        progressBarDialog.showDialog(context.getString(R.string.kr_params_render))
                    }
                    handler.post {
                        val render = LayoutRender(linearLayout)
                        render.renderList(actionParamInfos)
                        progressBarDialog.hideDialog()
                        if (actionShortClickHandler != null && actionShortClickHandler!!.onParamsView(action,
                                        linearLayout,
                                        Runnable { },
                                        Runnable {
                                            try {
                                                val params = render.readParamsValue(actionParamInfos)
                                                actionExecute(action, script, onExit, params)
                                            } catch (ex: java.lang.Exception) {
                                                Toast.makeText(context, "" + ex.message, Toast.LENGTH_LONG).show()
                                            }
                                        })) {
                        } else {
                            val dialogView = layoutInflater.inflate(R.layout.dialog_params, null)
                            dialogView.findViewById<ScrollView>(R.id.kr_param_dialog).addView(linearLayout)
                            dialogView.findViewById<TextView>(R.id.kr_param_dialog_title).setText(action.title)
                            val dialog = DialogHelper.animDialog(AlertDialog.Builder(context).setView(dialogView))

                            dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                                dialog!!.dismiss()
                            }
                            dialogView.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
                                try {
                                    val params = render.readParamsValue(actionParamInfos)
                                    dialog!!.dismiss()
                                    actionExecute(action, script, onExit, params)
                                } catch (ex: java.lang.Exception) {
                                    Toast.makeText(context, "" + ex.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }).start()

                return
            }
        }
        actionExecute(action, script, onExit, null)
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

    private fun executeScriptGetResult(context: Context, shellScript: String): String {
        return ScriptEnvironmen.executeResultRoot(context, shellScript);
    }

    private fun actionExecute(configItem: ConfigItemBase, script: String, onExit: Runnable, params: HashMap<String, String>?) {
        var shellHandler: ShellHandlerBase? = null
        if (actionShortClickHandler != null) {
            shellHandler = actionShortClickHandler!!.onExecute(configItem, onExit)
        }

        SimpleShellExecutor(context).execute(configItem, script, onExit, params, shellHandler)
    }
}
