package com.omarea.krscript.model

import android.content.Intent
import android.view.View
import com.omarea.krscript.ui.FileChooserRender

interface KrScriptActionHandler {
    fun openFileChooser(fileSelectedInterface: FileChooserRender.FileSelectedInterface): Boolean
    fun onSubPageClick(pageInfo: PageInfo)
    fun addToFavorites(configItemBase: ConfigItemBase, addToFavoritesHandler: AddToFavoritesHandler)
    fun openParamsPage(actionInfo: ActionInfo, view: View, onCompleted: Runnable): Boolean {
        return false
    }

    fun openExecutor(configItem: ConfigItemBase, onExit: Runnable): ShellHandlerBase? {
        return null
    }

    interface AddToFavoritesHandler {
        fun onAddToFavorites(configItem: ConfigItemBase, intent: Intent?)
    }
}
