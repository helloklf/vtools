package com.omarea.krscript.model

import android.text.Layout

class TextNode(currentPageConfigPath: String) : NodeInfoBase(currentPageConfigPath) {
    val rows = ArrayList<TextRow>()

    class TextRow {
        // 文字大小
        internal var size: Int = -1
        // 文字颜色
        internal var color: Int = -1
        // 文字背景色
        internal var bgColor: Int = -1
        // 是否加粗
        internal var bold: Boolean = false
        // 是否斜体
        internal var italic: Boolean = false
        // 是否显示下划线
        internal var underline: Boolean = false
        // 是否换行后显示
        internal var breakRow: Boolean = false
        // 对齐方式
        internal var align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
        // 点击后要跳转的网页链接
        internal var link: String = ""
        // 点击后要打开的活动
        internal var activity: String = ""
        // 文本内容
        internal var text: String = ""
        // 动态获取文本内容的脚本
        internal var dynamicTextSh: String = ""
        // 点击后执行的脚本
        internal var onClickScript: String = ""
    }
}
