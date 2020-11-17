package com.omarea.common.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.omarea.common.R

class DialogHelper {
    class DialogWrap(private val dialog: AlertDialog) {
        public fun dismiss() {
            try {
                dialog.dismiss()
            } catch (ex: Exception) {
            }
        }

        public fun hide() {
            try {
                dialog.hide()
            } catch (ex: Exception) {
            }
        }

        public val isShowing: Boolean
            get() {
                return dialog.isShowing()
            }
    }

    companion object {
        fun animDialog(dialog: AlertDialog?): DialogWrap? {
            if (dialog != null && !dialog.isShowing) {
                dialog.window!!.setWindowAnimations(R.style.windowAnim)
                dialog.show()
            }
            return if (dialog != null) DialogWrap(dialog) else null
        }

        fun animDialog(builder: AlertDialog.Builder): DialogWrap {
            val dialog = builder.create()
            animDialog(dialog)
            return DialogWrap(dialog)
        }

        fun helpInfo(context: Context, message: String, onDismiss: Runnable? = null): DialogWrap {
            return helpInfo(context, "", message, onDismiss)
        }

        fun helpInfo(context: Context, title: String, message: String, onDismiss: Runnable? = null): DialogWrap {
            val layoutInflater = LayoutInflater.from(context)
            val dialog = layoutInflater.inflate(R.layout.dialog_help_info, null)
            val alert = AlertDialog.Builder(context).setView(dialog)
            alert.setCancelable(true)

            (dialog.findViewById(R.id.dialog_help_title) as TextView).run {
                if (title.isNotEmpty()) {
                    text = message
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }

            (dialog.findViewById(R.id.dialog_help_info) as TextView).run {
                if (message.isNotEmpty()) {
                    text = message
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }
            if (onDismiss != null) {
                alert.setPositiveButton(R.string.btn_confirm) { d, _ ->
                    d.dismiss()
                }
                alert.setCancelable(false)
            }
            alert.setOnDismissListener {
                onDismiss?.run()
            }

            return animDialog(alert)
        }

        fun customDialog (context: Context, view: View): DialogWrap {
            return animDialog(
                    AlertDialog
                        .Builder(context)
                        .setView(view)
                        .setCancelable(true)
            )
        }

        fun helpInfo(context: Context, title: Int, message: Int): DialogWrap {
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
