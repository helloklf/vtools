package com.omarea.ui

import android.text.InputFilter
import android.text.Spanned
import java.util.regex.Pattern

class IntInputFilter(private var maxLength: Int = 3) : InputFilter {
    private val type = "int"

    override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        if (source != null && source.toString().contains("\"")) {
            return ""
        }

        if (maxLength >= 0) {
            val keep = maxLength - (dest.length - (dend - dstart))
            if (keep <= 0) {
                // 如果超出字数限制，就返回“”
                return ""
            }
        }

        if (type != "" && source != null) {
            if (type == "int") {
                val regex = Pattern.compile("^[0-9]{0,}$")
                val matcher = regex.matcher(source.toString())
                if (!matcher.matches()) {
                    return ""
                }
            } else if (type == "number") {
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