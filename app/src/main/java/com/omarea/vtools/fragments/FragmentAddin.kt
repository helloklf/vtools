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
import android.util.Log
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
import com.omarea.krscript.config.PageConfigReader
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.ConfigItemBase
import com.omarea.krscript.model.KrScriptActionHandler
import com.omarea.krscript.model.PageInfo
import com.omarea.krscript.ui.ActionListFragment
import com.omarea.krscript.ui.FileChooserRender
import com.omarea.shell_utils.PlatformUtils
import com.omarea.shell_utils.SysUtils
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
import kotlinx.android.synthetic.main.fragment_addin.*
import java.util.*


class FragmentAddin : Fragment() {
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
            add(createItem(getString(R.string.addin_deviceinfo), getString(R.string.addin_deviceinfo_desc), Runnable { DialogAddinModifydevice(context!!).modifyDeviceInfo() }, false))
            add(createItem(getString(R.string.addin_mac), getString(R.string.addin_mac_desc), Runnable { DialogCustomMAC(context!!).modifyMAC() }, false))
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                add(createItem(getString(R.string.addin_force_dex_compile), getString(R.string.addin_force_dex_compile_desc), Runnable { DexCompileAddin(context!!).run() }, false))
            }
            add(createItem(getString(R.string.addin_pm_dexopt), getString(R.string.addin_pm_dexopt_desc), Runnable { DexCompileAddin(context!!).modifyConfig() }, false))
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

    private fun loadSwitchsConfig() {
        if (page2ConfigLoaded)
            return
        val progressBarDialog = ProgressBarDialog(context!!)
        progressBarDialog.showDialog("读取配置，稍等...")
        Thread(Runnable {
            ScriptEnvironmen.init(this.context!!, "custom/executor.sh", "toolkit")
            val config = "custom/pages/page_list.xml"
            val items = PageConfigReader(this.activity!!).readConfigXml(config)
            myHandler.post {
                page2ConfigLoaded = true

                val favoritesFragment = ActionListFragment.create(items, getKrScriptActionHandler(config), null, themeMode)
                activity!!.supportFragmentManager.beginTransaction().add(R.id.list_page2, favoritesFragment).commit()
                progressBarDialog.hideDialog()
            }
        }).start()
    }


    private fun getKrScriptActionHandler(pageConfig: String): KrScriptActionHandler {
        return object : KrScriptActionHandler {
            override fun addToFavorites(configItemBase: ConfigItemBase, addToFavoritesHandler: KrScriptActionHandler.AddToFavoritesHandler) {
                val intent = Intent()

                intent.component = ComponentName(activity!!.applicationContext, ActionPage::class.java)
                intent.putExtra("config", pageConfig)
                intent.putExtra("title", "" + activity!!.title)
                intent.putExtra("autoRunItemId", configItemBase.key)

                addToFavoritesHandler.onAddToFavorites(configItemBase, intent)
            }

            override fun onSubPageClick(pageInfo: PageInfo) {
                _openPage(pageInfo)
            }

            override fun openFileChooser(fileSelectedInterface: FileChooserRender.FileSelectedInterface) : Boolean {
                return chooseFilePath(fileSelectedInterface)
            }
        }
    }

    fun _openPage(pageInfo: PageInfo) {
        try {
            val intent = Intent(activity, ActionPage::class.java)
            intent.putExtra("title", pageInfo.title)
            intent.putExtra("config", pageInfo.pageConfigPath)
            startActivity(intent)
        } catch (ex: java.lang.Exception) {
            Log.e("_openPage", "" + "" + ex.message)
        }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tabHost = view.findViewById(R.id.addin_tabhost) as TabHost
        tabHost.setup()

        val tabIconHelper = TabIconHelper(tabHost, this.activity!!)
        tabIconHelper.newTabSpec("收藏", context!!.getDrawable(R.drawable.addin_favorites)!!, R.id.tab0)
        val configTab = tabIconHelper.newTabSpec("全部", context!!.getDrawable(R.drawable.addin_pages)!!, R.id.tab2)

        tabHost.setOnTabChangedListener { tabId ->
            if (tabId == configTab) {
                loadSwitchsConfig()
            }
            tabIconHelper.updateHighlight()
        }

        tabHost.currentTab = 0
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
