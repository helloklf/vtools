package com.omarea.vboot

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView.OnItemClickListener
import android.widget.CheckBox
import android.widget.HeaderViewListAdapter
import android.widget.ListView
import android.widget.TabHost
import com.omarea.shared.Appinfo
import com.omarea.shared.Consts
import com.omarea.ui.AppListAdapter
import com.omarea.ui.ProgressBarDialog
import com.omarea.units.AppListHelper2
import com.omarea.vboot.dialogs.DialogAppOptions
import com.omarea.vboot.dialogs.DialogSingleAppOptions
import kotlinx.android.synthetic.main.layout_applictions.*
import java.util.*


class FragmentApplistions : Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog
    private lateinit var appListHelper: AppListHelper2
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

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater!!.inflate(R.layout.layout_applictions, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        processBarDialog = ProgressBarDialog(this.context)

        val tabHost = view!!.findViewById(R.id.blacklist_tabhost) as TabHost
        tabHost.setup()
        tabHost.addTab(tabHost.newTabSpec("tab_1").setContent(R.id.tab_apps_user).setIndicator("用户"))
        tabHost.addTab(tabHost.newTabSpec("tab_2").setContent(R.id.tab_apps_system).setIndicator("系统"))
        tabHost.addTab(tabHost.newTabSpec("tab_3").setContent(R.id.tab_apps_backuped).setIndicator("已备份"))
        tabHost.addTab(tabHost.newTabSpec("tab_3").setContent(R.id.tab_apps_helper).setIndicator("帮助"))
        tabHost.currentTab = 3

        apps_userlist.addHeaderView(this.getLayoutInflater().inflate(R.layout.app_list_headerview, null))
        apps_systemlist.addHeaderView(this.getLayoutInflater().inflate(R.layout.app_list_headerview, null))
        apps_backupedlist.addHeaderView(this.getLayoutInflater().inflate(R.layout.app_list_headerview, null))

        apps_userlist.setOnItemLongClickListener({
            parent, _, position, id ->
            DialogSingleAppOptions(context, apps_userlist.adapter.getItem(position) as Appinfo, myHandler).showUserAppOptions()
            true
        })

        apps_systemlist.setOnItemLongClickListener({
            parent, _, position, id ->
            DialogSingleAppOptions(context, apps_userlist.adapter.getItem(position) as Appinfo, myHandler).showSystemAppOptions()
            true
        })

        apps_backupedlist.setOnItemLongClickListener({
            parent, _, position, id ->
            DialogSingleAppOptions(context, apps_userlist.adapter.getItem(position) as Appinfo, myHandler).showBackupAppOptions()
            true
        })

        fab_apps_user.setOnClickListener(View.OnClickListener {
            val adapter = (apps_userlist.adapter as HeaderViewListAdapter).wrappedAdapter
            val selectedItems = (adapter as AppListAdapter).getSelectedItems()
            if (selectedItems.size == 0) {
                Snackbar.make(view, "一个应用也没有选中！", Snackbar.LENGTH_SHORT).show()
                return@OnClickListener
            }

            DialogAppOptions(context, selectedItems, myHandler).selectUserAppOptions()
        })

        fab_apps_system.setOnClickListener(View.OnClickListener {
            val adapter = (apps_systemlist.adapter as HeaderViewListAdapter).wrappedAdapter
            val selectedItems = (adapter as AppListAdapter).getSelectedItems()
            if (selectedItems.size == 0) {
                Snackbar.make(view, "一个应用也没有选中！", Snackbar.LENGTH_SHORT).show()
                return@OnClickListener
            }
            DialogAppOptions(context, selectedItems, myHandler).selectSystemAppOptions()
        })

        fab_apps_backuped.setOnClickListener(View.OnClickListener {
            val adapter = (apps_backupedlist.adapter as HeaderViewListAdapter).wrappedAdapter
            val selectedItems = (adapter as AppListAdapter).getSelectedItems()
            if (selectedItems.size == 0) {
                Snackbar.make(view, "一个应用也没有选中！", Snackbar.LENGTH_SHORT).show()
                return@OnClickListener
            }
            DialogAppOptions(context, selectedItems, myHandler).selectBackupOptions()
        })

        appListHelper = AppListHelper2(context)
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

    private class SearchTextWatcher(private var onChange: Runnable) : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onChange.run()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun afterTextChanged(s: Editable?) {
            //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
                val adapter = AppListAdapter(context, dl, apps_search_box.text.toString().toLowerCase())
                lv.adapter = adapter
                lv.onItemClickListener = OnItemClickListener { list, itemView, _, _ ->
                    try {
                        val checkBox = itemView.findViewById(R.id.select_state) as CheckBox
                        checkBox.isChecked = !checkBox.isChecked
                        //checkBox.isChecked = adapter.getIsAllSelected()
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