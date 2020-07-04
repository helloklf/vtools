package com.omarea.krscript

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.omarea.common.shell.KeepShellPublic

class TryOpenActivity(private val context:  Context, private val activity:String) {
    private fun getIntent(): Intent? {
        try {
            val intent = if (activity.contains("/")) (Intent(Intent.ACTION_VIEW).apply {
                val info = activity.split("/")
                val packageName = info.first()
                val className = info.last()
                setClassName(packageName, if (className.startsWith(".")) (packageName + className) else className)
            }) else {
                Intent(activity)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        } catch (ex: java.lang.Exception) {
            return null
        }
    }
    fun tryOpen(): Boolean {
        if (activity.startsWith("am ")) {
            return KeepShellPublic.doCmdSync(activity).contains("Start")
            // am start -W -n com.miui.voiceassist/com.xiaomi.voiceassistant.AiSettings.AiShortcutActivity -a action.intent.action.VIEW
        } else {
            try {
                context.startActivity(getIntent())
                return true
            } catch (ex: SecurityException) {
                val success = if (activity.contains("/")) {
                    KeepShellPublic.doCmdSync("am start-activity -W -n " + activity).contains("ok")
                } else {
                    KeepShellPublic.doCmdSync("am start-activity -W -a " + activity).contains("ok")
                }
                if (success) {
                    return true
                } else {
                    Toast.makeText(context, context.getString(R.string.kr_slice_activity_fail), Toast.LENGTH_SHORT).show()
                    return false
                }
            } catch (ex: Exception) {
                Toast.makeText(context, context.getString(R.string.kr_slice_activity_fail), Toast.LENGTH_SHORT).show()
                return false
            }
        }
    }
}
