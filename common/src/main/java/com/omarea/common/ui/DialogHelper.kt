package com.omarea.common.ui

import android.app.AlertDialog
import android.content.Context
import com.omarea.common.R

class DialogHelper {
    companion object {
        fun animDialog(dialog: AlertDialog?): AlertDialog? {
            if (dialog != null && !dialog.isShowing) {
                dialog.window!!.setWindowAnimations(R.style.windowAnim)
                dialog.show()
            }
            return dialog
        }

        fun animDialog(builder: AlertDialog.Builder): AlertDialog? {
            val dialog = builder.create()
            animDialog(dialog)
            return dialog
        }

        fun helpInfo(context: Context, title: String, message: String): AlertDialog? {
            val dialog = AlertDialog.Builder(context)
                    .setPositiveButton(R.string.btn_confirm) { _, _ ->
                    }
            if (title.isNotEmpty()) {
                dialog.setTitle(title)
            }
            if (message.isNotEmpty()) {
                dialog.setMessage(message)
            }
            return animDialog(dialog)
        }

        fun helpInfo(context: Context, title: Int, message: Int): AlertDialog? {
            val dialog =
                    AlertDialog.Builder(context)
                            .setTitle(title)
                            .setMessage(message)
                            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                            }
            return animDialog(dialog)
        }
    }
}
