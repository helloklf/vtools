package com.omarea.krscript.model

public class PageNode(currentConfigXml: String) : ClickableNode(currentConfigXml) {
    public var pageConfigPath: String = ""
    public var pageConfigSh: String = ""
    public var onlineHtmlPage: String = ""
    // 点击后要跳转的网页链接
    public var link: String = ""
    // 点击后要打开的活动
    public var activity: String = ""

    // 读取页面配置前
    public var beforeRead = ""
    // 读取页面配置后
    public var afterRead = ""

    // 菜单选项设置
    public var pageMenuOptions: ArrayList<PageMenuOption>? = null
    public var pageMenuOptionsSh: String = ""
    // 处理菜单和悬浮按钮点击事件的脚本
    public var pageHandlerSh:  String = ""

    // 页面加载失败
    public var loadSuccess = ""
    // 页面加载成功
    public var loadFail = ""
}
