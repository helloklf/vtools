package com.omarea.krscript.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.omarea.krscript.R
import com.omarea.krscript.model.ActionInfo
import com.omarea.krscript.model.ConfigItemBase

open class ListItemView(private val context: Context,
                        private val layoutId: Int,
                        private val config: ConfigItemBase = ConfigItemBase()) {
    protected var layout = LayoutInflater.from(context).inflate(layoutId, null)
    protected var mOnClickListener: OnClickListener? = null
    protected var mOnLongClickListener: OnLongClickListener? = null

    protected var summaryView = layout.findViewById<TextView?>(R.id.kr_desc)
    protected var titleView = layout.findViewById<TextView?>(R.id.kr_title)
    protected var children = ArrayList<ListItemView>()


    var title: String
        get() {
            return titleView?.text.toString()
        }
        set(value) {
            if (value.isEmpty()) {
                titleView?.visibility = View.GONE
            } else {
                titleView?.text = value
                titleView?.visibility = View.VISIBLE
            }
        }

    var summary: String
        get() {
            return summaryView?.text.toString()
        }
        set(value) {
            if (value.isEmpty()) {
                summaryView?.visibility = View.GONE
            } else {
                summaryView?.text = value
                summaryView?.visibility = View.VISIBLE
            }
        }

    val index: String
        get() {
            return config.index
        }

    val key: String
        get() {
            return config.key
        }

    fun getView(): View {
        return layout
    }

    fun addView(item: ListItemView): ListItemView {
        val content = layout.findViewById<ViewGroup>(android.R.id.content)
        content.addView(item.getView())

        children.add(item)

        return this
    }

    fun findItemByKey(key: String): ListItemView? {
        if (this.key == key) {
            this.mOnClickListener?.onClick(this)
            return this
        }
        for (child in this.children) {
            val r = child.findItemByKey(key)
            if (r != null) {
                return r
            }
        }
        return null
    }

    fun findItemByIndex(index: String): ListItemView? {
        if (this.index == index) {
            this.mOnClickListener?.onClick(this)
            return this
        }
        for (child in this.children) {
            val r = child.findItemByKey(index)
            if (r != null) {
                return r
            }
        }
        return null
    }

    fun triggerActionByKey(key: String): Boolean {
        if ((this.key == key)) {
            this.mOnClickListener?.onClick(this)
            return true
        }
        for (child in this.children) {
            if (child.triggerActionByKey(key)) {
                return true
            }
        }
        return false
    }

    fun triggerActionByIndex(index: String): Boolean {
        if (this.index == index) {
            this.mOnClickListener?.onClick(this)
            return true
        }
        for (child in this.children) {
            if (child.triggerActionByIndex(key)) {
                return true
            }
        }
        return false
    }

    fun setOnClickListener(onClickListener: OnClickListener): ListItemView {
        this.mOnClickListener = onClickListener

        return this
    }

    fun setOnLongClickListener(onLongClickListener: OnLongClickListener): ListItemView {
        this.mOnLongClickListener = onLongClickListener

        return this
    }

    init {
        if (summaryView == null && config is ActionInfo) {
            summaryView = layout.rootView.findViewById<TextView?>(R.id.kr_desc)
        }

        title = config.title
        summary = config.desc

        this.layout.setOnClickListener {
            this.mOnClickListener?.onClick(this)
        }
        this.layout.setOnLongClickListener {
            this.mOnLongClickListener?.onLongClick(this)
            true
        }
    }

    interface OnClickListener {
        fun onClick(listItemView: ListItemView)
    }

    interface OnLongClickListener {
        fun onLongClick(listItemView: ListItemView)
    }
}
