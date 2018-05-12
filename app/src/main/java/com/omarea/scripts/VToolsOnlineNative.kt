package com.omarea.scripts

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Handler
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebView
import com.omarea.shared.Consts
import com.omarea.shared.FileWrite
import com.omarea.shell.SuDo
import com.omarea.vboot.ActivityAddinOnline
import com.omarea.vboot.R
import java.nio.charset.Charset
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.omarea.scripts.action.ActionConfigReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream


/**
 * Created by Hello on 2018/05/06.
 */

class VToolsOnlineNative(var activity: ActivityAddinOnline, var webview: WebView) {
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

    private fun callback(function: String?,success: Boolean, message: String) {
        if(function!=null && !function.isNullOrEmpty())
            evaluateJavascript("javascript:$function({ result:$success, message: `$message` })")
        else
        {
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
                if(actionInfos == null || actionInfos.size == 0) {
                    callback(callback, false, "xmlConfig解析失败或其中不包含任何action信息！")
                } else {
                    val result = FileWrite.WritePrivateFile(configXml.toByteArray(Charset.defaultCharset()), "/online-addin/${addinID}.sh", activity)
                    if(!result) {
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
            if(File(path).exists()) {
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

    /** 移除已注册的附加功能组件
     * @param addinID 组件唯一标识
     */
    @JavascriptInterface
    public fun unregisterAddin(addinID: String?) {

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
            val outPath = FileWrite.getPrivateFilePath(activity, "powercfg\temp.sh")
            FileWrite.WritePrivateFile(shell.toByteArray(Charset.defaultCharset()), "powercfg\temp.sh", activity)
            myHandler.post {
                SuDo(activity).execCmdSync("cp '$outPath' ${Consts.POWER_CFG_PATH}; chmod 0777 ${Consts.POWER_CFG_PATH};sync;")
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
