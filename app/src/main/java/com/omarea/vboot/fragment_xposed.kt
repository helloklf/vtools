package com.omarea.vboot

import android.content.Context.MODE_PRIVATE
import android.content.Context.MODE_WORLD_READABLE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.Toast
import com.omarea.shared.SpfConfig
import com.omarea.shared.cmd_shellTools
import com.omarea.shared.xposed_check
import com.omarea.ui.list_adapter2
import kotlinx.android.synthetic.main.layout_battery.*
import kotlinx.android.synthetic.main.layout_xposed.*
import java.io.File
import java.util.ArrayList
import java.util.HashMap


class fragment_xposed : Fragment() {
    internal var cmdshellTools: cmd_shellTools? = null
    internal var thisview: main? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.layout_xposed, container, false)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        userVisibleHint = true
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        try {
            spf = activity.getSharedPreferences("xposed", 0x1)
        } catch (ex:Exception) {
            spf = context.getSharedPreferences("xposed", MODE_PRIVATE)
        }
        xposed_tabs.setup();

        xposed_tabs.addTab(xposed_tabs.newTabSpec("tab_a").setContent(R.id.xposed_tab_a).setIndicator(getString(R.string.xposed_tab_a)));
        xposed_tabs.addTab(xposed_tabs.newTabSpec("tab_b").setContent(R.id.xposed_tab_b).setIndicator(getString(R.string.xposed_tab_b)));
        xposed_tabs.setCurrentTab(0);

        vbootxposedservice_state.setOnClickListener {
            try {
                var intent = context.packageManager.getLaunchIntentForPackage("de.robv.android.xposed.installer");
                //intent.putExtra("section", "modules")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                Snackbar.make(getView()!!, context.getString(R.string.xposed_cannot_openxposed), Snackbar.LENGTH_SHORT).show()
            }
        }
        xposed_config_hight_fps.setOnCheckedChangeListener { a, value ->
            spf.edit()
            spf.edit().putBoolean("xposed_hight_fps", value).commit()
        }
        xposed_config_dpi_fix.setOnCheckedChangeListener { a, value ->
            spf.edit()
            spf.edit().putBoolean("xposed_dpi_fix", value).commit()
        }
        xposed_config_cm_su.setOnCheckedChangeListener { a, value ->
            spf.edit()
            spf.edit().putBoolean("xposed_hide_su", value).commit()
        }
        xposed_config_webview_debug.setOnCheckedChangeListener { a, value ->
            spf.edit()
            spf.edit().putBoolean("xposed_webview_debug", value).commit()
        }

        if (xposed_check.xposedIsRunning())
            vbootxposedservice_state.visibility = GONE;


        val config_powersavelistClick = object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val checkBox = view.findViewById(R.id.select_state) as CheckBox
                checkBox.isChecked = !checkBox.isChecked
            }
        }
        xposed_apps_dpifix.setOnItemClickListener(config_powersavelistClick)
        xposed_config_default_dpi.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                val dpi:Int
                try{
                    dpi = v.text.toString().toInt()
                    spf.edit().putInt("xposed_default_dpi", dpi).commit()
                    Snackbar.make(v, "设置已保存，可能需要重启手机才会生效。"+ v.text, Snackbar.LENGTH_SHORT ).show()
                }
                catch(e:Exception){
                }
            }
            false
        }
    }

    internal val myHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }
    lateinit var installedList: ArrayList<HashMap<String, Any>>
    lateinit var spf: SharedPreferences;
    override fun onResume() {
        super.onResume()
        xposed_config_hight_fps.isChecked = spf.getBoolean("xposed_hight_fps", false)
        xposed_config_dpi_fix.isChecked = spf.getBoolean("xposed_dpi_fix", false)
        xposed_config_cm_su.isChecked = spf.getBoolean("xposed_hide_su", false)
        xposed_config_webview_debug.isChecked = spf.getBoolean("xposed_webview_debug", false)
        val w = context.resources.displayMetrics.widthPixels.toFloat()
        val h = context.resources.displayMetrics.heightPixels.toFloat()
        val pixels = if (w > h) h else w
        val def = (pixels / 2.25).toInt()
        if (!spf.contains("xposed_default_dpi")) {
            spf.edit().putInt("xposed_default_dpi", def).commit()
        }
        xposed_config_default_dpi.setText(spf.getInt("xposed_default_dpi", def).toString())
        thisview!!.progressBar.visibility = View.VISIBLE
        Thread({
            installedList = loadList()
            myHandler.post {
                try {
                    xposed_apps_dpifix.setAdapter(list_adapter2(context, installedList))
                    val listadapter = xposed_apps_dpifix.getAdapter() as list_adapter2
                    var status = thisview!!.getSharedPreferences("xposed_dpifix", MODE_WORLD_READABLE).all
                    for (postion in status.keys) {
                        for (i in installedList.indices) {
                            if (installedList[i].get("packageName").toString() == postion) {
                                listadapter.states[i] = true
                            }
                        }
                    }
                    thisview!!.progressBar.visibility = View.GONE
                } catch (ex: Exception) {

                }
            }
        }).start()
    }

    override fun onPause() {
        super.onPause()
        try {
            if (xposed_apps_dpifix.getAdapter() == null) {
                return
            }
            val listadapter = xposed_apps_dpifix.getAdapter() as list_adapter2
            if (listadapter == null) {
                return
            }
            val states = listadapter.states
            var dpifixList = thisview!!.getSharedPreferences("xposed_dpifix", MODE_WORLD_READABLE).edit().clear()

            for (position in states.keys) {
                if (states[position] == true) {
                    dpifixList.putBoolean(installedList[position].get("packageName").toString(), true)
                }
            }
            dpifixList.commit()
        } catch (ex: Exception) {
            Toast.makeText(thisview, ex.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun loadList(): ArrayList<HashMap<String, Any>> {
        var packageManager = thisview!!.getPackageManager()
        var packageInfos = packageManager.getInstalledApplications(0)

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
            item.put("enabled_state", if (packageInfo.enabled) "" else "已冻结")

            item.put("name", packageInfo.loadLabel(packageManager))
            item.put("packageName", packageInfo.packageName)
            item.put("path", packageInfo.sourceDir)
            item.put("dir", file.getParent())
            list.add(item)
        }
        return list
    }
    companion object {
        fun Create(thisView: main, cmdshellTools: cmd_shellTools): Fragment {
            val fragment = fragment_xposed()
            fragment.cmdshellTools = cmdshellTools
            fragment.thisview = thisView
            return fragment
        }
    }
}
