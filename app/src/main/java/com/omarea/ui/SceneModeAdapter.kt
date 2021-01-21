package com.omarea.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
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
import com.omarea.model.Appinfo
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.vtools.R
import java.io.File
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by Hello on 2018/01/26.
 */

class SceneModeAdapter(private val context: Context, apps: ArrayList<Appinfo>, private val firstMode: String) : BaseAdapter() {
    private var keywords: String = ""
    private val list: ArrayList<Appinfo>?

    init {
        this.list = filterAppList(apps, keywords)
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
        return item.packageName.toString().toLowerCase(Locale.getDefault()).contains(text)
                || item.appName.toString().toLowerCase(Locale.getDefault()).contains(text)
                || item.path.toString().toLowerCase(Locale.getDefault()).contains(text)
    }

    private fun filterAppList(appList: ArrayList<Appinfo>, keywords: String): ArrayList<Appinfo> {
        val text = keywords.toLowerCase(Locale.getDefault())
        if (text.isEmpty())
            return appList
        return ArrayList(appList.filter { item ->
            keywordSearch(item, text)
        })
    }

    private fun loadIcon(viewHolder: ViewHolder, item: Appinfo) {
        Thread {
            var icon: Drawable? = null
            try {
                val installInfo = context.packageManager.getPackageInfo(item.packageName.toString(), 0)
                icon = installInfo.applicationInfo.loadIcon(context.packageManager)
            } catch (ex: Exception) {
                try {
                    val file = File(item.path.toString())
                    if (file.exists() && file.canRead()) {
                        val pm = context.packageManager
                        icon = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_ACTIVITIES)?.applicationInfo?.loadIcon(pm)
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
        }.start()
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

    fun updateRow(position: Int, listView: OverScrollListView, appinfo: Appinfo) {
        try {
            val visibleFirstPosi = listView.firstVisiblePosition
            val visibleLastPosi = listView.lastVisiblePosition

            if (position >= visibleFirstPosi && position <= visibleLastPosi) {
                list!![position] = appinfo
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
        viewHolder.itemTitle = convertView.findViewById(R.id.ItemTitle)
        viewHolder.summery = convertView.findViewById(R.id.ItemSummary)
        viewHolder.itemDesc = convertView.findViewById(R.id.ItemDesc)
        viewHolder.imgView = convertView.findViewById(R.id.ItemIcon)

        viewHolder.itemTitle?.text = keywordHightLight(if (item.sceneConfigInfo.freeze) ("*" + item.appName) else item.appName.toString())
        if (item.icon == null) {
            loadIcon(viewHolder, item)
        } else {
            viewHolder.imgView?.setImageDrawable(item.icon)
        }
        if (item.enabledState != null) {
            viewHolder.summery?.run {
                val mode = item.enabledState.toString()
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
            viewHolder.summery?.visibility = GONE
        }

        if (viewHolder.itemDesc != null)
            viewHolder.itemDesc?.text = item.desc
    }

    inner class ViewHolder {
        internal var itemTitle: TextView? = null
        internal var imgView: ImageView? = null
        internal var itemDesc: TextView? = null
        internal var summery: TextView? = null
    }
}
