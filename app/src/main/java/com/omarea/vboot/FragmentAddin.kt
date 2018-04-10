package com.omarea.vboot

import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TabHost
import com.omarea.shell.SysUtils
import com.omarea.shell.units.FlymeUnit
import com.omarea.shell.units.FullScreenSUnit
import com.omarea.shell.units.QQStyleUnit
import com.omarea.ui.ProgressBarDialog
import com.omarea.vboot.addin.*
import com.omarea.vboot.dialogs.DialogAddinModifyDPI
import com.omarea.vboot.dialogs.DialogAddinModifydevice
import com.omarea.vboot.dialogs.DialogAddinWIFI
import com.omarea.vboot.dialogs.DialogCustomMAC
import kotlinx.android.synthetic.main.layout_addin.*
import java.util.*


class FragmentAddin : Fragment() {
    internal var thisview: ActivityMain? = null
    internal val myHandler: Handler = Handler()
    private lateinit var processBarDialog: ProgressBarDialog

    private fun createItem(title: String, desc: String, runnable: Runnable?): HashMap<String, Any> {
        val item = HashMap<String, Any>()
        item.put("Title", title)
        item.put("Desc", desc)
        if (runnable != null)
            item.put("Action", runnable)
        return item
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater!!.inflate(R.layout.layout_addin, container, false)

    private fun initSoftAddin(view: View) {
        if (context != null) {
            val listView = view.findViewById(R.id.addin_soft_listview) as ListView
            val listItem = ArrayList<HashMap<String, Any>>().apply {
                add(createItem(getString(R.string.addin_qq_clear), getString(R.string.addin_qq_clear_desc), Runnable { QQStyleUnit().DisableQQStyle() }))
                add(createItem(getString(R.string.addin_qq_reset), getString(R.string.addin_qq_reset_desc), Runnable { QQStyleUnit().RestoreQQStyle() }))
                add(createItem(getString(R.string.addin_fullscreen_on), getString(R.string.addin_fullscreen_on_desc), Runnable { FullScreenSUnit().FullScreen() }))
                add(createItem(getString(R.string.addin_fullscreen_off), getString(R.string.addin_fullscreen_off_desc), Runnable { FullScreenSUnit().ExitFullScreen() }))
                add(createItem(getString(R.string.addin_flyme_static_blur), getString(R.string.addin_flyme_static_blur_desc), Runnable { FlymeUnit().StaticBlur() }))
                add(createItem(getString(R.string.addin_miui_hide_search), getString(R.string.addin_miui_hide_search_desc), Runnable { MiuiAddin(context!!).hideSearch() }))
                add(createItem(getString(R.string.addin_disable_x), getString(R.string.addin_disable_x_desc), Runnable { NetworkChecker(context!!).disableNetworkChecker() }))
                add(createItem(getString(R.string.addin_disable_google), getString(R.string.addin_disable_google_desc), Runnable { GoogleFrameworkAddin(context!!).disableFramework() }))
                add(createItem(getString(R.string.addin_enable_google), getString(R.string.addin_enable_google_desc), Runnable { GoogleFrameworkAddin(context!!).enableFramework() }))
            }

            val mSimpleAdapter = SimpleAdapter(
                    view.context, listItem,
                    R.layout.action_row_item,
                    arrayOf("Title", "Desc"),
                    intArrayOf(R.id.Title, R.id.Desc)
            )
            listView.adapter = mSimpleAdapter
            listView.onItemClickListener = onActionClick
        }
    }

    private fun getIP(): String {
        var r = SysUtils.executeCommandWithOutput(false, "ifconfig wlan0 | grep \"inet addr\" | awk '{ print \$2}' | awk -F: '{print \$2}'")
        if (r == null || r == "") {
            r = "IP"
        }
        return r.trim()
    }

    private fun initSystemAddin(view: View) {
        val listView = view.findViewById(R.id.addin_system_listview) as ListView
        val listItem = ArrayList<HashMap<String, Any>>().apply {
            add(createItem(getString(R.string.addin_drop_caches), getString(R.string.addin_drop_caches_desc), Runnable { SystemAddin(context!!).dropCache() }))
            add(createItem(getString(R.string.addin_thermal_remove), getString(R.string.addin_thermal_remove_desc), Runnable { ThermalAddin(context!!).removeThermal() }))
            add(createItem(getString(R.string.addin_thermal_resume), getString(R.string.addin_thermal_resume_desc), Runnable { ThermalAddin(context!!).resumeThermal() }))
            add(createItem(getString(R.string.addin_thermal_close), getString(R.string.addin_thermal_close_desc), Runnable { ThermalAddin(context!!).closeThermal() }))
            add(createItem(getString(R.string.addin_del_pwd), getString(R.string.addin_del_pwd_desc), Runnable { SystemAddin(context!!).deleteLockPwd() }))
            add(createItem(getString(R.string.addin_charge_disable), getString(R.string.addin_charge_disable_desc), Runnable { BatteryAddin(context!!).disbleCharge() }))
            add(createItem(getString(R.string.addin_charge_enable), getString(R.string.addin_charge_enable_desc), Runnable { BatteryAddin(context!!).resumeCharge() }))
            add(createItem(getString(R.string.addin_device_r11), getString(R.string.addin_device_r11_desc), Runnable { DeviceInfoAddin(context!!).modifyR11() }))
            add(createItem(getString(R.string.addin_device_x20), getString(R.string.addin_device_x20_desc), Runnable { DeviceInfoAddin(context!!).modifyX20() }))
            add(createItem(getString(R.string.addin_restore_buildprop), getString(R.string.addin_restore_buildprop_desc), Runnable { BuildPropRestore(context!!).restoreLast() }))
            add(createItem(getString(R.string.addin_adb_network), String.format(getString(R.string.addin_adb_network_desc), getIP()), Runnable { AdbAddin(context!!).openNetworkDebug() }))
            add(createItem(getString(R.string.addin_wifi), getString(R.string.addin_wifi_desc), Runnable { DialogAddinWIFI(context!!).show() }))
            add(createItem(getString(R.string.addin_battery_history_del), getString(R.string.addin_battery_history_del_desc), Runnable { BatteryAddin(context!!).deleteHistory() }))
        }

        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.action_row_item,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        listView.adapter = mSimpleAdapter
        listView.onItemClickListener = onActionClick
    }

    private var onActionClick = AdapterView.OnItemClickListener { parent, _, position, _ ->
        val item = parent.adapter.getItem(position) as HashMap<*, *>
        AlertDialog.Builder(thisview!!)
                .setTitle(getString(R.string.addin_execute))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.yes) { _, _ -> (item["Action"] as Runnable).run() }
                .setMessage(item["Title"].toString() + "：" + item["Desc"] + context!!.getString(R.string.addin_execute_message))
                .create().show()
    }

    private fun initCustomAddin(view: View) {
        val listItem = ArrayList<HashMap<String, Any>>().apply {
            add(createItem(getString(R.string.addin_dpi), getString(R.string.addin_dpi_desc), Runnable { DialogAddinModifyDPI(context!!).modifyDPI(thisview!!.windowManager.defaultDisplay) }))
            add(createItem(getString(R.string.addin_deviceinfo), getString(R.string.addin_deviceinfo_desc), Runnable { DialogAddinModifydevice(context!!).modifyDeviceInfo() }))
            add(createItem(getString(R.string.addin_mac), getString(R.string.addin_mac_desc), Runnable { DialogCustomMAC(context!!).modifyMAC() }))
            add(createItem(getString(R.string.addin_force_dex_compile), getString(R.string.addin_force_dex_compile_desc), Runnable { DexCompileAddin(context!!).run() }))
            add(createItem(getString(R.string.addin_pm_dexopt), getString(R.string.addin_pm_dexopt_desc), Runnable { DexCompileAddin(context!!).modifyConfig() }))
        }

        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.action_row_item,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        addin_custom_listview.adapter = mSimpleAdapter


        addin_custom_listview.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            ((parent.adapter.getItem(position) as HashMap<*, *>)["Action"] as Runnable).run()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        processBarDialog = ProgressBarDialog(this.context!!)
        val tabHost = view!!.findViewById(R.id.addinlist_tabhost) as TabHost

        tabHost.setup()

        tabHost.addTab(tabHost.newTabSpec("system_tab").setContent(R.id.system).setIndicator(getString(R.string.system)))
        tabHost.addTab(tabHost.newTabSpec("soft_tab").setContent(R.id.soft).setIndicator(getString(R.string.software)))
        tabHost.addTab(tabHost.newTabSpec("custom_tab").setContent(R.id.custom).setIndicator(getString(R.string.advance)))
        tabHost.currentTab = 0

        initSystemAddin(view)
        initSoftAddin(view)
        initCustomAddin(view)
    }

    companion object {

        fun createPage(thisView: ActivityMain): Fragment {
            val fragment = FragmentAddin()
            fragment.thisview = thisView
            return fragment
        }
    }
}
