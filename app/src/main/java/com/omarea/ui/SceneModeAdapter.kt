package com.omarea.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.omarea.common.ui.OverScrollListView
import com.omarea.library.basic.AppInfoLoader
import com.omarea.model.AppInfo
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.vtools.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by Hello on 2018/01/26.
 */

class SceneModeAdapter(private val context: Context, apps: ArrayList<AppInfo>, private val firstMode: String) : BaseAdapter() {
    private val appIconLoader = AppInfoLoader(context)
    private var keywords: String = ""
    private val list: ArrayList<AppInfo>?
    private var pm: PackageManager? = null

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
            convertView = View.inflate(context, R.layout.list_item_scene_app, null)
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

    private fun getResourceColor(colorId: Int): Int {
        val theme = context.theme
        val resources = context.resources
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            resources.getColor(colorId, theme)
        } else {
            resources.getColor(colorId)
        }
    }

    private val colors = HashMap<String, Int>().apply {
        put(ModeSwitcher.POWERSAVE, getResourceColor(R.color.color_powersave))
        put(ModeSwitcher.BALANCE, getResourceColor(R.color.color_balance))
        put(ModeSwitcher.PERFORMANCE, getResourceColor(R.color.color_performance))
        put(ModeSwitcher.FAST, getResourceColor(R.color.color_fast))
        put(ModeSwitcher.IGONED, Color.GRAY)
    }

    private fun getColor(mode: String): Int {
        if (colors.containsKey(mode)) {
            return colors[mode]!!
        } else if (mode.isEmpty() && colors.containsKey(firstMode)) {
            return colors[firstMode]!!
        }
        return Color.GRAY
    }

    fun updateRow(position: Int, convertView: View) {
        val item = getItem(position)
        val viewHolder = ViewHolder()
        viewHolder.run {
            itemTitle = convertView.findViewById(R.id.ItemTitle)
            summery = convertView.findViewById(R.id.ItemSummary)
            itemDesc = convertView.findViewById(R.id.ItemDesc)
            imgView = convertView.findViewById(R.id.ItemIcon)
            itemTitle?.text = keywordHightLight(if (item.sceneConfigInfo.freeze) ("*" + item.appName) else item.appName.toString())
            if (item.icon == null) {
                val id = item.path
                this.appPath = id
                GlobalScope.launch(Dispatchers.Main) {
                    val icon = appIconLoader.loadIcon(item).await()
                    val imgView = imgView!!
                    if (icon != null && appPath == id) {
                        imgView.setImageDrawable(icon)
                    }
                }
            } else {
                imgView!!.setImageDrawable(item.icon)
            }

            if (item.stateTags != null) {
                summery?.run {
                    val mode = item.stateTags.toString()
                    setTextColor(getColor(mode))
                    visibility = VISIBLE
                    text = mode.run {
                        ModeSwitcher.getModName(mode) + (if (isEmpty()) {
                            "(${ModeSwitcher.getModName(firstMode)})"
                        } else {
                            ""
                        })
                    }
                }
            } else {
                summery?.visibility = GONE
            }

            if (itemDesc != null)
                itemDesc?.text = item.desc
        }
    }

    inner class ViewHolder {
        internal var appPath: CharSequence? = null

        internal var itemTitle: TextView? = null
        internal var imgView: ImageView? = null
        internal var itemDesc: TextView? = null
        internal var summery: TextView? = null
    }
}
