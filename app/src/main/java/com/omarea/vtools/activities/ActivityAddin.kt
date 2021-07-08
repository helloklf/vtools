package com.omarea.vtools.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.SimpleAdapter
import com.omarea.common.ui.DialogHelper
import com.omarea.store.SpfConfig
import com.omarea.vtools.R
import com.omarea.vtools.addin.DexCompileAddin
import com.omarea.vtools.addin.Immersive
import com.omarea.vtools.dialogs.DialogAddinModifyDPI
import com.omarea.vtools.dialogs.DialogAddinModifyDevice
import com.omarea.vtools.dialogs.DialogAddinWIFI
import com.omarea.vtools.dialogs.DialogCustomMAC
import kotlinx.android.synthetic.main.activity_addin.*
import java.util.*


class ActivityAddin : ActivityBase() {
    private fun createItem(title: String, desc: String, runnable: Runnable?, wran: Boolean = true): HashMap<String, Any> {
        val item = HashMap<String, Any>()
        item.put("Title", title)
        item.put("Desc", desc)
        item.put("Wran", wran)
        if (runnable != null)
            item.put("Action", runnable)
        return item
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addin)

        setBackArrow()

        initAddin(this.addin_system_listview)
    }

    private fun initAddin(view: View) {
        val activity = this
        val context = this
        val listItem = ArrayList<HashMap<String, Any>>().apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                add(createItem(getString(R.string.addin_fullscreen_on), getString(R.string.addin_fullscreen_on_desc), { Immersive(activity).fullScreen() }, false))
            }

            add(createItem(getString(R.string.addin_wifi), getString(R.string.addin_wifi_desc), { DialogAddinWIFI(context).show() }, false))

            add(createItem(getString(R.string.addin_dpi), getString(R.string.addin_dpi_desc), { DialogAddinModifyDPI(context).modifyDPI(activity.windowManager.defaultDisplay, context) }, false))

            add(createItem(getString(R.string.addin_deviceinfo), getString(
                    R.string.addin_deviceinfo_desc),
                    {
                        DialogAddinModifyDevice(context).modifyDeviceInfo()
                    },
                    false))
            add(createItem(getString(R.string.addin_mac),
                    getString(R.string.addin_mac_desc),
                    {
                        DialogCustomMAC(context).modifyMAC(SpfConfig.GLOBAL_SPF_MAC_AUTOCHANGE_MODE_1)
                    },
                    false))
            add(createItem(getString(R.string.addin_mac_2),
                    getString(R.string.addin_mac_desc_2),
                    {
                        DialogCustomMAC(context).modifyMAC(SpfConfig.GLOBAL_SPF_MAC_AUTOCHANGE_MODE_2)
                    },
                    false))

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                add(createItem(getString(R.string.addin_force_dex_compile), getString(R.string.addin_force_dex_compile_desc), { DexCompileAddin(context).run() }, false))
            }
            /*
            add(createItem(getString(R.string.addin_pm_dexopt), getString(R.string.addin_pm_dexopt_desc), Runnable { DexCompileAddin(context).modifyConfig() }, false))
            */
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
            DialogHelper.confirm(this,
                    item["Title"].toString(),
                    item["Desc"].toString(), {
                (item["Action"] as Runnable).run()
            })
        }
    }

    override fun onResume() {
        super.onResume()
        title = getString(R.string.menu_sundry)
    }
}
