package com.omarea.krscript.model

interface ActionLongClickHandler {
    fun addToFavorites(actionInfo: ActionInfo)
    fun addToFavorites(switchInfo: SwitchInfo)
}
