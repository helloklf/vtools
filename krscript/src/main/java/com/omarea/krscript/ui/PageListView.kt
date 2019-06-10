package com.omarea.krscript.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.AdapterView
import android.widget.ListView
import com.omarea.common.ui.OverScrollListView
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.model.PageClickHandler
import com.omarea.krscript.model.PageInfo
import java.util.*

class PageListView : OverScrollListView {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    private val progressBarDialog: ProgressBarDialog

    init {
        this.progressBarDialog = ProgressBarDialog(context)
    }

    fun setListData(pageInfos: ArrayList<PageInfo>?, pageClickHandler: PageClickHandler) {
        if (pageInfos != null) {
            this.overScrollMode = ListView.OVER_SCROLL_ALWAYS
            this.adapter = PageListAdapter(pageInfos)
            this.onItemClickListener = OnItemClickListener { parent, _, position, _ ->
                val item = parent.adapter.getItem(position) as PageInfo
                pageClickHandler.openPage(item)
            }
        }
    }
}
