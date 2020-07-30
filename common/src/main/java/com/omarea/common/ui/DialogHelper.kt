package com.omarea.common.ui

import android.app.AlertDialog
import android.content.Context
import com.omarea.common.R
import java.lang.Exception

class DialogHelper {
    class DialogWrap(private val dialog: AlertDialog) {
        public fun dismiss() {
            try {
                dialog.dismiss()
            } catch (ex: Exception) {}
        }

        public fun hide() {
            try {
                dialog.hide()
            } catch (ex: Exception) {}
        }

        public val isShowing:Boolean
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

        fun helpInfo(context: Context, message: String): DialogWrap {
            val dialog = AlertDialog.Builder(context)
                    .setPositiveButton(R.string.btn_confirm) { _, _ ->
                    }
            if (message.isNotEmpty()) {
                dialog.setMessage(message)
            }
            return animDialog(dialog)
        }

        fun helpInfo(context: Context, title: String, message: String): DialogWrap {
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
