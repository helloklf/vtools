package com.omarea.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.omarea.model.Appinfo
import com.omarea.model.ProcessInfo
import com.omarea.vtools.R
import java.util.*

/**
 * Created by Hello on 2018/01/26.
 */

class ProcessAdapter(private val context: Context, processes: ArrayList<ProcessInfo>, private var keywords: String = "") : BaseAdapter() {
    private val list: ArrayList<ProcessInfo>?

    init {
        this.list = processes
    }

    override fun getCount(): Int {
        return list?.size ?: 0
    }

    override fun getItem(position: Int): ProcessInfo {
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

    private fun loadIcon(imageView: ImageView, item: ProcessInfo) {
        if (item.user.matches(Regex("u[0-9]+_.*"))) {
            Thread(Runnable {
                var icon: Drawable? = null
                try {
                    val name = if (item.name.contains(":")) item.name.substring(0, item.name.indexOf(":")) else item.name
                    val installInfo = context.packageManager.getPackageInfo(name, 0)
                    icon = installInfo.applicationInfo.loadIcon(context.packageManager)
                } catch (ex: Exception) {
                } finally {
                    if (icon != null) {
                        imageView.post {
                            imageView.setImageDrawable(icon)
                        }
                    } else {
                        imageView.post {
                            imageView.setImageDrawable(context.getDrawable(R.drawable.process_android))
                        }
                    }
                }
            }).start()
        } else {
            imageView.setImageDrawable(context.getDrawable(R.drawable.process_linux))
        }
    }

    private fun keywordHightLight(str: String): SpannableString {
        val spannableString = SpannableString(str)
        var index = 0
        if (keywords.isEmpty()) {
            return spannableString;
        }
        index = str.toLowerCase().indexOf(keywords.toLowerCase())
        if (index < 0)
            return spannableString

        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#0094ff")), index, index + keywords.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString;
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.list_item_process_item, null)
        }
        updateRow(position, convertView!!)
        return convertView
    }

    fun updateRow(position: Int, view: View) {
        val processInfo = getItem(position);
        view.run {
            findViewById<TextView>(R.id.ProcessName).text = processInfo.name
            findViewById<TextView>(R.id.ProcessPID).text = "PID:" + processInfo.pid
            findViewById<TextView>(R.id.ProcessCPU).text = "CPU:" + processInfo.cpu + "%"
            if (processInfo.rss > 8192) {
                findViewById<TextView>(R.id.ProcessRSS).text = "MEM:" + (processInfo.rss / 1024).toInt() + "MB"
            } else {
                findViewById<TextView>(R.id.ProcessRSS).text = "MEM:" + processInfo.rss + "KB"
            }
            findViewById<TextView>(R.id.ProcessUSER).text = processInfo.user
            loadIcon(findViewById<ImageView>(R.id.ProcessIcon), processInfo)
        }
    }
}
