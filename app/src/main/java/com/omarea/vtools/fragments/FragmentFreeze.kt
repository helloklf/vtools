package com.omarea.vtools.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.omarea.shared.AppConfigStore
import com.omarea.shared.AppListHelper
import com.omarea.shared.ShortcutHelper
import com.omarea.shared.model.Appinfo
import com.omarea.shell.KeepShellPublic
import com.omarea.ui.FreezeAppAdapter
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.layout_freeze.*


class FragmentFreeze : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_freeze, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (isDetached) {
            return
        }

        freeze_apps.setOnItemClickListener { parent, view, position, id ->
            try {
                val appInfo = (parent.adapter.getItem(position) as Appinfo)
                toggleEnable(appInfo)
                (freeze_apps.adapter as FreezeAppAdapter).updateRow(position, freeze_apps, appInfo)
            } catch (ex: Exception) {
            }
        }
        freeze_apps.setOnItemLongClickListener { parent, view, position, id ->
            val item = (parent.adapter.getItem(position) as Appinfo)
            showOptions(item, position, view)
            true
        }

        loadData()
    }

    private fun loadData() {
        val freezeApps = AppConfigStore(this.context).freezeAppList
        val allApp = AppListHelper(this.context!!).getAll()
        val freezeAppsInfo = ArrayList<Appinfo>()
        freezeApps.forEach {
            val packageName = it
            val result = allApp.find { it.packageName == packageName }
            if (result != null) {
                freezeAppsInfo.add(result)
            }
        }
        freeze_apps.adapter = FreezeAppAdapter(this.context!!, freezeAppsInfo)
    }

    private fun showOptions(appInfo: Appinfo, position: Int, view: View) {
        AlertDialog.Builder(this.context!!)
                .setTitle(appInfo.appName)
                .setItems(
                        arrayOf(
                                "打开",
                                "创建快捷方式",
                                if(appInfo.enabled) "冻结" else "解冻",
                                "移除"), { _, which ->

                    when (which) {
                        0 -> startApp(appInfo)
                        1 -> createShortcut(appInfo)
                        2 -> {
                            toggleEnable(appInfo)
                            (freeze_apps.adapter as FreezeAppAdapter).updateRow(position, freeze_apps, appInfo)
                        }
                        3 -> {
                            removeConfig(appInfo)
                            loadData()
                        }
                    }
                })
                .setCancelable(true)
                .create()
                .show()
    }

    private fun removeConfig(appInfo: Appinfo) {
        if (!appInfo.enabled) {
            enableApp(appInfo)
        }

        val store = AppConfigStore(context)
        val config = store.getAppConfig(appInfo.packageName.toString())
        config.freeze = false
        store.setAppConfig(config)

        ShortcutHelper().removeShortcut(this.context!!, appInfo)
    }

    private fun enableApp(appInfo: Appinfo) {
        KeepShellPublic.doCmdSync("pm enable " + appInfo.packageName)
    }

    private fun disableApp(appInfo: Appinfo) {
        KeepShellPublic.doCmdSync("pm disable " + appInfo.packageName)
    }

    private fun toggleEnable(appInfo: Appinfo) {
        if (appInfo.enabled) {
            disableApp(appInfo)
            Toast.makeText(this.context, "√ 已冻结应用", Toast.LENGTH_SHORT).show()
        } else {
            enableApp(appInfo)
            Toast.makeText(this.context, "× 已解冻应用", Toast.LENGTH_SHORT).show()
        }
        appInfo.enabled = !appInfo.enabled
    }

    private fun startApp (appInfo: Appinfo) {
        if (!appInfo.enabled) {
            enableApp(appInfo)
        }
        val intent = this.context!!.getPackageManager().getLaunchIntentForPackage(appInfo.packageName.toString())
        if (intent != null) {
            this.context!!.startActivity(intent)
        }
    }

    private fun createShortcut(appInfo: Appinfo) {
        if (!appInfo.enabled) {
            enableApp(appInfo)
        }
        if (ShortcutHelper().createShortcut(this.context!!, appInfo)) {
            Toast.makeText(this.context, "创建完成", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this.context, "创建失败", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentFreeze()
            return fragment
        }
    }
}
