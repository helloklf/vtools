package com.omarea.ui

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.omarea.vtools.R
import java.util.*

/* 测试 */
class AdapterModules(private val context: Context, private val list: ArrayList<String>) : RecyclerView.Adapter<AdapterModules.ViewHolder>() {
    private var keywords: String = ""

    fun getItem(position: Int): String {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private fun keywordHighLight(str: String): SpannableString {
        val spannableString = SpannableString(str)
        if (keywords.isEmpty()) {
            return spannableString
        }
        val index = str.toLowerCase(Locale.getDefault()).indexOf(keywords.toLowerCase(Locale.getDefault()))
        if (index < 0)
            return spannableString

        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#0094ff")), index, index + keywords.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString
    }

    private fun getResourceColor(colorId: Int): Int {
        val theme = context.theme
        val resources = context.resources
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            resources.getColor(colorId, theme)
        } else {
            resources.getColor(colorId)
        }
    }

    fun updateRow(position: Int, viewHolder: ViewHolder) {
        val item = getItem(position)

        viewHolder.itemTitle?.text = keywordHighLight(item.substring(item.indexOf("/") + 1))
        viewHolder.itemSource?.text = "@Magisk-Modules-Repo"

        if (viewHolder.itemDesc != null)
            viewHolder.itemDesc?.text = ""
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        internal var itemTitle: TextView? = null
        internal var itemDesc: TextView? = null
        internal var itemSource: TextView? = null
        internal var itemButton: Button? = null
    }

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    override fun getItemCount(): Int {
        return list.size ?: 0
    }
    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemClickListener? = null

    //提供setter方法
    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
    }
    //提供setter方法
    fun setOnItemLongClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemLongClickListener = onItemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // val convertView = View.inflate(context, R.layout.list_item_module, null)
        val convertView = LayoutInflater.from(context).inflate(R.layout.list_item_module, parent, false)
        val viewHolder = ViewHolder(convertView)
        viewHolder.itemTitle = convertView.findViewById(R.id.ItemTitle)
        viewHolder.itemDesc = convertView.findViewById(R.id.ItemDesc)
        viewHolder.itemSource = convertView.findViewById(R.id.ItemSource)
        viewHolder.itemButton = convertView.findViewById(R.id.download)

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemButton?.run {
            setOnClickListener {
                onItemClickListener?.onItemClick(this, position)
            }
            setOnLongClickListener {
                onItemLongClickListener?.onItemClick(this, position)
                true
            }
        }

        this.updateRow(position, holder)
    }
}
