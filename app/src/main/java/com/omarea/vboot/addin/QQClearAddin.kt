package com.omarea.vboot.addin

import android.content.Context

/**
 * Created by Hello on 2018/02/20.
 */

class QQClearAddin(private var context: Context) : AddinBase(context) {
    init {
        this.title = ""
        this.desc = ""
    }

    override fun run() {

    }
}
