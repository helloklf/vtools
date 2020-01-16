package com.omarea.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.omarea.common.ui.OverScrollGridView
import com.omarea.model.Appinfo
import com.omarea.vtools.R
import java.util.*

/**
 * Created by Hello on 2018/01/26.
 */
class FreezeAppAdapter(private val context: Context, private var apps: ArrayList<Appinfo>) : BaseAdapter(), Filterable {
    private var filter: Filter? = null
    internal var filterApps: ArrayList<Appinfo> = apps
    private val mLock = Any()

    private class ArrayFilter(private var adapter: FreezeAppAdapter) : Filter() {

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            adapter.filterApps = results!!.values as ArrayList<Appinfo>
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
                val list: ArrayList<Appinfo>
                synchronized(adapter.mLock) {
                    list = ArrayList<Appinfo>(adapter.apps)
                }
                results.values = list
                results.count = list.size
            } else {
                val prefixString = prefix.toLowerCase()

                val values: ArrayList<Appinfo>
                synchronized(adapter.mLock) {
                    values = ArrayList<Appinfo>(adapter.apps)
                }

                val count = values.size
                val newValues = ArrayList<Appinfo>()

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
        filterApps.sortBy { !it.enabled }
    }

    override fun getCount(): Int {
        return filterApps.size
    }

    override fun getItem(position: Int): Appinfo {
        return filterApps[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private fun loadIcon(viewHolder: ViewHolder, packageName: String) {
        Thread(Runnable {
            try {
                val icon: Drawable? = iconCaches.get(packageName)
                if (icon == null) {
                    val installInfo = context.packageManager.getPackageInfo(packageName, 0)
                    iconCaches.put(packageName, installInfo.applicationInfo.loadIcon(context.packageManager))
                }
            } catch (ex: Exception) {
            } finally {
                val icon: Drawable? = iconCaches.get(packageName)
                if (icon != null) {
                    viewHolder.imgView!!.post {
                        viewHolder.imgView!!.setImageDrawable(icon)
                    }
                }
            }
        }).start()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.list_item_freeze_app, null)
        }
        updateRow(position, convertView!!)
        return convertView
    }

    fun updateRow(position: Int, listView: OverScrollGridView, appinfo: Appinfo) {
        try {
            val visibleFirstPosi = listView.firstVisiblePosition
            val visibleLastPosi = listView.lastVisiblePosition

            if (position >= visibleFirstPosi && position <= visibleLastPosi) {
                filterApps[position] = appinfo
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
        viewHolder.imgView = convertView.findViewById(R.id.ItemIcon)
        viewHolder.imgView!!.setTag(getItem(position).packageName)
        viewHolder.itemTitle!!.text = item.appName.toString()
        viewHolder.imgView!!.alpha = if (item.enabled) 1f else 0.3f
        if (item.icon == null) {
            loadIcon(viewHolder, item.packageName.toString())
        } else {
            viewHolder.imgView!!.setImageDrawable(item.icon)
        }
    }

    inner class ViewHolder {
        internal var itemTitle: TextView? = null
        internal var imgView: ImageView? = null
    }
}
