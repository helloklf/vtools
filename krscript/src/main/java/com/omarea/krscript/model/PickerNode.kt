package com.omarea.krscript.model

class PickerNode : RunnableNode() {
    var options: ArrayList<ActionParamInfo.ActionParamOption>? = null
    var optionsSh = ""
    var value: String? = null

    var getState: String? = null

    // 参数名
    var name: String = ""
    // 是否允许多选
    var multiple: Boolean = false
    // 多个值的分隔符
    var separator: String = "\n"
}
