package com.omarea.krscript.model

import android.text.Layout

class TextInfo : ConfigItemBase() {
    val rows = ArrayList<TextRow>()

    class TextRow {
        internal var size: Int = -1
        internal var color: Int = -1
        internal var bgColor: Int = -1
        internal var bold: Boolean = false
        internal var italic: Boolean = false
        internal var underline: Boolean = false
        internal var breakRow: Boolean = false
        internal var align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
        internal var link: String = ""
        internal var activity: String = ""
        internal var text: String = ""
    }
}
