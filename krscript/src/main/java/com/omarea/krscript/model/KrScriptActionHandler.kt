package com.omarea.krscript.model

import android.content.Intent
import android.view.View
import com.omarea.krscript.ui.FileChooserRender

interface KrScriptActionHandler {
    fun openFileChooser(fileSelectedInterface: FileChooserRender.FileSelectedInterface): Boolean
    fun onSubPageClick(pageNode: PageNode)
    fun onActionCompleted(runnableNode: RunnableNode)
    fun addToFavorites(clickableNode: ClickableNode, addToFavoritesHandler: AddToFavoritesHandler)
    fun openParamsPage(actionNode: ActionNode, view: View, onCompleted: Runnable): Boolean {
        return false
    }

    interface AddToFavoritesHandler {
        fun onAddToFavorites(clickableNode: ClickableNode, intent: Intent?)
    }
}
