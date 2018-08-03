package com.omarea.vboot

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.omarea.shared.AppListHelper
import com.omarea.shared.CommonCmds
import com.omarea.shared.SpfConfig
import com.omarea.shared.model.Appinfo
import com.omarea.shell.KeepShellPublic
import com.omarea.ui.AppListAdapter
import com.omarea.ui.OverScrollListView
import com.omarea.ui.ProgressBarDialog
import com.omarea.ui.SearchTextWatcher
import com.omarea.vboot.dialogs.DialogAppOptions
import com.omarea.vboot.dialogs.DialogSingleAppOptions
import kotlinx.android.synthetic.main.layout_applictions.*


class FragmentApplistions : Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog
    private lateinit var appListHelper: AppListHelper
    private var installedList: ArrayList<Appinfo>? = null
    private var systemList: ArrayList<Appinfo>? = null
    private var backupedList: ArrayList<Appinfo>? = null
    private val myHandler: Handler = UpdateHandler(Runnable {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_applictions, container, false)

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        processBarDialog = ProgressBarDialog(this.context!!)

        val tabHost = view.findViewById(R.id.blacklist_tabhost) as TabHost
        tabHost.setup()
        tabHost.addTab(tabHost.newTabSpec("tab_1").setContent(R.id.tab_apps_user).setIndicator(getString(R.string.use)))
        tabHost.addTab(tabHost.newTabSpec("tab_2").setContent(R.id.tab_apps_system).setIndicator(getString(R.string.system)))
        tabHost.addTab(tabHost.newTabSpec("tab_3").setContent(R.id.tab_apps_backuped).setIndicator(getString(R.string.backuped)))
        tabHost.addTab(tabHost.newTabSpec("tab_3").setContent(R.id.tab_apps_helper).setIndicator(getString(R.string.help)))
        tabHost.currentTab = 3

        apps_userlist.addHeaderView(this.layoutInflater.inflate(R.layout.app_list_headerview, null))
        apps_systemlist.addHeaderView(this.layoutInflater.inflate(R.layout.app_list_headerview, null))
        apps_backupedlist.addHeaderView(this.layoutInflater.inflate(R.layout.app_list_headerview, null))

        val onItemLongClick = AdapterView.OnItemLongClickListener { parent, _, position, id ->
            if (position < 1)
                return@OnItemLongClickListener true
            val adapter = (parent.adapter as HeaderViewListAdapter).wrappedAdapter
            val app = adapter.getItem(position - 1) as Appinfo
            DialogSingleAppOptions(context!!, app, myHandler).showSingleAppOptions()
            true
        }

        apps_userlist.onItemLongClickListener = onItemLongClick
        apps_systemlist.onItemLongClickListener = onItemLongClick
        apps_backupedlist.onItemLongClickListener = onItemLongClick

        fab_apps_user.setOnClickListener({
            getSelectedAppShowOptions(Appinfo.AppType.USER)
        })
        fab_apps_system.setOnClickListener({
            getSelectedAppShowOptions(Appinfo.AppType.SYSTEM)
        })
        fab_apps_backuped.setOnClickListener({
            getSelectedAppShowOptions(Appinfo.AppType.BACKUPFILE)
        })

        appListHelper = AppListHelper(context!!)
        setList()
        apps_search_box.setOnEditorActionListener({ _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                setList()
            }
            false
        })
        apps_search_box.addTextChangedListener(SearchTextWatcher(Runnable {
            searchApp()
        }))

        app_btn_hide.setOnClickListener {
            showHideAppDialog()
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun showHideAppDialog() {
        val spf = context!!.getSharedPreferences(SpfConfig.APP_HIDE_HISTORY_SPF, Context.MODE_PRIVATE)
        val all = spf.all
        val apps = ArrayList<String>()
        val selected = ArrayList<Boolean>()
        for (item in all.values) {
            apps.add(item as String)
            selected.add(false)
        }
        AlertDialog.Builder(context).setTitle("应用隐藏记录")
                .setMultiChoiceItems(apps.toTypedArray(), selected.toBooleanArray(), { dialog, which, isChecked ->
                    selected[which] = isChecked
                })
                .setPositiveButton(R.string.btn_confirm) { dialog, which ->
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
                    if (cmds.length > 0) {
                        processBarDialog.showDialog("正在恢复应用，稍等...")
                        Thread(Runnable {
                            KeepShellPublic.doCmdSync(cmds.toString())
                            myHandler.post {
                                processBarDialog.hideDialog()
                                setList()
                                edit.commit()
                            }
                        }).start()
                    }
                }
                .create().show()
    }

    private fun getSelectedAppShowOptions(apptype: Appinfo.AppType) {
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
            Snackbar.make(this.view!!, getString(R.string.app_selected_none), Snackbar.LENGTH_SHORT).show()
            return
        }

        when (apptype) {
            Appinfo.AppType.SYSTEM ->
                DialogAppOptions(context!!, selectedItems, myHandler).selectSystemAppOptions()
            Appinfo.AppType.USER ->
                DialogAppOptions(context!!, selectedItems, myHandler).selectUserAppOptions()
            Appinfo.AppType.BACKUPFILE ->
                DialogAppOptions(context!!, selectedItems, myHandler).selectBackupOptions()
            else -> {
            }
        }
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
            setListData(installedList, apps_userlist)
            setListData(systemList, apps_systemlist)
            setListData(backupedList, apps_backupedlist)
        }).start()
    }

    private fun setListData(dl: ArrayList<Appinfo>?, lv: OverScrollListView) {
        if (dl == null)
            return
        myHandler.post {
            try {
                if (isDetached) {
                    return@post
                }
                processBarDialog.hideDialog()
                val adapter = AppListAdapter(context!!, dl, apps_search_box.text.toString().toLowerCase())
                lv.adapter = adapter
                lv.onItemClickListener = OnItemClickListener { list, itemView, postion, _ ->
                    if (postion == 0) {
                        val checkBox = itemView.findViewById(R.id.select_state_all) as CheckBox
                        checkBox.isChecked = !checkBox.isChecked
                        adapter.setSelecteStateAll(checkBox.isChecked)
                        adapter.notifyDataSetChanged()
                    } else {
                        val checkBox = itemView.findViewById(R.id.select_state) as CheckBox
                        checkBox.isChecked = !checkBox.isChecked
                        val all = lv.findViewById<CheckBox>(R.id.select_state_all)
                        all.isChecked = adapter.getIsAllSelected()
                    }
                }
                val all = lv.findViewById<CheckBox>(R.id.select_state_all)
                all.isChecked = false
            } catch (ex: Exception) {
            }
        }
    }

    override fun onDetach() {
        processBarDialog.hideDialog()
        super.onDetach()
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentApplistions()
            return fragment
        }
    }
}