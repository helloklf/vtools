package com.omarea.vtools.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.SimpleAdapter
import android.widget.TabHost
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.config.PageClickHandler
import com.omarea.krscript.config.PageListReader
import com.omarea.krscript.model.PageInfo
import com.omarea.shell.SysUtils
import com.omarea.ui.TabIconHelper
import com.omarea.vtools.R
import com.omarea.vtools.activitys.ActionPage
import com.omarea.vtools.addin.DexCompileAddin
import com.omarea.vtools.addin.FullScreenAddin
import com.omarea.vtools.addin.PerfBoostConfigAddin
import com.omarea.vtools.addin.ThermalAddin
import com.omarea.vtools.dialogs.DialogAddinModifyDPI
import com.omarea.vtools.dialogs.DialogAddinModifydevice
import com.omarea.vtools.dialogs.DialogAddinWIFI
import com.omarea.vtools.dialogs.DialogCustomMAC
import kotlinx.android.synthetic.main.fragment_addin.*
import java.util.*


class FragmentAddin : Fragment() {
    private var myHandler = Handler()
    private fun createItem(title: String, desc: String, runnable: Runnable?, wran: Boolean = true): HashMap<String, Any> {
        val item = HashMap<String, Any>()
        item.put("Title", title)
        item.put("Desc", desc)
        item.put("Wran", wran)
        if (runnable != null)
            item.put("Action", runnable)
        return item
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_addin, container, false)

    private fun getIP(): String {
        var r = SysUtils.executeCommandWithOutput(false, "ifconfig wlan0 | grep \"inet addr\" | awk '{ print \$2}' | awk -F: '{print \$2}'")
        if (r == null || r == "") {
            r = "IP"
        }
        return r.trim()
    }

    private fun initAddin(view: View) {
        val listItem = ArrayList<HashMap<String, Any>>().apply {
            add(createItem(getString(R.string.addin_fullscreen_on), getString(R.string.addin_fullscreen_on_desc), Runnable { FullScreenAddin(activity!!).fullScreen() }, false))

            val platform = com.omarea.shell.Platform().getCPUName()

            // if (!(platform == "sdm845" || platform == "msm8998" || platform == "msmnile")) {
            add(createItem(getString(R.string.addin_thermal_remove), getString(R.string.addin_thermal_remove_desc), Runnable { ThermalAddin(context!!).showOption() }, false))
            // }

            if (!(com.omarea.common.shared.MagiskExtend.moduleInstalled() && (platform == "sdm845" || platform == "msm8998" || platform == "msmnile"))) {
                add(createItem(getString(R.string.addin_thermal_remove2), getString(R.string.addin_thermal_remove2_desc), Runnable {
                    ThermalAddin(context!!).miuiSetThermalNo()
                }, false))
            }

            add(createItem(getString(R.string.addin_wifi), getString(R.string.addin_wifi_desc), Runnable { DialogAddinWIFI(context!!).show() }, false))

            add(createItem(getString(R.string.addin_dpi), getString(R.string.addin_dpi_desc), Runnable { DialogAddinModifyDPI(context!!).modifyDPI(activity!!.windowManager.defaultDisplay, context!!) }, false))
            add(createItem(getString(R.string.addin_deviceinfo), getString(R.string.addin_deviceinfo_desc), Runnable { DialogAddinModifydevice(context!!).modifyDeviceInfo() }, false))
            add(createItem(getString(R.string.addin_mac), getString(R.string.addin_mac_desc), Runnable { DialogCustomMAC(context!!).modifyMAC() }, false))
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                add(createItem(getString(R.string.addin_force_dex_compile), getString(R.string.addin_force_dex_compile_desc), Runnable { DexCompileAddin(context!!).run() }, false))
            }
            add(createItem(getString(R.string.addin_pm_dexopt), getString(R.string.addin_pm_dexopt_desc), Runnable { DexCompileAddin(context!!).modifyConfig() }, false))
            add(createItem(getString(R.string.addin_bpc), getString(R.string.addin_bpc_desc), Runnable { PerfBoostConfigAddin(context!!).install() }))
        }

        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.list_item_action,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        addin_system_listview.adapter = mSimpleAdapter
        addin_system_listview.onItemClickListener = onActionClick
    }

    private var onActionClick = AdapterView.OnItemClickListener { parent, _, position, _ ->
        val item = parent.adapter.getItem(position) as HashMap<*, *>
        if (item.get("Wran") == false) {
            (item["Action"] as Runnable).run()
        } else {
            DialogHelper.animDialog(AlertDialog.Builder(context!!)
                    .setTitle(item["Title"].toString())
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.addin_continue) { _, _ ->
                        (item["Action"] as Runnable).run()
                    }
                    .setMessage(item["Desc"].toString()))
        }
    }

    private var page2ConfigLoaded = false

    private fun loadSwitchsConfig() {
        if (page2ConfigLoaded)
            return
        val progressBarDialog = ProgressBarDialog(context!!)
        progressBarDialog.showDialog("读取配置，稍等...")
        Thread(Runnable {
            val items = PageListReader(this.activity!!).readPageList("custom/pages/page_list.xml")
            myHandler.post {
                page2ConfigLoaded = true
                list_page2.setListData(items, object : PageClickHandler {
                    override fun openPage(title: String, config: String) {
                        _openPage(title, config)
                    }

                    override fun openPage(pageInfo: PageInfo) {
                        _openPage(pageInfo.title, pageInfo.pageConfigPath)
                    }
                })
                progressBarDialog.hideDialog()
            }
        }).start()
    }


    fun _openPage(pageInfo: PageInfo) {
        _openPage(pageInfo.title, pageInfo.pageConfigPath)
    }

    fun _openPage(title: String, config: String) {
        try {
            val intent = Intent(context, ActionPage::class.java)
            intent.putExtra("title", title)
            intent.putExtra("config", config)
            startActivity(intent)
        } catch (ex: java.lang.Exception) {
            Log.e("_openPage", "" + ex.message)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tabHost = view.findViewById(R.id.addin_tabhost) as TabHost
        tabHost.setup()

        val tabIconHelper = TabIconHelper(tabHost, this.activity!!)
        tabIconHelper.newTabSpec("收藏", context!!.getDrawable(R.drawable.addin_favorites)!!, R.id.tab0)
        val configTab = tabIconHelper.newTabSpec("全部", context!!.getDrawable(R.drawable.addin_pages)!!, R.id.tab2)

        tabHost.setOnTabChangedListener({ tabId ->
            if (tabId == configTab) {
                loadSwitchsConfig()
            }
            tabIconHelper.updateHighlight()
        })

        tabHost.currentTab = 0
        initAddin(view)
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentAddin()
            return fragment
        }
    }
}
