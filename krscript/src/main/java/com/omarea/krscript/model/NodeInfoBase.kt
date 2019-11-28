package com.omarea.krscript.model

import java.util.*

open class NodeInfoBase {
    // 索引（自动生成）
    val index: String = UUID.randomUUID().toString()

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

}
