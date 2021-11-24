package com.omarea.vtools.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.*
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.AdapterAppChooser
import com.omarea.common.ui.DialogAppChooser
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.library.shell.GAppsUtilis
import com.omarea.model.AppInfo
import com.omarea.scene_mode.FreezeAppShortcutHelper
import com.omarea.scene_mode.LogoCacheManager
import com.omarea.scene_mode.SceneMode
import com.omarea.store.SceneConfigStore
import com.omarea.store.SpfConfig
import com.omarea.ui.AdapterFreezeApp
import com.omarea.ui.UMExpandLayout
import com.omarea.utils.AppListHelper
import com.omarea.vtools.R
import com.omarea.xposed.XposedCheck
import kotlinx.android.synthetic.main.activity_freeze_apps.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ActivityFreezeApps : ActivityBase() {
    private lateinit var processBarDialog: ProgressBarDialog
    private var freezeApps = java.util.ArrayList<String>()
    private var handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var config: SharedPreferences
    private var useSuspendMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_freeze_apps)
        setBackArrow()

        onViewCreated()

        // 使用壁纸高斯模糊作为窗口背景
        // val wallPaper = WallpaperManager.getInstance(this).getDrawable();
        // this.getWindow().setBackgroundDrawable(wallPaper);

        // this.getWindow().setBackgroundDrawable(BitmapDrawable(resources, rsBlur((wallPaper as BitmapDrawable).bitmap, 25)))
    }

    private fun rsBlur(source: Bitmap, radius: Int): Bitmap {
        val inputBmp = source
        val renderScript = RenderScript.create(this)

        // Allocate memory for Renderscript to work with
        //(2)
        val input = Allocation.createFromBitmap(renderScript, inputBmp)
        val output = Allocation.createTyped(renderScript, input.getType())
        //(3)
        // Load up an instance of the specific script that we want to use.
        val scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        //(4)
        scriptIntrinsicBlur.setInput(input)
        //(5)
        // Set the blur radius
        scriptIntrinsicBlur.setRadius(radius.toFloat())
        //(6)
        // Start the ScriptIntrinisicBlur
        scriptIntrinsicBlur.forEach(output)
        //(7)
        // Copy the output to the blurred bitmap
        output.copyTo(inputBmp)
        //(8)
        renderScript.destroy()

        return inputBmp
    }

    private fun onViewCreated() {
        config = this.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        processBarDialog = ProgressBarDialog(this)
        processBarDialog.showDialog()

        // 点击应用图标
        freeze_apps.setOnItemClickListener { parent, itemView, position, _ ->
            val appInfo = (parent.adapter.getItem(position) as AppInfo)
            if (appInfo.packageName == "plus") {
                addFreezeAppDialog()
            } else {
                try {
                    startApp(appInfo)
                } catch (ex: Exception) {
                }
            }
        }

        // 长按图标
        freeze_apps.setOnItemLongClickListener { parent, itemView, position, _ ->
            val item = (parent.adapter.getItem(position) as AppInfo)
            showOptions(item)
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
        Thread {
            try {
                // 数据库
                val store = SceneConfigStore(context)
                // 数据库中记录的已添加的偏见应用
                freezeApps = store.freezeAppList
                val checkShortcuts = config.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_ICON_NOTIFY, false)
                // 已添加到桌面的快捷方式
                val pinnedShortcuts = if (checkShortcuts) FreezeAppShortcutHelper().getPinnedShortcuts(context) else arrayListOf()

                val lostedShortcuts = ArrayList<AppInfo>()
                val lostedShortcutsName = StringBuilder()

                // val allApp = AppListHelper(context).getAll()
                val appListHelper = AppListHelper(context)

                val freezeAppsInfo = ArrayList<AppInfo>()
                // 遍历偏见应用列表 获取应用详情
                for (it in freezeApps) {
                    val packageName = it
                    val result = appListHelper.getApp(packageName)
                    if (result != null) {
                        freezeAppsInfo.add(result)

                        // 检查是否添加了快捷方式，如果没有则记录下来
                        if (checkShortcuts && !pinnedShortcuts.contains(it)) {
                            lostedShortcuts.add(result)
                            lostedShortcutsName.append(result.appName).append("\n")
                        }
                    }
                }
                store.close()

                handler.post {
                    try {
                        freeze_apps.adapter = AdapterFreezeApp(this.applicationContext, freezeAppsInfo)
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
        }.start()
    }

    /**
     * 显示快捷方式丢失，提示添加
     */
    private fun shortcutsLostDialog(lostedShortcutsName: String, lostedShortcuts: ArrayList<AppInfo>) {
        if (!config.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_ICON_NOTIFY, false)) {
            return
        }

        DialogHelper.confirm(this,
                getString(R.string.freeze_shortcut_lost),
                getString(R.string.freeze_shortcut_lost_desc) + "\n\n$lostedShortcutsName",
                {
                    processBarDialog.showDialog(getString(R.string.please_wait))
                    CreateShortcutThread(lostedShortcuts, context) {
                        handler.post {
                            loadData()
                            processBarDialog.hideDialog()
                        }
                    }.start()
                })
    }

    private fun showOptions(appInfo: AppInfo) {
        val view = layoutInflater.inflate(R.layout.dialog_freeze_app_opt, null)
        val dialog = DialogHelper.customDialog(this, view)

        view.findViewById<View>(R.id.app_options_open).setOnClickListener {
            dialog.dismiss()
            startApp(appInfo)
        }
        view.findViewById<View>(R.id.app_options_shortcut).setOnClickListener {
            dialog.dismiss()
            createShortcut(appInfo)
        }
        view.findViewById<View>(R.id.app_options_remove).setOnClickListener {
            dialog.dismiss()
            removeConfig(appInfo)
            loadData()
        }
        view.findViewById<View>(R.id.app_options_uninstall).setOnClickListener {
            dialog.dismiss()
            DialogHelper.confirm(this, "确认卸载？", "目标应用：${appInfo.appName}", {
                removeAndUninstall(appInfo)
                loadData()
            })
        }
        view.findViewById<View>(R.id.app_options_freeze).setOnClickListener {
            dialog.dismiss()
            toggleEnable(appInfo)
            loadData()
        }
    }

    private fun removeConfig(appInfo: AppInfo) {
        if ((!appInfo.enabled) || appInfo.suspended) {
            enableApp(appInfo)
        }

        val packageName = appInfo.packageName
        val store = SceneConfigStore(context)
        val config = store.getAppConfig(packageName)
        config.freeze = false
        store.setAppConfig(config)
        store.close()

        SceneMode.getCurrentInstance()?.removeFreezeAppHistory(packageName)
        FreezeAppShortcutHelper().removeShortcut(context, packageName)
    }

    // TODO:替换公共方法
    private fun getUserId(context: Context): Int {
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        val userHandle = Process.myUserHandle()

        var value = 0
        try {
            value = um.getSerialNumberForUser(userHandle).toInt()
        } catch (ignored: Exception) {
        }

        return value
    }

    // 切换[图标置灰模式]
    private fun switchSuspendMode() {
        processBarDialog.showDialog()
        GlobalScope.launch(Dispatchers.IO) {
            for (it in freezeApps) {
                enableApp(it)
                disableApp(it)
            }
            handler.post {
                processBarDialog.hideDialog()
                loadData()
            }
        }
    }

    private fun removeAndUninstall(appInfo: AppInfo) {
        removeConfig(appInfo)
        KeepShellPublic.doCmdSync("pm uninstall --user " + getUserId(context) + " " + appInfo.packageName)
    }

    private fun enableApp(appInfo: AppInfo) {
        enableApp(appInfo.packageName)
    }

    private fun enableApp(packageName: String) {
        SceneMode.unfreezeApp(packageName)
    }

    private fun disableApp(appInfo: AppInfo) {
        disableApp(appInfo.packageName)
    }

    private fun disableApp(packageName: String) {
        if (useSuspendMode) {
            SceneMode.suspendApp(packageName)
        } else {
            SceneMode.freezeApp(packageName)
        }
    }

    private fun toggleEnable(appInfo: AppInfo) {
        if (!appInfo.enabled || appInfo.suspended) {
            enableApp(appInfo)
            Toast.makeText(context, getString(R.string.freeze_enable_completed), Toast.LENGTH_SHORT).show()
            appInfo.enabled = true
            appInfo.suspended = false
        } else {
            disableApp(appInfo)
            Toast.makeText(context, getString(R.string.freeze_disable_completed), Toast.LENGTH_SHORT).show()
            appInfo.enabled = false
            appInfo.suspended = true
        }
    }

    private fun startApp(appInfo: AppInfo) {
        if (((!appInfo.enabled) || appInfo.suspended)) {
            enableApp(appInfo)
            appInfo.enabled = true
            appInfo.suspended = false
            (freeze_apps?.adapter as AdapterFreezeApp?)?.notifyDataSetChanged()
        }
        SceneMode.getCurrentInstance()?.setFreezeAppLeaveTime(appInfo.packageName)
        try {
            val intent = this.packageManager.getLaunchIntentForPackage(appInfo.packageName)
            // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_TASK_ON_HOME)

            // i.setFlags((i.getFlags() & ~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) | Intent.FLAG_ACTIVITY_NEW_TASK);
            // i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            // i.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            // i.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
            // i.setFlags(0x10200000);
            // Log.d("getAppSwitchIntent", "" + i.getFlags());
            intent?.run {
                flags = flags and Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED.inv() or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
                // setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // @参考 https://blog.csdn.net/weixin_34335458/article/details/88020972
                // setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // @参考 https://blog.csdn.net/weixin_34335458/article/details/88020972
                setPackage(null) // 加上这句代
            }

            if (intent != null) {
                this.startActivity(intent)
            }
        } catch (ex: java.lang.Exception) {
        }
    }

    private fun createShortcut(appInfo: AppInfo) {
        if ((!appInfo.enabled) || appInfo.suspended) {
            enableApp(appInfo)
        }
        if (FreezeAppShortcutHelper().createShortcut(context, appInfo.packageName)) {
            Toast.makeText(context, getString(R.string.freeze_shortcut_add_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, getString(R.string.freeze_shortcut_add_fail), Toast.LENGTH_SHORT).show()
        }
    }

    private class CreateShortcutThread(private var apps: ArrayList<AppInfo>, private var context: Context, private var onCompleted: Runnable) : Thread() {
        override fun run() {
            for (appInfo in apps) {
                if ((!appInfo.enabled) || appInfo.suspended) {
                    KeepShellPublic.doCmdSync("pm enable ${appInfo.packageName}")
                }
                sleep(3000)
                FreezeAppShortcutHelper().createShortcut(context, appInfo.packageName)
            }
            onCompleted.run()
        }
    }

    private fun addFreezeAppDialog() {
        processBarDialog.showDialog()
        Thread {
            val allApp = AppListHelper(context).getBootableApps(false, true)
            val apps = allApp.filter { !freezeApps.contains(it.packageName) }
            val options = ArrayList(apps.map {
                AdapterAppChooser.AppInfo().apply {
                    appName = "" + it.appName
                    packageName = "" + it.packageName
                    selected = false
                }
            })
            handler.post {
                try {
                    processBarDialog.hideDialog()

                    DialogAppChooser(themeMode.isDarkMode, options, true, object : DialogAppChooser.Callback {
                        override fun onConfirm(apps: List<AdapterAppChooser.AppInfo>) {
                            addFreezeApps(ArrayList(apps.map { it.packageName }))
                        }
                    })
                            .setExcludeApps(arrayOf(
                                    context.packageName,
                                    "com.topjohnwu.magisk",
                                    "eu.chainfire.supersu",
                                    "com.android.settings"))
                            .setAllowAllSelect(false)
                            .show(supportFragmentManager, "freeze-app-add")
                } catch (ex: java.lang.Exception) {
                }
            }
        }.start()
    }

    private fun addFreezeApps(selectedItems: List<String>) {
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
        AddFreezeAppsThread(context, selectedItems, next, useSuspendMode).start()
    }

    private class AddFreezeAppsThread(
            private var context: Context,
            private var selectedItems: List<String>,
            private var onCompleted: Runnable,
            private val useSuspendMode: Boolean
    ) : Thread() {
        val packageManager: PackageManager = context.packageManager
        private fun disableApp(packageName: String) {
            if (useSuspendMode) {
                SceneMode.suspendApp(packageName)
            } else {
                SceneMode.freezeApp(packageName)
            }
        }

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
                    disableApp(it)
                }
            }
            store.close()

            onCompleted.run()
        }

        private fun getAppIcon(packageName: String): Drawable? {
            try {
                return packageManager.getApplicationIcon(packageName)
            } catch (ex: java.lang.Exception) {
                return null
            }
        }
    }

    private fun freezeOptionsDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_freeze_menu, null)
        val dialog = DialogHelper.customDialog(this, view)

        view.findViewById<View>(R.id.menu_freeze).setOnClickListener { _ ->
            dialog.dismiss()
            processBarDialog.showDialog()
            GlobalScope.launch(Dispatchers.IO) {
                for (it in freezeApps) {
                    disableApp(it)
                }
                handler.post {
                    processBarDialog.hideDialog()
                    loadData()
                }
            }
        }

        useSuspendMode = config.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_SUSPEND, Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        val launcherHook = view.findViewById<CompoundButton>(R.id.freeze_any_unfreeze)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val freeze_suspend_mode = view.findViewById<CompoundButton>(R.id.freeze_suspend_mode)
            freeze_suspend_mode.run {
                isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                isChecked = useSuspendMode
                setOnClickListener {
                    val checked = (it as CompoundButton).isChecked
                    switchSuspendMode()

                    useSuspendMode = checked
                    config.edit().putBoolean(SpfConfig.GLOBAL_SPF_FREEZE_SUSPEND, useSuspendMode).apply()

                    launcherHook.isEnabled = checked && XposedCheck.xposedIsRunning()
                }
            }
        } else {
            view.findViewById<View>(R.id.freeze_suspend).visibility = View.GONE
            view.findViewById<View>(R.id.freeze_suspend_xposed).visibility = View.GONE
        }

        launcherHook.run {
            isEnabled = useSuspendMode && XposedCheck.xposedIsRunning()
            isChecked = XposedCheck.xposedIsRunning() && config.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_XPOSED_OPEN, false)
            setOnClickListener {
                config.edit().putBoolean(SpfConfig.GLOBAL_SPF_FREEZE_XPOSED_OPEN, (it as CompoundButton).isChecked).apply()
            }
        }

        val freeze_shortcut_suggest = view.findViewById<CompoundButton>(R.id.freeze_shortcut_suggest)
        freeze_shortcut_suggest.isChecked = config.getBoolean(SpfConfig.GLOBAL_SPF_FREEZE_ICON_NOTIFY, false)
        freeze_shortcut_suggest.setOnClickListener {
            config.edit().putBoolean(SpfConfig.GLOBAL_SPF_FREEZE_ICON_NOTIFY, (it as CompoundButton).isChecked).apply()
        }

        val freeze_time_limit = view.findViewById<SeekBar>(R.id.freeze_time_limit)
        val freeze_time_limit_text = view.findViewById<TextView>(R.id.freeze_time_limit_text)
        freeze_time_limit.run {
            progress = config.getInt(SpfConfig.GLOBAL_SPF_FREEZE_TIME_LIMIT, 2)
            freeze_time_limit_text.text = progress.toString()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    freeze_time_limit_text.text = progress.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.run {
                        config.edit().putInt(SpfConfig.GLOBAL_SPF_FREEZE_TIME_LIMIT, seekBar.progress).apply()
                    }
                }
            })
        }

        val freeze_quick_entry = view.findViewById<CompoundButton>(R.id.freeze_quick_entry)

        val p = packageManager
        val startActivity = ComponentName(this.applicationContext, ActivityFreezeApps::class.java)
        val activityEnabled = p.getComponentEnabledSetting(startActivity) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        freeze_quick_entry.isChecked = activityEnabled
        freeze_quick_entry.setOnClickListener {
            try {
                if ((it as CompoundButton).isChecked) {
                    p.setComponentEnabledSetting(startActivity, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                } else {
                    p.setComponentEnabledSetting(startActivity, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                }
                Toast.makeText(context, getString(R.string.freeze_entrance_changed), Toast.LENGTH_SHORT).show()
            } catch (ex: java.lang.Exception) {
            }
            (it as CompoundButton).isChecked = p.getComponentEnabledSetting(startActivity) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }

        view.findViewById<View>(R.id.menu_unfreeze).setOnClickListener { _ ->
            dialog.dismiss()

            processBarDialog.showDialog()
            Thread {
                for (it in freezeApps) {
                    enableApp(it)
                }
                handler.post {
                    processBarDialog.hideDialog()
                    loadData()
                }
            }.start()
        }
        view.findViewById<View>(R.id.menu_remove).setOnClickListener {
            dialog.dismiss()
            processBarDialog.showDialog()
            RemoveAllThread(context, freezeApps) {
                handler.post {
                    loadData()
                    processBarDialog.hideDialog()
                    Toast.makeText(context, getString(R.string.freeze_shortcut_delete_desc), Toast.LENGTH_LONG).show()
                }
            }.start()
        }
        view.findViewById<View>(R.id.menu_shortcut).setOnClickListener {
            dialog.dismiss()
            createShortcutAll()
        }
        view.findViewById<View>(R.id.menu_auto_add).setOnClickListener {
            dialog.dismiss()
            autoAddList()
        }

        val freezeOptions = view.findViewById<UMExpandLayout>(R.id.freeze_options)
        view.findViewById<View>(R.id.freeze_options_switch).setOnClickListener {
            freezeOptions.toggleExpand()
            (it as ImageView).setImageDrawable(ContextCompat.getDrawable(context, (if (freezeOptions.isExpand) {
                R.drawable.arrow_up
            } else {
                R.drawable.arrow_down
            })))
        }
        handler.postDelayed({
            freezeOptions.initExpand(false)
        }, 15)
    }

    private fun createShortcutAll() {
        DialogHelper.confirm(this, getString(R.string.freeze_batch_add), getString(R.string.freeze_batch_add_wran), {
            processBarDialog.showDialog(getString(R.string.please_wait))
            CreateShortcutAllThread(context, freezeApps) {
                handler.post {
                    processBarDialog.hideDialog()
                    loadData()
                }
            }.start()
        })
    }

    private class CreateShortcutAllThread(
            private var context: Context,
            private var freezeApps: ArrayList<String>,
            private var onCompleted: Runnable
    ) : Thread() {
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

    private class RemoveAllThread(
            private var context: Context,
            private var freezeApps: ArrayList<String>,
            private var onCompleted: Runnable
    ) : Thread() {
        override fun run() {
            val store = SceneConfigStore(context)
            val shortcutHelper = FreezeAppShortcutHelper()
            for (it in freezeApps) {
                if (it == "com.android.vending") {
                    GAppsUtilis().enable(KeepShellPublic.secondaryKeepShell)
                } else {
                    KeepShellPublic.doCmdSync("pm unsuspend ${it}\n pm unhide ${it}\n" + "pm enable $it")
                }
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

    // 自动筛选已冻结的普通应用，添加到应用偏见列表
    private fun autoAddList() {
        processBarDialog.showDialog(getString(R.string.please_wait))
        GlobalScope.launch(Dispatchers.IO) {
            val appListHelper = AppListHelper(context)
            val frozenApp = appListHelper.getUserAppList().filter {
                (!it.enabled || it.suspended) && !(freezeApps.contains(it.packageName) || appListHelper.isSystemApp(applicationInfo))
            }
            val pm = packageManager
            val appList = frozenApp.filter {
                val enabled = it.enabled
                // 冻结状态获取不到启动Activity，先解冻
                if (!enabled) {
                    enableApp(it)
                }
                try {
                    return@filter pm.getLaunchIntentForPackage(it.packageName) != null
                } catch (ex: java.lang.Exception) {
                } finally {
                    if (!enabled) {
                        disableApp(it)
                    }
                }
                return@filter false
            }.map { it.packageName }.toList()
            handler.post {
                addFreezeApps(appList)
            }
        }
    }
}