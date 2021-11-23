package com.omarea.vtools.fragments

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.CheckBox
import android.widget.HeaderViewListAdapter
import android.widget.Toast
import com.omarea.Scene
import com.omarea.common.ui.OverScrollListView
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.model.AppInfo
import com.omarea.ui.AdapterAppList
import com.omarea.utils.AppListHelper
import com.omarea.vtools.R
import com.omarea.vtools.dialogs.DialogAppOptions
import com.omarea.vtools.dialogs.DialogSingleAppOptions
import kotlinx.android.synthetic.main.fragment_app_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class FragmentAppUser(private val myHandler: Handler) : androidx.fragment.app.Fragment() {
    private lateinit var processBarDialog: ProgressBarDialog
    private lateinit var appListHelper: AppListHelper
    private var appList: ArrayList<AppInfo>? = null
    private var keywords = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        processBarDialog = ProgressBarDialog(activity!!, "FragmentAppUser")
        appListHelper = AppListHelper(context!!)

        return inflater.inflate(R.layout.fragment_app_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        app_list.addHeaderView(this.layoutInflater.inflate(R.layout.list_header_app, null))

        val onItemLongClick = AdapterView.OnItemLongClickListener { parent, _, position, id ->
            if (position < 1)
                return@OnItemLongClickListener true
            val adapter = (parent.adapter as HeaderViewListAdapter).wrappedAdapter
            val app = adapter.getItem(position - 1) as AppInfo
            DialogSingleAppOptions(activity!!, app, myHandler!!).showSingleAppOptions()
            true
        }

        app_list.onItemLongClickListener = onItemLongClick
        fab_apps.setOnClickListener {
            getSelectedAppShowOptions(activity!!)
        }

        this.setList()
    }

    private fun getSelectedAppShowOptions(activity: Activity) {
        var adapter = app_list.adapter
        adapter = (adapter as HeaderViewListAdapter).wrappedAdapter
        val selectedItems = (adapter as AdapterAppList).getSelectedItems()
        if (selectedItems.size == 0) {
            Scene.toast(R.string.app_selected_none, Toast.LENGTH_SHORT)
            return
        }

        if (selectedItems.size == 1) {
            DialogSingleAppOptions(activity, selectedItems.first(), myHandler!!).showSingleAppOptions()
        } else {
            DialogAppOptions(activity, selectedItems, myHandler!!).selectUserAppOptions()
        }
    }

    private fun setList() {
        processBarDialog.showDialog()
        GlobalScope.launch(Dispatchers.Main) {
            appList = appListHelper.getUserAppList()
            processBarDialog.hideDialog()
            app_list?.run {
                setListData(appList, this)
            }
        }
    }

    private fun setListData(dl: ArrayList<AppInfo>?, lv: OverScrollListView) {
        if (dl == null)
            return
        myHandler.post {
            try {
                val adapterObj = AdapterAppList(context!!, dl, keywords)
                val adapterAppList: WeakReference<AdapterAppList> = WeakReference(adapterObj)
                lv.adapter = adapterObj
                lv.onItemClickListener = OnItemClickListener { list, itemView, postion, _ ->
                    if (postion == 0) {
                        val checkBox = itemView.findViewById(R.id.select_state_all) as CheckBox
                        checkBox.isChecked = !checkBox.isChecked
                        if (adapterAppList.get() != null) {
                            adapterAppList.get()!!.setSelecteStateAll(checkBox.isChecked)
                            adapterAppList.get()!!.notifyDataSetChanged()
                        }
                    } else {
                        val checkBox = itemView.findViewById(R.id.select_state) as CheckBox
                        checkBox.isChecked = !checkBox.isChecked
                        val all = lv.findViewById<CheckBox>(R.id.select_state_all)
                        if (adapterAppList.get() != null) {
                            all.isChecked = adapterAppList.get()!!.getIsAllSelected()
                        }
                    }
                    fab_apps.visibility = if (adapterAppList.get()?.hasSelected() == true) View.VISIBLE else View.GONE
                }
                val all = lv.findViewById<CheckBox>(R.id.select_state_all)
                all.isChecked = false
                fab_apps.visibility = View.GONE
            } catch (ex: Exception) {
            }
        }
    }

    public var searchText: String
        get () {
            return keywords
        }
        set (value) {
            if (keywords != value) {
                keywords = value
                app_list?.run {
                    setListData(appList, this)
                }
            }
        }

    fun reloadList() {
        setList()
    }
}
