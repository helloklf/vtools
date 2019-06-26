package com.omarea.krscript.model

import android.view.View

interface ActionShortClickHandler {
    fun onParamsView(view: View, onCancel: Runnable, onComplete: Runnable)
    fun onExecute(configItem: ConfigItemBase, onExit: Runnable): ShellHandlerBase?
}
