package com.omarea.krscript.model

class GroupInfo : ConfigItemBase() {
    var supported: Boolean = true
    val children: ArrayList<ConfigItemBase> = ArrayList()
}
