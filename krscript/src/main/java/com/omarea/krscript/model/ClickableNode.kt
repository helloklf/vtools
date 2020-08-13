package com.omarea.krscript.model

import java.io.File

open class ClickableNode(currentPageConfigPath: String) : NodeInfoBase(currentPageConfigPath) {
    // 功能图标路径（列表中）
    var iconPath = ""

    // 功能图标路径（桌面快捷）
    var logoPath = ""

    // 是否允许添加快捷方式（非false，且具有key则默认允许）
    var allowShortcut:Boolean? = null
}
