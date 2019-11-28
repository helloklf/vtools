package com.omarea.krscript.model

open class ClickableNode : NodeInfoBase() {

    // 唯一标识（如果需要将功能添加到桌面作为快捷方式，则需要此标识来区分）
    var key: String = ""

    // 功能图标路径
    var iconPath = ""

}
