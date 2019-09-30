package com.omarea.krscript.ui

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.omarea.krscript.R
import com.omarea.krscript.model.PickerInfo

class ListItemPicker(private val context: Context,
                     private val layoutId: Int,
                     private val config: PickerInfo = PickerInfo()) : ListItemView(context, layoutId, config) {
    private val widgetView = layout.findViewById<ImageView?>(R.id.kr_widget)

    init {
        widgetView?.visibility = View.VISIBLE
        widgetView?.setImageDrawable(context.getDrawable(R.drawable.kr_picker))
    }
}
