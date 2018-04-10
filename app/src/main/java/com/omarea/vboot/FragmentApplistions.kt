package com.omarea.vboot

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView.OnItemClickListener
import android.widget.CheckBox
import android.widget.HeaderViewListAdapter
import android.widget.ListView
import android.widget.TabHost
import com.omarea.shared.Consts
import com.omarea.shared.model.Appinfo
import com.omarea.ui.AppListAdapter
import com.omarea.ui.ProgressBarDialog
import com.omarea.ui.SearchTextWatcher
import com.omarea.shared.AppListHelper
import com.omarea.vboot.dialogs.DialogAppOptions
import com.omarea.vboot.dialogs.DialogSingleAppOptions
import kotlinx.android.synthetic.main.layout_applictions.*
import java.util.*


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
            inflater!!.inflate(R.layout.layout_applictions, container, false)

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        processBarDialog = ProgressBarDialog(this.context!!)

        val tabHost = view!!.findViewById(R.id.blacklist_tabhost) as TabHost
        tabHost.setup()
        tabHost.addTab(tabHost.newTabSpec("tab_1").setContent(R.id.tab_apps_user).setIndicator(getString(R.string.use)))
        tabHost.addTab(tabHost.newTabSpec("tab_2").setContent(R.id.tab_apps_system).setIndicator(getString(R.string.system)))
        tabHost.addTab(tabHost.newTabSpec("tab_3").setContent(R.id.tab_apps_backuped).setIndicator(getString(R.string.backuped)))
        tabHost.addTab(tabHost.newTabSpec("tab_3").setContent(R.id.tab_apps_helper).setIndicator(getString(R.string.help)))
        tabHost.currentTab = 3

        apps_userlist.addHeaderView(this.getLayoutInflater().inflate(R.layout.app_list_headerview, null))
        apps_systemlist.addHeaderView(this.getLayoutInflater().inflate(R.layout.app_list_headerview, null))
        apps_backupedlist.addHeaderView(this.getLayoutInflater().inflate(R.layout.app_list_headerview, null))

        apps_userlist.setOnItemLongClickListener({
            parent, _, position, id ->
            val adapter = (parent.adapter as HeaderViewListAdapter).wrappedAdapter
            DialogSingleAppOptions(context!!, adapter.getItem(position - 1) as Appinfo, myHandler).showUserAppOptions()
            true
        })

        apps_systemlist.setOnItemLongClickListener({
            parent, _, position, id ->
            val adapter = (parent.adapter as HeaderViewListAdapter).wrappedAdapter
            DialogSingleAppOptions(context!!, adapter.getItem(position - 1) as Appinfo, myHandler).showSystemAppOptions()
            true
        })

        apps_backupedlist.setOnItemLongClickListener({
            parent, _, position, id ->
            val adapter = (parent.adapter as HeaderViewListAdapter).wrappedAdapter
            DialogSingleAppOptions(context!!, adapter.getItem(position - 1) as Appinfo, myHandler).showBackupAppOptions()
            true
        })

        fab_apps_user.setOnClickListener(View.OnClickListener {
            val adapter = (apps_userlist.adapter as HeaderViewListAdapter).wrappedAdapter
            val selectedItems = (adapter as AppListAdapter).getSelectedItems()
            if (selectedItems.size == 0) {
                Snackbar.make(view, getString(R.string.app_selected_none), Snackbar.LENGTH_SHORT).show()
                return@OnClickListener
            }

            DialogAppOptions(context!!, selectedItems, myHandler).selectUserAppOptions()
        })

        fab_apps_system.setOnClickListener(View.OnClickListener {
            val adapter = (apps_systemlist.adapter as HeaderViewListAdapter).wrappedAdapter
            val selectedItems = (adapter as AppListAdapter).getSelectedItems()
            if (selectedItems.size == 0) {
                Snackbar.make(view, getString(R.string.app_selected_none), Snackbar.LENGTH_SHORT).show()
                return@OnClickListener
            }
            DialogAppOptions(context!!, selectedItems, myHandler).selectSystemAppOptions()
        })

        fab_apps_backuped.setOnClickListener(View.OnClickListener {
            val adapter = (apps_backupedlist.adapter as HeaderViewListAdapter).wrappedAdapter
            val selectedItems = (adapter as AppListAdapter).getSelectedItems()
            if (selectedItems.size == 0) {
                Snackbar.make(view, getString(R.string.app_selected_none), Snackbar.LENGTH_SHORT).show()
                return@OnClickListener
            }
            DialogAppOptions(context!!, selectedItems, myHandler).selectBackupOptions()
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
            backupedList = appListHelper.getApkFilesInfoList(Consts.AbsBackUpDir)
            setListData(installedList, apps_userlist)
            setListData(systemList, apps_systemlist)
            setListData(backupedList, apps_backupedlist)
        }).start()
    }

    private fun setListData(dl: ArrayList<Appinfo>?, lv: ListView) {
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
                lv.onItemClickListener = OnItemClickListener { list, itemView, _, _ ->
                    try {
                        val checkBox = itemView.findViewById(R.id.select_state) as CheckBox
                        checkBox.isChecked = !checkBox.isChecked
                        val all = lv.findViewById<CheckBox>(R.id.select_state_all)
                        all.isChecked = adapter.getIsAllSelected()
                    } catch (ex: Exception) {
                        val checkBox = itemView.findViewById(R.id.select_state_all) as CheckBox
                        checkBox.isChecked = !checkBox.isChecked
                        adapter.setSelecteStateAll(checkBox.isChecked)
                        adapter.notifyDataSetChanged()
                    }
                }
            } catch (ex: Exception) {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentApplistions()
            return fragment
        }
    }
}