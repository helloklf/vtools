package com.omarea.krscript.model

import java.io.File

public class PageInfo : ConfigItemBase {
    constructor(parentPageConfigPath: String) : super() {
        this.parentPageConfigPath = parentPageConfigPath
    }

    public var parentPageConfigPath: String = ""
    public val parentPageConfigDir: String
        get() {
            if (parentPageConfigPath.isNotEmpty()) {
                return File(parentPageConfigPath).parent
            }
            return ""
        }

    public var pageConfigPath: String = ""
    public var pageConfigSh: String = ""
    public var onlineHtmlPage: String = ""
    public var statusBar: String = ""

    // 读取页面配置前
    public var beforeRead = ""
    // 读取页面配置后
    public var afterRead = ""

    // 页面加载失败
    public var loadSuccess = ""
    // 页面加载成功
    public var loadFail = ""
}
