package com.omarea.krscript.ui

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.common.ui.ThemeMode
import com.omarea.krscript.R
import com.omarea.krscript.ScriptTaskThread
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
        this.progressBarDialog = ProgressBarDialog(this.context!!)

        rootGroup = ListItemGroup(this.context!!, true)

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

    /**
     * 当switch项被点击
     */
    override fun onSwitchClick(item: SwitchNode, onCompleted: Runnable) {
        val toValue = !item.checked
        if (item.confirm) {
            DialogHelper.animDialog(AlertDialog.Builder(this.context!!)
                    .setTitle(item.title)
                    .setMessage(item.desc)
                    .setPositiveButton(this.context!!.getString(R.string.btn_execute)) { _, _ ->
                        switchExecute(item, toValue, onCompleted)
                    }
                    .setNegativeButton(this.context!!.getString(R.string.btn_cancel)) { _, _ ->
                    })
        } else {
            switchExecute(item, toValue, onCompleted)
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

    // 长按 添加收藏
    override fun onItemLongClick(clickableNode: ClickableNode) {
        if (clickableNode.key.isEmpty()) {
            DialogHelper.animDialog(AlertDialog.Builder(context).setTitle(R.string.kr_shortcut_create_fail)
                    .setMessage(R.string.kr_ushortcut_nsupported)
                    .setNeutralButton(R.string.btn_cancel) { _, _ ->
                    }
            )
        } else {
            krScriptActionHandler?.addToFavorites(clickableNode, object : KrScriptActionHandler.AddToFavoritesHandler {
                override fun onAddToFavorites(clickableNode: ClickableNode, intent: Intent?) {
                    if (intent != null) {
                        DialogHelper.animDialog(AlertDialog.Builder(context)
                                .setTitle(getString(R.string.kr_shortcut_create))
                                .setMessage(String.format(getString(R.string.kr_shortcut_create_desc), clickableNode.title))
                                .setPositiveButton(R.string.btn_confirm) { _, _ ->
                                    val result = ActionShortcutManager(context!!)
                                            .addShortcut(intent, IconPathAnalysis().loadIcon(context!!, clickableNode), clickableNode)
                                    if (!result) {
                                        Toast.makeText(context, R.string.kr_shortcut_create_fail, Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, getString(R.string.kr_shortcut_create_success), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .setNegativeButton(R.string.btn_cancel) { _, _ ->
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
        val paramInfo = ActionParamInfo()
        paramInfo.options = item.options
        paramInfo.optionsSh = item.optionsSh
        paramInfo.separator = item.separator

        val handler = Handler()

        progressBarDialog.showDialog(getString(R.string.kr_param_options_load))
        Thread(Runnable {
            // 获取当前值
            if (item.getState != null) {
                paramInfo.valueFromShell = executeScriptGetResult(item.getState!!)
            }

            // 获取可选项（合并options-sh和静态options的结果）
            val options = getParamOptions(paramInfo)

            val labels = if (options != null) options.map { (it["item"] as ActionParamInfo.ActionParamOption).desc }.toTypedArray() else arrayOf()
            val values = if (options != null) options.map { (it["item"] as ActionParamInfo.ActionParamOption).value }.toTypedArray() else arrayOf()

            handler.post {
                progressBarDialog.hideDialog()
                val builder = AlertDialog.Builder(this.context!!)
                        .setTitle(item.title)
                        .setNegativeButton(this.context!!.getString(R.string.btn_cancel)) { _, _ -> }

                // 多选
                if (item.multiple) {
                    val status = if (options == null) {
                        booleanArrayOf()
                    } else {
                        ActionParamsLayoutRender.getParamOptionsSelectedStatus(
                                paramInfo,
                                options
                        )
                    }
                    builder.setMultiChoiceItems(labels, status) { _, index, isChecked ->
                        status[index] = isChecked
                    }.setPositiveButton(R.string.btn_confirm) { _, _ ->
                        val result = ArrayList<String?>()
                        for (index in status.indices) {
                            if (status[index]) {
                                values[index]?.run {
                                    result.add(this)
                                }
                            }
                        }
                        pickerExecute(item, "" + result.joinToString(item.separator), onCompleted)
                    }
                } else {
                    // 单选
                    var index = if (options == null) -1 else ActionParamsLayoutRender.getParamOptionsCurrentIndex(paramInfo, options)
                    builder.setSingleChoiceItems(labels, index) { _, which ->
                        index = which
                    }.setPositiveButton(this.context!!.getString(R.string.btn_execute)) { _, _ ->
                        pickerExecute(item, "" + (if (index > -1) values[index] else ""), onCompleted)
                    }
                }

                DialogHelper.animDialog(builder)
            }
        }).start()
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
        if (item.confirm) {
            DialogHelper.animDialog(AlertDialog.Builder(this.context!!)
                    .setTitle(item.title)
                    .setMessage(item.desc)
                    .setPositiveButton(this.context!!.getString(R.string.btn_execute)) { _, _ ->
                        actionExecute(item, onCompleted)
                    }
                    .setNegativeButton(this.context!!.getString(R.string.btn_cancel)) { _, _ -> })
        } else {
            actionExecute(item, onCompleted)
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
                            actionParamInfo.valueFromShell = executeScriptGetResult(actionParamInfo.valueShell!!)
                        }
                        handler.post {
                            progressBarDialog.showDialog(this.context!!.getString(R.string.kr_param_options_load) + if (!actionParamInfo.label.isNullOrEmpty()) actionParamInfo.label else actionParamInfo.name)
                        }
                        actionParamInfo.optionsFromShell = getParamOptions(actionParamInfo) // 获取参数的可用选项
                    }
                    handler.post {
                        progressBarDialog.showDialog(this.context!!.getString(R.string.kr_params_render))
                    }
                    handler.post {
                        val render = ActionParamsLayoutRender(linearLayout)
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

                            val builder = if (isLongList) AlertDialog.Builder(this.context, if (darkMode) R.style.kr_full_screen_dialog_dark else R.style.kr_full_screen_dialog_light) else AlertDialog.Builder(this.context)
                            val dialog = builder.setView(dialogView).create()
                            if (!isLongList) {
                                DialogHelper.animDialog(dialog)
                            } else {
                                dialog.show()
                            }

                            dialogView.findViewById<TextView>(R.id.title).text = action.title

                            dialogView.findViewById<View>(R.id.btn_cancel).setOnClickListener {
                                dialog!!.dismiss()
                            }
                            dialogView.findViewById<View>(R.id.btn_confirm).setOnClickListener {
                                try {
                                    val params = render.readParamsValue(actionParamInfos)
                                    dialog!!.dismiss()
                                    actionExecute(action, script, onExit, params)
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
    private fun getParamOptions(actionParamInfo: ActionParamInfo): ArrayList<HashMap<String, Any>>? {
        val options = ArrayList<HashMap<String, Any>>()
        var shellResult = ""
        if (!actionParamInfo.optionsSh.isEmpty()) {
            shellResult = executeScriptGetResult(actionParamInfo.optionsSh)
        }

        if (!(shellResult == "error" || shellResult == "null" || shellResult.isEmpty())) {
            for (item in shellResult.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (item.contains("|")) {
                    val itemSplit = item.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    options.add(object : HashMap<String, Any>() {
                        init {
                            var descText = itemSplit[0]
                            if (itemSplit.size > 0) {
                                descText = itemSplit[1]
                            }
                            put("title", descText)
                            put("item", object : ActionParamInfo.ActionParamOption() {
                                init {
                                    value = itemSplit[0]
                                    desc = descText
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

    private fun executeScriptGetResult(shellScript: String): String {
        return ScriptEnvironmen.executeResultRoot(this.context!!, shellScript);
    }

    private val taskResultReceiver = ArrayList<BroadcastReceiver>()

    private fun actionExecute(nodeInfo: RunnableNode, script: String, onExit: Runnable, params: HashMap<String, String>?) {
        val context = context!!
        val applicationContext = context.applicationContext

        if (nodeInfo.backgroundTask) {
            var receiver: BroadcastReceiver? = null
            val onDismiss = Runnable {
                krScriptActionHandler?.onActionCompleted(nodeInfo)
                try {
                    taskResultReceiver.remove(receiver)
                    applicationContext.unregisterReceiver(receiver)
                } catch (ex: java.lang.Exception) {
                }
            }
            receiver = ScriptTaskThread.startTask(context, script, params, nodeInfo, onExit, onDismiss)
            taskResultReceiver.add(receiver)
        } else {
            val onDismiss = Runnable {
                krScriptActionHandler?.onActionCompleted(nodeInfo)
            }
            val darkMode = themeMode != null && themeMode!!.isDarkMode

            val dialog = DialogLogFragment.create(nodeInfo, onExit, onDismiss, script, params, darkMode)
            dialog.show(fragmentManager, "")
            dialog.isCancelable = false
        }
    }
}
