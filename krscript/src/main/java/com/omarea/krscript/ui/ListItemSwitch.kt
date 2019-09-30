package com.omarea.krscript.ui

import android.content.Context
import android.widget.Switch
import com.omarea.krscript.R
import com.omarea.krscript.model.SwitchInfo

class ListItemSwitch(private val context: Context,
                     private val layoutId: Int,
                     private val config: SwitchInfo = SwitchInfo()) : ListItemView(context, layoutId, config) {
    protected var switchView = layout.findViewById<Switch?>(R.id.kr_switch)

    var checked: Boolean
        get() {
            return if (switchView != null) switchView!!.isChecked else false
        }
        set(value) {
            switchView?.isChecked = value
        }

    init {
        checked = config.checked
        this.layout.setOnClickListener {
            this.checked = !config.checked
            this.mOnClickListener?.onClick(this)
        }
    }
}
