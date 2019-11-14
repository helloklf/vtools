package com.omarea.krscript.model

import java.util.*

open class ConfigItemBase {
    // 索引（自动生成）
    val index: String = UUID.randomUUID().toString()

    // 唯一标识（如果需要将功能添加到桌面作为快捷方式，则需要此标识来区分）
    var key: String = ""

    // 分组标题
    var separator: String = ""
    // 标题
    var title: String = ""
    // 描述
    var desc: String = ""
    // 描述（脚本）
    var descSh: String = ""
    // 摘要信息
    var summary: String = ""
    // 摘要信息(脚本)
    var summarySh: String = ""

    // 是否在开始前显示操作确认提示
    var confirm: Boolean = false
    // 执行完成后是否自动关闭界面
    var autoOff: Boolean = false
    // 是否可中断执行
    var interruptable: Boolean = true
    // 是否在执行完以后重载整个界面
    var reloadPage: Boolean = false

    // 是否是后台运行的任务
    var backgroundTask: Boolean = false
}
