package com.omarea.krscript.config

import android.text.InputFilter
import android.text.Spanned

import java.util.ArrayList
import java.util.regex.Matcher
import java.util.regex.Pattern

class ActionParamInfo {
    // 参数名：必需保持唯一
    var name: String? = null

    // 描述
    var desc: String? = null

    // 值
    var value: String? = null
    var valueShell: String? = null
    var valueFromShell: String? = null
    var maxLength = -1
    var type: String? = null
    var readonly: Boolean = false
    var options: ArrayList<ActionParamOption>? = null
    var optionsSh = ""
    // 是否支持
    var supported: Boolean = true


    class ParamInfoFilter(private val paramInfo: ActionParamInfo) : InputFilter {

        override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
            if (source != null && source.toString().contains("\"")) {
                return ""
            }

            if (paramInfo.maxLength >= 0) {
                val keep = paramInfo.maxLength - (dest.length - (dend - dstart))
                if (keep <= 0) {
                    // 如果超出字数限制，就返回“”
                    return ""
                }
            }

            if (paramInfo.type != null && paramInfo.type != "" && source != null) {
                if (paramInfo.type == "int") {
                    val regex = Pattern.compile("^[0-9]{0,}$")
                    val matcher = regex.matcher(source.toString())
                    if (!matcher.matches()) {
                        return ""
                    }
                } else if (paramInfo.type == "number") {
                    val regex = Pattern.compile("^[\\-.,0-9]{0,}$")
                    val matcher = regex.matcher(source.toString())
                    if (!matcher.matches()) {
                        return ""
                    }
                }
            }
            return null
        }
    }

    open class ActionParamOption {
        var value: String? = null
        var desc: String? = null
    }
}
