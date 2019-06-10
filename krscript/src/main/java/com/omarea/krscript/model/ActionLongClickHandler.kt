package com.omarea.krscript.model

import com.omarea.krscript.model.PageInfo

interface ActionLongClickHandler {
    fun addToFavorites(actionInfo: ActionInfo)
    fun addToFavorites(switchInfo: SwitchInfo)
}
