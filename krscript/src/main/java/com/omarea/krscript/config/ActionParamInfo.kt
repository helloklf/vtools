package com.omarea.krscript.config

import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ActionParamInfo {
    // 参数名：必需保持唯一
    var name: String? = null

    var title: String? = null

    var label: String? = null

    // 描述
    var desc: String? = null

    // 值
    var value: String? = null
    var valueShell: String? = null
    var valueFromShell: String? = null
    var maxLength = -1 // input only
    var type: String? = null
    var max: Int = Int.MAX_VALUE // seekbar only
    var min: Int = Int.MIN_VALUE // seekbar only
    var required: Boolean = false // 是否是必需的
    var readonly: Boolean = false
    var options: ArrayList<ActionParamOption>? = null
    var optionsFromShell: ArrayList<HashMap<String, Any>>? = null
    var optionsSh = ""
    // 是否支持
    var supported: Boolean = true

    open class ActionParamOption {
        var value: String? = null
        var desc: String? = null
    }
}
