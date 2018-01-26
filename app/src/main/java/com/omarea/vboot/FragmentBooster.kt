package com.omarea.vboot

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.omarea.shared.ServiceHelper
import com.omarea.shared.SpfConfig
import com.omarea.ui.list_adapter
import kotlinx.android.synthetic.main.layout_booster.*
import java.util.*
import kotlin.collections.ArrayList


class FragmentBooster : Fragment() {

    private lateinit var frameView: View
    private var thisview: ActivityMain? = null
    private lateinit var blacklist: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onResume() {
        super.onResume()

        val serviceState = ServiceHelper.serviceIsRunning(context)
        btn_booster_service_not_active.visibility = if (serviceState) GONE else VISIBLE
        btn_booster_dynamicservice_not_active.visibility = if (serviceState && !context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE).getBoolean(SpfConfig.GLOBAL_SPF_AUTO_BOOSTER, false)) VISIBLE else GONE
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater!!.inflate(R.layout.layout_booster, container, false)


    @SuppressLint("ApplySharedPref")
    private fun bindSPF(checkBox: Switch, spf: SharedPreferences, prop: String, defValue: Boolean = false) {
        checkBox.isChecked = spf.getBoolean(prop, defValue)
        checkBox.setOnCheckedChangeListener{
            _, isChecked ->
            spf.edit().putBoolean(prop, isChecked).commit()
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun bindSPF(checkBox: CheckBox, spf: SharedPreferences, prop: String, defValue: Boolean = false) {
        checkBox.isChecked = spf.getBoolean(prop, defValue)
        checkBox.setOnCheckedChangeListener{
            _, isChecked ->
            spf.edit().putBoolean(prop, isChecked).commit()
        }
    }

    @SuppressLint("ApplySharedPref", "CommitPrefEdits")
    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        this.frameView = view!!
        blacklist = context.getSharedPreferences(SpfConfig.BOOSTER_BLACKLIST_SPF, Context.MODE_PRIVATE)
        editor = blacklist.edit()

        val spfAutoConfig = context.getSharedPreferences(SpfConfig.BOOSTER_SPF_CFG_SPF, Context.MODE_PRIVATE)

        bindSPF(cacheclear, spfAutoConfig, SpfConfig.BOOSTER_SPF_CFG_SPF_CLEAR_CACHE, false)
        bindSPF(dozemod, spfAutoConfig, SpfConfig.BOOSTER_SPF_CFG_SPF_DOZE_MOD, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        bindSPF(auto_clear_tasks, spfAutoConfig, SpfConfig.BOOSTER_SPF_CFG_SPF_CLEAR_TASKS, true)

        bindSPF(auto_switch_network_on_wifi, spfAutoConfig, SpfConfig.WIFI + SpfConfig.ON, false)
        bindSPF(auto_switch_network_on_data, spfAutoConfig, SpfConfig.DATA + SpfConfig.ON, false)
        bindSPF(auto_switch_network_on_nfc, spfAutoConfig, SpfConfig.NFC + SpfConfig.ON, false)
        bindSPF(auto_switch_network_on_gps, spfAutoConfig, SpfConfig.GPS + SpfConfig.ON, false)

        bindSPF(auto_switch_network_off_wifi, spfAutoConfig, SpfConfig.WIFI + SpfConfig.OFF, false)
        bindSPF(auto_switch_network_off_data, spfAutoConfig, SpfConfig.DATA + SpfConfig.OFF, false)
        bindSPF(auto_switch_network_off_nfc, spfAutoConfig, SpfConfig.NFC + SpfConfig.OFF, false)
        bindSPF(auto_switch_network_off_gps, spfAutoConfig, SpfConfig.GPS + SpfConfig.OFF, false)

        btn_booster_service_not_active.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        btn_booster_dynamicservice_not_active.setOnClickListener {
            val intent = Intent(thisview, ActivityAccessibilitySettings::class.java)
            startActivity(intent)
        }

        val tabHost = view.findViewById(R.id.blacklist_tabhost) as TabHost
        tabHost.setup()

        tabHost.addTab(tabHost.newTabSpec("tab_1")
                .setContent(R.id.blacklist_tab1).setIndicator(context.getString(R.string.autobooster_tab_blacklist)))
        tabHost.addTab(tabHost.newTabSpec("tab_2")
                .setContent(R.id.blacklist_tab2).setIndicator(context.getString(R.string.autobooster_tab_details)))
        tabHost.addTab(tabHost.newTabSpec("tab_3")
                .setContent(R.id.blacklist_tab3).setIndicator(context.getString(R.string.autobooster_tab_screen)))
        tabHost.currentTab = 0

        setList()
        booster_blacklist.onItemClickListener = OnItemClickListener { _, itemView, _, _ ->
            toogleBlackItem((itemView.findViewById(R.id.ItemText) as TextView).text.toString())
            val checkBox = itemView.findViewById(R.id.select_state) as CheckBox
            checkBox.isChecked = !checkBox.isChecked
        }
    }

    internal val myHandler: Handler = Handler()

    private fun setList() {
        thisview!!.progressBar.visibility = View.VISIBLE

        Thread(Runnable {
            if (installedList == null) {
                loadList()
            }
            setListData(installedList!!, booster_blacklist)
        }).start()
    }

    private fun setListData(dl: ArrayList<HashMap<String, Any>>, lv: ListView) {
        myHandler.post {
            thisview!!.progressBar.visibility = View.GONE
            lv.adapter = list_adapter(context, dl)
        }
    }

    private lateinit var packageManager: PackageManager
    private var installedList: ArrayList<HashMap<String, Any>>? = null

    private fun loadList() {
        packageManager = thisview!!.packageManager
        val packageInfos = packageManager.getInstalledApplications(0)

        installedList = ArrayList()
        for (i in packageInfos.indices) {
            val packageInfo = packageInfos[i]
            if (packageInfo.sourceDir.indexOf("/system") == 0)
                continue
            val item = HashMap<String, Any>()
            val d = packageInfo.loadIcon(packageManager)
            item.put("icon", d)
            val pkgName = packageInfo.packageName
            item.put("select_state", false)
            if (blacklist.contains(pkgName)) {
                item.put("select_state", true)
            } else {
                item.put("select_state", false)
            }
            item.put("name", packageInfo.loadLabel(packageManager))
            item.put("packageName", pkgName)
            installedList!!.add(item)
        }
    }

    private fun toogleBlackItem(pkgName: String) {
        if (blacklist.contains(pkgName)) {
            editor.remove(pkgName).commit()
        } else {
            editor.putBoolean(pkgName, true).commit()
        }
    }

    companion object {
        fun createPage(thisView: ActivityMain): Fragment {
            val fragment = FragmentBooster()
            fragment.thisview = thisView
            return fragment
        }
    }
}
