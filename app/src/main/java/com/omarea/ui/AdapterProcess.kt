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
import com.omarea.library.basic.AppInfoLoader
import com.omarea.model.ProcessInfo
import com.omarea.vtools.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AdapterProcess(private val context: Context,
                     private var processes: ArrayList<ProcessInfo> = ArrayList(),
                     private var keywords: String = "",
                     private var sortMode: Int = SORT_MODE_CPU,
                     private var filterMode: Int = FILTER_ANDROID_USER) : BaseAdapter() {
    private val appInfoLoader = AppInfoLoader(context, 100)
    private val androidIcon = context.getDrawable(R.drawable.process_android)
    private val linuxIcon = context.getDrawable(R.drawable.process_linux)

    companion object {
        val SORT_MODE_DEFAULT = 1;
        val SORT_MODE_CPU = 4;
        val SORT_MODE_RES = 8;
        val SORT_MODE_PID = 16;

        val FILTER_ALL = 1;
        val FILTER_OTHER = 4;
        val FILTER_ANDROID_USER = 8;
        val FILTER_ANDROID_SYSTEM = 16;
        val FILTER_ANDROID = 32;
    }

    private val pm = context.packageManager
    private lateinit var list: ArrayList<ProcessInfo>
    private val nameCache = context.getSharedPreferences("ProcessNameCache", Context.MODE_PRIVATE)

    init {
        setList()
        if (processes.size > 0) {
            loadLabel()
        }
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
        return item.friendlyName.toString().toLowerCase().contains(text) || item.name.toString().toLowerCase().contains(text) || item.user.toString().toLowerCase().contains(text) || item.command.toString().toLowerCase().contains(text) || item.cmdline.toString().toLowerCase().contains(text)
    }

    private fun filterAppList(): ArrayList<ProcessInfo> {
        val text = keywords.toLowerCase()
        val keywordsEmpty = text.isEmpty()
        return ArrayList(processes.filter { it ->
            (keywordsEmpty || keywordSearch(it, text)) && (
                    when (filterMode) {
                        FILTER_ALL -> true
                        FILTER_ANDROID_USER -> isAndroidUserProcess(it)
                        FILTER_ANDROID_SYSTEM -> isSystemProcess(it)
                        FILTER_ANDROID -> isAndroidProcess(it)
                        FILTER_OTHER -> !isAndroidProcess(it)
                        else -> true
                    })
        }.sortedBy {
            when (sortMode) {
                SORT_MODE_DEFAULT -> it.pid
                SORT_MODE_CPU -> -(it.getCpu() * 10).toInt()
                SORT_MODE_RES -> -(it.res * 100).toInt()
                SORT_MODE_PID -> -it.pid
                else -> it.pid
            }
        })
    }

    private val regexUser = Regex("u[0-9]+_.*")
    private val regexPackageName = Regex(".*\\..*")

    private fun isAndroidProcess(processInfo: ProcessInfo): Boolean {
        return (processInfo.command.contains("app_process") && processInfo.name.matches(regexPackageName))
    }

    private fun isSystemProcess(processInfo: ProcessInfo): Boolean {
        return isAndroidProcess(processInfo) && !processInfo.user.matches(regexUser)
    }

    private fun isAndroidUserProcess(processInfo: ProcessInfo): Boolean {
        return isAndroidProcess(processInfo) && processInfo.user.matches(regexUser)
    }

    private fun isUserProcess(processInfo: ProcessInfo): Boolean {
        return processInfo.user.matches(regexUser)
    }

    private fun loadIcon(imageView: ImageView, item: ProcessInfo) {
        if (("" + imageView.tag).equals(item.name)) {
            return
        } else {
            if (isAndroidProcess(item)) {
                GlobalScope.launch(Dispatchers.Main) {
                    var icon: Drawable? = null
                    try {
                        val name = if (item.name.contains(":")) item.name.substring(0, item.name.indexOf(":")) else item.name
                        icon = appInfoLoader.loadIcon(name).await()
                    } catch (ex: Exception) {
                    }
                    imageView.post {
                        imageView.setImageDrawable(if (icon != null) icon else androidIcon)
                        imageView.tag = item.name
                    }
                }
            } else {
                imageView.setImageDrawable(linuxIcon)
                imageView.tag = item.name
            }
        }
    }
    private fun loadLabel(clearAll: Boolean = false) {
        val count = nameCache.all.size
        val editor = nameCache.edit()
        if (clearAll) {
            editor.clear()
        }

        for (item in processes) {
            if (isAndroidProcess(item)) {
                if (nameCache.contains(item.name)) {
                    item.friendlyName = nameCache.getString(item.name, item.name)
                } else {
                    val name = if (item.name.contains(":")) item.name.substring(0, item.name.indexOf(":")) else item.name
                    try {
                        val app = pm.getApplicationInfo(name, 0)
                        item.friendlyName = "" + app.loadLabel(pm)
                    } catch (ex: java.lang.Exception) {
                        item.friendlyName = name
                    } finally {
                        editor.putString(item.name, item.friendlyName)
                    }
                }
            } else {
                item.friendlyName = item.name
            }
        }

        editor.apply()
        if (nameCache.all.size != count) {
            notifyDataSetChanged()
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
            if (processInfo.friendlyName == processInfo.name) {
                findViewById<TextView>(R.id.ProcessFriendlyName).text = keywordHightLight(processInfo.friendlyName)
                findViewById<TextView>(R.id.ProcessName).run {
                    visibility = View.GONE
                    text = ""
                }
            } else {
                findViewById<TextView>(R.id.ProcessFriendlyName).text = keywordHightLight(processInfo.friendlyName)
                findViewById<TextView>(R.id.ProcessName).run {
                    visibility = View.VISIBLE
                    text = keywordHightLight(processInfo.name)
                }
            }
            findViewById<TextView>(R.id.ProcessPID).text = processInfo.pid.toString()
            findViewById<TextView>(R.id.ProcessCPU).text = "" + processInfo.getCpu() + "%"
            if (processInfo.res > 8192) {
                findViewById<TextView>(R.id.ProcessRES).text = "" + (processInfo.res / 1024).toInt() + "MB"
            } else {
                findViewById<TextView>(R.id.ProcessRES).text = "" + processInfo.res + "KB"
            }
            loadIcon(findViewById<ImageView>(R.id.ProcessIcon), processInfo)
        }
    }

    fun removeItem(position: Int) {
        list.removeAt(position)
        notifyDataSetChanged()
    }
}
