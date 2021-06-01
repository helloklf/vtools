package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.CheckBox
import com.omarea.common.shell.KeepShell
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.FileOwner
import com.omarea.library.basic.UninstalledApp
import com.omarea.model.AppInfo
import com.omarea.ui.AppListAdapter
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_hidden_apps.*
import java.lang.ref.WeakReference

class ActivityHiddenApps : ActivityBase() {
    private lateinit var progressBarDialog: ProgressBarDialog
    private var adapter: WeakReference<AppListAdapter>? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var pm: PackageManager
    private val keepShell = KeepShell()

    override fun onDestroy() {
        keepShell.tryExit()
        super.onDestroy()
    }

    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()
    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hidden_apps)
        setBackArrow()

        pm = packageManager
        progressBarDialog = ProgressBarDialog(this)
        hidden_app.addHeaderView(this.layoutInflater.inflate(R.layout.list_header_app, null))
    }

    private fun getAppInfo(it: ApplicationInfo): AppInfo {
        val app = AppInfo()
        app.appName = "" + it.loadLabel(pm)
        app.packageName = it.packageName
        app.enabled = it.enabled
        app.path = it.sourceDir
        return app
    }

    private fun loadData() {
        progressBarDialog.showDialog("正在获取应用状态")

        Thread {
            // 获得已卸载的应用（包括：隐藏的、卸载的）
            val uninstalledApp = UninstalledApp().getUninstalledApp(this)
            val appList = ArrayList<AppInfo>()
            uninstalledApp.forEach {
                // spf.edit().putString(it.packageName, it.loadLabel(pm).toString())
                appList.add(getAppInfo(it))
            }
            handler.post {
                progressBarDialog.hideDialog()
                val adapterObj = AppListAdapter(context, appList)
                hidden_app.adapter = adapterObj
                adapter = WeakReference(adapterObj)
                hidden_app.onItemClickListener = AdapterView.OnItemClickListener { _, itemView, postion, _ ->
                    if (postion == 0) {
                        val checkBox = itemView.findViewById(R.id.select_state_all) as CheckBox
                        checkBox.isChecked = !checkBox.isChecked
                        if (adapter?.get() != null) {
                            adapter?.get()!!.setSelecteStateAll(checkBox.isChecked)
                            adapter?.get()!!.notifyDataSetChanged()
                        }
                    } else {
                        val checkBox = itemView.findViewById(R.id.select_state) as CheckBox
                        checkBox.isChecked = !checkBox.isChecked
                        val all = hidden_app.findViewById<CheckBox>(R.id.select_state_all)
                        if (adapter?.get() != null) {
                            all.isChecked = adapter?.get()!!.getIsAllSelected()
                        }
                    }
                }
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.confirm, menu)
        return true
    }

    //右上角菜单
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_confirm -> {
                // 获取选中项
                val items = adapter?.get()!!.getSelectedItems()
                if (items.size > 0) {
                    val cmds = StringBuilder()
                    for (app in items) {
                        cmds.append("pm unhide ${app.packageName}\n")
                        cmds.append("pm enable ${app.packageName}\n")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            cmds.append("pm unsuspend ${app.packageName}\n")
                        }
                    }

                    progressBarDialog.showDialog(getString(R.string.please_wait))
                    Thread {
                        keepShell.doCmdSync(cmds.toString())
                        reInstallAppShell(items)

                        val uninstalledApp = UninstalledApp().getUninstalledApp(this)
                        val fail: ArrayList<AppInfo> = ArrayList()
                        for (app in uninstalledApp) {
                            val result = items.filter { it.packageName == app.packageName }
                            if (result.isNotEmpty()) {
                                fail.add(getAppInfo(app))
                            }
                        }

                        handler.post {
                            progressBarDialog.hideDialog()
                            if (fail.size > 0) {
                                val msg = StringBuilder()
                                for (app in fail) {
                                    msg.append(app.appName)
                                    msg.append("\n")
                                }

                                DialogHelper.helpInfo(this, "以下应用未能恢复", msg.toString() + "\n\n可尝试在Recovery(TWRP)模式备份并删除 /data/system/users/0/package-restrictions.xml")

                                if (uninstalledApp.size != items.size) {
                                    loadData()
                                }
                            } else {
                                loadData()
                            }
                        }
                    }.start()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // 如果恢复不了，也可修改 /data/system/users/$uid/package-restrictions.xml
    private fun reInstallAppShell(apps: ArrayList<AppInfo>) {
        val uid = FileOwner(this).userId
        for (app in apps) {
            val cmd = "pm install-existing --user $uid ${app.packageName}"
            Log.d("Scene", cmd)
            keepShell.doCmdSync(cmd)
        }
    }
}
