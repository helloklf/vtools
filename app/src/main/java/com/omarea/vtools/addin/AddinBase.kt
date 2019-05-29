package com.omarea.vtools.addin

import android.content.Context
import com.omarea.common.ui.ProgressBarDialog

/**
 * Created by Hello on 2018/02/20.
 */

open class AddinBase(private var context: Context) : ProgressBarDialog(context) {
    var title: String? = null
    var desc: String? = null
    var command: String? = null

    open fun run() {
        if (command != null) {
            execShell(command!!)
        }
    }
}
