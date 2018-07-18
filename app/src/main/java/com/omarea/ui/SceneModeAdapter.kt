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
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.omarea.shared.model.Appinfo
import com.omarea.vboot.R
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import kotlin.Comparator

/**
 * Created by Hello on 2018/01/26.
 */

class SceneModeAdapter(private val context: Context, apps: ArrayList<Appinfo>, private var keywords: String = "") : BaseAdapter() {
    private val list: ArrayList<Appinfo>?
    @SuppressLint("UseSparseArrays")
    var states = HashMap<Int, Boolean>()

    private var viewHolder: SceneModeAdapter.ViewHolder? = null

    init {
        this.list = filterAppList(apps, keywords)
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
        if (keywords.length == 0) {
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
            viewHolder = ViewHolder()
            convertView = View.inflate(context, R.layout.scene_mode_item, null)
            viewHolder!!.itemTitle = convertView!!.findViewById(R.id.ItemTitle)
            viewHolder!!.enabledStateText = convertView.findViewById(R.id.ItemEnabledStateText)
            viewHolder!!.itemText = convertView.findViewById(R.id.ItemText)
            viewHolder!!.itemDesc = convertView.findViewById(R.id.ItemDesc)
            viewHolder!!.imgView = convertView.findViewById(R.id.ItemIcon)
            viewHolder!!.imgView!!.setTag(getItem(position).packageName)
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

        val item = getItem(position)
        viewHolder!!.itemTitle!!.text = keywordHightLight(item.appName.toString())
        viewHolder!!.itemText!!.text = keywordHightLight(item.packageName.toString())
        if (item.icon == null) {
            loadIcon(viewHolder!!, item)
        } else {
            viewHolder!!.imgView!!.setImageDrawable(item.icon)
        }
        if (item.enabledState != null)
        {
            val config = item.enabledState
            var enabledState = ""
            when (config) {
                "powersave" -> {
                    enabledState = "省电模式"
                    viewHolder!!.enabledStateText!!.setTextColor(Color.parseColor("#0091D5"))
                }
                "performance" -> {
                    enabledState = "性能模式"
                    viewHolder!!.enabledStateText!!.setTextColor(Color.parseColor("#6ECB00"))
                }
                "fast" -> {
                    enabledState = "极速模式"
                    viewHolder!!.enabledStateText!!.setTextColor(Color.parseColor("#FF7E00"))
                }
                "igoned" -> {
                    enabledState = ""
                    viewHolder!!.enabledStateText!!.setTextColor(Color.parseColor("#888888"))
                }
                else -> {
                    enabledState = "均衡模式"
                    viewHolder!!.enabledStateText!!.setTextColor(Color.parseColor("#00B78A"))
                }
            }
            viewHolder!!.enabledStateText!!.visibility = VISIBLE
            viewHolder!!.enabledStateText!!.text = enabledState
        }
        else
            viewHolder!!.enabledStateText!!.visibility = GONE

        if (viewHolder!!.itemDesc != null)
            viewHolder!!.itemDesc!!.text = item.desc

        return convertView
    }

    inner class ViewHolder {
        internal var itemTitle: TextView? = null
        internal var imgView: ImageView? = null
        internal var itemText: TextView? = null
        internal var itemDesc: TextView? = null
        internal var enabledStateText: TextView? = null
    }
}
