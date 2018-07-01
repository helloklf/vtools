package com.omarea.scripts

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.*
import com.omarea.scripts.action.ActionConfigReader
import com.omarea.scripts.action.ActionInfo
import com.omarea.scripts.action.ActionParamInfo
import com.omarea.scripts.action.BuildConfigXml
import com.omarea.shared.Consts
import com.omarea.shared.FileWrite
import com.omarea.shell.KeepShellSync
import com.omarea.ui.ProgressBarDialog
import com.omarea.vboot.ActivityAddinOnline
import com.omarea.vboot.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.Charset
import java.util.*


/**
 * Created by Hello on 2018/05/06.
 */

class VToolsOnlineNative(var activity: ActivityAddinOnline, var webview: WebView) {

    init {
        webview.webChromeClient = VToolsWebClient()
    }

    private class VToolsWebClient : WebChromeClient() {
        private var processBarDialog: ProgressBarDialog? = null
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            if (view == null) {
                super.onProgressChanged(view, newProgress)
                return
            }
            if (newProgress != 100) {
                if (processBarDialog == null)
                    processBarDialog = ProgressBarDialog(view.context)
                processBarDialog!!.showDialog("正在加载页面 " + newProgress + "% ...")
            } else if (newProgress == 100) {
                if (processBarDialog != null)
                    processBarDialog!!.hideDialog()
            }
            super.onProgressChanged(view, newProgress)
        }

        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            if (view == null || result == null)
                return false;
            AlertDialog.Builder(view.context)
                    .setTitle("提示").setMessage(message)
                    .setPositiveButton(R.string.btn_confirm, DialogInterface.OnClickListener { dialog, which ->
                        result.confirm()
                    })
                    .setCancelable(false)
                    .create()
                    .show()
            return true
            //return super.onJsAlert(view, url, message, result)
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            if (view == null || result == null)
                return false;
            AlertDialog.Builder(view.context)
                    .setTitle("提示").setMessage(message)
                    .setPositiveButton(R.string.btn_confirm, DialogInterface.OnClickListener { dialog, which ->
                        result.confirm()
                    })
                    .setNegativeButton(R.string.btn_cancel, DialogInterface.OnClickListener { dialog, which ->
                        result.cancel()
                    })
                    .setCancelable(false)
                    .create()
                    .show()
            return true
            //return super.onJsConfirm(view, url, message, result)
        }

        override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
            return super.onJsPrompt(view, url, message, defaultValue, result)
        }
    }

    private var myHandler = Handler()
    private fun evaluateJavascript(scripts: String) {
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            webview.evaluateJavascript(scripts, android.webkit.ValueCallback<String> { value ->
                Log.d("registerAddin", value)
            })
        } else {
            myHandler.post {
                webview.evaluateJavascript(scripts, android.webkit.ValueCallback<String> { value ->
                    Log.d("registerAddin", value)
                })
            }
        }
    }

    private fun callback(function: String?, success: Boolean, message: String) {
        if (function != null && !function.isNullOrEmpty())
            evaluateJavascript("javascript:$function({ result: $success, message: `$message` })")
        else {
            myHandler.post {
                AlertDialog.Builder(activity).setMessage(message).create().show()
            }
        }
    }

    /**
     * 保存addin
     */
    private fun writeAddin(addinID: String, xml: String, callback: String?) {
        val configXml = xml.trim()
        myHandler.post {
            try {
                val actionInfos = ActionConfigReader.readActionConfigXml(activity, ByteArrayInputStream(configXml.toByteArray(Charset.defaultCharset())))
                if (actionInfos == null || actionInfos.size == 0) {
                    callback(callback, false, "xmlConfig解析失败或其中不包含任何action信息！")
                } else {
                    val result = FileWrite.WritePrivateFile(configXml.toByteArray(Charset.defaultCharset()), "/online-addin/${addinID}.sh", activity)
                    if (!result) {
                        callback(callback, false, "抱歉，存储文件失败！")
                        return@post;
                    }
                    if (callback != null && !callback.isEmpty()) {
                        evaluateJavascript("javascript:${callback}({result:true, message: '重新打开【附加功能】-【自定义】页面，即可看到你刚刚添加的功能！'})")
                    } else {
                        callback(callback, false, "重新打开【附加功能】-【自定义】页面，即可看到你刚刚添加的功能！")
                    }
                }
            } catch (ex: Exception) {
                callback(callback, false, "xmlConfig解析失败\n" + ex.message)
            }
        }
    }

    /** 注册自定义附加功能组件
     * @param addinID 组件唯一标识
     * @param configXml 配置信息，格式可参考 https://github.com/helloklf/kr-scripts 中的action定义
     * @param callback 回调要执行的方法名称Function({ result: 'true/false', message: '错误信息' })
     */
    @JavascriptInterface
    public fun registerAddin(addinID: String?, configXml: String?, callback: String?) {
        if (addinID != null && !addinID.isEmpty() && configXml != null && !configXml.isEmpty()) {
            val path = FileWrite.getPrivateFilePath(activity, "/online-addin/${addinID}.sh")
            if (File(path).exists()) {
                myHandler.post {
                    AlertDialog.Builder(activity)
                            .setTitle("是否覆盖？")
                            .setMessage("已存在相同addinID的自定义功能，是否覆盖已有的addin？")
                            .setPositiveButton(R.string.btn_confirm, DialogInterface.OnClickListener { dialog, which ->
                                writeAddin(addinID, configXml, callback)
                            })
                            .setNegativeButton(R.string.btn_cancel, DialogInterface.OnClickListener { dialog, which ->
                                callback(callback, false, "已取消添加！")
                            })
                            .create()
                            .show()
                }
            } else {
                writeAddin(addinID, configXml, callback)
            }
        } else if (callback != null && !callback.isEmpty()) {
            callback(callback, false, "无法添加功能，addinID为空或configXml为空！")
        } else {
            callback(callback, false, "传入的参数有误，addinID为空或configXml为空！")
        }
    }

    /**
     * 创建自定义附加功能组件
     * @param addinID 组件唯一标识
     * @param configInfo 配置信息
     * @param callback 回调要执行的方法名称Function({ result: 'true/false', message: '错误信息' })
     */
    @JavascriptInterface
    public fun createAddin(addinID: String?, configInfo: String, callback: String?) {
        val jsonObject: JSONObject
        try {
            jsonObject = JSONObject(configInfo)
            val actionInfo = ActionInfo()
            actionInfo.title = jsonObject.get("title") as String
            if (jsonObject.has("start")) {
                actionInfo.start = jsonObject.getString("start")
            }
            //FIXME:语法兼容
            if (jsonObject.has("confirm") && jsonObject.get("confirm") is Boolean)
                actionInfo.confirm = jsonObject.getBoolean("confirm")
            if (jsonObject.has("root") && jsonObject.get("root") is Boolean)
                actionInfo.root = jsonObject.getBoolean("root")

            if (jsonObject.has("desc")) {
                val descObj = jsonObject.get("desc")
                if (descObj is JSONObject) {
                    if (descObj.has("text"))
                        actionInfo.desc = descObj.getString("text")
                    if (descObj.has("su")) {
                        actionInfo.descPollingSUShell = descObj.getString("su")
                    }
                    if (descObj.has("sh")) {
                        actionInfo.descPollingSUShell = descObj.getString("sh")
                    }
                } else {
                    actionInfo.desc = descObj as String
                }
            }

            if (jsonObject.has("script"))
                actionInfo.script = jsonObject.getString("script") as String
            actionInfo.scriptType = ActionInfo.ActionScript.SCRIPT
            if (jsonObject.has("params")) {
                val paramsArr = jsonObject.get("params")
                actionInfo.params = ArrayList()
                if (paramsArr is JSONArray) {
                    for (index in 0..paramsArr.length() - 1) {
                        val actionParamInfo = ActionParamInfo()
                        val item = paramsArr.get(index)
                        if (item is JSONObject && item.has("name") && !item.getString("name").isNullOrEmpty()) {
                            actionParamInfo.name = item.getString("name")
                            if (item.has("desc") && !item.getString("desc").isNullOrEmpty())
                                actionParamInfo.desc = item.getString("desc")
                            //TODO:
                            //actionParamInfo.maxLength =
                            //actionParamInfo.readonly = false
                            //actionParamInfo.type =
                            if (item.has("type") && item.get("type") is String)
                                actionParamInfo.type = item.getString("type")
                            if (item.has("readonly") && item.get("readonly") is Boolean)
                                actionParamInfo.readonly = item.getBoolean("readonly")
                            if (item.has("maxLength") && item.get("maxLength") is Int)
                                actionParamInfo.maxLength = item.getInt("maxLength")
                            if (item.has("value")) {
                                val value = item.get("value")
                                if (value is Boolean) {
                                    actionParamInfo.value = if (value == true) "1" else "0";
                                } else if (value is Number) {
                                    actionParamInfo.value = "" + value
                                } else if (value is String) {
                                    actionParamInfo.value = item.getString("value")
                                }
                            }
                            if (item.has("value-su") && item.get("value-su") is String)
                                actionParamInfo.valueSUShell = item.getString("value-su")
                            if (item.has("value-sh") && item.get("value-sh") is String)
                                actionParamInfo.valueShell = item.getString("value-sh")
                            if (item.has("options-sh") && item.get("options-sh") is String) {
                                actionParamInfo.optionsSh = item.getString("options-sh")
                            }
                            actionParamInfo.options = ArrayList<ActionParamInfo.ActionParamOption>()
                            if (item.has("options") && item.get("options") is JSONArray) {
                                val options = item.getJSONArray("options")
                                for (oi in 0..options.length() - 1) {
                                    val option = ActionParamInfo.ActionParamOption()
                                    val optionItem = options.get(oi)
                                    if (optionItem is JSONObject) {
                                        if (optionItem.has("desc")) {
                                            option.value = optionItem.getString("desc")
                                            option.desc = optionItem.getString("desc")
                                        }
                                        if (optionItem.has("label")) {
                                            option.value = optionItem.getString("label")
                                            option.desc = optionItem.getString("label")
                                        }
                                        if (optionItem.has("value")) {
                                            val value = optionItem.get("value")
                                            if (value is Boolean) {
                                                option.value = if (value) "1" else "0"
                                            } else if (value is Number) {
                                                option.value = "" + value
                                            } else if (value is String) {
                                                option.value = value
                                            } else {
                                                option.value = value.toString()
                                            }
                                        }
                                    } else if (optionItem is String) {
                                        option.value = optionItem
                                        option.desc = optionItem
                                    }
                                    actionParamInfo.options.add(option)
                                }
                            }
                        } else {
                            callback(callback, false, "解析数据失败，param参数格式错误！")
                            return
                        }
                        actionInfo.params.add(actionParamInfo)
                    }
                } else {
                    callback(callback, false, "解析数据失败，params参数不正确，应该是一个数组！")
                    return
                }
            }
            val path = FileWrite.getPrivateFilePath(activity, "/online-addin/${addinID}.xml")
            if (File(path).exists()) {
                myHandler.post {
                    AlertDialog.Builder(activity)
                            .setTitle("是否覆盖？")
                            .setMessage("已存在相同addinID的自定义功能，是否覆盖已有的addin？")
                            .setPositiveButton(R.string.btn_confirm, DialogInterface.OnClickListener { dialog, which ->
                                val success = BuildConfigXml().build(actionInfo, addinID!!, activity)
                                callback(callback, success, if (success) "添加成功" else "添加失败")
                            })
                            .setNegativeButton(R.string.btn_cancel, DialogInterface.OnClickListener { dialog, which ->
                                callback(callback, false, "已取消添加！")
                            })
                            .create()
                            .show()
                }
            } else {
                val success = BuildConfigXml().build(actionInfo, addinID!!, activity)
                callback(callback, success, if (success) "添加成功" else "添加失败")
            }
        } catch (ex: Exception) {
            callback(callback, false, "解析数据失败：" + ex.message)
        }
    }

    /** 移除已注册的附加功能组件
     * @param addinID 组件唯一标识
     */
    @JavascriptInterface
    public fun unregisterAddin(addinID: String?, callback: String?) {
        val path = FileWrite.getPrivateFilePath(activity, "/online-addin/${addinID}.xml")
        if (File(path).exists()) {
            File(path).delete()
            callback(callback, true, "功能已从本地移除，但你要重新打开附加功能列表！")
        } else {
            callback(callback, false, "并没有添加这个功能！")
        }
    }

    /** 添加开关组件
     * @param addinID 唯一标识
     * @param configXml 配置信息，格式可参考 https://github.com/helloklf/kr-scripts 中的switch定义
     */
    @JavascriptInterface
    public fun registerSwitch(addinID: String?, configXml: String?) {

    }


    /** 删除开关组件
     * @param addinID 唯一标识
     */
    @JavascriptInterface
    public fun unregisterSwitch(addinID: String?) {

    }

    /** 更细powercfg
     * @param powercfg 配置脚本内容
     */
    @JavascriptInterface
    public fun updatePowercfg(powercfg: String?, callback: String?) {
        if (powercfg != null && !powercfg.isEmpty()) {
            val shell = powercfg.replace(Regex("\\r"), "")
            val outPath = FileWrite.getPrivateFilePath(activity, "powercfg\temp.xml")
            FileWrite.WritePrivateFile(shell.toByteArray(Charset.defaultCharset()), "powercfg\temp.xml", activity)
            myHandler.post {
                KeepShellSync.doCmdSync("cp '$outPath' ${Consts.POWER_CFG_PATH}; chmod 0777 ${Consts.POWER_CFG_PATH};sync;")
            }
            if (callback != null && !callback.isEmpty()) {
                webview.evaluateJavascript("${callback}({ result: true, message: '配置已写入到/data/powercf，现在去开启“动态响应功能”，即可体验根据前台应用自动调节GPU、CPU调度功能！' })", ValueCallback { });
            } else {
                AlertDialog.Builder(activity)
                        .setTitle("Powercfg已更新")
                        .setMessage("配置已写入到/data/powercf，现在去开启“动态响应功能”，即可体验根据前台应用自动调节GPU、CPU调度功能！")
                        .setPositiveButton(R.string.btn_confirm, DialogInterface.OnClickListener { dialog, which -> })
                        .create()
                        .show()
            }
        } else {

        }
    }
}
