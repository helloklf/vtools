package com.omarea.vboot.addin

import android.content.Context
import com.omarea.ui.ProgressBarDialog

/**
 * Created by Hello on 2018/02/20.
 */

open class AddinBase(private var context: Context) : ProgressBarDialog(context) {
    var title: String? = null
    var desc: String? = null

    open fun run() {

    }
}
