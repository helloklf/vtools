package com.omarea.common.ui

import android.app.AlertDialog
import android.content.Context
import com.omarea.common.R

class DialogHelper {
    companion object {
        fun animDialog(dialog: AlertDialog?) {
            if (dialog != null && !dialog.isShowing) {
                dialog.window!!.setWindowAnimations(R.style.windowAnim)
                dialog.show()
            }
        }

        fun animDialog(builder: AlertDialog.Builder) {
            animDialog(builder.create())
        }

        fun helpInfo(context: Context, title:String, message: String) {
            DialogHelper.animDialog(
                    AlertDialog.Builder(context)
                            .setTitle(title)
                            .setMessage(message)
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                            }))
        }

        fun helpInfo(context: Context, title:Int, message: Int) {
            DialogHelper.animDialog(
                    AlertDialog.Builder(context)
                            .setTitle(title)
                            .setMessage(message)
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                            }))
        }
    }
}
