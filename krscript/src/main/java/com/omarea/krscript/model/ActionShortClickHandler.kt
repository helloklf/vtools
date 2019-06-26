package com.omarea.krscript.model

import android.view.View

interface ActionShortClickHandler {
    fun onParamsView(actionInfo: ActionInfo, view: View, onCancel: Runnable, onComplete: Runnable):Boolean
    fun onExecute(configItem: ConfigItemBase, onExit: Runnable): ShellHandlerBase?
}
