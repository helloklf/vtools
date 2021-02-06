package com.omarea.krscript.model

open class ClickableNode(currentPageConfigPath: String) : NodeInfoBase(currentPageConfigPath) {
    // 功能图标路径（列表中）
    var iconPath = ""

    // 功能图标路径（桌面快捷）
    var logoPath = ""

    // 是否允许添加快捷方式（非false，且具有key则默认允许）
    var allowShortcut:Boolean? = null

    // 是否锁定
    var locked: Boolean = false
    // 锁定状态获取（脚本）
    var lockShell: String = ""

    // 此功能的Android SDK版本要求
    var targetSdkVersion = 0
    var minSdkVersion = 0
    var maxSdkVersion = 100
}
