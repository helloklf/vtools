package com.omarea.vboot

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.Context.MODE_WORLD_READABLE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import com.omarea.shared.SpfConfig
import com.omarea.xposed.XposedCheck
import com.omarea.ui.ProgressBarDialog
import com.omarea.ui.SearchTextWatcher
import com.omarea.ui.list_adapter2
import kotlinx.android.synthetic.main.layout_xposed.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

//TODO: 从最近任务隐藏的APP不会被选中修复

class FragmentXposed : Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.layout_xposed, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        userVisibleHint = true
    }

    @SuppressLint("ApplySharedPref", "WrongConstant", "WorldReadableFiles")
    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        processBarDialog = ProgressBarDialog(this.context)
        dpi_spf = context.getSharedPreferences(SpfConfig.XPOSED_DPI_SPF, MODE_WORLD_READABLE)
        rencent_spf = context.getSharedPreferences(SpfConfig.XPOSED_HIDETASK_SPF, MODE_WORLD_READABLE)
        val w = context.resources.displayMetrics.widthPixels.toFloat()
        val h = context.resources.displayMetrics.heightPixels.toFloat()
        val pixels = if (w > h) h else w
        def = (pixels / 2.25).toInt()


        try {
            spf = activity.getSharedPreferences("xposed", 0x1)
        } catch (ex: Exception) {
            spf = context.getSharedPreferences("xposed", MODE_PRIVATE)
        }
        spf.edit().putInt("xposed_default_dpi", def).commit()

        xposed_tabs.setup()

        xposed_tabs.addTab(xposed_tabs.newTabSpec("tab_a").setContent(R.id.xposed_tab_a).setIndicator(getString(R.string.xposed_tab_a)))
        xposed_tabs.addTab(xposed_tabs.newTabSpec("tab_b").setContent(R.id.xposed_tab_b).setIndicator(getString(R.string.xposed_tab_b)))
        xposed_tabs.setCurrentTab(0)

        vbootxposedservice_state.setOnClickListener {
            try {
                var intent = context.packageManager.getLaunchIntentForPackage("de.robv.android.xposed.installer")
                //intent.putExtra("section", "modules")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                Snackbar.make(getView()!!, context.getString(R.string.xposed_cannot_openxposed), Snackbar.LENGTH_SHORT).show()
            }
        }
        xposed_config_scroll.setOnCheckedChangeListener { _, value ->
            spf.edit().putBoolean("xposed_config_scroll", value).commit()
        }
        xposed_config_hight_fps.setOnCheckedChangeListener { _, value ->
            spf.edit().putBoolean("xposed_hight_fps", value).commit()
        }
        xposed_config_dpi_fix.setOnCheckedChangeListener { _, value ->
            spf.edit().putBoolean("xposed_dpi_fix", value).commit()
        }
        xposed_config_cm_su.setOnCheckedChangeListener { _, value ->
            spf.edit().putBoolean("xposed_hide_su", value).commit()
        }
        xposed_config_webview_debug.setOnCheckedChangeListener { _, value ->
            spf.edit().putBoolean("xposed_webview_debug", value).commit()
        }

        if (XposedCheck.xposedIsRunning())
            vbootxposedservice_state.visibility = GONE

        val config_powersavelistClick = object : AdapterView.OnItemClickListener {
            @SuppressLint("ApplySharedPref")
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val checkBox = view.findViewById(R.id.select_state) as CheckBox
                val textView = view.findViewById(R.id.ItemEnabledStateText) as TextView

                val list = parent.adapter as list_adapter2
                val layoutInflater = LayoutInflater.from(context)
                val edit = layoutInflater.inflate(R.layout.dpi_input_layout, null)
                val input = (edit.findViewById(R.id.dpi_input)) as EditText
                val hide_in_recent = (edit.findViewById(R.id.hide_in_recent)) as CheckBox
                val packageName = list.getItem(position).get("packageName").toString()
                val dpi = dpi_spf.getInt(packageName, 0)
                input.setText(if(dpi > 0) "" + dpi else "")
                hide_in_recent.isChecked = rencent_spf.getBoolean(packageName, false)

                AlertDialog.Builder(context)
                        .setTitle("应用配置")
                        .setNeutralButton("清除设置", { _, _ ->
                            list.states[position] = false
                            checkBox.isChecked = false
                            dpi_spf.edit().remove(packageName).commit()
                            rencent_spf.edit().remove(packageName).commit()
                            list.getItem(position).put("enabled_state", "")
                            textView.setText("")
                        })
                        .setNegativeButton("确定", { _, _ ->
                            val text = input.text.toString()
                            if ((text.isEmpty() || text.toInt() < 72) && hide_in_recent.isChecked == rencent_spf.getBoolean(packageName, false)) {
                                return@setNegativeButton
                            }
                            if (!text.isEmpty()) {
                                list.getItem(position).put("enabled_state", text)
                                textView.setText(text)
                                dpi_spf.edit().putInt(packageName, text.toInt()).commit()
                            }
                            rencent_spf.edit().putBoolean(packageName, hide_in_recent.isChecked).commit()
                            checkBox.isChecked = true
                            list.states[position] = true
                        })
                        .setView(edit)
                        .create()
                        .show()
            }
        }
        xposed_apps_dpifix.setOnItemClickListener(config_powersavelistClick)


        xposed_config_search.addTextChangedListener(SearchTextWatcher(Runnable {
            val text = xposed_config_search.text.toString()

            val list = installedList.filter { item ->
                if (item.get("packageName").toString().contains(text) || item.get("name").toString().contains(text)) true else false
            }

            xposed_apps_dpifix.setAdapter(list_adapter2(context, java.util.ArrayList<HashMap<String, Any>>(list)))
        }))
    }

    internal val myHandler: Handler = Handler()

    private var def = 0
    private lateinit var installedList: ArrayList<HashMap<String, Any>>
    private lateinit var spf: SharedPreferences
    private lateinit var dpi_spf: SharedPreferences
    private lateinit var rencent_spf: SharedPreferences
    override fun onResume() {
        super.onResume()
        xposed_config_hight_fps.isChecked = spf.getBoolean("xposed_hight_fps", false)
        xposed_config_scroll.isChecked = spf.getBoolean("xposed_config_scroll", false)
        xposed_config_dpi_fix.isChecked = spf.getBoolean("xposed_dpi_fix", true)
        xposed_config_cm_su.isChecked = spf.getBoolean("xposed_hide_su", false)
        xposed_config_webview_debug.isChecked = spf.getBoolean("xposed_webview_debug", false)

        processBarDialog.showDialog()
        Thread({
            installedList = loadList()

            val status = dpi_spf.all
            for (app in installedList) {
                val packageName = app.get("packageName").toString()
                if (dpi_spf.contains(packageName) || rencent_spf.contains(packageName)) {
                    app["enabled_state"] = "" + dpi_spf.getInt(packageName, def) + "," + (if (rencent_spf.getBoolean(packageName, false)) 1 else 0)
                }
            }

            myHandler.post {
                try {
                    val ld = list_adapter2(context, sortAppList(installedList))

                    for (i in installedList.indices) {
                        ld.states[i] = installedList[i]["enabled_state"].toString().length > 0
                    }
                    xposed_apps_dpifix.setAdapter(ld)

                    processBarDialog.hideDialog()
                } catch (ex: Exception) {

                }
            }
        }).start()
    }

    private fun sortAppList(list: ArrayList<HashMap<String, Any>>): ArrayList<HashMap<String, Any>> {
        list.sortWith(Comparator { l, r ->
            val les = l["enabled_state"].toString()
            val res = r["enabled_state"].toString()
            when {
                les < res -> 1
                les > res -> -1
                else -> {
                    val lp = l["packageName"].toString()
                    val rp = r["packageName"].toString()
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

    private fun loadList(): ArrayList<HashMap<String, Any>> {
        val packageManager = this.context.packageManager
        val packageInfos = packageManager.getInstalledApplications(0)

        val list = ArrayList<HashMap<String, Any>>()/*在数组中存放数据*/
        for (i in packageInfos.indices) {
            val packageInfo = packageInfos.get(i)

            val file = File(packageInfo.publicSourceDir)
            if (!file.exists())
                continue

            val item = HashMap<String, Any>()
            val d = packageInfo.loadIcon(packageManager)
            item.put("icon", d)
            item.put("select_state", false)
            item.put("dir", packageInfo.sourceDir)
            item.put("enabled", packageInfo.enabled)
            item.put("enabled_state", "")

            item.put("name", packageInfo.loadLabel(packageManager))
            item.put("packageName", packageInfo.packageName)
            item.put("path", packageInfo.sourceDir)
            item.put("dir", file.getParent())
            list.add(item)
        }
        return list
    }

    companion object {
        fun Create(): Fragment {
            val fragment = FragmentXposed()
            return fragment
        }
    }
}
