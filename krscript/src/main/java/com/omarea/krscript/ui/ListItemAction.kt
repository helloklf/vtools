package com.omarea.krscript.ui

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.omarea.krscript.R
import com.omarea.krscript.model.ActionInfo

class ListItemAction(private val context: Context,
                     private val layoutId: Int,
                     private val config: ActionInfo = ActionInfo()) : ListItemView(context, layoutId, config) {
    private val widgetView = layout.findViewById<ImageView?>(R.id.kr_widget)

    init {
        widgetView?.visibility = View.VISIBLE
        if (config.params != null && config.params!!.size > 0) {
            widgetView?.setImageDrawable(context.getDrawable(R.drawable.kr_list))
        } else {
            widgetView?.setImageDrawable(context.getDrawable(R.drawable.kr_run))
        }
    }
}
