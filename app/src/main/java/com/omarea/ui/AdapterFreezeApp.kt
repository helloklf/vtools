package com.omarea.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.omarea.common.ui.OverScrollGridView
import com.omarea.library.basic.AppInfoLoader
import com.omarea.model.AppInfo
import com.omarea.vtools.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class AdapterFreezeApp(private val context: Context, private var apps: ArrayList<AppInfo>) : BaseAdapter(), Filterable {
    private val appIconLoader = AppInfoLoader(context)
    private var filter: Filter? = null
    internal var filterApps: ArrayList<AppInfo> = apps
    private val mLock = Any()

    private class ArrayFilter(private var adapter: AdapterFreezeApp) : Filter() {
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            adapter.filterApps = results!!.values as ArrayList<AppInfo>
            if (results.count > 0) {
                adapter.notifyDataSetChanged()
            } else {
                adapter.notifyDataSetInvalidated()
            }
        }

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            val prefix: String = if (constraint == null) "" else constraint.toString()

            if (prefix.isEmpty()) {
                val list: ArrayList<AppInfo>
                synchronized(adapter.mLock) {
                    list = ArrayList<AppInfo>(adapter.apps)
                }
                results.values = list
                results.count = list.size
            } else {
                val prefixString = prefix.toLowerCase(Locale.getDefault())

                val values: ArrayList<AppInfo>
                synchronized(adapter.mLock) {
                    values = ArrayList<AppInfo>(adapter.apps)
                }

                val count = values.size
                val newValues = ArrayList<AppInfo>()

                for (i in 0 until count) {
                    val value = values[i]
                    val valueText = value.appName.toLowerCase(Locale.getDefault())

                    if (valueText.contains(prefixString)) {
                        newValues.add(value)
                    } else {
                        val words = valueText.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                        val wordCount = words.size

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

    init {
        filterApps.sortBy { !it.enabled || it.suspended }
    }

    override fun getCount(): Int {
        return filterApps.size + 1
    }

    override fun getItem(position: Int): AppInfo {
        if (position < filterApps.size) {
            return filterApps[position]
        } else {
            // 虚拟一个listItem 用于显示列表末尾的添加按钮
            return AppInfo.getItem().apply {
                packageName = "plus"
                appName = "添加应用"
            }
        }
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
            val visibleFirst = listView.firstVisiblePosition
            val visibleLast = listView.lastVisiblePosition

            if (position in visibleFirst..visibleLast) {
                filterApps[position] = appInfo
                val view = listView.getChildAt(position - visibleFirst)
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
        viewHolder.imgView!!.tag = getItem(position).packageName
        viewHolder.itemTitle!!.text = item.appName
        viewHolder.imgView!!.alpha = if (item.enabled && !item.suspended) 1f else 0.3f

        if (item.packageName == "plus") {
            viewHolder.imgView!!.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.icon_add_app))
        } else {
            viewHolder.run {
                GlobalScope.launch(Dispatchers.Main) {
                    val icon = appIconLoader.loadIcon(item.packageName).await()
                    val imgView = imgView!!
                    if (icon != null && viewHolder.packageName == packageName) {
                        imgView.setImageDrawable(icon)
                    }
                }
            }
        }
    }

    inner class ViewHolder {
        internal var packageName: String? = null

        internal var itemTitle: TextView? = null
        internal var imgView: ImageView? = null
    }
}
