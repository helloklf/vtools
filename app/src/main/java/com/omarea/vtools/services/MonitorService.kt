package com.omarea.vtools.services

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.widget.Toast
import com.omarea.shared.ServiceHelper2
import com.omarea.shell.AsynSuShellUnit

class MonitorService : Service() {
    var serviceHelper: ServiceHelper2? = null
    var task: AsynSuShellUnit? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (serviceHelper == null) {
            task = AsynSuShellUnit(object : Handler() {
                override fun handleMessage(msg: Message?) {
                    super.handleMessage(msg)
                    if (msg != null && msg.what == 1) {
                        onOutput(msg.obj.toString())
                    }
                }
            })
            task!!.exec(if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) "am monitor" else "cmd activity monitor")
            task!!.waitFor(Runnable {
                stopSelf()
            })
            serviceHelper = ServiceHelper2(this)
            Toast.makeText(this, "试验阶段的动态响应实现方案，如果出现问题，请换回经典方案（激活Scene的辅助服务）", Toast.LENGTH_LONG).show()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        if (task != null) {
            task!!.destroy()
            task = null
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    var last = ""
    private fun onOutput(row: String) {
        if (!(row.contains("Activity") && row.contains(":"))) {
            return
        }
        val app = row.substring(row.indexOf(":") + 1).trim()
        if (app == "com.android.systemui" || app == "com.omarea.vtools") {
            return
        }
        if (last != app) {
            last = app
            // Toast.makeText(this, app, Toast.LENGTH_SHORT).show()
        }
        serviceHelper!!.onFocusAppChanged(app)
    }
}
