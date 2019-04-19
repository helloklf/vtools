package com.omarea.vtools.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
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
import com.omarea.ui.ProgressBarDialog
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.layout_freeze.*


class FragmentFreeze : Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog
    private var freezeApps = java.util.ArrayList<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_freeze, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        processBarDialog = ProgressBarDialog(context!!)
        processBarDialog.showDialog()
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

        freeze_add.setOnClickListener {
            addFreezeAppDialog()
        }
        freeze_settings.setOnClickListener {
            freezeOptionsDialog()
        }

        loadData()
    }

    private fun loadData() {
        freezeApps = AppConfigStore(this.context).freezeAppList
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
        processBarDialog.hideDialog()
    }

    private fun showOptions(appInfo: Appinfo, position: Int, view: View) {
        AlertDialog.Builder(this.context!!)
                .setTitle(appInfo.appName)
                .setItems(
                        arrayOf(
                                "打开",
                                "创建快捷方式",
                                if (appInfo.enabled) "冻结" else "解冻",
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

    private fun startApp(appInfo: Appinfo) {
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

    private fun addFreezeAppDialog() {
        processBarDialog.showDialog()
        val allApp = AppListHelper(this.context!!).getBootableApps()
        val apps = allApp.filter { !freezeApps.contains(it.packageName) }
        val items = apps.map { it.appName.toString() }.toTypedArray()
        val states = items.map { false }.toBooleanArray()

        AlertDialog.Builder(this.context)
                .setTitle("添加偏见应用")
                .setMultiChoiceItems(
                        items,
                        states,
                        { dialog, which, isChecked ->
                            states[which] = isChecked
                        })
                .setPositiveButton(R.string.btn_confirm, { _, _ ->
                    val selectedItems = ArrayList<String>()
                    for (index in 0..states.size - 1) {
                        if (states[index]) {
                            selectedItems.add(apps[index].packageName.toString())
                        }
                    }
                    addFreezeApps(selectedItems)
                })
                .setNegativeButton(R.string.btn_cancel, { _, _ ->
                })
                .setCancelable(false)
                .create()
                .show()
        processBarDialog.hideDialog()
    }

    private fun addFreezeApps(selectedItems: ArrayList<String>) {
        processBarDialog.showDialog()
        val store = AppConfigStore(context)
        val shortcutHelper = ShortcutHelper()
        selectedItems.forEach {
            val config = store.getAppConfig(it)
            if (shortcutHelper.createShortcut(context, it)) {
                config.freeze = true
                if (store.setAppConfig(config)) {
                    Log.e("添加", config.packageName)
                    KeepShellPublic.doCmdSync("pm disable " + it)
                } else {
                    Toast.makeText(context, "保存配置失败\n" + it, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "添加快捷方式失败\n" + it, Toast.LENGTH_SHORT).show()
            }
        }
        loadData()
        processBarDialog.hideDialog()
    }

    private fun freezeOptionsDialog() {
        AlertDialog.Builder(this.context!!)
                .setTitle("偏好应用管理")
                .setItems(
                        arrayOf(
                                "重建快捷方式",
                                "全部解冻",
                                "全部冻结",
                                "清空全部"), { _, which ->

                    when (which) {
                        0 -> createShortcutAll()
                        1 -> enableAll()
                        2 -> disableAll()
                        3 -> removeAll()
                    }
                })
                .setCancelable(true)
                .create()
                .show()
    }

    private fun createShortcutAll() {
        processBarDialog.showDialog()
        val shortcutHelper = ShortcutHelper()
        freezeApps.forEach {
            KeepShellPublic.doCmdSync("pm enable " + it)
            shortcutHelper.createShortcut(context, it)
            KeepShellPublic.doCmdSync("pm disable " + it)
        }
        processBarDialog.hideDialog()
        loadData()
    }

    private fun disableAll() {
        processBarDialog.showDialog()
        freezeApps.forEach {
            KeepShellPublic.doCmdSync("pm disable " + it)
        }
        processBarDialog.hideDialog()
        loadData()
    }

    private fun enableAll() {
        processBarDialog.showDialog()
        freezeApps.forEach {
            KeepShellPublic.doCmdSync("pm enable " + it)
        }
        processBarDialog.hideDialog()
        loadData()
    }

    private fun removeAll() {
        processBarDialog.showDialog()
        val store = AppConfigStore(context)
        val shortcutHelper = ShortcutHelper()
        freezeApps.forEach {
            val config = store.getAppConfig(it)
            config.freeze = false
            store.setAppConfig(config)
            shortcutHelper.removeShortcut(context, it)
        }
        loadData()
        processBarDialog.hideDialog()
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentFreeze()
            return fragment
        }
    }
}
