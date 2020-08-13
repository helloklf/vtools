package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.CheckBox
import com.omarea.common.shell.KeepShell
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.FileOwner
import com.omarea.model.Appinfo
import com.omarea.ui.AppListAdapter
import com.omarea.utils.AppListHelper2
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        /*
        val dir = filesDir
        val uid = dir.parentFile.parentFile.name
        val configPath = "/data/system/users/$uid/package-restrictions.xml"


        DialogHelper.animDialog(AlertDialog.Builder(this).setTitle("实验性功能 风险警告")
                .setMessage("还原卸载的(不包括隐藏的)应用时，会修改\n$configPath\n在不兼容的设备上可能导致应用和数据丢失。\n\n请不要轻易在你工作的手机上尝试！！！")
                .setPositiveButton(R.string.btn_confirm) { _, _ ->
                }
                .setCancelable(false)
        )
        */
    }

    private fun getAppInfo(it: ApplicationInfo): Appinfo {
        val app = Appinfo()
        app.appName = it.loadLabel(pm)
        app.packageName = it.packageName
        app.enabled = it.enabled
        app.path = it.sourceDir
        return app
    }

    private fun loadData() {
        progressBarDialog.showDialog("正在获取应用状态")

        Thread {
            // 获得已卸载的应用（包括：隐藏的、卸载的）
            val uninstalledApp = AppListHelper2().getUninstalledApp(this)
            val appList = ArrayList<Appinfo>()
            uninstalledApp.forEach {
                // spf.edit().putString(it.packageName, it.loadLabel(pm).toString())
                appList.add(getAppInfo(it))
            }
            handler.post {
                progressBarDialog.hideDialog()
                val adapterObj = AppListAdapter(appList)
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
                        val hasConfigChange = reInstallAppShell(items)

                        val uninstalledApp = AppListHelper2().getUninstalledApp(this)
                        val fail: ArrayList<Appinfo> = ArrayList()
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

                                if (hasConfigChange) {
                                    DialogHelper.animDialog(AlertDialog.Builder(this)
                                            .setTitle("需要重启手机来恢复以下应用")
                                            .setMessage(msg.toString())
                                            .setPositiveButton(R.string.btn_reboot) { _, _ ->
                                                keepShell.doCmdSync("sync\nsleep 2\nreboot")
                                            }
                                            .setNeutralButton(R.string.btn_not_now) { _, _ ->
                                            }
                                            .setCancelable(false))
                                } else {
                                    DialogHelper.helpInfo(this, "以下应用未能恢复", msg.toString())
                                }

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

    private fun reInstallAppShell(apps: ArrayList<Appinfo>): Boolean {
        val uid = FileOwner(this).userId
        for (app in apps) {
            keepShell.doCmdSync("pm install-existing --user $uid ${app.packageName}")
        }
        return true
    }

    /*
    private fun reInstallAppShellOld(apps: ArrayList<Appinfo>): Boolean {
        val dir = filesDir
        val uid = dir.parentFile.parentFile.name
        val configPath = "/data/system/users/$uid/package-restrictions.xml"

        val copyPath = FileWrite.getPrivateFilePath(this, "t-package-restrictions.xml")
        var hasChange = false

        if (keepShell.doCmdSync("cp -f $configPath $copyPath\nchmod 777 $copyPath") != "error") {
            val file = File(copyPath)
            if (file.exists()) {
                try {
                    val inputStream = file.inputStream()
                    val factory = DocumentBuilderFactory.newInstance()
                    val builder = factory.newDocumentBuilder()
                    val dom = builder.parse(inputStream)

                    val rootNode = dom.documentElement
                    val pkgs = rootNode.getElementsByTagName("pkg")
                    pkgs?.run {
                        for (pkgIndex in 0 until pkgs.length) {
                            val pkg = pkgs.item(pkgIndex) as Element
                            if (pkg.hasAttribute("name") && pkg.hasAttribute("inst") && pkg.getAttribute("inst") == "false") {
                                val packageName = pkg.getAttribute("name")
                                val result = apps.filter { it.packageName == packageName }
                                if (result.isNotEmpty()) {
                                    pkg.setAttribute("inst", "true")
                                    hasChange = true
                                }
                            }
                        }
                    }
                    inputStream.close()
                    if (hasChange) {
                        val os = file.outputStream()
                        val str = documentToString(dom)
                        os.write(str.toByteArray(Charset.defaultCharset()))
                        // (dom as XmlDocument).write(os)
                        os.close()
                        keepShell.doCmdSync("if [[ ! -f $configPath.bak ]]\nthen\ncp $configPath $configPath.bak\nfi\ncp $copyPath $configPath\nchown system:system $configPath\nchmod 664 $configPath")
                    }
                } catch (ex: Exception) {
                }
            }
        }
        keepShell.doCmdSync("rm -f $copyPath")
        return hasChange
    }


    private fun documentToString(newDoc: Document): String {
        val domSource = DOMSource(newDoc)
        val transformer = TransformerFactory.newInstance().newTransformer()
        val sw = StringWriter()
        val sr = StreamResult(sw)
        transformer.transform(domSource, sr)
        // Log.d("xmlStr", sw.toString())

        return sw.toString()
    }
   */
}
