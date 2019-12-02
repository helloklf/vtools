package com.omarea.krscript.model

open class RunnableNode : ClickableNode() {

    // 是否在开始前显示操作确认提示
    var confirm: Boolean = false
    // 执行完成后是否自动关闭日志界面
    var autoOff: Boolean = false
    // 是否可中断执行
    var interruptable: Boolean = true
    // 是否在执行完以后重载整个界面
    var reloadPage: Boolean = false
    // 执行完之后要刷新的功能区域 (id)
    var updateBlocks: Array<String>? = null
    // 执行完成后是否自动关闭页面
    var autoFinish = false

    // 是否是后台运行的任务
    var backgroundTask: Boolean = false

    //
    var setState: String? = null
}
