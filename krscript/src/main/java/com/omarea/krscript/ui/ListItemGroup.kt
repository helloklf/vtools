package com.omarea.krscript.ui

import android.content.Context
import com.omarea.krscript.model.GroupInfo

class ListItemGroup(private val context: Context,
                    private val layoutId: Int,
                    private val config: GroupInfo = GroupInfo()) : ListItemView(context, layoutId, config) {

    init {
        title = config.separator
    }
}
