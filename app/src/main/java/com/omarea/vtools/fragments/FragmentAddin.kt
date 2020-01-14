package com.omarea.vtools.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.SimpleAdapter
import android.widget.TabHost
import android.widget.Toast
import com.omarea.common.shared.FilePathResolver
import com.omarea.common.shared.MagiskExtend
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.common.ui.ThemeMode
import com.omarea.kr.KrScriptConfig
import com.omarea.kr.PageConfigSh
import com.omarea.krscript.config.PageConfigReader
import com.omarea.krscript.model.*
import com.omarea.krscript.ui.ActionListFragment
import com.omarea.krscript.ui.FileChooserRender
import com.omarea.shell_utils.PlatformUtils
import com.omarea.shell_utils.SysUtils
import com.omarea.store.SpfConfig
import com.omarea.ui.TabIconHelper
import com.omarea.vtools.R
import com.omarea.vtools.activities.ActionPage
import com.omarea.vtools.addin.DexCompileAddin
import com.omarea.vtools.addin.FullScreenAddin
import com.omarea.vtools.addin.PerfBoostConfigAddin
import com.omarea.vtools.addin.ThermalAddin
import com.omarea.vtools.dialogs.DialogAddinModifyDPI
import com.omarea.vtools.dialogs.DialogAddinModifydevice
import com.omarea.vtools.dialogs.DialogAddinWIFI
import com.omarea.vtools.dialogs.DialogCustomMAC
import com.projectkr.shell.OpenPageHelper
import kotlinx.android.synthetic.main.fragment_addin.*
import java.util.*


class FragmentAddin : Fragment() {
    private lateinit var krScriptConfig: KrScriptConfig

    private var myHandler = Handler()
    private fun createItem(title: String, desc: String, runnable: Runnable?, wran: Boolean = true): HashMap<String, Any> {
        val item = HashMap<String, Any>()
        item.put("Title", title)
        item.put("Desc", desc)
        item.put("Wran", wran)
        if (runnable != null)
            item.put("Action", runnable)
        return item
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_addin, container, false)

    private fun getIP(): String {
        var r = SysUtils.executeCommandWithOutput(false, "ifconfig wlan0 | grep \"inet addr\" | awk '{ print \$2}' | awk -F: '{print \$2}'")
        if (r == null || r == "") {
            r = "IP"
        }
        return r.trim()
    }

    private fun initAddin(view: View) {
        val listItem = ArrayList<HashMap<String, Any>>().apply {
            add(createItem(getString(R.string.addin_fullscreen_on), getString(R.string.addin_fullscreen_on_desc), Runnable { FullScreenAddin(activity!!).fullScreen() }, false))

            val platform = PlatformUtils().getCPUName()

            // if (!(platform == "sdm845" || platform == "msm8998" || platform == "msmnile")) {
            add(createItem(getString(R.string.addin_thermal_remove), getString(R.string.addin_thermal_remove_desc), Runnable { ThermalAddin(context!!).showOption() }, false))
            // }

            if (!(MagiskExtend.moduleInstalled() && (
                            platform == "sdm845" ||
                                    platform == "msm8998" ||
                                    platform == "sdm710" ||
                                    platform == "msmnile"
                            ))) {
                add(createItem(getString(R.string.addin_thermal_remove2), getString(R.string.addin_thermal_remove2_desc), Runnable {
                    ThermalAddin(context!!).miuiSetThermalNo()
                }, false))
            }

            add(createItem(getString(R.string.addin_wifi), getString(R.string.addin_wifi_desc), Runnable { DialogAddinWIFI(context!!).show() }, false))

            add(createItem(getString(R.string.addin_dpi), getString(R.string.addin_dpi_desc), Runnable { DialogAddinModifyDPI(context!!).modifyDPI(activity!!.windowManager.defaultDisplay, context!!) }, false))

            add(createItem(getString(R.string.addin_deviceinfo), getString(
                    R.string.addin_deviceinfo_desc),
                    Runnable {
                        DialogAddinModifydevice(context!!).modifyDeviceInfo()
                    },
                    false))
            add(createItem(getString(R.string.addin_mac),
                    getString(R.string.addin_mac_desc),
                    Runnable {
                        DialogCustomMAC(context!!).modifyMAC(SpfConfig.GLOBAL_SPF_MAC_AUTOCHANGE_MODE_1)
                    },
                    false))
            add(createItem(getString(R.string.addin_mac_2),
                    getString(R.string.addin_mac_desc_2),
                    Runnable {
                        DialogCustomMAC(context!!).modifyMAC(SpfConfig.GLOBAL_SPF_MAC_AUTOCHANGE_MODE_2)
                    },
                    false))

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                add(createItem(getString(R.string.addin_force_dex_compile), getString(R.string.addin_force_dex_compile_desc), Runnable { DexCompileAddin(context!!).run() }, false))
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(createItem(getString(R.string.addin_pm_dexopt), getString(R.string.addin_pm_dexopt_desc), Runnable { DexCompileAddin(context!!).modifyConfig() }, false))
            }
            add(createItem(getString(R.string.addin_bpc), getString(R.string.addin_bpc_desc), Runnable { PerfBoostConfigAddin(context!!).install() }))
        }

        val mSimpleAdapter = SimpleAdapter(
                view.context, listItem,
                R.layout.list_item_action,
                arrayOf("Title", "Desc"),
                intArrayOf(R.id.Title, R.id.Desc)
        )
        addin_system_listview.adapter = mSimpleAdapter
        addin_system_listview.onItemClickListener = onActionClick
    }

    private var onActionClick = AdapterView.OnItemClickListener { parent, _, position, _ ->
        val item = parent.adapter.getItem(position) as HashMap<*, *>
        if (item.get("Wran") == false) {
            (item["Action"] as Runnable).run()
        } else {
            DialogHelper.animDialog(AlertDialog.Builder(context!!)
                    .setTitle(item["Title"].toString())
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.addin_continue) { _, _ ->
                        (item["Action"] as Runnable).run()
                    }
                    .setMessage(item["Desc"].toString()))
        }
    }

    private var page2ConfigLoaded = false

    private fun getItems(pageNode: PageNode): ArrayList<NodeInfoBase>? {
        var items: ArrayList<NodeInfoBase>? = null

        if (pageNode.pageConfigSh.isNotEmpty()) {
            items = PageConfigSh(this.activity!!, pageNode.pageConfigSh).execute()
        }
        if (items == null && pageNode.pageConfigPath.isNotEmpty()) {
            items = PageConfigReader(this.context!!, pageNode.pageConfigPath).readConfigXml()
        }

        return items
    }


    private fun loadPageConfig() {
        if (page2ConfigLoaded)
            return
        val progressBarDialog = ProgressBarDialog(context!!)
        progressBarDialog.showDialog("读取配置，稍等...")
        Thread(Runnable {
            val items = getItems(krScriptConfig.pageListConfig)
            myHandler.post {
                page2ConfigLoaded = true

                val favoritesFragment = ActionListFragment.create(items, getKrScriptActionHandler(), null, themeMode)
                activity!!.supportFragmentManager.beginTransaction().replace(R.id.list_page2, favoritesFragment).commit()
                progressBarDialog.hideDialog()
            }
        }).start()
    }

    private fun getKrScriptActionHandler(): KrScriptActionHandler {
        return object : KrScriptActionHandler {
            override fun onActionCompleted(runnableNode: RunnableNode) {
                if (runnableNode.reloadPage) {
                    loadPageConfig()
                }
            }

            override fun addToFavorites(clickableNode: ClickableNode, addToFavoritesHandler: KrScriptActionHandler.AddToFavoritesHandler) {
                val intent = Intent()

                intent.component = ComponentName(activity!!.applicationContext, ActionPage::class.java)
                val pageNode = krScriptConfig.pageListConfig

                intent.putExtra("title", "" + pageNode.title)
                intent.putExtra("beforeRead", "")
                intent.putExtra("config", pageNode.pageConfigPath)
                intent.putExtra("pageConfigSh", pageNode.pageConfigSh)
                intent.putExtra("afterRead", "")
                intent.putExtra("loadSuccess", "")
                intent.putExtra("loadFail", "")
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

                if (clickableNode is RunnableNode) {
                    intent.putExtra("autoRunItemId", clickableNode.key)
                } else if (clickableNode is PageNode) {
                }

                addToFavoritesHandler.onAddToFavorites(clickableNode, intent)
            }

            override fun onSubPageClick(pageNode: PageNode) {
                _openPage(pageNode)
            }

            override fun openFileChooser(fileSelectedInterface: FileChooserRender.FileSelectedInterface): Boolean {
                return chooseFilePath(fileSelectedInterface)
            }
        }
    }

    fun _openPage(pageInfo: PageNode) {
        OpenPageHelper(this.activity!!).openPage(pageInfo)
    }

    private var fileSelectedInterface: FileChooserRender.FileSelectedInterface? = null
    private val ACTION_FILE_PATH_CHOOSER = 65400
    private fun chooseFilePath(fileSelectedInterface: FileChooserRender.FileSelectedInterface): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity!!.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity!!, getString(R.string.kr_write_external_storage), Toast.LENGTH_LONG).show()
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2);
            return false
        } else {
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*")
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, ACTION_FILE_PATH_CHOOSER);
                this.fileSelectedInterface = fileSelectedInterface
                return true;
            } catch (ex: Exception) {
                return false
            }
        }
    }

    private fun getPath(uri: Uri): String? {
        try {
            return FilePathResolver().getPath(this.context, uri)
        } catch (ex: Exception) {
            return null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTION_FILE_PATH_CHOOSER) {
            val result = if (data == null || resultCode != Activity.RESULT_OK) null else data.data
            if (fileSelectedInterface != null) {
                if (result != null) {
                    val absPath = getPath(result)
                    fileSelectedInterface?.onFileSelected(absPath)
                } else {
                    fileSelectedInterface?.onFileSelected(null)
                }
            }
            this.fileSelectedInterface = null
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()

        activity!!.title = getString(R.string.menu_additional)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tabHost = view.findViewById(R.id.addin_tabhost) as TabHost
        tabHost.setup()

        val tabIconHelper = TabIconHelper(tabHost, this.activity!!)
        tabIconHelper.newTabSpec("收藏", context!!.getDrawable(R.drawable.addin_favorites)!!, R.id.addin_system_listview)
        val configTab = tabIconHelper.newTabSpec("全部", context!!.getDrawable(R.drawable.addin_pages)!!, R.id.list_page2)

        tabHost.setOnTabChangedListener { tabId ->
            if (tabId == configTab) {
                loadPageConfig()
            }
            tabIconHelper.updateHighlight()
        }

        tabHost.currentTab = 0
        krScriptConfig = KrScriptConfig().init(context!!)
        initAddin(view)
    }

    private lateinit var themeMode: ThemeMode

    companion object {
        fun createPage(themeMode: ThemeMode): Fragment {
            val fragment = FragmentAddin()
            fragment.themeMode = themeMode

            return fragment
        }
    }
}
