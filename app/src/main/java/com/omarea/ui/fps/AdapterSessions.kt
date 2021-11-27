package com.omarea.ui.fps

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.omarea.model.FpsWatchSession
import com.omarea.vtools.R
import java.text.SimpleDateFormat
import java.util.*

class AdapterSessions(private val context: Context, private val list: ArrayList<FpsWatchSession>) : RecyclerView.Adapter<AdapterSessions.ViewHolder>() {
    private var keywords: String = ""

    fun getItem(position: Int): FpsWatchSession {
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

    // private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    fun updateRow(position: Int, viewHolder: ViewHolder) {
        val item = getItem(position)

        viewHolder.itemTitle?.text = keywordHighLight(item.appName)
        viewHolder.itemIcon?.setImageDrawable(item.appIcon)

        if (viewHolder.itemDesc != null)
            viewHolder.itemDesc?.text = dateFormat.format(Date(item.beginTime))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        internal var itemIcon: ImageView? = null
        internal var itemTitle: TextView? = null
        internal var itemDesc: TextView? = null
        internal var itemButton: ImageButton? = null
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    override fun getItemCount(): Int {
        return list.size ?: 0
    }
    private var onItemClickListener: OnItemClickListener? = null
    private var onItemDeleteClickListener: OnItemClickListener? = null

    //提供setter方法
    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemClickListener = onItemClickListener
    }
    //提供setter方法
    fun setOnItemDeleteClickListener(onItemClickListener: OnItemClickListener?) {
        this.onItemDeleteClickListener = onItemClickListener
    }

    fun removeItem(position: Int) {
        this.list.removeAt(position)
        // notifyItemRemoved(position)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // val convertView = View.inflate(context, R.layout.list_item_fps, null)
        val convertView = LayoutInflater.from(context).inflate(R.layout.list_item_fps, parent, false)
        val viewHolder = ViewHolder(convertView)
        viewHolder.itemIcon = convertView.findViewById(R.id.ItemIcon)
        viewHolder.itemTitle = convertView.findViewById(R.id.ItemTitle)
        viewHolder.itemDesc = convertView.findViewById(R.id.ItemDesc)
        viewHolder.itemButton = convertView.findViewById(R.id.download)

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.run {
            setOnClickListener {
                onItemClickListener?.onItemClick(position)
            }
        }
        holder.itemButton?.run {
            setOnClickListener {
                onItemDeleteClickListener?.onItemClick(position)
            }
        }

        this.updateRow(position, holder)
    }
}
