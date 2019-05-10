package com.omarea.krscripts.action

import android.content.Context
import android.util.Log
import android.util.Xml
import android.widget.Toast
import com.omarea.krscripts.ExtractAssets
import com.omarea.krscripts.ScriptEnvironmen
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
                    } else if ("separator" == parser.name) {
                        if (actions != null) {
                            val separator = ActionInfo()
                            separator.separator = parser.nextText()
                            actions.add(separator)
                        }
                    } else if ("group" == parser.name) {
                        if (actions != null) {
                            for (i in 0 until parser.attributeCount) {
                                val attrName = parser.getAttributeName(i)
                                if (attrName == "title") {
                                    val separator = ActionInfo()
                                    separator.separator = parser.getAttributeValue(i)
                                    actions.add(separator)
                                    break
                                }
                            }
                        }
                    } else if ("action" == parser.name) {
                        action = ActionInfo()
                        for (i in 0 until parser.attributeCount) {
                            if (action == null) {
                                break
                            }
                            when (parser.getAttributeName(i)) {
                                "confirm" -> action.confirm = parser.getAttributeValue(i) == "true"
                                "start" -> action.start = parser.getAttributeValue(i)
                                "support" -> {
                                    if (executeResultRoot(context, parser.getAttributeValue(i)) != "1") {
                                        action = null
                                    }
                                }
                            }
                        }
                    } else if ("resource" == parser.name) {
                        for (i in 0 until parser.attributeCount) {
                            if (parser.getAttributeName(i) == "file") {
                                val file = parser.getAttributeValue(i).trim()
                                if (file.startsWith(ASSETS_FILE)) {
                                    ExtractAssets(context).extractResource(file)
                                }
                            }
                        }
                    } else if (action != null) {
                        if ("title" == parser.name) {
                            action.title = parser.nextText()
                        } else if ("desc" == parser.name) {
                            for (i in 0 until parser.attributeCount) {
                                val attrName = parser.getAttributeName(i)
                                if (attrName == "su" || attrName == "sh") {
                                    action.descPollingShell = parser.getAttributeValue(i)
                                    action.desc = executeResultRoot(context, action.descPollingShell)
                                }
                            }
                            if (action.desc == null || action.desc.isEmpty())
                                action.desc = parser.nextText()
                        } else if ("script" == parser.name) {
                            action.script = parser.nextText()
                        } else if ("param" == parser.name) {
                            if (actionParamInfos == null) {
                                actionParamInfos = ArrayList()
                            }
                            actionParamInfo = ActionParamInfo()
                            for (i in 0 until parser.attributeCount) {
                                val attrName = parser.getAttributeName(i)
                                when {
                                    attrName == "name" -> actionParamInfo.name = parser.getAttributeValue(i)
                                    attrName == "desc" -> actionParamInfo.desc = parser.getAttributeValue(i)
                                    attrName == "value" -> actionParamInfo.value = parser.getAttributeValue(i)
                                    attrName == "type" -> actionParamInfo.type = parser.getAttributeValue(i).toLowerCase().trim { it <= ' ' }
                                    attrName == "readonly" -> actionParamInfo.readonly = parser.getAttributeValue(i).toLowerCase().trim { it <= ' ' } == "readonly"
                                    attrName == "maxlength" -> actionParamInfo.maxLength = Integer.parseInt(parser.getAttributeValue(i))
                                    attrName == "value-sh" || attrName == "value-su" -> {
                                        val script = parser.getAttributeValue(i)
                                        actionParamInfo.valueShell = script
                                    }
                                    attrName == "options-sh" || attrName == "options-su" -> {
                                        if (actionParamInfo.options == null)
                                            actionParamInfo.options = ArrayList<ActionParamInfo.ActionParamOption>()
                                        val script = parser.getAttributeValue(i)
                                        actionParamInfo.optionsSh = script
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
                                val attrName = parser.getAttributeName(i)
                                if (attrName == "val" || attrName == "value") {
                                    option.value = parser.getAttributeValue(i)
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

    private fun executeResultRoot(context: Context, scriptIn: String): String {
        return ScriptEnvironmen.executeResultRoot(context, scriptIn);
    }
}
