package com.omarea.common.model


/*
示例1： 用于Spinner
ArrayAdapter(context, R.layout.kr_spinner_default, R.id.text, options).apply {
    setDropDownViewResource(R.layout.kr_spinner_dropdown)
}
*/

class SelectItem {
    var title: String? = null
    // var desc: String = ""
    var value: String? = null
    var selected: Boolean = false

    override fun toString(): String {
        if (!title.isNullOrEmpty()) {
            return title!!
        } else if (!value.isNullOrEmpty()) {
            return value!!
        } else {
            return "" // super.toString()
        }
    }
}