package com.omarea.scripts.action

import android.os.Handler
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.*
import com.omarea.scripts.ActionAdapter
import com.omarea.scripts.simple.shell.SimpleShellExecutor
import com.omarea.shell.KeepShellPublic
import com.omarea.ui.OverScrollListView
import com.omarea.ui.ProgressBarDialog
import com.omarea.vboot.R
import java.util.*

class ActionListConfig(private val context: FragmentActivity) {
    private var listView: OverScrollListView? = null
    private val progressBarDialog: ProgressBarDialog

    init {
        this.progressBarDialog = ProgressBarDialog(context)
    }

    fun setListData(actionInfos: ArrayList<ActionInfo>?) {
        if (actionInfos != null) {
            listView = context.findViewById(R.id.list_actions)
            assert(listView != null)
            listView!!.overScrollMode = ListView.OVER_SCROLL_ALWAYS
            listView!!.adapter = ActionAdapter(actionInfos)
            listView!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                onActionClick(parent.adapter.getItem(position) as ActionInfo, Runnable {
                    if (listView != null) {
                        listView!!.post { (listView!!.adapter as ActionAdapter).update(position, listView) }
                    }
                })
            }
        }
    }

    private fun onActionClick(action: ActionInfo, onExit: Runnable) {
        if (action.confirm) {
            AlertDialog.Builder(context)
                    .setTitle(action.title)
                    .setMessage(action.desc)
                    .setPositiveButton("执行") { dialog, which -> executeScript(action, onExit) }
                    .setNegativeButton("取消") { dialog, which -> }
                    .create()
                    .show()
        } else {
            executeScript(action, onExit)
        }
    }

    private fun executeScript(action: ActionInfo, onExit: Runnable) {
        val script = action.script ?: return
        val cmds = StringBuilder()
        cmds.append(script)

        var startPath = context.filesDir.absolutePath
        if (action.start != null) {
            startPath = action.start
        }
        if (action.params != null) {
            val actionParamInfos = action.params
            if (actionParamInfos.size > 0) {
                val layoutInflater = context.layoutInflater
                val view = layoutInflater.inflate(R.layout.dialog_params, null)
                val linearLayout = view.findViewById<LinearLayout>(R.id.params_list)

                val handler = Handler()
                val finalStartPath = startPath
                progressBarDialog.showDialog("正在读取数据...")
                Thread(Runnable {
                    for (actionParamInfo in actionParamInfos) {
                        if (actionParamInfo.valueSUShell != null) {
                            actionParamInfo.valueFromShell = KeepShellPublic.doCmdSync(actionParamInfo.valueSUShell)
                        } else if (actionParamInfo.valueShell != null) {
                            actionParamInfo.valueFromShell = KeepShellPublic.doCmdSync(actionParamInfo.valueShell)
                        }
                    }
                    handler.post {
                        progressBarDialog.hideDialog()
                        for (actionParamInfo in actionParamInfos) {
                            if (actionParamInfo.options != null && actionParamInfo.options.size > 0 || actionParamInfo.optionsSh != null && !actionParamInfo.optionsSh.isEmpty() || actionParamInfo.optionsSU != null && !actionParamInfo.optionsSU.isEmpty()) {
                                if (actionParamInfo.options == null)
                                    actionParamInfo.options = ArrayList<ActionParamInfo.ActionParamOption>()
                                val spinner = Spinner(context)
                                val options = ArrayList<HashMap<String, Any>>()
                                var selectedIndex = -1
                                var index = 0
                                val valList = ArrayList<String>()

                                if (actionParamInfo.optionsSh != null && !actionParamInfo.optionsSh.isEmpty() || actionParamInfo.optionsSU != null && !actionParamInfo.optionsSU.isEmpty()) {
                                    var shellResult: String? = ""
                                    if (actionParamInfo.optionsSh != null && !actionParamInfo.optionsSh.isEmpty()) {
                                        shellResult = KeepShellPublic.doCmdSync(actionParamInfo.optionsSh)
                                    } else {
                                        shellResult = KeepShellPublic.doCmdSync(actionParamInfo.optionsSU)
                                    }
                                    if (shellResult != "error" && !shellResult.isEmpty()) {
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
                                    } else {
                                        for (option in actionParamInfo.options) {
                                            val opt = HashMap<String, Any>()
                                            opt["title"] = option.desc
                                            opt["item"] = option
                                            options.add(opt)
                                        }
                                    }
                                } else {
                                    for (option in actionParamInfo.options) {
                                        val opt = HashMap<String, Any>()
                                        opt["title"] = option.desc
                                        opt["item"] = option
                                        options.add(opt)
                                    }
                                }

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
                                spinner.adapter = SimpleAdapter(context, options, R.layout.string_item, arrayOf("title"), intArrayOf(R.id.text))
                                spinner.tag = actionParamInfo
                                if (options.size > 6) {
                                    //TODO:列表过长时切换为弹窗
                                }
                                if (actionParamInfo.desc != null && !actionParamInfo.desc.isEmpty()) {
                                    spinner.prompt = actionParamInfo.desc
                                }
                                if (actionParamInfo.desc != null && !actionParamInfo.desc.isEmpty()) {
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
                            } else if (actionParamInfo.type != null && actionParamInfo.type == "bool") {
                                val checkBox = CheckBox(context)
                                checkBox.hint = if (actionParamInfo.desc != null) actionParamInfo.desc else actionParamInfo.name

                                if (actionParamInfo.valueFromShell != null)
                                    checkBox.isChecked = actionParamInfo.valueFromShell == "1" || actionParamInfo.valueFromShell.toLowerCase() == "true"
                                else if (actionParamInfo.value != null)
                                    checkBox.isChecked = actionParamInfo.value == "1" || actionParamInfo.value.toLowerCase() == "true"

                                checkBox.tag = actionParamInfo
                                linearLayout.addView(checkBox)
                                val lp = checkBox.layoutParams as LinearLayout.LayoutParams
                                lp.setMargins(0, 10, 0, 20)
                                checkBox.layoutParams = lp
                            } else {
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
                                editText.tag = actionParamInfo
                                linearLayout.addView(editText)
                                val lp = editText.layoutParams as LinearLayout.LayoutParams
                                lp.setMargins(0, 10, 0, 20)
                                editText.layoutParams = lp
                            }
                        }
                        AlertDialog.Builder(context)
                                .setTitle(action.title)
                                .setView(view)
                                .setPositiveButton("确定") { dialog, which -> executeScript(action.title, action.root, cmds, finalStartPath, onExit, readInput(actionParamInfos, linearLayout, cmds, action)) }
                                .create()
                                .show()
                    }
                }).start()


                return
            }
        }
        cmds.append("\n\n")
        executeScript(action.title, action.root, cmds, startPath, onExit, null)
    }

    private fun readInput(actionParamInfos: ArrayList<ActionParamInfo>, linearLayout: LinearLayout, cmds: StringBuilder, actionInfo: ActionInfo): HashMap<String, String> {
        val params = HashMap<String, String>()
        for (actionParamInfo in actionParamInfos) {
            val view = linearLayout.findViewWithTag<View>(actionParamInfo)
            if (view is EditText) {
                actionParamInfo.value = view.text.toString()
            } else if (view is CheckBox) {
                actionParamInfo.value = if (view.isChecked) "1" else "0"
            } else if (view is Spinner) {
                val item = view.selectedItem
                if (item is HashMap<*, *>) {
                    val opt = (item as HashMap<String, Any>)["item"] as ActionParamInfo.ActionParamOption
                    actionParamInfo.value = opt.value
                } else
                    actionParamInfo.value = item.toString()
            }
            cmds.insert(0, actionParamInfo.name + "=\"" + actionParamInfo.value.replace("\"".toRegex(), " ") + "\"\n")
            if (actionInfo.scriptType == ActionInfo.ActionScript.ASSETS_FILE) {
                cmds.append(" $")
                cmds.append(actionParamInfo.name)
            }
            params[actionParamInfo.name] = actionParamInfo.value
        }
        cmds.append("\n\n")
        return params
    }

    private fun executeScript(title: String, root: Boolean?, cmds: StringBuilder, startPath: String, onExit: Runnable, params: HashMap<String, String>?) {
        SimpleShellExecutor(context, context.window).execute(root, title, cmds, startPath, onExit, params)
    }
}
