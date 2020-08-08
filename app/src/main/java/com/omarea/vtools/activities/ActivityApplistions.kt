package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.omarea.Scene
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.OverScrollListView
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.model.Appinfo
import com.omarea.store.SpfConfig
import com.omarea.ui.AppListAdapter
import com.omarea.ui.SearchTextWatcher
import com.omarea.ui.TabIconHelper
import com.omarea.utils.AppListHelper
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R
import com.omarea.vtools.dialogs.DialogAppOptions
import com.omarea.vtools.dialogs.DialogSingleAppOptions
import kotlinx.android.synthetic.main.activity_applictions.*
import java.lang.ref.WeakReference


class ActivityApplistions : ActivityBase() {
    private lateinit var processBarDialog: ProgressBarDialog
    private lateinit var appListHelper: AppListHelper
    private var installedList: ArrayList<Appinfo>? = null
    private var systemList: ArrayList<Appinfo>? = null
    private var backupedList: ArrayList<Appinfo>? = null
    private var myHandler: Handler? = UpdateHandler(Runnable {
        setList()
    })

    class UpdateHandler(private var updateList: Runnable?) : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == 2) {
                if (updateList != null) {
                    updateList!!.run()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_applictions)

        setBackArrow()


        val context = this
        val activity = this
        processBarDialog = ProgressBarDialog(this)

        val tabHost = findViewById(R.id.applications_tabhost) as TabHost
        tabHost.setup()
        val tabIconHelper = TabIconHelper(tabHost, this)

        tabIconHelper.newTabSpec("data", getDrawable(R.drawable.tab_app)!!, R.id.tab_apps_user)
        tabIconHelper.newTabSpec("system", getDrawable(R.drawable.tab_security)!!, R.id.tab_apps_system)
        tabIconHelper.newTabSpec("backups", getDrawable(R.drawable.tab_package)!!, R.id.tab_apps_backuped)
        tabIconHelper.newTabSpec("帮助", getDrawable(R.drawable.tab_help)!!, R.id.tab_apps_helper)
        tabHost.setOnTabChangedListener { tabId ->
            tabIconHelper.updateHighlight()
        }

        apps_userlist.addHeaderView(this.layoutInflater.inflate(R.layout.list_header_app, null))
        apps_systemlist.addHeaderView(this.layoutInflater.inflate(R.layout.list_header_app, null))
        apps_backupedlist.addHeaderView(this.layoutInflater.inflate(R.layout.list_header_app, null))

        val onItemLongClick = AdapterView.OnItemLongClickListener { parent, _, position, id ->
            if (position < 1)
                return@OnItemLongClickListener true
            val adapter = (parent.adapter as HeaderViewListAdapter).wrappedAdapter
            val app = adapter.getItem(position - 1) as Appinfo
            DialogSingleAppOptions(context, app, myHandler!!).showSingleAppOptions(activity)
            true
        }

        apps_userlist.onItemLongClickListener = onItemLongClick
        apps_systemlist.onItemLongClickListener = onItemLongClick
        apps_backupedlist.onItemLongClickListener = onItemLongClick

        fab_apps_user.setOnClickListener {
            getSelectedAppShowOptions(Appinfo.AppType.USER, activity)
        }
        fab_apps_system.setOnClickListener {
            getSelectedAppShowOptions(Appinfo.AppType.SYSTEM, activity)
        }
        fab_apps_backuped.setOnClickListener {
            getSelectedAppShowOptions(Appinfo.AppType.BACKUPFILE, activity)
        }

        appListHelper = AppListHelper(context)
        setList()
        apps_search_box.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                setList()
            }
            false
        }
        apps_search_box.addTextChangedListener(SearchTextWatcher(Runnable {
            searchApp()
        }))

        app_btn_hide.setOnClickListener {
            showHideAppDialog()
        }
        app_btn_hide2.setOnClickListener {
            val intent = Intent(context, ActivityHiddenApps::class.java)
            startActivity(intent)
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun showHideAppDialog() {
        // pm list -u
        val spf = getSharedPreferences(SpfConfig.APP_HIDE_HISTORY_SPF, Context.MODE_PRIVATE)

        val all = spf.all
        val apps = ArrayList<String>()
        val selected = ArrayList<Boolean>()
        for (item in all.values) {
            apps.add(item as String)
            selected.add(false)
        }

        DialogHelper.animDialog(AlertDialog.Builder(this).setTitle("应用隐藏记录")
                .setMultiChoiceItems(apps.toTypedArray(), selected.toBooleanArray()) { _, which, isChecked ->
                    selected[which] = isChecked
                }
                .setNeutralButton(R.string.btn_again_hide) { _, _ ->
                    val keys = all.keys.toList()
                    val cmds = StringBuffer()
                    val edit = spf.edit()
                    for (i in selected.indices) {
                        if (selected[i]) {
                            cmds.append("pm disable ")
                            cmds.append(keys.get(i))
                            cmds.append("\n")
                            cmds.append("pm hide ")
                            cmds.append(keys[i])
                            cmds.append("\n")
                            edit.remove(keys.get(i))
                        }
                    }
                    if (cmds.isNotEmpty()) {
                        processBarDialog.showDialog("正在隐藏应用，稍等...")
                        Thread(Runnable {
                            KeepShellPublic.doCmdSync(cmds.toString())
                            if (myHandler != null) {
                                myHandler!!.post {
                                    processBarDialog.hideDialog()
                                    setList()
                                    edit.commit()
                                }
                            }
                        }).start()
                    }
                }
                .setPositiveButton(R.string.btn_resume) { _, _ ->
                    val keys = all.keys.toList()
                    val cmds = StringBuffer()
                    val edit = spf.edit()
                    for (i in selected.indices) {
                        if (selected[i]) {
                            cmds.append("pm unhide ")
                            cmds.append(keys.get(i))
                            cmds.append("\n")
                            cmds.append("pm enable ")
                            cmds.append(keys.get(i))
                            cmds.append("\n")
                            edit.remove(keys.get(i))
                        }
                    }
                    if (cmds.isNotEmpty()) {
                        processBarDialog.showDialog("正在恢复应用，稍等...")
                        Thread(Runnable {
                            KeepShellPublic.doCmdSync(cmds.toString())
                            if (myHandler != null) {
                                myHandler!!.post {
                                    processBarDialog.hideDialog()
                                    setList()
                                    edit.commit()
                                }
                            }
                        }).start()
                    }
                })
    }

    private fun getSelectedAppShowOptions(apptype: Appinfo.AppType, activity: Activity) {
        var adapter: Adapter? = null
        when (apptype) {
            Appinfo.AppType.USER -> adapter = apps_userlist.adapter
            Appinfo.AppType.SYSTEM -> adapter = apps_systemlist.adapter
            Appinfo.AppType.BACKUPFILE -> adapter = apps_backupedlist.adapter
            else -> {
            }
        }
        if (adapter == null)
            return
        adapter = (adapter as HeaderViewListAdapter).wrappedAdapter
        val selectedItems = (adapter as AppListAdapter).getSelectedItems()
        if (selectedItems.size == 0) {
            Scene.toast(R.string.app_selected_none, Toast.LENGTH_SHORT)
            return
        }

        when (apptype) {
            Appinfo.AppType.SYSTEM ->
                DialogAppOptions(this, selectedItems, myHandler!!).selectSystemAppOptions(activity)
            Appinfo.AppType.USER ->
                DialogAppOptions(this, selectedItems, myHandler!!).selectUserAppOptions(activity)
            Appinfo.AppType.BACKUPFILE ->
                DialogAppOptions(this, selectedItems, myHandler!!).selectBackupOptions()
            else -> {
            }
        }
    }

    override fun onResume() {
        super.onResume()

        title = getString(R.string.menu_applictions)
    }

    private fun searchApp() {
        setListData(installedList, apps_userlist)
        setListData(systemList, apps_systemlist)
        setListData(backupedList, apps_backupedlist)
    }

    private fun setList() {
        processBarDialog.showDialog()
        Thread(Runnable {
            systemList = appListHelper.getSystemAppList()
            installedList = appListHelper.getUserAppList()
            backupedList = appListHelper.getApkFilesInfoList(CommonCmds.AbsBackUpDir)
            apps_userlist?.run {
                setListData(installedList, this)
            }
            apps_systemlist?.run {
                setListData(systemList, this)
            }
            apps_backupedlist?.run {
                setListData(backupedList, this)
            }
        }).start()
    }

    private fun setListData(dl: ArrayList<Appinfo>?, lv: OverScrollListView) {
        if (dl == null)
            return
        if (myHandler != null) {
            myHandler!!.post {
                try {
                    processBarDialog.hideDialog()
                    val adapterObj = AppListAdapter(dl, apps_search_box.text.toString().toLowerCase())
                    val adapter: WeakReference<AppListAdapter> = WeakReference(adapterObj)
                    lv.adapter = adapterObj
                    lv.onItemClickListener = OnItemClickListener { list, itemView, postion, _ ->
                        if (postion == 0) {
                            val checkBox = itemView.findViewById(R.id.select_state_all) as CheckBox
                            checkBox.isChecked = !checkBox.isChecked
                            if (adapter.get() != null) {
                                adapter.get()!!.setSelecteStateAll(checkBox.isChecked)
                                adapter.get()!!.notifyDataSetChanged()
                            }
                        } else {
                            val checkBox = itemView.findViewById(R.id.select_state) as CheckBox
                            checkBox.isChecked = !checkBox.isChecked
                            val all = lv.findViewById<CheckBox>(R.id.select_state_all)
                            if (adapter.get() != null) {
                                all.isChecked = adapter.get()!!.getIsAllSelected()
                            }
                        }
                    }
                    val all = lv.findViewById<CheckBox>(R.id.select_state_all)
                    all.isChecked = false
                } catch (ex: Exception) {
                }
            }
        }
    }

    override fun onDestroy() {
        processBarDialog.hideDialog()
        myHandler = null
        super.onDestroy()
    }
}