package com.omarea.krscript.ui

import android.content.Context
import android.view.View
import com.omarea.krscript.R
import com.omarea.krscript.model.ClickableNode

open class ListItemClickable(context: Context,
                             layoutId: Int,
                             private val config: ClickableNode) : ListItemView(context, layoutId, config) {
    protected var mOnClickListener: OnClickListener? = null
    protected var mOnLongClickListener: OnLongClickListener? = null

    protected var shortcutIconView = layout.findViewById<View?>(R.id.kr_shortcut_icon)

    val key: String
        get() {
            return config.key
        }

    fun setOnClickListener(onClickListener: OnClickListener): ListItemClickable {
        this.mOnClickListener = onClickListener

        return this
    }

    fun setOnLongClickListener(onLongClickListener: OnLongClickListener): ListItemClickable {
        this.mOnLongClickListener = onLongClickListener

        return this
    }

    fun triggerAction() {
        this.mOnClickListener?.onClick(this)
    }

    init {
        title = config.title
        desc = config.desc
        summary = config.summary

        this.layout.setOnClickListener {
            this.mOnClickListener?.onClick(this)
        }
        if (!this.key.isEmpty()) {
            this.layout.setOnLongClickListener {
                this.mOnLongClickListener?.onLongClick(this)
                true
            }
        }

        if (this.key.isEmpty()) {
            shortcutIconView?.visibility = View.GONE
        } else {
            shortcutIconView?.visibility = View.VISIBLE
        }
    }

    interface OnClickListener {
        fun onClick(listItemView: ListItemClickable)
    }

    interface OnLongClickListener {
        fun onLongClick(listItemView: ListItemClickable)
    }
}
