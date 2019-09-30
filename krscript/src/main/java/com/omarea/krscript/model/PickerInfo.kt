package com.omarea.krscript.model

import com.omarea.krscript.config.ActionParamInfo

class PickerInfo : ConfigItemBase() {
    var options: ArrayList<ActionParamInfo.ActionParamOption>? = null
    var optionsFromShell: ArrayList<HashMap<String, Any>>? = null
    var optionsSh = ""
    var value: String? = null

    var getState: String? = null
    var setState: String? = null

    // 参数名
    var name: String = ""
}
