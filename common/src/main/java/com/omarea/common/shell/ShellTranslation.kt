package com.omarea.common.shell

import android.content.Context
import com.omarea.common.shared.ResourceStringResolver

// 从Resource解析字符串，实现输出内容多语言
class ShellTranslation(context: Context) : ResourceStringResolver(context) {
    fun getTranslatedResult(shellCommand: String, executor: KeepShell?): String {
        val shell = executor?: KeepShellPublic.getDefaultInstance()
        val rows = shell.doCmdSync(shellCommand).split("\n")
        return if (rows.isNotEmpty()) {
            resolveRows(rows)
        } else {
            ""
        }
    }
}