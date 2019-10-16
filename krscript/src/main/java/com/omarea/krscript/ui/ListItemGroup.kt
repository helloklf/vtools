package com.omarea.krscript.ui

import android.content.Context
import com.omarea.krscript.model.GroupInfo

class ListItemGroup(context: Context,
                    layoutId: Int,
                    config: GroupInfo = GroupInfo()) : ListItemView(context, layoutId, config) {

    init {
        title = config.separator
    }
}
