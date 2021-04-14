package com.omarea.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.omarea.common.ui.OverScrollGridView
import com.omarea.library.basic.AppInfoLoader
import com.omarea.model.AppInfo
import com.omarea.vtools.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by Hello on 2018/01/26.
 */
class FreezeAppAdapter(private val context: Context, private var apps: ArrayList<AppInfo>) : BaseAdapter(), Filterable {
    private val appIconLoader = AppInfoLoader(context)
    private var filter: Filter? = null
    internal var filterApps: ArrayList<AppInfo> = apps
    private val mLock = Any()

    private class ArrayFilter(private var adapter: FreezeAppAdapter) : Filter() {

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            adapter.filterApps = results!!.values as ArrayList<AppInfo>
            if (results.count > 0) {
                adapter.notifyDataSetChanged()
            } else {
                adapter.notifyDataSetInvalidated()
            }
        }

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = Filter.FilterResults()
            val prefix: String = if (constraint == null) "" else constraint.toString()

            if (prefix.isEmpty()) {
                val list: ArrayList<AppInfo>
                synchronized(adapter.mLock) {
                    list = ArrayList<AppInfo>(adapter.apps)
                }
                results.values = list
                results.count = list.size
            } else {
                val prefixString = prefix.toLowerCase()

                val values: ArrayList<AppInfo>
                synchronized(adapter.mLock) {
                    values = ArrayList<AppInfo>(adapter.apps)
                }

                val count = values.size
                val newValues = ArrayList<AppInfo>()

                for (i in 0 until count) {
                    val value = values[i]
                    val valueText = value.appName!!.toString().toLowerCase()

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

    private val iconCaches = LruCache<String, Drawable>(100)

    init {
        filterApps.sortBy { !it.enabled || it.suspended }
    }

    override fun getCount(): Int {
        return filterApps.size
    }

    override fun getItem(position: Int): AppInfo {
        return filterApps[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.list_item_freeze_app, null)
        }
        updateRow(position, convertView!!)
        return convertView
    }

    fun updateRow(position: Int, listView: OverScrollGridView, appInfo: AppInfo) {
        try {
            val visibleFirstPosi = listView.firstVisiblePosition
            val visibleLastPosi = listView.lastVisiblePosition

            if (position >= visibleFirstPosi && position <= visibleLastPosi) {
                filterApps[position] = appInfo
                val view = listView.getChildAt(position - visibleFirstPosi)
                updateRow(position, view)
            }
        } catch (ex: Exception) {

        }
    }

    fun updateRow(position: Int, convertView: View) {
        val item = getItem(position)
        val viewHolder = ViewHolder()
        val packageName = item.packageName
        viewHolder.packageName = packageName
        viewHolder.itemTitle = convertView.findViewById(R.id.ItemTitle)
        viewHolder.imgView = convertView.findViewById(R.id.ItemIcon)
        viewHolder.imgView!!.setTag(getItem(position).packageName)
        viewHolder.itemTitle!!.text = item.appName.toString()
        viewHolder.imgView!!.alpha = if (item.enabled && !item.suspended) 1f else 0.3f

        if (item.icon == null) {
            viewHolder.run {
                GlobalScope.launch(Dispatchers.Main) {
                    val icon = appIconLoader.loadIcon(item.packageName).await()
                    val imgView = imgView!!
                    if (icon != null && viewHolder.packageName == packageName) {
                        imgView.setImageDrawable(icon)
                    }
                }
            }
        } else {
            viewHolder.imgView!!.setImageDrawable(item.icon)
        }
    }

    inner class ViewHolder {
        internal var packageName: String? = null

        internal var itemTitle: TextView? = null
        internal var imgView: ImageView? = null
    }
}
