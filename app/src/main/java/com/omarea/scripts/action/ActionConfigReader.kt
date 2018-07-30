package com.omarea.scripts.action

import android.content.Context
import android.util.Log
import android.util.Xml
import android.widget.Toast
import com.omarea.scripts.ExtractAssets
import com.omarea.shell.KeepShellSync
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.*

/**
 * Created by Hello on 2018/04/01.
 */

object ActionConfigReader {
    private const val ASSETS_FILE = "file:///android_asset/"

    fun readActionConfigXml(context: Context, fileInputStream: InputStream): ArrayList<ActionInfo>? {
        try {
            val parser = Xml.newPullParser()// 获取xml解析器
            parser.setInput(fileInputStream, "utf-8")// 参数分别为输入流和字符编码
            var type = parser.eventType
            var actions: ArrayList<ActionInfo>? = null
            var action: ActionInfo? = null
            var actionParamInfos: ArrayList<ActionParamInfo>? = null
            var actionParamInfo: ActionParamInfo? = null
            while (type != XmlPullParser.END_DOCUMENT) {// 如果事件不等于文档结束事件就继续循环
                when (type) {
                    XmlPullParser.START_TAG -> if ("actions" == parser.name) {
                        actions = ArrayList()
                    } else if ("action" == parser.name) {
                        action = ActionInfo()
                        for (i in 0 until parser.attributeCount) {
                            when (parser.getAttributeName(i)) {
                                "root" -> {
                                    action.root = parser.getAttributeValue(i) == "true"
                                }
                                "confirm" -> {
                                    action.confirm = parser.getAttributeValue(i) == "true"
                                }
                                "start" -> {
                                    action.start = parser.getAttributeValue(i)
                                }
                            }
                        }
                    } else if (action != null) {
                        if ("title" == parser.name) {
                            action.title = parser.nextText()
                        } else if ("desc" == parser.name) {
                            for (i in 0 until parser.attributeCount) {
                                val attrValue = parser.getAttributeValue(i)
                                when (parser.getAttributeName(i)) {
                                    "su" -> {
                                        if (attrValue.trim { it <= ' ' }.startsWith(ASSETS_FILE)) {
                                            val path = ExtractAssets(context).extractToFilesDir(attrValue.trim { it <= ' ' })
                                            action.descPollingSUShell = "chmod 7777 $path\n$path"
                                        } else {
                                            action.descPollingSUShell = attrValue
                                        }
                                        action.desc = executeResultRoot(context, action.descPollingSUShell)
                                    }
                                    "sh" -> {
                                        if (attrValue.trim { it <= ' ' }.startsWith(ASSETS_FILE)) {
                                            val path = ExtractAssets(context).extractToFilesDir(attrValue.trim { it <= ' ' })
                                            action.descPollingShell = "chmod 7777 $path\n$path"
                                        } else {
                                            action.descPollingShell = attrValue
                                        }
                                        action.desc = executeResult(context, action.descPollingShell)
                                    }
                                    "polling" -> {
                                        try {
                                            if (!attrValue.isEmpty()) {
                                                val polling = Integer.parseInt(attrValue)
                                                if (polling > 0)
                                                    action.polling = polling
                                            }
                                        } catch (ignored: Exception) {
                                        }

                                    }
                                }
                            }
                            if (action.desc == null || action.desc.isEmpty())
                                action.desc = parser.nextText()
                        } else if ("script" == parser.name) {
                            val script = parser.nextText()
                            if (script.trim { it <= ' ' }.startsWith(ASSETS_FILE)) {
                                action.scriptType = ActionInfo.ActionScript.ASSETS_FILE
                                val path = ExtractAssets(context).extractToFilesDir(script.trim { it <= ' ' })
                                action.script = "chmod 7777 $path\n$path"
                            } else {
                                action.script = script
                            }
                        } else if ("param" == parser.name) {
                            if (actionParamInfos == null) {
                                actionParamInfos = ArrayList()
                            }
                            actionParamInfo = ActionParamInfo()
                            //actionParamInfo.desc = parser.nextText();
                            for (i in 0 until parser.attributeCount) {
                                when (parser.getAttributeName(i)) {
                                    "name" -> {
                                        actionParamInfo.name = parser.getAttributeValue(i)
                                    }
                                    "desc" -> {
                                        actionParamInfo.desc = parser.getAttributeValue(i)
                                    }
                                    "value" -> {
                                        actionParamInfo.value = parser.getAttributeValue(i)
                                    }
                                    "type" -> {
                                        actionParamInfo.type = parser.getAttributeValue(i).toLowerCase().trim { it <= ' ' }
                                    }
                                    "readonly" -> {
                                        actionParamInfo.readonly = parser.getAttributeValue(i).toLowerCase().trim { it <= ' ' } == "readonly"
                                    }
                                    "maxlength" -> {
                                        actionParamInfo.maxLength = Integer.parseInt(parser.getAttributeValue(i))
                                    }
                                    "value-sh" -> {
                                        val script = parser.getAttributeValue(i)
                                        if (script.trim { it <= ' ' }.startsWith(ASSETS_FILE)) {
                                            val path = ExtractAssets(context).extractToFilesDir(script.trim { it <= ' ' })
                                            actionParamInfo.valueShell = "chmod 7777 $path\n$path"
                                        } else {
                                            actionParamInfo.valueShell = script
                                        }
                                    }
                                    "value-su" -> {
                                        val script = parser.getAttributeValue(i)
                                        if (script.trim { it <= ' ' }.startsWith(ASSETS_FILE)) {
                                            val path = ExtractAssets(context).extractToFilesDir(script.trim { it <= ' ' })
                                            actionParamInfo.valueSUShell = "chmod 7777 $path\n$path"
                                        } else {
                                            actionParamInfo.valueSUShell = script
                                        }
                                    }
                                    "maxLength" -> {
                                        actionParamInfo.maxLength = Integer.parseInt(parser.getAttributeValue(i))
                                    }
                                    "options-sh" -> {
                                        if (actionParamInfo.options == null)
                                            actionParamInfo.options = ArrayList<ActionParamInfo.ActionParamOption>()
                                        val script = parser.getAttributeValue(i)
                                        if (script.trim { it <= ' ' }.startsWith(ASSETS_FILE)) {
                                            val path = ExtractAssets(context).extractToFilesDir(script.trim { it <= ' ' })
                                            actionParamInfo.optionsSh = "chmod 7777 $path\n$path"
                                        } else {
                                            actionParamInfo.optionsSh = script
                                        }
                                    }
                                    "options-su" -> {
                                        if (actionParamInfo.options == null)
                                            actionParamInfo.options = ArrayList<ActionParamInfo.ActionParamOption>()
                                        val script = parser.getAttributeValue(i)
                                        if (script.trim { it <= ' ' }.startsWith(ASSETS_FILE)) {
                                            val path = ExtractAssets(context).extractToFilesDir(script.trim { it <= ' ' })
                                            actionParamInfo.optionsSU = "chmod 7777 $path\n$path"
                                        } else {
                                            actionParamInfo.optionsSU = script
                                        }
                                    }
                                }
                            }
                            if (actionParamInfo.name != null && actionParamInfo.name.trim { it <= ' ' } != "") {
                                actionParamInfos.add(actionParamInfo)
                            }
                        } else if (actionParamInfo != null && "option" == parser.name) {
                            if (actionParamInfo.options == null) {
                                actionParamInfo.options = ArrayList<ActionParamInfo.ActionParamOption>()
                            }
                            val option = ActionParamInfo.ActionParamOption()
                            for (i in 0 until parser.attributeCount) {
                                when (parser.getAttributeName(i)) {
                                    "value" -> {
                                        option.value = parser.getAttributeValue(i)
                                    }
                                    "val" -> {
                                        option.value = parser.getAttributeValue(i)
                                    }
                                }
                            }
                            option.desc = parser.nextText()
                            if (option.value == null)
                                option.value = option.desc
                            actionParamInfo.options.add(option)
                        }
                    }
                    XmlPullParser.END_TAG -> if ("action" == parser.name && actions != null && action != null) {
                        if (action.title == null) {
                            action.title = ""
                        }
                        if (action.desc == null) {
                            action.desc = ""
                        }
                        if (action.script == null) {
                            action.script = ""
                        }
                        action.params = actionParamInfos

                        actions.add(action)
                        actionParamInfos = null
                        action = null
                    }
                }
                type = parser.next()// 继续下一个事件
            }

            return actions
        } catch (ex: Exception) {
            Toast.makeText(context, ex.message, Toast.LENGTH_LONG).show()
            Log.d("VTools ReadConfig Fail！", ex.message)
        }

        return null
    }

    fun readActionConfigXml(context: Context): ArrayList<ActionInfo>? {
        try {
            val fileInputStream = context.assets.open("actions.xml")
            return readActionConfigXml(context, fileInputStream)
        } catch (ex: Exception) {
            Toast.makeText(context, ex.message, Toast.LENGTH_LONG).show()
            Log.d("VTools ReadConfig Fail！", ex.message)
        }

        return null
    }

    private fun executeResult(context: Context, script: String): String {
        var script = script
        if (script.trim { it <= ' ' }.startsWith(ASSETS_FILE)) {
            val path = ExtractAssets(context).extractToFilesDir(script.trim { it <= ' ' })
            script = "chmod 7777 $path\n$path"
        }
        return KeepShellSync.doCmdSync(script)
    }

    private fun executeResultRoot(context: Context, script: String): String {
        var script = script
        if (script.trim { it <= ' ' }.startsWith(ASSETS_FILE)) {
            val path = ExtractAssets(context).extractToFilesDir(script.trim { it <= ' ' })
            script = "chmod 7777 $path\n$path"
        }
        return KeepShellSync.doCmdSync(script)
    }
}
