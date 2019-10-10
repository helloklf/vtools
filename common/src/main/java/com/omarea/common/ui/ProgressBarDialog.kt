package com.omarea.common.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.R
import com.omarea.common.shell.AsynSuShellUnit

/**
 * Loading弹窗
 * Created by Hello on 2018/02/27.
 */
open class ProgressBarDialog(private var context: Context) {
    private var alert: AlertDialog? = null
    private var textView: TextView? = null

    class DefaultHandler(private var alertDialog: AlertDialog?) : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)

            try {
                if (msg == null || alertDialog == null) {
                    return
                }
                if (msg.what == 10) {
                    alertDialog!!.dismiss()
                    alertDialog!!.hide()
                    if (msg.obj == true) {
                        Toast.makeText(alertDialog!!.context, R.string.execute_success, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(alertDialog!!.context, R.string.execute_fail, Toast.LENGTH_LONG).show()
                    }
                } else if (msg.what == -1) {
                    Toast.makeText(alertDialog!!.context, R.string.execute_fail, Toast.LENGTH_LONG).show()
                } else if (msg.what == 0 && msg.obj == false) {
                    alertDialog!!.dismiss()
                    alertDialog!!.hide()
                    Toast.makeText(alertDialog!!.context, R.string.execute_fail, Toast.LENGTH_LONG).show()
                }
            } catch (ex: Exception) {
            }
        }
    }

    @SuppressLint("InflateParams")
    public fun execShell(cmd: String, handler: Handler? = null) {
        hideDialog()

        val layoutInflater = LayoutInflater.from(context)
        val dialog = layoutInflater.inflate(R.layout.dialog_loading, null)
        val textView = (dialog.findViewById(R.id.dialog_app_details_pkgname) as TextView)
        textView.text = context.getString(R.string.execute_wait)
        alert = AlertDialog.Builder(context).setView(dialog).setCancelable(false).create()
        if (handler == null) {
            AsynSuShellUnit(DefaultHandler(alert)).exec(cmd).waitFor()
        } else {
            AsynSuShellUnit(handler).exec(cmd).waitFor()
        }
        DialogHelper.animDialog(alert)
    }

    public fun execShell(sb: StringBuilder, handler: Handler? = null) {
        execShell(sb.toString(), handler)
    }

    public fun isDialogShow(): Boolean {
        return this.alert != null
    }

    public fun hideDialog() {
        try {
            if (alert != null) {
                alert!!.dismiss()
                alert!!.hide()
                alert = null
            }
        } catch (ex: Exception) {
        }
    }

    @SuppressLint("InflateParams")
    public fun showDialog(text: String = "正在加载，请稍等..."): AlertDialog? {
        if (textView != null && alert != null) {
            textView!!.text = text
        } else {
            hideDialog()
            val layoutInflater = LayoutInflater.from(context)
            val dialog = layoutInflater.inflate(R.layout.dialog_loading, null)
            textView = (dialog.findViewById(R.id.dialog_app_details_pkgname) as TextView)
            textView!!.text = text
            alert = AlertDialog.Builder(context).setView(dialog).setCancelable(false).create()
            alert!!.window!!.setWindowAnimations(R.style.windowAnim)

            try {
                alert!!.show()
            } catch (ex: java.lang.Exception) {

            }
        }
        return alert
    }
}
