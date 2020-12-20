package com.omarea.common.shared

import android.content.Context
import java.nio.charset.Charset

object RawText {
    fun getRawText(context: Context, id: Int): String {
        try {
            return String(context.resources.openRawResource(id).readBytes(), Charset.defaultCharset()).replace(Regex("\r\n"), "\n").replace(Regex("\r\t"), "\t").replace(Regex("\r"), "\n")
        } catch (ex: Exception) {
            return ""
        }
    }
}
