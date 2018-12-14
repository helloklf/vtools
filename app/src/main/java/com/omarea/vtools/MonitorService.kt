package com.omarea.vtools

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
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AsynSuShellUnit(object : Handler() {
            override fun handleMessage(msg: Message?) {
                super.handleMessage(msg)
                if (msg != null && msg.what == 1) {
                    onOutput(msg.obj.toString())
                }
            }
        }).exec(if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) "am monitor" else "cmd activity monitor").waitFor(Runnable{
            stopSelf()
        })
        if (serviceHelper != null) {
            serviceHelper!!.onInterrupt()
        }
        serviceHelper = ServiceHelper2(this)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    var last = ""
    private fun onOutput(row:String) {
        if (!(row.contains("Activity") && row.contains(":"))) {
            return
        }
        val app = row.substring(row.indexOf(":") + 1).trim()
        if (app == "com.android.systemui") {
            return
        }
        if (last != app) {
            last = app
            Toast.makeText(this, app, Toast.LENGTH_SHORT).show()
        }
        serviceHelper!!.onFocusAppChanged(app)
    }
}
