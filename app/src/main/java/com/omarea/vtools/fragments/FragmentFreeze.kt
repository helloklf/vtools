package com.omarea.vtools.fragments

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filterable
import android.widget.Toast
import com.omarea.shared.AppConfigStore
import com.omarea.shared.AppListHelper
import com.omarea.shared.SceneMode
import com.omarea.shared.helper.ShortcutHelper
import com.omarea.shared.model.Appinfo
import com.omarea.shell.KeepShellPublic
import com.omarea.ui.FreezeAppAdapter
import com.omarea.ui.ProgressBarDialog
import com.omarea.vtools.R
import com.omarea.vtools.activitys.ActivityFreezeApps
import kotlinx.android.synthetic.main.layout_freeze.*


class FragmentFreeze : Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog
    private var freezeApps = java.util.ArrayList<String>()
    private var handler: Handler = Handler()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_freeze, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        processBarDialog = ProgressBarDialog(context!!)
        processBarDialog.showDialog()

        freeze_apps.setOnItemClickListener { parent, view, position, _ ->
            try {
                val appInfo = (parent.adapter.getItem(position) as Appinfo)
                toggleEnable(appInfo)
                (freeze_apps.adapter as FreezeAppAdapter).updateRow(position, freeze_apps, appInfo)
            } catch (ex: Exception) {
            }
        }
        freeze_apps.setOnItemLongClickListener { parent, view, position, _ ->
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

        freeze_apps_search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                (freeze_apps.adapter as Filterable).getFilter().filter(if(s == null) "" else s.toString())
            }
        })
    }

    override fun onResume() {
        super.onResume()
    }

    private fun loadData() {
        Thread(Runnable {
            try {
                val store = AppConfigStore(this.context)
                freezeApps = store.freezeAppList
                val pinnedShortcuts = ShortcutHelper().getPinnedShortcuts(this.context);

                val lostedShortcuts = ArrayList<Appinfo>()
                val lostedShortcutsName = StringBuilder()

                val allApp = AppListHelper(this.context!!).getAll()
                allApp.forEach {
                    if (pinnedShortcuts.contains(it.packageName) && !it.enabled) {
                        if (!freezeApps.contains(it.packageName)) {
                            val config = store.getAppConfig(it.packageName.toString())
                            config.freeze = true
                            store.setAppConfig(config)
                            freezeApps.add(it.packageName.toString())
                        }
                    }
                }

                val freezeAppsInfo = ArrayList<Appinfo>()
                for (it in freezeApps) {
                    val packageName = it
                    val result = allApp.find { it.packageName == packageName }
                    if (result != null) {
                        freezeAppsInfo.add(result)

                        if (!pinnedShortcuts.contains(it)) {
                            lostedShortcuts.add(result)
                            lostedShortcutsName.append(result.appName).append("\n")
                        }
                    } else {
                        store.removeAppConfig(it);
                    }
                }
                store.close()

                handler.post {
                    try {
                        freeze_apps.adapter = FreezeAppAdapter(this.context!!.applicationContext, freezeAppsInfo)
                        processBarDialog.hideDialog()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // 即时是Oreo也可能出现获取不到已添加的快捷方式的情况，例如换了第三方桌面
                            if (lostedShortcuts.size > 0) {
                                shortcutsLostDialog(lostedShortcutsName.toString(), lostedShortcuts)
                            }
                        }
                    } catch (ex: java.lang.Exception) {}
                }
            } catch (ex: java.lang.Exception) {

            }
        }).start()
    }

    private fun shortcutsLostDialog(lostedShortcutsName:String, lostedShortcuts: ArrayList<Appinfo>) {
        AlertDialog.Builder(context)
                .setTitle("快捷方式丢失")
                .setMessage("以下被加入偏见模式的应用，启动快捷方式已丢失或未创建成功，是否立即重新创建？\n\n$lostedShortcutsName")
                .setPositiveButton(R.string.btn_confirm, { _, _ ->
                    processBarDialog.showDialog("请稍等...")
                    CreateShortcutThread(lostedShortcuts, this.context!!, Runnable {
                        handler.post {
                            loadData()
                            processBarDialog.hideDialog()
                        }
                    }).start()
                })
                .setNegativeButton(R.string.btn_cancel, { _, _ ->
                })
                .create()
                .show()
    }

    private fun showOptions(appInfo: Appinfo, position: Int, view: View) {
        AlertDialog.Builder(this.context!!)
                .setTitle(appInfo.appName)
                .setItems(
                        arrayOf(
                                "打开",
                                "创建快捷方式",
                                if (appInfo.enabled) "冻结" else "解冻",
                                "移除",
                                "移除并卸载"), { _, which ->

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
                        4 -> {
                            removeAndUninstall(appInfo)
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
        store.close()

        ShortcutHelper().removeShortcut(this.context!!, appInfo.packageName.toString())
    }

    private fun removeAndUninstall(appInfo: Appinfo) {
        removeConfig(appInfo)
        KeepShellPublic.doCmdSync("pm uninstall --user " + context!!.filesDir.getParentFile().getParentFile().getName() + " " + appInfo.packageName)
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
            Toast.makeText(this.context, "× 已冻结应用", Toast.LENGTH_SHORT).show()
        } else {
            enableApp(appInfo)
            Toast.makeText(this.context, "√ 已解冻应用", Toast.LENGTH_SHORT).show()
        }
        appInfo.enabled = !appInfo.enabled
    }

    private fun startApp(appInfo: Appinfo) {
        if (!appInfo.enabled) {
            enableApp(appInfo)
        }
        try {
            val intent = this.context!!.getPackageManager().getLaunchIntentForPackage(appInfo.packageName.toString())
            if (intent != null) {
                this.context!!.startActivity(intent)
                SceneMode.setFreezeAppStartTime(appInfo.packageName.toString())
            }
        } catch (ex: java.lang.Exception) {}
    }

    private fun createShortcut(appInfo: Appinfo) {
        if (!appInfo.enabled) {
            enableApp(appInfo)
        }
        if (ShortcutHelper().createShortcut(this.context!!, appInfo.packageName.toString())) {
            Toast.makeText(this.context, "创建完成", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this.context, "创建失败", Toast.LENGTH_SHORT).show()
        }
    }

    private class CreateShortcutThread(private var apps: ArrayList<Appinfo>, private var context: Context, private var onCompleted: Runnable) : Thread() {
        override fun run() {
            for (appinfo in apps) {
                if (!appinfo.enabled) {
                    KeepShellPublic.doCmdSync("pm enable " + appinfo.packageName)
                }
                Thread.sleep(3000)
                ShortcutHelper().createShortcut(this.context, appinfo.packageName.toString())
            }
            onCompleted.run()
        }
    }

    private fun addFreezeAppDialog() {
        // processBarDialog.showDialog()
        val allApp = AppListHelper(this.context!!).getBootableApps(false, true)
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
                .setCancelable(true)
                .create()
                .show()
        // processBarDialog.hideDialog()
    }

    private fun addFreezeApps(selectedItems: ArrayList<String>) {
        processBarDialog.showDialog("正在处理...")
        val next = Runnable {
            handler.post {
                try {
                    loadData()
                    processBarDialog.hideDialog()
                } catch (ex: java.lang.Exception) {}
            }
        }
        AddFreezeAppsThread(this.context!!, selectedItems, next).start()
    }

    private class AddFreezeAppsThread(private var context: Context, private var selectedItems: ArrayList<String>, private var onCompleted: Runnable) : Thread() {
        override fun run() {
            val store = AppConfigStore(context)
            val shortcutHelper = ShortcutHelper()
            for (it in selectedItems) {
                val config = store.getAppConfig(it)
                if (shortcutHelper.createShortcut(context, it)) {
                    Thread.sleep(2005)
                    config.freeze = true
                    if (store.setAppConfig(config)) {
                        KeepShellPublic.doCmdSync("pm disable " + it)
                    } else {
                        // Toast.makeText(context, "保存配置失败\n" + it, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Toast.makeText(context, "添加快捷方式失败\n" + it, Toast.LENGTH_SHORT).show()
                }
            }
            store.close()

            onCompleted.run()
        }
    }

    private fun freezeOptionsDialog() {
        val p = context!!.packageManager
        val startActivity = ComponentName(this.context!!.applicationContext, ActivityFreezeApps::class.java)
        val enabled = p.getComponentEnabledSetting(startActivity) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        AlertDialog.Builder(this.context!!)
                .setTitle("偏好应用管理")
                .setItems(
                        arrayOf(
                                "重建快捷方式",
                                "全部解冻",
                                "全部冻结",
                                if (enabled) "从桌面隐藏本界面入口" else "添加本界面入口到桌面",
                                "清空全部"), { _, which ->

                    when (which) {
                        0 -> createShortcutAll()
                        1 -> enableAll()
                        2 -> disableAll()
                        3 -> {
                            try {
                                if (enabled) {
                                    p.setComponentEnabledSetting(startActivity, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                                } else {
                                    p.setComponentEnabledSetting(startActivity, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                                }
                                Toast.makeText(this.context, "√ 图标显示状态已切换", Toast.LENGTH_SHORT).show()
                            } catch (ex: java.lang.Exception) {
                            }
                        }
                        4 -> removeAll()
                    }
                })
                .setCancelable(true)
                .create()
                .show()
    }

    private fun createShortcutAll() {
        processBarDialog.showDialog("请稍等，这可能有点慢...")
        CreateShortcutAllThread(this.context!!, freezeApps, Runnable {
            handler.post {
                processBarDialog.hideDialog()
                loadData()
            }
        }).start()
    }

    private class CreateShortcutAllThread(private var context: Context, private var freezeApps: ArrayList<String>, private var onCompleted: Runnable) : Thread() {
        override fun run() {
            val shortcutHelper = ShortcutHelper()
            for (it in freezeApps) {
                KeepShellPublic.doCmdSync("pm enable " + it)
                Thread.sleep(3000)
                shortcutHelper.createShortcut(context, it)
                // KeepShellPublic.doCmdSync("pm disable " + it)
            }
            onCompleted.run()
        }
    }

    private fun disableAll() {
        processBarDialog.showDialog()
        for (it in freezeApps) {
            KeepShellPublic.doCmdSync("pm disable " + it)
        }
        processBarDialog.hideDialog()
        loadData()
    }

    private fun enableAll() {
        processBarDialog.showDialog()
        for (it in freezeApps) {
            KeepShellPublic.doCmdSync("pm enable " + it)
        }
        processBarDialog.hideDialog()
        loadData()
    }

    private fun removeAll() {
        processBarDialog.showDialog()
        RemoveAllThread(this.context!!, freezeApps, Runnable {
            handler.post {
                loadData()
                processBarDialog.hideDialog()
                Toast.makeText(context, "你可能需要手动删除已添加的快捷方式！", Toast.LENGTH_LONG).show()
            }
        }).start()
    }

    private class RemoveAllThread(private var context: Context, private var freezeApps: ArrayList<String>, private var onCompleted: Runnable) : Thread() {
        override fun run() {

            val store = AppConfigStore(context)
            val shortcutHelper = ShortcutHelper()
            for (it in freezeApps) {
                KeepShellPublic.doCmdSync("pm enable " + it)
                val config = store.getAppConfig(it)
                config.freeze = false
                store.setAppConfig(config)
                shortcutHelper.removeShortcut(context, it)
            }
            store.close()

            onCompleted.run()
        }
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentFreeze()
            return fragment
        }
    }
}
