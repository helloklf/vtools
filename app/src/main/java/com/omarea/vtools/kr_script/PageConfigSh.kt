package com.omarea.vtools.kr_script

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.omarea.krscript.config.PageConfigReader
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.ConfigItemBase
import com.omarea.vtools.R
import java.io.ByteArrayInputStream

class PageConfigSh(private var activity: Activity, private var pageConfigSh: String) {
    private var handler = Handler(Looper.getMainLooper())

    private fun pageConfigShError(content: String) {
        handler.post {
            Toast.makeText(activity, activity.getString(R.string.kr_page_sh_invalid) + "\n" + content, Toast.LENGTH_LONG).show()
        }
    }

    private fun noReadPermission() {
        handler.post {
            Toast.makeText(activity, activity.getString(R.string.kr_page_sh_file_permission), Toast.LENGTH_LONG).show()
        }
    }

    fun execute(): ArrayList<ConfigItemBase>? {
        var items: ArrayList<ConfigItemBase>? = null

        val result = ScriptEnvironmen.executeResultRoot(activity, pageConfigSh)?.trim()
        Log.d("xmlexecute", result)
        if (result != null) {
            if (result.endsWith(".xml")) {
                items = PageConfigReader(activity, result).readConfigXml()
                if (items == null) {
                    noReadPermission()
                }
            } else if (result.startsWith("<?xml") && result.endsWith(">")) {
                val inputStream = ByteArrayInputStream(result.toByteArray())
                items = PageConfigReader(activity, inputStream).readConfigXml()
            } else if (result.isNotEmpty()) {
                pageConfigShError(result)
            }
        }
        return items
    }
}
