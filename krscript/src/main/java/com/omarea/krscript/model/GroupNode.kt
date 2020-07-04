package com.omarea.krscript.model

class GroupNode(currentPageConfigPath: String) : NodeInfoBase(currentPageConfigPath){
    var supported: Boolean = true
    val children: ArrayList<NodeInfoBase> = ArrayList()
}
