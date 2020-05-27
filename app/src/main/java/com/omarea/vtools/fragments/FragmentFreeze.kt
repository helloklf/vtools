package com.omarea.vtools.fragments

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.UserManager
import androidx.fragment.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filterable
import android.widget.TabHost
import android.widget.Toast
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.model.Appinfo
import com.omarea.scene_mode.FreezeAppShortcutHelper
import com.omarea.scene_mode.LogoCacheManager
import com.omarea.scene_mode.SceneMode
import com.omarea.store.SceneConfigStore
import com.omarea.store.SpfConfig
import com.omarea.ui.FreezeAppAdapter
import com.omarea.ui.TabIconHelper
import com.omarea.utils.AppListHelper
import com.omarea.vtools.R
import com.omarea.vtools.activities.ActivityFreezeApps
import com.omarea.vtools.activities.ActivityMain
import kotlinx.android.synthetic.main.fragment_freeze.*


class FragmentFreeze : androidx.fragment.app.Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog
    private var freezeApps = java.util.ArrayList<String>()
    private var handler: Handler = Handler()
    private lateinit var config: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_freeze, container, false)

    override fun onResume() {
        super.onResume()
        activity!!.title = getString(R.string.menu_freeze)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabHost = freeze_tabhost
        tabHost.setup()
        val tabIconHelper = TabIconHelper(tabHost, this.activity!!)
        tabIconHelper.newTabSpec("应用", context!!.getDrawable(R.drawable.tab_app)!!, R.id.tab_freeze_apps)
        tabIconHelper.newTabSpec("设置", context!!.getDrawable(R.drawable.tab_settings)!!, R.id.tab_freeze_settings)
        tabHost.setOnTabChangedListener { tabId ->
            tabIconHelper.updateHighlight()
        }

        config = this.context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        processBarDialog = ProgressBarDialog(context!!)

        processBarDialog.showDialog()

        freeze_apps.setOnItemClickListener { parent, itemView, position, _ ->
            try {
                val appInfo = (parent.adapter.getItem(position) as Appinfo)
                toggleEnable(appInfo)
                (freeze_apps.adapter as FreezeAppAdapter).updateRow(position, freeze_apps, appInfo)
            } catch (ex: Exception) {
            }
        }

        freeze_apps.setOnItemLongClickListener { parent, itemView, position, _ ->
            val item = (parent.adapter.getItem(position) as Appinfo)
            showOptions(item, position, itemView)
            true
        }

        // 菜单按钮
        freeze_menu.setOnClickListener {
            freezeOptionsDialog()
        }

        loadData()

        freeze_apps_search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                (freeze_apps.adapter as Filterable).getFilter().filter(if (s == null) "" else s.toString())
            }
        })
    }

    private fun loadData() {
        Thread(Runnable {
            try {
                // 数据库
                val store = SceneConfigStore(this.context)
                // 数据库中记录的已添加的偏见应用
                freezeApps = store.freezeAppList
                // 已添加到桌面的快捷方式
                val pinnedShortcuts = FreezeAppShortcutHelper().getPinnedShortcuts(this.context);

                val lostedShortcuts = ArrayList<Appinfo>()
                val lostedShortcutsName = StringBuilder()

                val allApp = AppListHelper(this.context!!).getAll()

                // 遍历应用 检测已添加快捷方式并冻结的应用（如果不在数据库中，自动添加到数据，通常由于Scene数据被清理导致）
                allApp.forEach {
                    if (pinnedShortcuts.contains(it.packageName) && ((!it.enabled) || it.suspended)) {
                        if (!freezeApps.contains(it.packageName)) {
                            val config = store.getAppConfig(it.packageName.toString())
                            config.freeze = true
                            store.setAppConfig(config)
                            freezeApps.add(it.packageName.toString())
                        }
                    }
                }

                val freezeAppsInfo = ArrayList<Appinfo>()
                // 遍历偏见应用列表 获取应用详情
                for (it in freezeApps) {
                    val packageName = it
                    val result = allApp.find { it.packageName == packageName }
                    if (result != null) {
                        freezeAppsInfo.add(result)

                        // 检查是否添加了快捷方式，如果没有则记录下来
                        if (!pinnedShortcuts.contains(it)) {
                            lostedShortcuts.add(result)
                            lostedShortcutsName.append(result.appName).append("\n")
                        }
                    }
                }
                store.close()

                handler.post {
                    try {
                        freeze_apps.adapter = FreezeAppAdapter(this.context!!.applicationContext, freezeAppsInfo)
                        processBarDialog.hideDialog()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // 即时是Oreo也可能出现获取不到已添加的快捷方式的情况，例如换了第三方桌面
                            // 如果发现有快捷方式丢失，提示是否重新添加
                            if (lostedShortcuts.size > 0) {
                                shortcutsLostDialog(lostedShortcutsName.toString(), lostedShortcuts)
                            }
                        }
                    } catch (ex: java.lang.Exception) {
                    }
                }
            } catch (ex: java.lang.Exception) {
            }
        }).start()
    }

    /**
     * 显示快捷方式丢失，提示添加
     */
    private fun shortcutsLostDialog(lostedShortcutsName: String, lostedShortcuts: ArrayList<Appinfo>) {
        val global = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (!global.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_ICON_NOTIFY, true)) {
            return
        }
        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setTitle(getString(R.string.freeze_shortcut_lost))
                .setMessage(getString(R.string.freeze_shortcut_lost_desc) + "\n\n$lostedShortcutsName")
                .setPositiveButton(R.string.btn_confirm) { _, _ ->
                    processBarDialog.showDialog(getString(R.string.please_wait))
                    CreateShortcutThread(lostedShortcuts, this.context!!, Runnable {
                        handler.post {
                            loadData()
                            processBarDialog.hideDialog()
                        }
                    }).start()
                }
                .setNegativeButton(R.string.btn_cancel) { _, _ ->
                }
                .setNeutralButton(R.string.btn_dontshow) { _, _ ->
                    global.edit().putBoolean(SpfConfig.GLOBAL_SPF_FREEZE_ICON_NOTIFY, false).apply()
                })
    }

    private fun showOptions(appInfo: Appinfo, position: Int, view: View) {
        DialogHelper.animDialog(AlertDialog.Builder(this.context!!)
                .setTitle(appInfo.appName)
                .setItems(
                        arrayOf(
                                getString(R.string.freeze_open),
                                getString(R.string.freeze_shortcut_rebuild),
                                getString(R.string.freeze_remove),
                                getString(R.string.freeze_remove_uninstall))) { _, which ->

                    when (which) {
                        0 -> startApp(appInfo)
                        1 -> createShortcut(appInfo)
                        2 -> {
                            removeConfig(appInfo)
                            loadData()
                        }
                        3 -> {
                            removeAndUninstall(appInfo)
                            loadData()
                        }
                    }
                }
                .setCancelable(true))
    }

    private fun removeConfig(appInfo: Appinfo) {
        if ((!appInfo.enabled) || appInfo.suspended) {
            enableApp(appInfo)
        }

        val packageName = appInfo.packageName.toString()
        val store = SceneConfigStore(context)
        val config = store.getAppConfig(packageName)
        config.freeze = false
        store.setAppConfig(config)
        store.close()

        SceneMode.getCurrentInstance()?.removeFreezeAppHistory(packageName)
        FreezeAppShortcutHelper().removeShortcut(this.context!!, packageName)
    }

    fun getUserId(context: Context): Int {
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        val userHandle = android.os.Process.myUserHandle()

        var value = 0
        try {
            value = um.getSerialNumberForUser(userHandle).toInt()
        } catch (ignored: Exception) {
        }

        return value
    }

    private fun removeAndUninstall(appInfo: Appinfo) {
        removeConfig(appInfo)
        KeepShellPublic.doCmdSync("pm uninstall --user " + getUserId(context!!) + " " + appInfo.packageName)
    }

    private fun enableApp(appInfo: Appinfo) {
        enableApp(appInfo.packageName.toString())
    }

    private fun enableApp(packageName: String) {
        SceneMode.unfreezeApp(packageName)
    }

    private fun disableApp(appInfo: Appinfo) {
        disableApp(appInfo.packageName.toString())
    }

    private fun disableApp(packageName: String) {
        SceneMode.freezeApp(packageName)
    }

    private fun toggleEnable(appInfo: Appinfo) {
        if (!appInfo.enabled || appInfo.suspended) {
            enableApp(appInfo)
            Toast.makeText(this.context, getString(R.string.freeze_enable_completed), Toast.LENGTH_SHORT).show()
            appInfo.enabled = true
            appInfo.suspended = false
        } else {
            disableApp(appInfo)
            Toast.makeText(this.context, getString(R.string.freeze_disable_completed), Toast.LENGTH_SHORT).show()
            appInfo.enabled = false
            appInfo.suspended = true
        }
    }

    private fun startApp(appInfo: Appinfo) {
        if (((!appInfo.enabled) || appInfo.suspended) || config.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_SUSPEND, false)) {
            enableApp(appInfo)
        }
        try {
            val intent = this.context!!.packageManager.getLaunchIntentForPackage(appInfo.packageName.toString())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
            if (intent != null) {
                this.context!!.startActivity(intent)
                SceneMode.getCurrentInstance()?.setFreezeAppStartTime(appInfo.packageName.toString())
            }
        } catch (ex: java.lang.Exception) {
        }

        if (this.activity!!.javaClass.name == ActivityFreezeApps::javaClass.name) {
            this.activity!!.finish()
        }
    }

    private fun createShortcut(appInfo: Appinfo) {
        if ((!appInfo.enabled) || appInfo.suspended) {
            enableApp(appInfo)
        }
        if (FreezeAppShortcutHelper().createShortcut(this.context!!, appInfo.packageName.toString())) {
            Toast.makeText(this.context, getString(R.string.freeze_shortcut_add_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this.context, getString(R.string.freeze_shortcut_add_fail), Toast.LENGTH_SHORT).show()
        }
    }

    private class CreateShortcutThread(private var apps: ArrayList<Appinfo>, private var context: Context, private var onCompleted: Runnable) : Thread() {
        override fun run() {
            for (appInfo in apps) {
                if ((!appInfo.enabled) || appInfo.suspended) {
                    KeepShellPublic.doCmdSync("pm enable ${appInfo.packageName}")
                }
                sleep(3000)
                FreezeAppShortcutHelper().createShortcut(this.context, appInfo.packageName.toString())
            }
            onCompleted.run()
        }
    }

    private fun addFreezeAppDialog() {
        val allApp = AppListHelper(this.context!!).getBootableApps(false, true)
        val apps = allApp.filter { !freezeApps.contains(it.packageName) }
        val items = apps.map { it.appName.toString() }.toTypedArray()
        val states = items.map { false }.toBooleanArray()

        DialogHelper.animDialog(AlertDialog.Builder(this.context)
                .setTitle(getString(R.string.freeze_add))
                .setMultiChoiceItems(items, states) { dialog, which, isChecked ->
                    states[which] = isChecked
                }
                .setPositiveButton(R.string.btn_confirm) { _, _ ->
                    val selectedItems = ArrayList<String>()
                    for (index in states.indices) {
                        if (states[index]) {
                            selectedItems.add(apps[index].packageName.toString())
                        }
                    }
                    addFreezeApps(selectedItems)
                }
                .setNegativeButton(R.string.btn_cancel, { _, _ -> })
                .setCancelable(true))
    }

    private fun addFreezeApps(selectedItems: ArrayList<String>) {
        processBarDialog.showDialog(getString(R.string.please_wait))
        val next = Runnable {
            handler.post {
                try {
                    loadData()
                    processBarDialog.hideDialog()
                } catch (ex: java.lang.Exception) {
                }
            }
        }
        AddFreezeAppsThread(this.context!!, selectedItems, next).start()
    }

    private class AddFreezeAppsThread(private var context: Context, private var selectedItems: ArrayList<String>, private var onCompleted: Runnable) : Thread() {
        val packageManager: PackageManager = context.packageManager
        override fun run() {
            val store = SceneConfigStore(context)
            val iconManager = LogoCacheManager(context)

            for (it in selectedItems) {
                val config = store.getAppConfig(it)

                config.freeze = true
                if (store.setAppConfig(config)) {
                    val icon = getAppIcon(it)
                    if (icon != null) {
                        iconManager.saveIcon(icon, it)
                    }
                }
            }
            store.close()

            onCompleted.run()
        }

        private fun getAppIcon(packageName: String): Drawable? {
            return packageManager.getApplicationIcon(packageName)
        }
    }

    private fun freezeOptionsDialog() {
        val p = context!!.packageManager
        val startActivity = ComponentName(this.context!!.applicationContext, ActivityFreezeApps::class.java)
        val enabled = p.getComponentEnabledSetting(startActivity) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        DialogHelper.animDialog(AlertDialog.Builder(this.context!!)
                .setTitle(getString(R.string.freeze_apps_manage))
                .setItems(
                        arrayOf(
                                getString(R.string.freeze_add_app),
                                getString(R.string.freeze_shortcut_rebuild),
                                getString(R.string.freeze_enable_all),
                                getString(R.string.freeze_disable_all),
                                if (enabled) getString(R.string.freeze_hidden_entrance) else getString(R.string.freeze_show_entrance),
                                getString(R.string.freeze_clear_all))) { _, which ->

                    when (which) {
                        0 -> addFreezeAppDialog()
                        1 -> createShortcutAll()
                        2 -> {
                            processBarDialog.showDialog()
                            for (it in freezeApps) {
                                enableApp(it)
                            }
                            processBarDialog.hideDialog()
                            loadData()
                        }
                        3 -> {
                            processBarDialog.showDialog()
                            for (it in freezeApps) {
                                disableApp(it)
                            }
                            processBarDialog.hideDialog()
                            loadData()
                        }
                        4 -> {
                            try {
                                if (enabled) {
                                    p.setComponentEnabledSetting(startActivity, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                                } else {
                                    p.setComponentEnabledSetting(startActivity, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                                }
                                Toast.makeText(this.context, getString(R.string.freeze_entrance_changed), Toast.LENGTH_SHORT).show()
                            } catch (ex: java.lang.Exception) {
                            }
                        }
                        5 -> {
                            processBarDialog.showDialog()
                            RemoveAllThread(this.context!!, freezeApps, Runnable {
                                handler.post {
                                    loadData()
                                    processBarDialog.hideDialog()
                                    Toast.makeText(context, getString(R.string.freeze_shortcut_delete_desc), Toast.LENGTH_LONG).show()
                                }
                            }).start()
                        }
                    }
                })
    }

    private fun createShortcutAll() {
        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setMessage(R.string.freeze_batch_add_wran).setPositiveButton(R.string.btn_confirm) { _, _ ->
                    processBarDialog.showDialog(getString(R.string.please_wait))
                    CreateShortcutAllThread(this.context!!, freezeApps, Runnable {
                        handler.post {
                            processBarDialog.hideDialog()
                            loadData()
                        }
                    }).start()
                }
                .setNeutralButton(R.string.btn_cancel) { _, _ ->
                })
    }

    private class CreateShortcutAllThread(private var context: Context, private var freezeApps: ArrayList<String>, private var onCompleted: Runnable) : Thread() {
        override fun run() {
            val shortcutHelper = FreezeAppShortcutHelper()
            for (it in freezeApps) {
                KeepShellPublic.doCmdSync("pm unhide $it\npm enable $it")
                sleep(3000)
                shortcutHelper.createShortcut(context, it)
            }
            onCompleted.run()
        }
    }

    private class RemoveAllThread(private var context: Context, private var freezeApps: ArrayList<String>, private var onCompleted: Runnable) : Thread() {
        override fun run() {

            val store = SceneConfigStore(context)
            val shortcutHelper = FreezeAppShortcutHelper()
            for (it in freezeApps) {
                KeepShellPublic.doCmdSync("pm unhide " + it + "\n" + "pm enable " + it)
                val config = store.getAppConfig(it)
                config.freeze = false
                store.setAppConfig(config)
                shortcutHelper.removeShortcut(context, it)
                SceneMode.getCurrentInstance()?.removeFreezeAppHistory(it)
            }
            store.close()

            onCompleted.run()
        }
    }

    companion object {
        fun createPage(): androidx.fragment.app.Fragment {
            val fragment = FragmentFreeze()
            return fragment
        }
    }
}
