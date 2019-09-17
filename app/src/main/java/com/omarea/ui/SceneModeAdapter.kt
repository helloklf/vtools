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

/**
 * Created by Hello on 2018/01/26.
 */

class SceneModeAdapter(private val context: Context, apps: ArrayList<Appinfo>, private var keywords: String = "") : BaseAdapter() {
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
        return item.packageName.toString().toLowerCase().contains(text)
                || item.appName.toString().toLowerCase().contains(text)
                || item.path.toString().toLowerCase().contains(text)
    }

    private fun filterAppList(appList: ArrayList<Appinfo>, keywords: String): ArrayList<Appinfo> {
        val text = keywords.toLowerCase()
        if (text.isEmpty())
            return appList
        return java.util.ArrayList(appList.filter { item ->
            keywordSearch(item, text)
        })
    }

    private fun loadIcon(viewHolder: ViewHolder, item: Appinfo) {
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
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.list_item_scenen_app, null)
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

    fun updateRow(position: Int, convertView: View) {
        val item = getItem(position)
        val viewHolder = ViewHolder()
        viewHolder.itemTitle = convertView.findViewById(R.id.ItemTitle)
        viewHolder.enabledStateText = convertView.findViewById(R.id.ItemEnabledStateText)
        viewHolder.itemText = convertView.findViewById(R.id.ItemText)
        viewHolder.itemDesc = convertView.findViewById(R.id.ItemDesc)
        viewHolder.imgView = convertView.findViewById(R.id.ItemIcon)
        viewHolder.imgView!!.setTag(getItem(position).packageName)
        // convertView.tag = viewHolder
        viewHolder.itemTitle!!.text = keywordHightLight(if (item.appConfigInfo.freeze) ("*" + item.appName) else item.appName.toString())
        viewHolder.itemText!!.text = keywordHightLight(item.packageName.toString())
        if (item.icon == null) {
            loadIcon(viewHolder, item)
        } else {
            viewHolder.imgView!!.setImageDrawable(item.icon)
        }
        if (item.enabledState != null) {
            val config = item.enabledState
            var enabledState = ""
            enabledState = ModeSwitcher.getModName(config.toString())
            when (config) {
                ModeSwitcher.POWERSAVE -> {
                    viewHolder.enabledStateText!!.setTextColor(Color.parseColor("#0091D5"))
                }
                ModeSwitcher.PERFORMANCE -> {
                    enabledState = ModeSwitcher.getModName(config.toString())
                    viewHolder.enabledStateText!!.setTextColor(Color.parseColor("#6ECB00"))
                }
                ModeSwitcher.FAST -> {
                    enabledState = ModeSwitcher.getModName(config.toString())
                    viewHolder.enabledStateText!!.setTextColor(Color.parseColor("#FF7E00"))
                }
                ModeSwitcher.IGONED -> {
                    enabledState = ModeSwitcher.getModName(config.toString())
                    viewHolder.enabledStateText!!.setTextColor(Color.parseColor("#888888"))
                }
                ModeSwitcher.BALANCE -> {
                    enabledState = ModeSwitcher.getModName(config.toString())
                    viewHolder.enabledStateText!!.setTextColor(Color.parseColor("#00B78A"))
                }
                else -> {
                    viewHolder.enabledStateText!!.setTextColor(Color.parseColor("#00B78A"))
                }
            }
            viewHolder.enabledStateText!!.visibility = VISIBLE
            viewHolder.enabledStateText!!.text = enabledState
        } else
            viewHolder.enabledStateText!!.visibility = GONE

        if (viewHolder.itemDesc != null)
            viewHolder.itemDesc!!.text = item.desc

    }

    inner class ViewHolder {
        internal var itemTitle: TextView? = null
        internal var imgView: ImageView? = null
        internal var itemText: TextView? = null
        internal var itemDesc: TextView? = null
        internal var enabledStateText: TextView? = null
    }
}
