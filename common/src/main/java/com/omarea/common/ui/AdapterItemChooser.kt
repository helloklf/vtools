package com.omarea.common.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.omarea.common.R
import com.omarea.common.model.SelectItem
import java.util.*

class AdapterItemChooser(private val context: Context, private var items: ArrayList<SelectItem>, private val multiple: Boolean) : BaseAdapter(), Filterable {
    interface SelectStateListener {
        fun onSelectChange(selected: List<SelectItem>)
    }

    private var selectStateListener: SelectStateListener? = null
    private var filter: Filter? = null
    internal var filterItems: ArrayList<SelectItem> = items
    private val mLock = Any()

    private class ArrayFilter(private var adapter: AdapterItemChooser) : Filter() {
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            adapter.filterItems = results!!.values as ArrayList<SelectItem>
            if (results.count > 0) {
                adapter.notifyDataSetChanged()
            } else {
                adapter.notifyDataSetInvalidated()
            }
        }

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = Filter.FilterResults()
            val prefix: String = constraint?.toString() ?: ""

            if (prefix.isEmpty()) {
                val list: ArrayList<SelectItem>
                synchronized(adapter.mLock) {
                    list = ArrayList<SelectItem>(adapter.items)
                }
                results.values = list
                results.count = list.size
            } else {
                val prefixString = prefix.toLowerCase()

                val values: ArrayList<SelectItem>
                synchronized(adapter.mLock) {
                    values = ArrayList<SelectItem>(adapter.items)
                }
                val selected = adapter.getSelectedItems()

                val count = values.size
                val newValues = ArrayList<SelectItem>()

                for (i in 0 until count) {
                    val value = values[i]
                    if (selected.contains(value)) {
                        newValues.add(value)
                    } else {
                        val valueText = if (value.title == null) "" else value.title!!.toLowerCase()

                        // First match against the whole, non-splitted value
                        if (valueText.contains(prefixString)) {
                            newValues.add(value)
                        } else {
                            val words = valueText.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                            val wordCount = words.size

                            // Start at index 0, in case valueText starts with space(s)
                            for (k in 0 until wordCount) {
                                if (words[k].contains(prefixString)) {
                                    newValues.add(value)
                                    break
                                }
                            }
                        }
                    }
                }

                results.values = newValues
                results.count = newValues.size
            }

            return results
        }
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = ArrayFilter(this)
        }
        return filter!!
    }

    override fun getCount(): Int {
        return filterItems.size
    }

    override fun getItem(position: Int): SelectItem {
        return filterItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view
        if (convertView == null) {
            convertView = View.inflate(context, if (multiple) {
                R.layout.item_multiple_chooser_item
            } else {
                R.layout.item_single_chooser_item
            }, null)
        }
        updateRow(position, convertView!!)
        return convertView
    }

    fun updateRow(position: Int, listView: OverScrollGridView, SelectItem: SelectItem) {
        try {
            val visibleFirstPosi = listView.firstVisiblePosition
            val visibleLastPosi = listView.lastVisiblePosition

            if (position >= visibleFirstPosi && position <= visibleLastPosi) {
                filterItems[position] = SelectItem
                val view = listView.getChildAt(position - visibleFirstPosi)
                updateRow(position, view)
            }
        } catch (ex: Exception) {
        }
    }

    fun updateRow(position: Int, convertView: View) {
        val item = getItem(position)
        val viewHolder = ViewHolder()
        viewHolder.itemTitle = convertView.findViewById(R.id.ItemTitle)
        viewHolder.itemDesc = convertView.findViewById(R.id.ItemDesc)
        viewHolder.imgView = convertView.findViewById(R.id.ItemIcon)
        viewHolder.checkBox = convertView.findViewById(R.id.ItemChecBox)

        convertView.setOnClickListener {
            if (multiple) {
                item.selected = !item.selected
                viewHolder.checkBox?.isChecked = item.selected
            } else {
                if (item.selected) {
                    return@setOnClickListener
                } else {
                    val current = items.find { it.selected }
                    current?.selected = false
                    item.selected = true
                    notifyDataSetChanged()
                }
            }
            selectStateListener?.onSelectChange(getSelectedItems())
        }

        viewHolder.itemTitle?.text = item.title
        viewHolder.itemDesc?.run{
            if (item.title.isNullOrEmpty()) {
                text = item.title
            } else {
                visibility = View.GONE
            }
        }
        viewHolder.checkBox?.isChecked = item.selected
    }

    fun setSelectAllState(allSelected: Boolean) {
        items.forEach {
            it.selected = allSelected
        }
        notifyDataSetChanged()
    }

    fun setSelectStateListener(selectStateListener: SelectStateListener?) {
        this.selectStateListener = selectStateListener
    }

    fun getSelectedItems(): List<SelectItem> {
        return items.filter { it.selected }
    }

    fun getSelectStatus (): BooleanArray {
        return items.map { it.selected }.toBooleanArray()
    }

    inner class ViewHolder {
        internal var itemTitle: TextView? = null
        internal var itemDesc: TextView? = null
        internal var imgView: ImageView? = null
        internal var checkBox: CompoundButton? = null
    }
}
