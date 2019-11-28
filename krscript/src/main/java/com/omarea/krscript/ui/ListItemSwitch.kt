package com.omarea.krscript.ui

import android.content.Context
import android.widget.Switch
import com.omarea.krscript.R
import com.omarea.krscript.model.SwitchNode

class ListItemSwitch(context: Context,
                     config: SwitchNode = SwitchNode()) : ListItemClickable(context, R.layout.kr_switch_list_item, config) {
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
    }
}
