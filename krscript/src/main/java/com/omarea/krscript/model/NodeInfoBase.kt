package com.omarea.krscript.model

import java.util.*

open class NodeInfoBase {
    // 唯一标识（如果需要将功能添加到桌面作为快捷方式，则需要此标识来区分）
    var key: String = ""
    // 索引（自动生成）
    val index: String = UUID.randomUUID().toString()
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

}
