package com.omarea.vboot

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import com.omarea.shared.AppListHelper
import com.omarea.shared.SpfConfig
import com.omarea.shared.helper.KeepShell
import com.omarea.shared.model.Appinfo
import com.omarea.shell.SysUtils
import com.omarea.ui.AppListAdapter
import com.omarea.ui.ProgressBarDialog
import kotlinx.android.synthetic.main.layout_whitelist.*


class FragmentWhitelist : Fragment() {

    private lateinit var dialog: ProgressBarDialog
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_whitelist, container, false)

    private lateinit var keepShell: KeepShell

    @SuppressLint("ApplySharedPref")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        keepShell = KeepShell(context!!)
        dialog = ProgressBarDialog(context!!)
        whitelist_tabhost.setup()
        whitelist_tabhost.addTab(whitelist_tabhost.newTabSpec("tab_1")
                .setContent(R.id.whitelist_tab1).setIndicator(context!!.getString(R.string.autobooster_tab_disDoze)))
        whitelist_tabhost.addTab(whitelist_tabhost.newTabSpec("tab_2")
                .setContent(R.id.whitelist_tab2).setIndicator(context!!.getString(R.string.autobooster_tab_details)))
        whitelist_tabhost.currentTab = 0

        val spf = context!!.getSharedPreferences(SpfConfig.WHITE_LIST_SPF, Context.MODE_PRIVATE)
        booster_whitelist.setOnItemClickListener({ parent, itemView, position, id ->
            val checkBox = itemView.findViewById(R.id.select_state) as CheckBox
            checkBox.isChecked = !checkBox.isChecked

            val item = (parent.adapter.getItem(position) as Appinfo)
            val packageName = item.packageName.toString()
            spf.edit().putBoolean(packageName, checkBox.isChecked).commit()
            keepShell.doCmd((if (checkBox.isChecked) "dumpsys deviceidle whitelist +$packageName" else "dumpsys deviceidle whitelist -$packageName"))
        })
        val globalSpf = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        doze_whitelist_autoset.isChecked = globalSpf.getBoolean(SpfConfig.GLOBAL_SPF_DOZELIST_AUTOSET, false)
        doze_whitelist_autoset.setOnCheckedChangeListener({ buttonView, isChecked ->
            globalSpf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DOZELIST_AUTOSET, isChecked).commit()
        })
    }

    override fun onResume() {
        super.onResume()
        setList()
    }

    override fun onDestroy() {
        super.onDestroy()
        keepShell.tryExit()
    }

    private var handler = Handler()

    private fun setList() {
        dialog.showDialog()
        Thread(Runnable {
            val apps = AppListHelper(context!!).getAll()
            var whitelist = SysUtils.executeCommandWithOutput(true, "dumpsys deviceidle whitelist")
            if (whitelist == null) {
                whitelist = "";
            }
            val arr = ArrayList<String>()
            for (item in whitelist.split("\n").listIterator()) {
                if (item.indexOf(",") > 0 && item.lastIndexOf(",") > item.indexOf(",")) {
                    arr.add(item.substring(item.indexOf(",") + 1, item.lastIndexOf(",")))
                }
            }
            for (item in apps) {
                val b = arr.contains(item.packageName.toString())
                item.selectState = b
                item.enabledState = if (b) "" else " "
            }
            handler.post {
                if (!this.isDetached) {
                    dialog.hideDialog()
                    booster_whitelist.adapter = AppListAdapter(context!!, apps)
                }
            }
        }).start()
    }

    override fun onDetach() {
        dialog.hideDialog()
        super.onDetach()
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentWhitelist()
            return fragment
        }
    }
}
