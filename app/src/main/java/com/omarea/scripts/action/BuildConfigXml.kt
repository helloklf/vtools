package com.omarea.scripts.action

import android.content.Context
import com.omarea.shared.FileWrite
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


/**
 * Created by Hello on 2018/05/13.
 */

class BuildConfigXml {
    fun build(actionInfo: ActionInfo, addinID: String, context: Context): Boolean {
        val path = FileWrite.getPrivateFilePath(context, "/online-addin/${addinID}.xml")
        var dir = FileWrite.getPrivateFilePath(context, "/online-addin/${addinID}")
        if (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length - 1)
        }
        try {
            //实例化一个DocmentBuilderFactory，调用其静态方法获取
            val builderFactory = DocumentBuilderFactory.newInstance()
            //实例化一个DocumentBuilder
            val builder = builderFactory.newDocumentBuilder()
            //实例化一个xml文件
            val newxml = builder.newDocument()
            //创建一个根标签
            val actions = newxml.createElement("actions")
            val action = newxml.createElement("action")

            val title = newxml.createElement("title")
            title.textContent = actionInfo.title
            action.appendChild(title)

            val desc = newxml.createElement("desc")
            if (actionInfo.desc != null && !actionInfo.desc.isNullOrEmpty()) {
                desc.textContent = actionInfo.desc
            }
            if (actionInfo.descPollingSUShell != null && !actionInfo.descPollingSUShell.isNullOrEmpty()) {
                desc.setAttribute("su", writeScript(actionInfo.descPollingSUShell, "/$addinID/desc-polling-su.sh", context))
            }
            if (actionInfo.descPollingShell != null && !actionInfo.descPollingShell.isNullOrEmpty()) {
                desc.setAttribute("sh", writeScript(actionInfo.descPollingShell, "/$addinID/desc-polling-sh.sh", context))
            }
            action.appendChild(desc)

            val script = newxml.createElement("script")
            if (actionInfo.script != null && !actionInfo.script.isNullOrEmpty()) {
                script.textContent = writeScript(actionInfo.script, "/$addinID/script.sh", actionInfo.params, context)
            }
            action.appendChild(script)

            if (actionInfo.root) {
                action.setAttribute("root", "true")
            }

            if (actionInfo.confirm) {
                action.setAttribute("confirm", "true")
            }

            if (actionInfo.start != null && !actionInfo.start.isNullOrEmpty()) {
                action.setAttribute("start", actionInfo.start)
            }

            if (actionInfo.params != null && actionInfo.params.size > 0) {
                val params = newxml.createElement("params")
                for (paramInfo in actionInfo.params) {
                    val param = newxml.createElement("param")
                    if (paramInfo.name != null && !paramInfo.name.isNullOrEmpty()) {
                        param.setAttribute("name", paramInfo.name)
                        if (paramInfo.optionsSh != null && !paramInfo.optionsSh.isNullOrEmpty()) {
                            param.setAttribute("options-sh", writeScript(paramInfo.optionsSh, "/$addinID/param-${paramInfo.name}-options-sh.sh", context))
                        }
                        if (paramInfo.readonly) {
                            param.setAttribute("readonly", "true")
                        }
                        if (paramInfo.desc != null && !paramInfo.desc.isNullOrEmpty()) {
                            param.setAttribute("desc", paramInfo.desc)
                        }
                        if (paramInfo.maxLength > -1) {
                            param.setAttribute("maxlength", "" + paramInfo.maxLength)
                        }
                        if (paramInfo.type != null) {
                            param.setAttribute("type", paramInfo.type)
                        }
                        if (paramInfo.valueSUShell != null && !paramInfo.valueSUShell.isNullOrEmpty()) {
                            param.setAttribute("value-sh", writeScript(paramInfo.valueSUShell, "/$addinID/param-${paramInfo.name}-value-su.sh", context))
                        }
                        if (paramInfo.valueShell != null && !paramInfo.valueShell.isNullOrEmpty()) {
                            param.setAttribute("value-sh", writeScript(paramInfo.valueShell, "/$addinID/param-${paramInfo.name}-value-sh.sh", context))
                        }
                        if (paramInfo.options != null && paramInfo.options.size > 0) {
                            for (optionInfo in paramInfo.options) {
                                val option = newxml.createElement("option")
                                if (optionInfo.value != null) {
                                    option.setAttribute("value", optionInfo.value)
                                }
                                if (optionInfo.desc != null) {
                                    option.setAttribute("desc", optionInfo.desc)
                                    option.textContent = optionInfo.desc
                                }
                                param.appendChild(option)
                            }
                        }
                        params.appendChild(param)
                    }
                }

                action.appendChild(params)
            }

            actions.appendChild(action)
            newxml.appendChild(actions)

            val transformerFactory = TransformerFactory.newInstance()
            //获取到Transformer
            val transformer = transformerFactory.newTransformer()
            //设置输出格式
            transformer.setOutputProperty("encoding", "UTF-8")
            val fileDir = File(path).parentFile
            if (!fileDir.exists())
                fileDir.mkdirs()
            //设置输出流
            val os = FileOutputStream(path)
            //将文件写出
            transformer.transform(DOMSource(newxml), StreamResult(os))
        } catch (ex: Exception) {
            return false
        }
        return true
    }

    private fun writeScript(script: String, outName: String, context: Context): String {
        if (script.startsWith("#!/")) {
            FileWrite.writePrivateFile(script.replace(Regex("\r\n"), "\n").toByteArray(Charset.defaultCharset()), outName, context)
            return FileWrite.getPrivateFilePath(context, outName)
        } else {
            return script
        }
    }

    private fun writeScript(script: String, outName: String, params: ArrayList<ActionParamInfo>?, context: Context): String {
        if (script.startsWith("#!/")) {
            FileWrite.writePrivateFile(script.replace(Regex("\r\n"), "\n").toByteArray(Charset.defaultCharset()), outName, context)
            val path = FileWrite.getPrivateFilePath(context, outName)
            if (params != null && params.size > 0) {
                val stringBuilder = StringBuilder(path)
                for (param in params) {
                    stringBuilder.append(" $")
                    stringBuilder.append(param.name)
                }
                stringBuilder.append(";\n")
                return stringBuilder.toString()
            }
            return path
        } else {
            return script
        }
    }
}
