package com.omarea.vboot

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
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
import kotlinx.android.synthetic.main.layout_applictions.*
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import kotlin.Any
import kotlin.Boolean
import kotlin.Comparator
import kotlin.Exception
import kotlin.String
import kotlin.arrayOf
import kotlin.toString


class fragment_applistions : Fragment() {
    internal var frameView: View? = null

    internal var cmdshellTools: cmd_shellTools? = null
    internal var thisview: main? = null

    lateinit internal var apps_userlist: ListView
    lateinit internal var apps_systemlist: ListView
    lateinit internal var apps_backupedlist: ListView
    lateinit internal var appListHelper: AppListHelper

    internal var installedList: ArrayList<HashMap<String, Any>>? = null
    internal var systemList: ArrayList<HashMap<String, Any>>? = null
    internal var backupedList: ArrayList<HashMap<String, Any>>? = null

    internal val myHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.layout_applictions, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        this.frameView = view

        val tabHost = view!!.findViewById(R.id.blacklist_tabhost) as TabHost
        tabHost.setup()
        tabHost.addTab(tabHost.newTabSpec("tab_1")
                .setContent(R.id.tab_apps_user).setIndicator("用户程序"))
        tabHost.addTab(tabHost.newTabSpec("tab_2")
                .setContent(R.id.tab_apps_system).setIndicator("系统自带"))
        tabHost.addTab(tabHost.newTabSpec("tab_3")
                .setContent(R.id.tab_apps_backuped).setIndicator("已备份"))
        tabHost.currentTab = 0

        apps_userlist = view.findViewById(R.id.apps_userlist) as ListView
        apps_systemlist = view.findViewById(R.id.apps_systemlist) as ListView
        apps_backupedlist = view.findViewById(R.id.apps_backupedlist) as ListView

        val toggleSelectState = OnItemClickListener { parent, view, position, id ->
            val checkBox = view.findViewById(R.id.select_state) as CheckBox
            checkBox.isChecked = !checkBox.isChecked
        }
        apps_userlist.onItemClickListener = toggleSelectState
        apps_systemlist.onItemClickListener = toggleSelectState
        apps_backupedlist.onItemClickListener = toggleSelectState

        view.findViewById(R.id.fab_apps_user).setOnClickListener(View.OnClickListener {
            val selectedItems = getSelectedItems(apps_userlist.adapter)
            if (selectedItems.size == 0)
                return@OnClickListener

            val builder = AlertDialog.Builder(context)
            builder.setTitle("请选择操作")
            builder.setItems(arrayOf("冻结", "解冻", "完全卸载", "卸载-保留数据", "备份应用")) { dialog, which ->
                when (which) {
                    0 -> disabledApp(selectedItems, false)
                    1 -> enableApp(selectedItems, false)
                    2 -> uninstallApp(selectedItems, false)
                    3 -> uninstallApp(selectedItems, true)
                    4 -> backupApp(selectedItems)
                }
            }
            builder.show()
        })


        view.findViewById(R.id.fab_apps_system).setOnClickListener(View.OnClickListener {
            val selectedItems = getSelectedItems(apps_systemlist.adapter)
            if (selectedItems.size == 0)
                return@OnClickListener

            val builder = AlertDialog.Builder(context)
            builder.setTitle("系统应用，请谨慎操作！！！")
            builder.setItems(arrayOf("冻结", "解冻", "删除-需要重启")) { dialog, which ->
                when (which) {
                    0 -> disabledApp(selectedItems, true)
                    1 -> enableApp(selectedItems, true)
                    2 -> deleteApp(selectedItems)
                }
            }
            builder.show()
        })

        view.findViewById(R.id.fab_apps_backuped).setOnClickListener(View.OnClickListener {
            val selectedItems = getSelectedItems(apps_backupedlist.adapter)
            if (selectedItems.size == 0)
                return@OnClickListener

            val builder = AlertDialog.Builder(context)
            builder.setTitle("系统应用，请谨慎操作！！！")
            builder.setItems(arrayOf("安装（ROOT模式）", "删除备份")) { dialog, which ->
                when (which) {
                    0 -> installApp(selectedItems)
                    1 -> deleteBackup(selectedItems)
                }
            }
            builder.show()
        })

        appListHelper = AppListHelper(context)
        setList()
        apps_search_box.setOnEditorActionListener({ tv, actionId, key ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                filterList(tv.text.toString())
                setList()
            }
            false
        })
    }

    private fun getSelectedItems(adapter: ListAdapter): ArrayList<HashMap<String, Any>> {
        val listadapter = adapter as list_adapter2? ?: return ArrayList()
        val states = listadapter.states
        val selectedItems = ArrayList<HashMap<String, Any>>()
        for (position in states.keys) {
            if (states[position] == true)
                selectedItems.add(listadapter.getItem(position))
        }

        if (selectedItems.size == 0) {
            Snackbar.make(view!!, "一个应用也没有选中！", Snackbar.LENGTH_SHORT).show()
            return ArrayList()
        }
        return selectedItems
    }

    private fun installApp(apps: ArrayList<HashMap<String, Any>>?) {
        if (apps == null || apps.size == 0)
            return
        val builder = AlertDialog.Builder(context)
        builder.setTitle("安装提示")
        builder.setMessage("\n这需要好些时间，而且期间你看不到安装进度，只能通过桌面程序观察应用是否已经安装！\n\n")
        builder.setPositiveButton("确定") { dialog, which ->
            try {
                val stringBuffer = StringBuffer()
                for (i in apps.indices) {
                    val path = apps[i]["path"].toString()
                    val file = File(path)
                    if (file.exists()) {
                        stringBuffer.append("pm install -r \"")
                        stringBuffer.append(path)
                        stringBuffer.append("\";\n")
                    }
                }
                thisview!!.progressBar.visibility = View.VISIBLE
                Thread(Runnable {
                    cmdshellTools!!.DoCmdSync(stringBuffer.toString())
                    backupedList = null
                    installedList = null
                    systemList = null
                    setList()
                }).start()
            } catch (ex: Exception) {

            }
        }
        builder.setNegativeButton("取消") { dialog, which ->
            for (i in apps.indices) {
                val file = File(apps[i]["path"].toString())
                if (file.exists() && file.canWrite())
                    file.delete()
            }
        }
        builder.show()
    }

    private fun deleteBackup(apps: ArrayList<HashMap<String, Any>>?) {
        if (apps == null || apps.size == 0)
            return
        val builder = AlertDialog.Builder(context)
        builder.setTitle("删除提示")
        builder.setMessage("\n确定删除？\n\n")
        builder.setPositiveButton("确定") { dialog, which ->
            for (i in apps.indices) {
                val file = File(apps[i]["path"].toString())
                if (file.exists() && file.canWrite())
                    file.delete()
            }
            backupedList = null
            setList()
        }
        builder.setNegativeButton("取消") { dialog, which -> }
        builder.show()
    }

    private fun backupApp(apps: ArrayList<HashMap<String, Any>>?) {
        if (apps == null || apps.size == 0)
            return

        val builder = AlertDialog.Builder(context)
        builder.setTitle("备份提示")
        builder.setMessage("\n我只能帮你提取apk文件，暂时不支持备份应用数据。\n备份完后在 /sdcard/Android/apps 下！\n\n")
        builder.setPositiveButton("确定备份") { dialog, which ->
            val stringBuffer = StringBuffer()
            stringBuffer.append("mkdir /sdcard/Android/apps;\n")
            for (i in apps.indices) {
                stringBuffer.append("cp -f ")
                stringBuffer.append(apps[i]["path"])
                stringBuffer.append(" /sdcard/Android/apps/")
                stringBuffer.append(apps[i]["packageName"])
                stringBuffer.append(".apk;\n")
            }
            thisview!!.progressBar.visibility = View.VISIBLE

            Thread(Runnable {
                cmdshellTools!!.DoCmdSync(stringBuffer.toString())
                backupedList = null
                setList()
            }).start()
        }
        builder.setNegativeButton("取消") { dialog, which -> }
        builder.show()
    }

    private fun uninstallApp(apps: ArrayList<HashMap<String, Any>>?, keepData: Boolean) {
        if (apps == null || apps.size == 0)
            return

        val builder = AlertDialog.Builder(context)
        builder.setTitle("确定要卸载 " + apps.size + " 个应用吗？")
        builder.setMessage("\n最好不要一口气吃成大胖子，卸载错了我可不管！\n\n")
        builder.setPositiveButton("确定卸载") { dialog, which ->
            val stringBuffer = StringBuffer()
            for (i in apps.indices) {
                stringBuffer.append(if (keepData) "pm uninstall -k " else "pm uninstall ")
                stringBuffer.append(apps[i]["packageName"])
                stringBuffer.append(";\n")
            }
            thisview!!.progressBar.visibility = View.VISIBLE

            Thread(Runnable {
                cmdshellTools!!.DoCmdSync(stringBuffer.toString())
                installedList = null
                setList()
            }).start()
        }
        builder.setNegativeButton("取消") { dialog, which -> }
        builder.show()
    }

    private fun enableApp(apps: ArrayList<HashMap<String, Any>>?, isSystem: Boolean) {
        if (apps == null || apps.size == 0)
            return

        val stringBuffer = StringBuffer()
        for (i in apps.indices) {
            stringBuffer.append("pm enable ")
            stringBuffer.append(apps[i]["packageName"])
            stringBuffer.append(";\n")
        }
        thisview!!.progressBar.visibility = View.VISIBLE

        Thread(Runnable {
            cmdshellTools!!.DoCmdSync(stringBuffer.toString())
            if (isSystem)
                systemList = null
            else
                installedList = null
            setList()
        }).start()
    }

    private fun disabledApp(apps: ArrayList<HashMap<String, Any>>?, isSystem: Boolean) {
        if (apps == null || apps.size == 0)
            return

        val builder = AlertDialog.Builder(context)
        builder.setTitle("确定要冻结 " + apps.size + " 个应用吗？")
        builder.setMessage(if (isSystem) "\n如果你不知道这些应用是干嘛的，千万别乱冻结，随时会挂掉的！！！\n\n" else "\n一口气干掉太多容易闪到腰哦，搞错了我可不管！！！\n\n")
        builder.setPositiveButton("确定冻结") { dialog, which ->
            val stringBuffer = StringBuffer()
            for (i in apps.indices) {
                stringBuffer.append("pm disable ")
                stringBuffer.append(apps[i]["packageName"])
                stringBuffer.append(";\n")
            }
            thisview!!.progressBar.visibility = View.VISIBLE

            Thread(Runnable {
                cmdshellTools!!.DoCmdSync(stringBuffer.toString())
                if (isSystem)
                    systemList = null
                else
                    installedList = null
                setList()
            }).start()
        }
        builder.setNegativeButton("取消") { dialog, which -> }
        builder.show()
    }

    private fun deleteApp(apps: ArrayList<HashMap<String, Any>>?) {
        if (apps == null || apps.size == 0)
            return

        val builder = AlertDialog.Builder(context)
        builder.setTitle("删除提示")
        builder.setMessage("\n这是个非常危险的操作，如果你删错了重要的应用，手机可能会没法开机。\n\n你最好有个可用的救机方式！或者有十足的把握，确定勾选的都是无用的应用。\n\n")
        builder.setPositiveButton("确定删除") { dialog, which ->
            val stringBuffer = StringBuffer()
            stringBuffer.append(Consts.MountSystemRW)
            for (i in apps.indices) {
                stringBuffer.append("rm -rf ")
                stringBuffer.append(apps[i]["dir"])
                stringBuffer.append("\n")
            }
            thisview!!.progressBar.visibility = View.VISIBLE

            Thread(Runnable {
                cmdshellTools!!.DoCmdSync(stringBuffer.toString())
                systemList = null
                setList()
            }).start()
        }
        builder.setNegativeButton("取消（推荐）") { dialog, which -> }
        builder.show()
    }

    private fun filterList(text: String) {
        systemList = java.util.ArrayList<HashMap<String, Any>>(appListHelper.getSystemAppList().filter { item ->
            if (item.get("packageName").toString().contains(text) || item.get("name").toString().contains(text)) true else false
        })
        installedList = java.util.ArrayList<HashMap<String, Any>>(appListHelper.getUserAppList().filter { item ->
            if (item.get("packageName").toString().contains(text) || item.get("name").toString().contains(text)) true else false
        })
        backupedList = java.util.ArrayList<HashMap<String, Any>>(appListHelper.getApkFilesInfoList("/sdcard/Android/apps").filter { item ->
            if (item.get("packageName").toString().contains(text) || item.get("name").toString().contains(text)) true else false
        })
    }

    private fun sortAppList(list: ArrayList<HashMap<String, Any>>): ArrayList<HashMap<String, Any>> {
        list.sortWith(Comparator { l, r ->
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
        })
        return list
    }

    private fun setList() {
        thisview!!.progressBar.visibility = View.VISIBLE

        Thread(Runnable {
            if (apps_search_box.text.length > 0) {
                filterList(apps_search_box.text.toString())
            } else {
                if (installedList == null)
                    installedList = appListHelper.getUserAppList()
                if (systemList == null)
                    systemList = appListHelper.getSystemAppList()
                if (backupedList == null)
                    backupedList = appListHelper.getApkFilesInfoList("/sdcard/Android/apps")
            }

            setListData(installedList, apps_userlist)
            setListData(systemList, apps_systemlist)
            setListData(backupedList, apps_backupedlist)
        }).start()
    }

    internal fun setListData(dl: ArrayList<HashMap<String, Any>>?, lv: ListView) {
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

        fun Create(thisView: main, cmdshellTools: cmd_shellTools): Fragment {
            val fragment = fragment_applistions()
            fragment.cmdshellTools = cmdshellTools
            fragment.thisview = thisView
            return fragment
        }
    }
}