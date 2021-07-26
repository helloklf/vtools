package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import com.omarea.Scene
import com.omarea.common.ui.OverScrollListView
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.model.AppInfo
import com.omarea.model.SceneConfigInfo
import com.omarea.store.SpfConfig
import com.omarea.store.XposedExtension
import com.omarea.ui.XposedAppsAdapter
import com.omarea.utils.AppListHelper
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_app_xposed_config.*
import java.util.*
import kotlin.collections.ArrayList


class ActivityAppXposedConfig : ActivityBase() {
    private lateinit var processBarDialog: ProgressBarDialog
    private lateinit var globalSPF: SharedPreferences
    private lateinit var applistHelper: AppListHelper
    private var installedList: ArrayList<AppInfo>? = null
    private var displayList: ArrayList<AppInfo>? = null
    private lateinit var xposedExtension: XposedExtension

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_xposed_config)

        setBackArrow()
        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        xposedExtension = XposedExtension(this)

        this.onViewCreated()
    }

    private fun onViewCreated() {
        processBarDialog = ProgressBarDialog(this)
        applistHelper = AppListHelper(this)
        globalSPF = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        scene_app_list.setOnItemClickListener { parent, view2, position, _ ->
            try {
                val item = (parent.adapter.getItem(position) as AppInfo)
                val intent = Intent(this.context, ActivityAppXposedDetails::class.java)
                intent.putExtra("app", item.packageName)
                startActivityForResult(intent, REQUEST_APP_CONFIG)
                lastClickRow = view2
            } catch (ex: Exception) {
            }
        }

        config_search_box.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                loadList()
                return@setOnEditorActionListener true
            }
            false
        }

        configlist_type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadList()
            }
        }

    }

    private val REQUEST_APP_CONFIG = 0
    private var lastClickRow: View? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_APP_CONFIG && data != null && displayList != null) {
            try {
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    val adapter = (scene_app_list.adapter as XposedAppsAdapter)
                    var index = -1
                    val packageName = data.extras!!.getString("app")
                    for (i in 0 until displayList!!.size) {
                        if (displayList!![i].packageName == packageName) {
                            index = i
                        }
                    }
                    if (index < 0) {
                        return
                    }
                    val item = adapter.getItem(index)
                    setAppRowDesc(item)
                    (scene_app_list.adapter as XposedAppsAdapter?)?.run {
                        updateRow(index, lastClickRow!!)
                    }
                    //loadList(false)
                }
            } catch (ex: Exception) {
                Log.e("update-list", "" + ex.message)
            }
        }
    }

    private fun sortAppList(list: ArrayList<AppInfo>): ArrayList<AppInfo> {
        list.sortWith { l, r ->
            try {
                val les = l.stateTags.toString()
                val res = r.stateTags.toString()
                when {
                    les < res -> -1
                    les > res -> 1
                    else -> {
                        val lp = l.packageName.toString()
                        val rp = r.packageName.toString()
                        when {
                            lp < rp -> -1
                            lp > rp -> 1
                            else -> 0
                        }
                    }
                }
            } catch (ex: Exception) {
                0
            }
        }
        return list
    }

    private fun setListData(dl: ArrayList<AppInfo>?, lv: OverScrollListView) {
        Scene.post {
            lv.adapter = XposedAppsAdapter(
                    this,
                    dl!!
            )
            processBarDialog.hideDialog()
        }
    }

    private var onLoading = false

    @SuppressLint("ApplySharedPref")
    private fun loadList(foreceReload: Boolean = false) {
        if (onLoading) {
            return
        }
        processBarDialog.showDialog()

        Thread(Runnable {
            onLoading = true
            if (foreceReload || installedList == null || installedList!!.size == 0) {
                installedList = ArrayList()/*在数组中存放数据*/
                installedList = applistHelper.getAll()
            }
            if (config_search_box == null) {
                Scene.post {
                    processBarDialog.hideDialog()
                }
                return@Runnable
            }
            val keyword = config_search_box.text.toString().toLowerCase(Locale.getDefault())
            val search = keyword.isNotEmpty()
            var filterAppType = ""
            when (configlist_type.selectedItemPosition) {
                0 -> filterAppType = "/data"
                1 -> filterAppType = "/system"
                2 -> filterAppType = "*"
            }
            displayList = ArrayList()
            for (i in installedList!!.indices) {
                val item = installedList!![i]
                val packageName = item.packageName
                if (search && !(packageName.toLowerCase(Locale.getDefault()).contains(keyword) || item.appName.toLowerCase(Locale.getDefault()).contains(keyword))) {
                    continue
                } else {
                    if (filterAppType == "*" || item.path.startsWith(filterAppType)) {
                        displayList!!.add(item)
                        setAppRowDesc(item)
                    }
                }
            }
            sortAppList(displayList!!)
            Scene.post {
                processBarDialog.hideDialog()
                setListData(displayList, scene_app_list)
            }
            onLoading = false
        }).start()
    }

    private fun setAppRowDesc(item: AppInfo) {
        item.selected = false
        val packageName = item.packageName
        val configInfo = SceneConfigInfo()
        configInfo.packageName = packageName
        item.sceneConfigInfo = configInfo

        if (xposedExtension.current != null) {
            xposedExtension.getAppConfig(packageName)?.run {
                val desc = StringBuilder()
                if (dpi > 0) {
                    desc.append("DPI:${dpi}  ")
                }
                if (excludeRecent) {
                    desc.append("隐藏后台  ")
                }
                if (smoothScroll) {
                    desc.append("弹性慢速滚动  ")
                }
                if (webDebug) {
                    desc.append("Web调试  ")
                }
                item.desc = desc.toString()
            }
        }

    }

    override fun onDestroy() {
        xposedExtension.unbindService()
        processBarDialog.hideDialog()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        xposedExtension.bindService {
            loadList()
        }
        title = getString(R.string.menu_xposed_app)
    }
}
