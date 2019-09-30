package com.omarea.krscript.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.OverScrollView
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.common.ui.ThemeMode
import com.omarea.krscript.R
import com.omarea.krscript.config.ActionParamInfo
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.executor.SimpleShellExecutor
import com.omarea.krscript.model.*
import com.omarea.krscript.shortcut.ActionShortcutManager

class ActionListFragment : Fragment(), PageLayoutRender.OnItemClickListener {
    companion object {
        fun create(
                actionInfos: ArrayList<ConfigItemBase>?,
                krScriptActionHandler: KrScriptActionHandler? = null,
                autoRunTask: AutoRunTask? = null,
                themeMode: ThemeMode? = null): ActionListFragment {
            val fragment = ActionListFragment()
            fragment.setListData(actionInfos, krScriptActionHandler, autoRunTask, themeMode)
            return fragment
        }
    }

    private lateinit var actionInfos: ArrayList<ConfigItemBase>

    private lateinit var progressBarDialog: ProgressBarDialog
    private var krScriptActionHandler: KrScriptActionHandler? = null
    private var autoRunTask: AutoRunTask? = null
    private var themeMode: ThemeMode? = null

    private fun setListData(
            actionInfos: ArrayList<ConfigItemBase>?,
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


    private lateinit var layoutBuilder: ListItemView
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.progressBarDialog = ProgressBarDialog(this.context!!)

        layoutBuilder = ListItemView(this.context!!, R.layout.kr_group_list_root)

        PageLayoutRender(this.context!!, actionInfos, this, layoutBuilder)
        val layout = layoutBuilder.getView()

        (this.view?.findViewById<OverScrollView?>(R.id.kr_content))?.addView(layout)
        triggerAction(autoRunTask)
    }

    private fun triggerAction(autoRunTask: AutoRunTask?) {
        autoRunTask?.run {
            if (!key.isNullOrEmpty()) {
                onCompleted(layoutBuilder.triggerActionByKey(key!!))
            }
        }
    }

    /**
     * 当switch项被点击
     */
    override fun onSwitchClick(item: SwitchInfo, onCompleted: Runnable) {
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
    private fun switchExecute(switchInfo: SwitchInfo, toValue: Boolean, onExit: Runnable) {
        val script = switchInfo.setState ?: return

        actionExecute(switchInfo, script, onExit, object : java.util.HashMap<String, String>() {
            init {
                put("state", if (toValue) "1" else "0")
            }
        })
    }


    override fun onPageClick(item: PageInfo, onCompleted: Runnable) {
        krScriptActionHandler?.onSubPageClick(item)
    }

    // 长按 添加收藏
    override fun onItemLongClick(item: ConfigItemBase) {
        if (item.key.isEmpty()) {
            DialogHelper.animDialog(AlertDialog.Builder(context).setTitle(R.string.kr_shortcut_create_fail)
                    .setMessage(R.string.kr_ushortcut_nsupported)
                    .setNeutralButton(R.string.btn_cancel) { _, _ ->
                    }
            )
        } else {
            krScriptActionHandler?.addToFavorites(item, object : KrScriptActionHandler.AddToFavoritesHandler {
                override fun onAddToFavorites(configItem: ConfigItemBase, intent: Intent?) {
                    if (intent != null) {
                        DialogHelper.animDialog(AlertDialog.Builder(context)
                                .setTitle(getString(R.string.kr_shortcut_create))
                                .setMessage(String.format(getString(R.string.kr_shortcut_create_desc), configItem.title))
                                .setPositiveButton(R.string.btn_confirm) { _, _ ->
                                    val result = ActionShortcutManager(context!!).addShortcut(intent, context!!.getDrawable(R.drawable.kr_shortcut_logo)!!, configItem)
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
     * 单选列表点击
     */
    override fun onPickerClick(item: PickerInfo, onCompleted: Runnable) {
        val paramInfo = ActionParamInfo()
        paramInfo.options = item.options
        paramInfo.optionsSh = item.optionsSh

        // 获取当前值
        if (item.getState != null) {
            paramInfo.valueFromShell = executeScriptGetResult(this.context!!, item.getState!!)
        }

        // 获取可选项（合并options-sh和静态options的结果）
        val coalescentOptions = getParamOptions(paramInfo)

        val options = if (coalescentOptions != null) coalescentOptions.map { (it["item"] as ActionParamInfo.ActionParamOption).desc }.toTypedArray() else arrayOf()
        val values = if (coalescentOptions != null) coalescentOptions.map { (it["item"] as ActionParamInfo.ActionParamOption).value }.toTypedArray() else arrayOf()

        var index = -1
        if (coalescentOptions != null) {
            index = ActionParamsLayoutRender.getParamOptionsCurrentIndex(paramInfo, coalescentOptions)
        }

        DialogHelper.animDialog(
                AlertDialog.Builder(this.context!!)
                        .setTitle(item.title)
                        .setSingleChoiceItems(options, index) { _, which ->
                            index = which
                        }
                        .setPositiveButton(this.context!!.getString(R.string.btn_execute)) { _, _ ->
                            pickerExecute(item, "" + (if (index > -1) values[index] else ""), onCompleted)
                        }
                        .setNegativeButton(this.context!!.getString(R.string.btn_cancel)) { _, _ ->
                        })
    }

    /**
     * 执行picker的操作
     */
    private fun pickerExecute(pickerInfo: PickerInfo, toValue: String, onExit: Runnable) {
        val script = pickerInfo.setState ?: return

        actionExecute(pickerInfo, script, onExit, object : java.util.HashMap<String, String>() {
            init {
                put("state", toValue)
            }
        })
    }

    /**
     * 列表项点击时（如果需要确认界面，则显示确认界面，否则直接准备执行）
     */
    override fun onActionClick(item: ActionInfo, onCompleted: Runnable) {
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
    private fun actionExecute(action: ActionInfo, onExit: Runnable) {
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
                            actionParamInfo.valueFromShell = executeScriptGetResult(this.context!!, actionParamInfo.valueShell!!)
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
                        render.renderList(actionParamInfos, object : FileChooserRender.FileChooserInterface {
                            override fun openFileChooser(fileSelectedInterface: FileChooserRender.FileSelectedInterface): Boolean {
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
                            val dialogView = LayoutInflater.from(context).inflate(if(isLongList) R.layout.kr_dialog_params else R.layout.kr_dialog_params_small, null)
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
            shellResult = executeScriptGetResult(this.context!!, actionParamInfo.optionsSh)
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
        return ScriptEnvironmen.executeResultRoot(this.context!!, shellScript);
    }

    private fun actionExecute(configItem: ConfigItemBase, script: String, onExit: Runnable, params: HashMap<String, String>?) {
        var shellHandler: ShellHandlerBase? = null
        if (krScriptActionHandler != null) {
            shellHandler = krScriptActionHandler!!.openExecutor(configItem, onExit)
        }
        if (shellHandler == null) {
            val darkMode = themeMode != null && themeMode!!.isDarkMode

            val dialog = DialogLogFragment.create(configItem, onExit, script, params, darkMode)
            dialog.show(fragmentManager, "")
            dialog.isCancelable = false

            // val outValue = TypedValue()
            // context!!.theme.resolveAttribute(R.attr.alertDialogTheme, outValue, true)
            // Log.d("alertDialogTheme", "" + outValue.data)
        } else {
            SimpleShellExecutor().execute(this.context!!, configItem, script, onExit, params, shellHandler)
        }
    }
}
