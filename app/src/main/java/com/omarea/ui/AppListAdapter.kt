package com.omarea.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.omarea.model.Appinfo
import com.omarea.vtools.R
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import kotlin.Comparator

/**
 * Created by Hello on 2018/01/26.
 */

class AppListAdapter(apps: ArrayList<Appinfo>, private var keywords: String = "") : BaseAdapter() {
    private val list: ArrayList<Appinfo>?
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

    init {
        this.list = sortAppList(filterAppList(apps, keywords))
        for (i in this.list.indices) {
            states[i] = !(this.list[i].enabledState == null || !this.list[i].selectState)
        }
    }

    override fun getCount(): Int {
        return list?.size ?: 0
    }

    override fun getItem(position: Int): Appinfo {
        return list!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private fun keywordSearch(item: Appinfo, text: String): Boolean {
        return item.packageName.toString().toLowerCase().contains(text)
                || item.appName.toString().toLowerCase().contains(text)
                || item.path.toString().toLowerCase().contains(text)
    }

    private fun filterAppList(appList: ArrayList<Appinfo>, keywords: String): ArrayList<Appinfo> {
        val text = keywords.toLowerCase()
        if (text.isEmpty())
            return appList
        return ArrayList(appList.filter { item ->
            keywordSearch(item, text)
        })
    }

    private fun sortAppList(list: ArrayList<Appinfo>): ArrayList<Appinfo> {
        list.sortWith(Comparator { l, r ->
            val les = l.enabledState.toString()
            val res = r.enabledState.toString()
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

    fun getSelectedItems(): ArrayList<Appinfo> {
        val states = states
        val selectedItems = states.keys
                .filter { states[it] == true }
                .mapTo(ArrayList()) { getItem(it) }

        if (selectedItems.size == 0) {
            return ArrayList()
        }
        return selectedItems
    }

    private fun loadIcon(context: Context, viewHolder: ViewHolder, item: Appinfo) {
        Thread(Runnable {
            var icon: Drawable? = null
            try {
                val installInfo = context.packageManager.getPackageInfo(item.packageName.toString(), 0)
                icon = installInfo.applicationInfo.loadIcon(context.packageManager)
            } catch (ex: Exception) {
                try {
                    val file = File(item.path.toString())
                    if (file.exists() && file.canRead()) {
                        val pm = context.packageManager
                        icon = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_ACTIVITIES).applicationInfo.loadIcon(pm)
                    }
                } catch (ex: Exception) {
                }
            } finally {
                if (icon != null) {
                    viewHolder.imgView!!.post {
                        viewHolder.imgView!!.setImageDrawable(icon)
                    }
                }
            }
        }).start()
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
            viewHolder!!.itemTitle = convertView!!.findViewById(R.id.ItemTitle)
            viewHolder!!.enabledStateText = convertView.findViewById(R.id.ItemEnabledStateText)
            viewHolder!!.itemText = convertView.findViewById(R.id.ItemText)
            viewHolder!!.imgView = convertView.findViewById(R.id.ItemIcon)
            viewHolder!!.itemChecke = convertView.findViewById(R.id.select_state)
            viewHolder!!.itemPath = convertView.findViewById(R.id.ItemPath)
            viewHolder!!.imgView!!.setTag(getItem(position).packageName)
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

        val item = getItem(position)
        viewHolder!!.itemTitle!!.text = keywordHightLight(item.appName.toString())
        viewHolder!!.itemText!!.text = keywordHightLight(item.packageName.toString())
        if (item.icon == null) {
            loadIcon(context, viewHolder!!, item)
        } else {
            viewHolder!!.imgView!!.setImageDrawable(item.icon)
        }
        if (item.enabledState != null)
            viewHolder!!.enabledStateText!!.text = item.enabledState
        else
            viewHolder!!.enabledStateText!!.text = ""

        //为checkbox添加复选监听,把当前位置的checkbox的状态存进一个HashMap里面
        viewHolder!!.itemChecke!!.setOnCheckedChangeListener { buttonView, isChecked ->
            states[position] = isChecked
        }
        //从hashmap里面取出我们的状态值,然后赋值给listview对应位置的checkbox
        viewHolder!!.itemChecke!!.setChecked(states[position] == true)

        viewHolder!!.itemPath!!.text = item.path

        return convertView
    }

    inner class ViewHolder {
        internal var itemTitle: TextView? = null
        internal var itemChecke: CheckBox? = null
        internal var imgView: ImageView? = null
        internal var itemText: TextView? = null
        internal var enabledStateText: TextView? = null
        internal var itemPath: TextView? = null
    }
}
