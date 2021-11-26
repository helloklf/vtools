package com.omarea.ui

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.omarea.common.ui.OverScrollListView
import com.omarea.library.basic.AppInfoLoader
import com.omarea.model.AppInfo
import com.omarea.vtools.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class XposedAppsAdapter(private val context: Context, apps: ArrayList<AppInfo>) : BaseAdapter() {
    private val appIconLoader = AppInfoLoader(context)
    private var keywords: String = ""
    private val list: ArrayList<AppInfo>?

    init {
        this.list = filterAppList(apps, keywords)
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
        return item.packageName.toLowerCase(Locale.getDefault()).contains(text)
                || item.appName.toLowerCase(Locale.getDefault()).contains(text)
                || item.path.toString().toLowerCase(Locale.getDefault()).contains(text)
    }

    private fun filterAppList(appList: ArrayList<AppInfo>, keywords: String): ArrayList<AppInfo> {
        val text = keywords.toLowerCase(Locale.getDefault())
        if (text.isEmpty())
            return appList
        return ArrayList(appList.filter { item ->
            keywordSearch(item, text)
        })
    }

    private fun keywordHightLight(str: String): SpannableString {
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

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.list_item_xposed_app, null)
        }
        updateRow(position, convertView!!)
        return convertView
    }

    fun updateRow(position: Int, listView: OverScrollListView, appInfo: AppInfo) {
        try {
            val visibleFirstPosi = listView.firstVisiblePosition
            val visibleLastPosi = listView.lastVisiblePosition

            if (position >= visibleFirstPosi && position <= visibleLastPosi) {
                list!![position] = appInfo
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

        viewHolder.itemTitle?.text = keywordHightLight(if (item.sceneConfigInfo.freeze) ("*" + item.appName) else item.appName.toString())

        viewHolder.run {
            val id = item.path
            this.appPath = id
            GlobalScope.launch(Dispatchers.Main) {
                val icon = appIconLoader.loadIcon(item).await()
                val imgView = imgView!!
                if (icon != null && appPath == id) {
                    imgView.setImageDrawable(icon)
                }
            }
        }

        if (!item.desc.isNullOrEmpty()) {
            viewHolder.itemDesc?.text = item.desc
            viewHolder.itemDesc?.visibility = View.VISIBLE
        } else {
            viewHolder.itemDesc?.visibility = View.GONE
        }
    }

    inner class ViewHolder {
        internal var appPath: CharSequence? = null

        internal var itemTitle: TextView? = null
        internal var imgView: ImageView? = null
        internal var itemDesc: TextView? = null
    }
}
