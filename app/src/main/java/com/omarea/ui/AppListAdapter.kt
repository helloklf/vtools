package com.omarea.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.omarea.library.basic.AppInfoLoader
import com.omarea.model.AppInfo
import com.omarea.vtools.R
import kotlinx.coroutines.*
import java.util.ArrayList
import java.util.HashMap
import kotlin.Comparator

/**
 * Created by Hello on 2018/01/26.
 */

class AppListAdapter(private val context: Context, apps: ArrayList<AppInfo>, private var keywords: String = "") : BaseAdapter() {
    private val list: ArrayList<AppInfo>?
    private val appInfoLoader = AppInfoLoader(context)

    @SuppressLint("UseSparseArrays")
    var states = HashMap<Int, Boolean>()

    //private val mImageCache: LruCache<String, Drawable> = LruCache(20)

    private var viewHolder: AppListAdapter.ViewHolder? = null

    fun setSelecteStateAll(selected: Boolean = true) {
        for (item in states) {
            states[item.key] = selected
        }
    }

    fun getIsAllSelected(): Boolean {
        var count = 0
        for (item in states) {
            if (item.value == true) {
                count++
            }
        }
        return count == this.list!!.size
    }

    fun hasSelected(): Boolean {
        return this.states.filter { it.value == true }.isNotEmpty()
    }

    init {
        this.list = sortAppList(filterAppList(apps, keywords))
        for (i in this.list.indices) {
            states[i] = !(this.list[i].stateTags == null || !this.list[i].selected)
        }
    }

    override fun getCount(): Int {
        return list?.size ?: 0
    }

    override fun getItem(position: Int): AppInfo {
        return list!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private fun keywordSearch(item: AppInfo, text: String): Boolean {
        return item.packageName.toLowerCase().contains(text)
                || item.appName.toLowerCase().contains(text)
                || item.path.toString().toLowerCase().contains(text)
    }

    private fun filterAppList(appList: ArrayList<AppInfo>, keywords: String): ArrayList<AppInfo> {
        val text = keywords.toLowerCase()
        if (text.isEmpty())
            return appList
        return ArrayList(appList.filter { item ->
            keywordSearch(item, text)
        })
    }

    private fun sortAppList(list: ArrayList<AppInfo>): ArrayList<AppInfo> {
        list.sortWith(Comparator { l, r ->
            val les = l.stateTags.toString()
            val res = r.stateTags.toString()
            when {
                les < res -> -1
                les > res -> 1
                else -> {
                    val lp = l.packageName.toString()
                    val rp = r.packageName.toString()
                    when {
                        lp < rp -> -1
                        lp > rp -> 1
                        else -> 0
                    }
                }
            }
        })
        return list
    }

    fun getSelectedItems(): ArrayList<AppInfo> {
        val states = states
        val selectedItems = states.keys
                .filter { states[it] == true }
                .mapTo(ArrayList()) { getItem(it) }

        if (selectedItems.size == 0) {
            return ArrayList()
        }
        return selectedItems
    }

    private fun keywordHightLight(str: String): SpannableString {
        val spannableString = SpannableString(str)
        var index = 0
        if (keywords.isEmpty()) {
            return spannableString;
        }
        index = str.toLowerCase().indexOf(keywords.toLowerCase());
        if (index < 0)
            return spannableString

        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#0094ff")), index, index + keywords.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString;
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view
        val context = parent.context
        if (convertView == null) {
            viewHolder = ViewHolder()
            convertView = View.inflate(context, R.layout.list_item_app, null)
            viewHolder?.run {
                itemTitle = convertView!!.findViewById(R.id.ItemTitle)
                enabledStateText = convertView.findViewById(R.id.ItemEnabledStateText)
                itemText = convertView.findViewById(R.id.ItemText)
                imgView = convertView.findViewById(R.id.ItemIcon)
                itemChecke = convertView.findViewById(R.id.select_state)
                // itemPath = convertView.findViewById(R.id.ItemPath)
                imgView!!.setTag(getItem(position).packageName)
            }
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }
        viewHolder?.run {
            val item = getItem(position)
            itemTitle?.text = keywordHightLight(item.appName)
            itemText?.text = keywordHightLight(item.packageName)

            if (item.icon == null) {
                val id = item.path
                this.appPath = id
                GlobalScope.launch(Dispatchers.Main) {
                    val icon = appInfoLoader.loadIcon(item).await()
                    val imgView = imgView!!
                    if (icon != null && appPath == id) {
                        imgView.setImageDrawable(icon)
                    }
                }
            } else {
                imgView!!.setImageDrawable(item.icon)
            }

            enabledStateText?.run {
                if (item.stateTags.isNullOrEmpty()) {
                    text = ""
                    visibility = View.GONE
                } else {
                    text = item.stateTags
                    visibility = View.VISIBLE
                }
            }

            //为checkbox添加复选监听,把当前位置的checkbox的状态存进一个HashMap里面
            itemChecke?.setOnCheckedChangeListener { buttonView, isChecked ->
                states[position] = isChecked
            }
            //从hashmap里面取出我们的状态值,然后赋值给listview对应位置的checkbox
            itemChecke?.setChecked(states[position] == true)

            // viewHolder?.itemPath?.text = item.path
        }

        return convertView!!
    }

    inner class ViewHolder {
        internal var appPath: CharSequence? = null

        internal var itemTitle: TextView? = null
        internal var itemChecke: CheckBox? = null
        internal var imgView: ImageView? = null
        internal var itemText: TextView? = null
        internal var enabledStateText: TextView? = null
        internal var itemPath: TextView? = null
    }
}
