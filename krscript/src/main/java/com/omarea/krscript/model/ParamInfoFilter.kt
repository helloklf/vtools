package com.omarea.krscript.model

import android.text.InputFilter
import android.text.Spanned
import java.util.regex.Pattern

class ParamInfoFilter(private val paramInfo: ActionParamInfo) : InputFilter {
    override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
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
