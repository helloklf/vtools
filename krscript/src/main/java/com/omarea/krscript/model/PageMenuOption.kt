package com.omarea.krscript.model

public class PageMenuOption(currentConfigXml: String) : RunnableNode(currentConfigXml) {
    // 类型为普通菜单项还是其它具有特定行为的菜单项
    // 例如，类型为finish 点击后会关闭当前页面，类型为refresh点击后会刷新当前页面，而类型为file点击后则需要先选择文件
    public var type: String = ""
    // 是否显示为悬浮按钮
    public var isFab = false;

    // 文件mime类型（仅限type=file有效）
    var mime: String = ""
    // 文件后缀（仅限type=file有效）
    var suffix: String = ""
}