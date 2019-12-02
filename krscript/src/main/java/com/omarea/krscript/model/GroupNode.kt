package com.omarea.krscript.model

class GroupNode : NodeInfoBase() {
    // 分组标题
    var separator: String = ""

    var supported: Boolean = true
    val children: ArrayList<NodeInfoBase> = ArrayList()
}
