package com.omarea.common.ui

import android.app.AlertDialog
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
    }
}
