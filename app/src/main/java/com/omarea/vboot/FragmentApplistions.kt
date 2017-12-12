package com.omarea.vboot

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
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TabHost
import com.omarea.shared.Consts
import com.omarea.shared.cmd_shellTools
import com.omarea.ui.list_adapter2
import com.omarea.units.AppListHelper
import com.omarea.vboot.dialogs.DialogAppOptions
import kotlinx.android.synthetic.main.layout_applictions.*
import java.util.ArrayList
import java.util.HashMap
import kotlin.Comparator


class FragmentApplistions : Fragment() {
    private var frameView: View? = null
    internal var thisview: ActivityMain? = null
    private lateinit var appListHelper: AppListHelper
    private var installedList: ArrayList<HashMap<String, Any>>? = null
    private var systemList: ArrayList<HashMap<String, Any>>? = null
    private var backupedList: ArrayList<HashMap<String, Any>>? = null
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
        this.frameView = view

        val tabHost = view!!.findViewById(R.id.blacklist_tabhost) as TabHost
        tabHost.setup()
        tabHost.addTab(tabHost.newTabSpec("tab_1").setContent(R.id.tab_apps_user).setIndicator("用户"))
        tabHost.addTab(tabHost.newTabSpec("tab_2").setContent(R.id.tab_apps_system).setIndicator("系统"))
        tabHost.addTab(tabHost.newTabSpec("tab_3").setContent(R.id.tab_apps_backuped).setIndicator("已备份"))
        tabHost.currentTab = 0

        val toggleSelectState = OnItemClickListener { _, itemView, _, _ ->
            val checkBox = itemView.findViewById(R.id.select_state) as CheckBox
            checkBox.isChecked = !checkBox.isChecked
        }
        apps_userlist.onItemClickListener = toggleSelectState
        apps_systemlist.onItemClickListener = toggleSelectState
        apps_backupedlist.onItemClickListener = toggleSelectState

        /*
        apps_userlist.setOnItemLongClickListener({
            parent, view, position, id ->
            val item = parent.adapter.getItem(position)
            val list = ArrayList<HashMap<String,Any>>()
            list.add(item as HashMap<String, Any>)
            dialog_app_options(context, list).selectUserAppOptions(Runnable{
                refreshList()
            })
            false
        })
        */

        fab_apps_user.setOnClickListener(View.OnClickListener {
            val selectedItems = getSelectedItems(apps_userlist.adapter)
            if (selectedItems.size == 0)
                return@OnClickListener

            DialogAppOptions(context, selectedItems, myHandler).selectUserAppOptions()
        })

        fab_apps_system.setOnClickListener(View.OnClickListener {
            val selectedItems = getSelectedItems(apps_systemlist.adapter)
            if (selectedItems.size == 0)
                return@OnClickListener
            DialogAppOptions(context, selectedItems, myHandler).selectSystemAppOptions()
        })

        fab_apps_backuped.setOnClickListener(View.OnClickListener {
            val selectedItems = getSelectedItems(apps_backupedlist.adapter)
            if (selectedItems.size == 0) {
                return@OnClickListener
            }
            DialogAppOptions(context, selectedItems, myHandler).selectBackupOptions()
        })

        appListHelper = AppListHelper(context)
        setList()
        apps_search_box.setOnEditorActionListener({ _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                setList()
            }
            false
        })
    }

    private fun getSelectedItems(adapter: ListAdapter): ArrayList<HashMap<String, Any>> {
        val listadapter = adapter as list_adapter2? ?: return ArrayList()
        val states = listadapter.states
        val selectedItems = states.keys
                .filter { states[it] == true }
                .mapTo(ArrayList<HashMap<String, Any>>()) { listadapter.getItem(it) }

        if (selectedItems.size == 0) {
            Snackbar.make(view!!, "一个应用也没有选中！", Snackbar.LENGTH_SHORT).show()
            return ArrayList()
        }
        return selectedItems
    }

    private fun filterAppList(appList: ArrayList<HashMap<String, Any>>): ArrayList<HashMap<String, Any>> {
        val text = apps_search_box.text.toString().toLowerCase()
        if (text.isEmpty())
            return appList
        return java.util.ArrayList(appList.filter { item ->
            item["packageName"].toString().toLowerCase().contains(text) || item["name"].toString().toLowerCase().contains(text)
        })
    }

    private fun sortAppList(list: ArrayList<HashMap<String, Any>>): ArrayList<HashMap<String, Any>> {
        list.sortWith(Comparator { l, r ->
            val wl = l["wran_state"].toString()
            val wr = r["wran_state"].toString()
            when {
                wl.isNotEmpty() && wr.isEmpty() -> 1
                wr.isNotEmpty() && wl.isEmpty() -> -1
                else -> {
                    val les = l["enabled_state"].toString()
                    val res = r["enabled_state"].toString()
                    when {
                        les < res -> -1
                        les > res -> 1
                        else -> {
                            val lp = l["packageName"].toString()
                            val rp = r["packageName"].toString()
                            when {
                                lp < rp -> -1
                                lp > rp -> 1
                                else -> 0
                            }
                        }
                    }
                }
            }
        })
        return list
    }

    private fun setList() {
        thisview!!.progressBar.visibility = View.VISIBLE
        Thread(Runnable {
            systemList = filterAppList(appListHelper.getSystemAppList())
            installedList = filterAppList(appListHelper.getUserAppList())
            backupedList = filterAppList(appListHelper.getApkFilesInfoList(Consts.BackUpDir))

            setListData(installedList, apps_userlist)
            setListData(systemList, apps_systemlist)
            setListData(backupedList, apps_backupedlist)
        }).start()
    }

    private fun setListData(dl: ArrayList<HashMap<String, Any>>?, lv: ListView) {
        sortAppList(dl!!)
        myHandler.post {
            try {
                thisview!!.progressBar.visibility = View.GONE
                lv.adapter = list_adapter2(context, dl)
            } catch (ex: Exception) {
            }
        }
    }

    companion object {
        fun createPage(thisView: ActivityMain): Fragment {
            val fragment = FragmentApplistions()
            fragment.thisview = thisView
            return fragment
        }
    }
}