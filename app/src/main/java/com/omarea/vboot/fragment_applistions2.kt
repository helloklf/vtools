package com.omarea.vboot

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView.OnItemClickListener
import android.widget.CheckBox
import android.widget.ListView
import android.widget.TabHost
import com.omarea.shared.Consts
import com.omarea.shared.cmd_shellTools
import com.omarea.ui.list_adapter2
import com.omarea.units.AppListHelper
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
import kotlinx.android.synthetic.main.layout_applictions.*


class fragment_applistions2 : Fragment() {

    internal var frameView: View? = null

    internal var cmdshellTools: cmd_shellTools? = null
    internal var thisview: main? = null
    lateinit var applistHelper: AppListHelper

    internal val myHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }
    internal var installedList: ArrayList<HashMap<String, Any>>? = null
    internal var systemList: ArrayList<HashMap<String, Any>>? = null
    internal var backupedList: ArrayList<HashMap<String, Any>>? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.layout_applictions, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        this.frameView = view
        applistHelper = AppListHelper(context)

        val tabHost = view!!.findViewById(R.id.blacklist_tabhost) as TabHost
        tabHost.setup()

        tabHost.addTab(tabHost.newTabSpec("tab_1").setContent(R.id.apps_userlist).setIndicator("用户程序"))
        tabHost.addTab(tabHost.newTabSpec("tab_2").setContent(R.id.apps_systemlist).setIndicator("系统自带"))
        tabHost.addTab(tabHost.newTabSpec("tab_3").setContent(R.id.apps_backupedlist).setIndicator("已备份"))
        tabHost.currentTab = 0

        val config_powersavelistClick = OnItemClickListener { parent, view, position, id ->
            val checkBox = view.findViewById(R.id.select_state) as CheckBox
            checkBox.isChecked = !checkBox.isChecked
        }
        apps_userlist.onItemClickListener = config_powersavelistClick
        apps_systemlist.onItemClickListener = config_powersavelistClick
        apps_backupedlist.onItemClickListener = config_powersavelistClick

        fab_apps_user.setOnClickListener({
            val listadapter = apps_userlist.adapter as list_adapter2
            val states = listadapter.states
            val builder = AlertDialog.Builder(thisview)
            builder.setTitle("请选择操作")
            builder.setItems(
                    arrayOf("冻结", "解冻", "卸载-删除数据", "卸载-保留数据", "备份应用"),
                    { dialog, which ->
                        val selectedItems = ArrayList<HashMap<String, Any>>()
                        for (position in states.keys) {
                            if (states[position] == true)
                                selectedItems.add(installedList!![position])
                        }
                        when (which) {
                            0 -> disabledApp(selectedItems, false)
                            1 -> enableApp(selectedItems, false)
                            2 -> uninstallApp(selectedItems, false)
                            3 -> uninstallApp(selectedItems, true)
                            4 -> backupApp(selectedItems)
                        }
                    }
            )
            builder.show()
        })

        fab_apps_system.setOnClickListener({
            val listadapter = apps_systemlist.adapter as list_adapter2
            val states = listadapter.states
            val builder = AlertDialog.Builder(thisview)
            builder.setTitle("系统应用，请谨慎操作！！！")
            builder.setItems(
                    arrayOf("冻结", "解冻", "删除-需要重启"),
                    { dialog, which ->
                        val selectedItems = ArrayList<HashMap<String, Any>>()
                        for (position in states.keys) {
                            if (states[position] == true)
                                selectedItems.add(systemList!![position])
                        }

                        when (which) {
                            0 -> disabledApp(selectedItems, true)
                            1 -> enableApp(selectedItems, true)
                            2 -> deleteApp(selectedItems)
                        }
                    }
            )
            builder.show()
        })

        fab_apps_backuped.setOnClickListener({
            val listadapter = apps_backupedlist.adapter as list_adapter2
            val states = listadapter.states
            val builder = AlertDialog.Builder(thisview)
            builder.setTitle("请选择操作")
            builder.setItems(
                    arrayOf("安装（ROOT模式）", "删除备份"),
                    { dialog, which ->
                        val selectedItems = ArrayList<HashMap<String, Any>>()
                        for (position in states.keys) {
                            if (states[position] == true)
                                selectedItems.add(backupedList!![position])
                        }

                        when (which) {
                            0 -> installApp(selectedItems)
                            1 -> deleteBackup(selectedItems)
                        }
                    }
            )
            builder.show()
        })
        setList()
        apps_search_box.setOnEditorActionListener({ tv, actionId, key ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                filterList(tv.text.toString())
                setList()
            }
            false
        })
    }

    private fun installApp(apps: ArrayList<HashMap<String, Any>>?) {
        if (apps == null || apps.size == 0)
            return
        val builder = AlertDialog.Builder(thisview)
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
        val builder = AlertDialog.Builder(thisview)
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

        val builder = AlertDialog.Builder(thisview)
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
            //Toast.makeText(getContext(),stringBuffer.toString(),Toast.LENGTH_LONG).show();
        }
        builder.setNegativeButton("取消") { dialog, which -> }
        builder.show()
    }

    private fun uninstallApp(apps: ArrayList<HashMap<String, Any>>?, keepData: Boolean) {
        if (apps == null || apps.size == 0)
            return

        val builder = AlertDialog.Builder(thisview)
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
            //Toast.makeText(getContext(),stringBuffer.toString(),Toast.LENGTH_LONG).show();
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

        val builder = AlertDialog.Builder(thisview)
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
            //Toast.makeText(getContext(),stringBuffer.toString(),Toast.LENGTH_LONG).show();
        }
        builder.setNegativeButton("取消") { dialog, which -> }
        builder.show()
    }

    private fun deleteApp(apps: ArrayList<HashMap<String, Any>>?) {
        if (apps == null || apps.size == 0)
            return

        val builder = AlertDialog.Builder(thisview)
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

            //Toast.makeText(getContext(),stringBuffer.toString(),Toast.LENGTH_LONG).show();
            Thread(Runnable {
                cmdshellTools!!.DoCmdSync(stringBuffer.toString())
                systemList = null
                setList()
            }).start()
        }
        builder.setNegativeButton("取消（推荐）") { dialog, which -> }
        builder.show()
    }

    private fun setList() {
        thisview!!.progressBar.visibility = View.VISIBLE

        Thread(Runnable {
            if (apps_search_box.text.length > 0) {
                filterList(apps_search_box.text.toString())
            } else {
                if (installedList == null)
                    installedList = applistHelper.getUserAppList()
                if (systemList == null)
                    systemList = applistHelper.getSystemAppList()
                if (backupedList == null)
                    backupedList = applistHelper.getApkFilesInfoList("/sdcard/Android/apps")
            }

            setListData(installedList, apps_userlist)
            setListData(systemList, apps_systemlist)
            setListData(backupedList, apps_backupedlist)
        }).start()
    }

    private fun filterList(text: String) {
        systemList = java.util.ArrayList<HashMap<String,Any>>(applistHelper.getSystemAppList().filter {
            item ->
            if (item.get("packageName").toString().contains(text) || item.get("name").toString().contains(text)) true else false
        })
        installedList = java.util.ArrayList<HashMap<String,Any>>(applistHelper.getUserAppList().filter {
            item ->
            if (item.get("packageName").toString().contains(text) || item.get("name").toString().contains(text)) true else false
        })
        backupedList = java.util.ArrayList<HashMap<String,Any>>(applistHelper.getApkFilesInfoList("/sdcard/Android/apps").filter {
            item ->
            if (item.get("packageName").toString().contains(text) || item.get("name").toString().contains(text)) true else false
        })
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

    companion object {
        fun Create(thisView: main, cmdshellTools: cmd_shellTools): Fragment {
            val fragment = fragment_applistions2()
            fragment.cmdshellTools = cmdshellTools
            fragment.thisview = thisView
            return fragment
        }
    }
}
