package com.omarea.krscript.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.model.SelectItem
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.DialogItemChooser
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.common.ui.ThemeMode
import com.omarea.krscript.BgTaskThread
import com.omarea.krscript.HiddenTaskThread
import com.omarea.krscript.R
import com.omarea.krscript.TryOpenActivity
import com.omarea.krscript.config.IconPathAnalysis
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.*
import com.omarea.krscript.shortcut.ActionShortcutManager

class ActionListFragment : androidx.fragment.app.Fragment(), PageLayoutRender.OnItemClickListener {
    companion object {
        fun create(
                actionInfos: ArrayList<NodeInfoBase>?,
                krScriptActionHandler: KrScriptActionHandler? = null,
                autoRunTask: AutoRunTask? = null,
                themeMode: ThemeMode? = null): ActionListFragment {
            val fragment = ActionListFragment()
            fragment.setListData(actionInfos, krScriptActionHandler, autoRunTask, themeMode)
            return fragment
        }
    }

    private var actionInfos: ArrayList<NodeInfoBase>? = null

    private lateinit var progressBarDialog: ProgressBarDialog
    private var krScriptActionHandler: KrScriptActionHandler? = null
    private var autoRunTask: AutoRunTask? = null
    private var themeMode: ThemeMode? = null

    private fun setListData(
            actionInfos: ArrayList<NodeInfoBase>?,
            krScriptActionHandler: KrScriptActionHandler? = null,
            autoRunTask: AutoRunTask? = null,
            themeMode: ThemeMode? = null) {
        if (actionInfos != null) {
            this.actionInfos = actionInfos
            this.krScriptActionHandler = krScriptActionHandler
            this.autoRunTask = autoRunTask
            this.themeMode = themeMode
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.kr_action_list_fragment, container, false)
    }


    private lateinit var rootGroup: ListItemGroup
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.progressBarDialog = ProgressBarDialog(this.activity!!)

        rootGroup = ListItemGroup(this.context!!, true, GroupNode(""))

        if (actionInfos != null) {
            PageLayoutRender(this.context!!, actionInfos!!, this, rootGroup)
            val layout = rootGroup.getView()

            val rootView = (this.view?.findViewById<ScrollView?>(R.id.kr_content))
            rootView?.removeAllViews()
            rootView?.addView(layout)
            triggerAction(autoRunTask)
        }
    }

    private fun triggerAction(autoRunTask: AutoRunTask?) {
        autoRunTask?.run {
            if (!key.isNullOrEmpty()) {
                onCompleted(rootGroup.triggerActionByKey(key!!))
            }
        }
    }

    private fun nodeUnlocked(clickableNode: ClickableNode): Boolean {
        val currentSDK = Build.VERSION.SDK_INT
        if (clickableNode.targetSdkVersion > 0 && currentSDK != clickableNode.targetSdkVersion) {
            DialogHelper.helpInfo(context!!,
                    getString(R.string.kr_sdk_discrepancy),
                    getString(R.string.kr_sdk_discrepancy_message).format(clickableNode.targetSdkVersion)
            )
            return false
        } else if (currentSDK > clickableNode.maxSdkVersion) {
            DialogHelper.helpInfo(context!!,
                    getString(R.string.kr_sdk_overtop),
                    getString(R.string.kr_sdk_message).format(clickableNode.minSdkVersion, clickableNode.maxSdkVersion)
            )
            return false
        } else if (currentSDK < clickableNode.minSdkVersion) {
            DialogHelper.helpInfo(context!!,
                    getString(R.string.kr_sdk_too_low),
                    getString(R.string.kr_sdk_message).format(clickableNode.minSdkVersion, clickableNode.maxSdkVersion)
            )
            return false
        }

        var message = ""
        val unlocked = (if (clickableNode.lockShell.isNotEmpty()) {
            message = ScriptEnvironmen.executeResultRoot(context, clickableNode.lockShell, clickableNode)
            message == "unlock" || message == "unlocked" || message == "false" || message == "0"
        } else {
            !clickableNode.locked
        })
        if (!unlocked) {
            Toast.makeText(context, if (message.isNotEmpty()) {
                message
            } else {
                getString(R.string.kr_lock_message)
            }, Toast.LENGTH_LONG).show()
        }
        return unlocked
    }

    /**
     * 当switch项被点击
     */
    override fun onSwitchClick(item: SwitchNode, onCompleted: Runnable) {
        if (nodeUnlocked(item)) {
            val toValue = !item.checked
            if (item.confirm) {
                DialogHelper.warning(activity!!, item.title, item.desc, {
                    switchExecute(item, toValue, onCompleted)
                })
            } else if (item.warning.isNotEmpty()) {
                DialogHelper.warning(activity!!, item.title, item.warning, {
                    switchExecute(item, toValue, onCompleted)
                })
            } else {
                switchExecute(item, toValue, onCompleted)
            }
        }
    }

    /**
     * 执行switch的操作
     */
    private fun switchExecute(switchNode: SwitchNode, toValue: Boolean, onExit: Runnable) {
        val script = switchNode.setState ?: return

        actionExecute(switchNode, script, onExit, object : java.util.HashMap<String, String>() {
            init {
                put("state", if (toValue) "1" else "0")
            }
        })
    }


    override fun onPageClick(item: PageNode, onCompleted: Runnable) {
        if (nodeUnlocked(item)) {
            if (context != null && item.link.isNotEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context?.startActivity(intent)
                } catch (ex: Exception) {
                    Toast.makeText(context, context?.getString(R.string.kr_slice_activity_fail), Toast.LENGTH_SHORT).show()
                }
            } else if (context != null && item.activity.isNotEmpty()) {
                TryOpenActivity(context!!, item.activity).tryOpen()
            } else {
                krScriptActionHandler?.onSubPageClick(item)
            }
        }
    }

    // 长按 添加收藏
    override fun onItemLongClick(clickableNode: ClickableNode) {
        if (clickableNode.key.isEmpty()) {
            DialogHelper.alert(
                    this.activity!!,
                    getString(R.string.kr_shortcut_create_fail),
                    getString(R.string.kr_ushortcut_nsupported)
            )
        } else {
            krScriptActionHandler?.addToFavorites(clickableNode, object : KrScriptActionHandler.AddToFavoritesHandler {
                override fun onAddToFavorites(clickableNode: ClickableNode, intent: Intent?) {
                    if (intent != null) {
                        DialogHelper.confirm(activity!!,
                                getString(R.string.kr_shortcut_create),
                                String.format(getString(R.string.kr_shortcut_create_desc), clickableNode.title),
                                {
                                    val result = ActionShortcutManager(context!!)
                                            .addShortcut(intent, IconPathAnalysis().loadLogo(context!!, clickableNode), clickableNode)
                                    if (!result) {
                                        Toast.makeText(context, R.string.kr_shortcut_create_fail, Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, getString(R.string.kr_shortcut_create_success), Toast.LENGTH_SHORT).show()
                                    }
                                })
                    }
                }
            })
        }
    }

    /**
     * Picker点击
     */
    override fun onPickerClick(item: PickerNode, onCompleted: Runnable) {
        if (nodeUnlocked(item)) {
            if (item.confirm) {
                DialogHelper.warning(activity!!, item.title, item.desc, {
                    pickerExecute(item, onCompleted)
                })
            } else if (item.warning.isNotEmpty()) {
                DialogHelper.warning(activity!!, item.title, item.warning, {
                    pickerExecute(item, onCompleted)
                })
            } else {
                pickerExecute(item, onCompleted)
            }
        }
    }

    private fun pickerExecute(item: PickerNode, onCompleted: Runnable) {
        val paramInfo = ActionParamInfo()
        paramInfo.options = item.options
        paramInfo.optionsSh = item.optionsSh
        paramInfo.separator = item.separator

        val handler = Handler()

        progressBarDialog.showDialog(getString(R.string.kr_param_options_load))
        Thread {
            // 获取当前值
            if (item.getState != null) {
                paramInfo.valueFromShell = executeScriptGetResult(item.getState!!, item)
            }

            // 获取可选项（合并options-sh和静态options的结果）
            val options = getParamOptions(paramInfo, item)
            val optionsSorted = (if (options != null) {
                ActionParamsLayoutRender.setParamOptionsSelectedStatus(paramInfo, options)
            } else {
                null
            })

            handler.post {
                progressBarDialog.hideDialog()

                if (optionsSorted != null) {
                    val systemUiVisibility = activity!!.window?.decorView?.systemUiVisibility
                    val darkMode = systemUiVisibility != null && (systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) == 0
                    DialogItemChooser(darkMode, optionsSorted, item.multiple, object : DialogItemChooser.Callback {
                        override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                            if (item.multiple) {
                                pickerExecute(item, (selected.map { "" + it.value }).joinToString(item.separator), onCompleted)
                            } else {
                                if (selected.size > 0) {
                                    pickerExecute(item, "" + (
                                            if (selected.size > 0) {
                                                "" + selected[0].value
                                            } else {
                                                ""
                                            }), onCompleted)
                                } else {
                                    Toast.makeText(context, getString(R.string.picker_select_none), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }).show(activity!!.supportFragmentManager, "picker-item-chooser")
                } else {
                    Toast.makeText(context, getString(R.string.picker_not_item), Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    /**
     * 执行picker的操作
     */
    private fun pickerExecute(pickerNode: PickerNode, toValue: String, onExit: Runnable) {
        val script = pickerNode.setState ?: return

        actionExecute(pickerNode, script, onExit, object : java.util.HashMap<String, String>() {
            init {
                put("state", toValue)
            }
        })
    }

    /**
     * 列表项点击时（如果需要确认界面，则显示确认界面，否则直接准备执行）
     */
    override fun onActionClick(item: ActionNode, onCompleted: Runnable) {
        if (nodeUnlocked(item)) {
            if (item.confirm) {
                DialogHelper.warning(activity!!, item.title, item.desc, {
                    actionExecute(item, onCompleted)
                })
            } else if (item.warning.isNotEmpty() && (item.params == null || item.params?.size == 0)) {
                DialogHelper.warning(activity!!, item.title, item.warning, {
                    actionExecute(item, onCompleted)
                })
            } else {
                actionExecute(item, onCompleted)
            }
        }
    }

    /**
     * action执行参数界面
     */
    private fun actionExecute(action: ActionNode, onExit: Runnable) {
        val script = action.setState ?: return

        if (action.params != null) {
            val actionParamInfos = action.params!!
            if (actionParamInfos.size > 0) {
                val layoutInflater = LayoutInflater.from(this.context!!)
                val linearLayout = layoutInflater.inflate(R.layout.kr_params_list, null) as LinearLayout

                val handler = Handler()
                progressBarDialog.showDialog(this.context!!.getString(R.string.onloading))
                Thread(Runnable {
                    for (actionParamInfo in actionParamInfos) {
                        handler.post {
                            progressBarDialog.showDialog(this.context!!.getString(R.string.kr_param_load) + if (!actionParamInfo.label.isNullOrEmpty()) actionParamInfo.label else actionParamInfo.name)
                        }
                        if (actionParamInfo.valueShell != null) {
                            actionParamInfo.valueFromShell = executeScriptGetResult(actionParamInfo.valueShell!!, action)
                        }
                        handler.post {
                            progressBarDialog.showDialog(this.context!!.getString(R.string.kr_param_options_load) + if (!actionParamInfo.label.isNullOrEmpty()) actionParamInfo.label else actionParamInfo.name)
                        }
                        actionParamInfo.optionsFromShell = getParamOptions(actionParamInfo, action) // 获取参数的可用选项
                    }
                    handler.post {
                        progressBarDialog.showDialog(this.context!!.getString(R.string.kr_params_render))
                    }
                    handler.post {
                        val render = ActionParamsLayoutRender(linearLayout, activity!!)
                        render.renderList(actionParamInfos, object : ParamsFileChooserRender.FileChooserInterface {
                            override fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
                                return if (krScriptActionHandler == null) {
                                    false
                                } else {
                                    krScriptActionHandler!!.openFileChooser(fileSelectedInterface)
                                }
                            }
                        })
                        progressBarDialog.hideDialog()

                        // 自定义参数输入界面
                        val customRunner = krScriptActionHandler?.openParamsPage(action,
                                linearLayout,
                                Runnable {
                                    try {
                                        val params = render.readParamsValue(actionParamInfos)
                                        actionExecute(action, script, onExit, params)
                                    } catch (ex: Exception) {
                                        Toast.makeText(this.context!!, "" + ex.message, Toast.LENGTH_LONG).show()
                                    }
                                })

                        // 内置的参数输入界面
                        if (customRunner != true) {
                            val isLongList = (action.params != null && action.params!!.size > 4)
                            val dialogView = LayoutInflater.from(context).inflate(if (isLongList) R.layout.kr_dialog_params else R.layout.kr_dialog_params_small, null)
                            val center = dialogView.findViewById<ViewGroup>(R.id.kr_params_center)
                            center.removeAllViews()
                            center.addView(linearLayout)

                            val darkMode = themeMode != null && themeMode!!.isDarkMode

                            val dialog = (if (isLongList) {
                                val builder = AlertDialog.Builder(this.context, if (darkMode) R.style.kr_full_screen_dialog_dark else R.style.kr_full_screen_dialog_light)
                                builder.setView(dialogView).create().apply {
                                    show()
                                    val window = this.window
                                    val activity = activity
                                    if (window != null && activity != null) {
                                        DialogHelper.setWindowBlurBg(window, activity)
                                    }
                                }
                            } else {
                                // AlertDialog.Builder(this.context).create()
                                DialogHelper.customDialog(activity!!, dialogView).dialog
                            })

                            dialogView.findViewById<TextView>(R.id.title).text = action.title
                            if (action.desc.isEmpty()) {
                                dialogView.findViewById<TextView>(R.id.desc).visibility = View.GONE
                            } else {
                                dialogView.findViewById<TextView>(R.id.desc).text = action.desc
                            }
                            if (action.warning.isEmpty()) {
                                dialogView.findViewById<TextView>(R.id.warn).visibility = View.GONE
                            } else {
                                dialogView.findViewById<TextView>(R.id.warn).text = action.warning
                            }

                            dialogView.findViewById<View>(R.id.btn_cancel).setOnClickListener {
                                try {
                                    dialog!!.dismiss()
                                } catch (ex: java.lang.Exception) {
                                }
                            }
                            dialogView.findViewById<View>(R.id.btn_confirm).setOnClickListener {
                                try {
                                    val params = render.readParamsValue(actionParamInfos)
                                    actionExecute(action, script, onExit, params)
                                    dialog!!.dismiss()
                                } catch (ex: Exception) {
                                    Toast.makeText(this.context!!, "" + ex.message, Toast.LENGTH_LONG).show()
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
    private fun getParamOptions(actionParamInfo: ActionParamInfo, nodeInfoBase: NodeInfoBase): ArrayList<SelectItem>? {
        val options = ArrayList<SelectItem>()
        var shellResult = ""
        if (!actionParamInfo.optionsSh.isEmpty()) {
            shellResult = executeScriptGetResult(actionParamInfo.optionsSh, nodeInfoBase)
        }

        if (!(shellResult == "error" || shellResult == "null" || shellResult.isEmpty())) {
            for (item in shellResult.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (item.contains("|")) {
                    val itemSplit = item.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    options.add(SelectItem().apply {
                        var descText = itemSplit[0]
                        if (itemSplit.size > 0) {
                            descText = itemSplit[1]
                        }
                        title = descText
                        value = itemSplit[0]
                    })
                } else {
                    options.add(SelectItem().apply {
                        title = item
                        value = item
                    })
                }
            }
        } else if (actionParamInfo.options != null) {
            for (option in actionParamInfo.options!!) {
                options.add(option)
            }
        } else {
            return null
        }

        return options
    }

    private fun executeScriptGetResult(shellScript: String, nodeInfoBase: NodeInfoBase): String {
        return ScriptEnvironmen.executeResultRoot(this.context!!, shellScript, nodeInfoBase)
    }


    // 标识是否有隐藏任务在运行中
    var hiddenTaskRunning = false
    private fun actionExecute(nodeInfo: RunnableNode, script: String, onExit: Runnable, params: HashMap<String, String>?) {
        val context = context!!

        if (nodeInfo.shell == RunnableNode.shellModeBgTask) {
            val onDismiss = Runnable {
                krScriptActionHandler?.onActionCompleted(nodeInfo)
            }
            BgTaskThread.startTask(context, script, params, nodeInfo, onExit, onDismiss)
        } else if (nodeInfo.shell == RunnableNode.shellModeHidden) {
            if (hiddenTaskRunning) {
                Toast.makeText(context, getString(R.string.kr_hidden_task_running), Toast.LENGTH_SHORT).show()
            } else {
                hiddenTaskRunning = true
                val onDismiss = Runnable {
                    hiddenTaskRunning = false
                    krScriptActionHandler?.onActionCompleted(nodeInfo)
                }
                HiddenTaskThread.startTask(context, script, params, nodeInfo, onExit, onDismiss)
            }
        } else {
            val onDismiss = Runnable {
                krScriptActionHandler?.onActionCompleted(nodeInfo)
            }
            val darkMode = themeMode != null && themeMode!!.isDarkMode

            val dialog = DialogLogFragment.create(nodeInfo, onExit, onDismiss, script, params, darkMode)
            dialog.isCancelable = false
            dialog.show(fragmentManager!!, "")
        }
    }
}
