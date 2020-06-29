package com.omarea.ui

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.omarea.model.Appinfo
import com.omarea.vtools.R
import java.io.File

class AppMultipleChoiceAdapter(private val listview: ListView, private val apps: List<Appinfo>) : BaseAdapter() {
    private val context = listview.context

    override fun getCount(): Int {
        return apps.size
    }

    override fun getItem(position: Int): Appinfo {
        return apps[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
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

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.list_item_app_simple, null)
        }
        updateRow(position, convertView!!)
        return convertView
    }

    fun updateRow(position: Int, convertView: View) {
        val item = getItem(position)
        val viewHolder = ViewHolder()
        viewHolder.itemRoot = convertView.findViewById(R.id.ItemRoot)
        viewHolder.itemChecked = convertView.findViewById(R.id.ItemChecked)
        viewHolder.itemTitle = convertView.findViewById(R.id.ItemTitle)
        viewHolder.itemDesc = convertView.findViewById(R.id.ItemDesc)
        viewHolder.imgView = convertView.findViewById(R.id.ItemIcon)

        viewHolder.itemTitle!!.text = if (item.sceneConfigInfo != null && item.sceneConfigInfo.freeze) ("*" + item.appName) else item.appName.toString()

        if (item.icon == null) {
            loadIcon(viewHolder, item)
        } else {
            viewHolder.imgView!!.setImageDrawable(item.icon)
        }

        viewHolder.itemDesc?.text = item.packageName

        viewHolder.itemChecked?.run {
            isChecked = item.selectState
            setOnClickListener {
                getItem(position).selectState = isChecked
            }
        }
    }

    fun getCheckedItems(): List<Appinfo> {
        return apps.filter {
            it.selectState
        }
    }

    fun getAll(): List<Appinfo> {
        return apps
    }

    inner class ViewHolder {
        internal var itemRoot: View? = null
        internal var itemChecked: CheckBox? = null
        internal var itemTitle: TextView? = null
        internal var imgView: ImageView? = null
        internal var itemDesc: TextView? = null
    }
}
