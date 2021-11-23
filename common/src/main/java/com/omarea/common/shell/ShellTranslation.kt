package com.omarea.common.shell

import android.content.Context
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

// 从Resource解析字符串，实现输出内容多语言
class ShellTranslation(val context: Context) {
    // 示例：
    // @string:home_shell_01
    private val regex1 = Regex("^@(string|dimen):[_a-z]+.*", RegexOption.IGNORE_CASE)
    // 示例
    // @string/home_shell_01
    private val regex2 = Regex("^@(string|dimen)/[_a-z]+.*", RegexOption.IGNORE_CASE)

    fun resolveRow(originRow: String): String {
        val separator = if (regex1.matches(originRow)) {
            ':'
        } else if (regex2.matches(originRow)) {
            '/'
        } else {
            null
        }
        if (separator != null) {
            val row = originRow.trim()
            val resources = context.resources
            val type = row.substring(1, row.indexOf(separator)).toLowerCase(Locale.ENGLISH)
            val name = row.substring(row.indexOf(separator) + 1)

            try {
                val id = resources.getIdentifier(name, type, context.packageName)
                when (type) {
                    "string" -> {
                        return resources.getString(id)
                    }
                    "dimen" -> {
                        return resources.getDimension(id).toString()
                    }
                }
            } catch (ex: Exception) {
                if (row.contains("[(") && row.contains(")]")) {
                    return row.substring(row.indexOf("[(") + 2, row.indexOf(")]"))
                }
            }
        }

        return originRow
    }

    fun resolveRows(rows: List<String>): String {
        val builder = StringBuilder()
        var rowIndex = 0
        for (row in rows) {
            if (rowIndex > 0) {
                builder.append("\n")
            }
            builder.append(resolveRow(row))
            rowIndex ++
        }
        return builder.toString()
    }

    fun getTranslatedResult(shellCommand: String, executor: KeepShell?): String {
        val shell = executor?: KeepShellPublic.getDefaultInstance()
        val rows = shell.doCmdSync(shellCommand).split("\n")
        if (rows.isNotEmpty()) {
            return resolveRows(rows)
        } else {
            return ""
        }
    }
}