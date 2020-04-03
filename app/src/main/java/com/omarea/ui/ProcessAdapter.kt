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
import com.omarea.model.ProcessInfo
import com.omarea.vtools.R

/**
 * Created by Hello on 2018/01/26.
 */

class ProcessAdapter(private val context: Context,
                     private var processes: ArrayList<ProcessInfo> = ArrayList(),
                     private var keywords: String = "",
                     private var sortMode: Int = SORT_MODE_CPU,
                     private var filterMode: Int = FILTER_USER) : BaseAdapter() {
    companion object {
        val SORT_MODE_DEFAULT = 1;
        val SORT_MODE_CPU = 4;
        val SORT_MODE_MEM = 8;
        val SORT_MODE_PID = 16;

        val FILTER_ALL = 1;
        val FILTER_KERNEL = 4;
        val FILTER_USER = 8;
    }

    private val pm = context.packageManager
    private lateinit var list: ArrayList<ProcessInfo>
    private val nameCache = HashMap<String, String>()

    init {
        loadLabel()
        setList()
    }

    override fun getCount(): Int {
        return list.size ?: 0
    }

    override fun getItem(position: Int): ProcessInfo {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private fun setList() {
        this.list = filterAppList()
        notifyDataSetChanged()
    }

    private fun keywordSearch(item: ProcessInfo, text: String): Boolean {
        return item.friendlyName.toString().toLowerCase().contains(text) || item.name.toString().toLowerCase().contains(text) || item.user.toString().toLowerCase().contains(text)
    }

    private fun filterAppList(): ArrayList<ProcessInfo> {
        val text = keywords.toLowerCase()
        val keywordsEmpty = text.isEmpty()
        return ArrayList(processes.filter { it ->
            (keywordsEmpty || keywordSearch(it, text)) && (
                    when (filterMode) {
                        FILTER_ALL -> true
                        FILTER_USER -> isUserProcess(it)
                        FILTER_KERNEL -> !isUserProcess(it)
                        else -> true
                    })
        }.sortedBy {
            when (sortMode) {
                SORT_MODE_DEFAULT -> it.pid
                SORT_MODE_CPU -> -(it.cpu * 10).toInt()
                SORT_MODE_MEM -> -(it.rss * 100).toInt()
                SORT_MODE_PID -> -it.pid
                else -> it.pid
            }
        })
    }

    private fun isUserProcess(processInfo: ProcessInfo): Boolean {
        return processInfo.user.matches(Regex("u[0-9]+_.*"))
    }

    private fun loadIcon(imageView: ImageView, item: ProcessInfo) {
        if (("" + imageView.tag).equals(item.name)) {
            return
        } else {
            if (isUserProcess(item)) {
                Thread(Runnable {
                    var icon: Drawable? = null
                    try {
                        val name = if (item.name.contains(":")) item.name.substring(0, item.name.indexOf(":")) else item.name
                        val installInfo = pm.getPackageInfo(name, 0)
                        icon = installInfo.applicationInfo.loadIcon(pm)
                    } catch (ex: Exception) {
                    } finally {
                        if (icon != null) {
                            imageView.post {
                                imageView.setImageDrawable(icon)
                                imageView.tag = item.name
                            }
                        } else {
                            imageView.post {
                                imageView.setImageDrawable(context.getDrawable(R.drawable.process_android))
                                imageView.tag = item.name
                            }
                        }
                    }
                }).start()
            } else {
                imageView.setImageDrawable(context.getDrawable(R.drawable.process_linux))
                imageView.tag = item.name
            }
        }
    }

    private fun loadLabel() {
        for (item in processes) {
            if (isUserProcess(item)) {
                if (nameCache.containsKey(item.name)) {
                    item.friendlyName = nameCache.get(item.name)
                } else {
                    val name = if (item.name.contains(":")) item.name.substring(0, item.name.indexOf(":")) else item.name
                    try {
                        val app = pm.getApplicationInfo(name, 0)
                        item.friendlyName = "" + app.loadLabel(pm)
                    } catch (ex: java.lang.Exception) {
                        item.friendlyName = name
                    } finally {
                        nameCache[item.name] = item.friendlyName
                    }
                }
            } else {
                item.friendlyName = item.name
            }
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

    fun updateKeywords(keywords: String) {
        this.keywords = keywords
        setList()
    }

    fun updateSortMode(sortMode: Int) {
        this.sortMode = sortMode
        setList()
    }

    fun updateFilterMode(filterMode: Int) {
        this.filterMode = filterMode
        setList()
    }

    fun setList(processes: ArrayList<ProcessInfo>) {
        this.processes = processes
        loadLabel()
        setList()
    }

    private fun updateRow(position: Int, view: View) {
        val processInfo = getItem(position);
        view.run {
            findViewById<TextView>(R.id.ProcessFriendlyName).text = keywordHightLight(processInfo.friendlyName)
            findViewById<TextView>(R.id.ProcessName).text = keywordHightLight(processInfo.name)
            findViewById<TextView>(R.id.ProcessPID).text = "PID: " + processInfo.pid
            findViewById<TextView>(R.id.ProcessCPU).text = "CPU: " + processInfo.cpu + "%"
            if (processInfo.rss > 8192) {
                findViewById<TextView>(R.id.ProcessRSS).text = "RAM: " + (processInfo.rss / 1024).toInt() + "MB"
            } else {
                findViewById<TextView>(R.id.ProcessRSS).text = "RAM: " + processInfo.rss + "KB"
            }
            findViewById<TextView>(R.id.ProcessUSER).text = keywordHightLight(processInfo.user)
            loadIcon(findViewById<ImageView>(R.id.ProcessIcon), processInfo)
        }
    }
}
