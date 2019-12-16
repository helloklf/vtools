package com.omarea.vtools.activities

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Toast
import com.omarea.common.shared.FilePathResolver
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.common.ui.ThemeMode
import com.omarea.krscript.config.PageConfigReader
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.*
import com.omarea.krscript.ui.ActionListFragment
import com.omarea.krscript.ui.FileChooserRender
import com.omarea.vtools.R
import com.omarea.vtools.kr_script.PageConfigSh
import com.projectkr.shell.OpenPageHelper

class ActionPage : AppCompatActivity() {
    private val progressBarDialog = ProgressBarDialog(this)
    private var actionsLoaded = false
    private var handler = Handler()
    private var pageConfig: String = ""
    private var parentDir: String = ""
    private var autoRun: String = ""
    private var pageTitle = ""

    // 读取页面配置前
    private var beforeRead = ""
    // 读取页面配置后
    private var afterRead = ""

    private var loadSuccess = ""
    private var loadFail = ""

    // 页面配置脚本
    private var pageConfigSh = ""
    private lateinit var themeMode: ThemeMode

    private var autoRunCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        this.themeMode = ThemeSwitch.switchTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action_page)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        // setTitle(R.string.app_name)

        // 显示返回按钮
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        /*
        // 设置个漂亮的白色顶栏
        val window = window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else {
            getWindow().decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        */

        // 读取intent里的参数
        val intent = this.intent
        if (intent.extras != null) {
            val extras = intent.extras
            if (extras != null) {
                if (extras.containsKey("title")) {
                    pageTitle = extras.getString("title")!!
                    title = pageTitle
                }
                if (extras.containsKey("config")) {
                    pageConfig = extras.getString("config")!!
                }
                if (extras.containsKey("parentDir")) {
                    parentDir = extras.getString("parentDir")!!
                }
                if (extras.containsKey("pageConfigSh")) {
                    pageConfigSh = extras.getString("pageConfigSh")!!
                }

                if (extras.containsKey("beforeRead")) {
                    beforeRead = extras.getString("beforeRead")!!
                }
                if (extras.containsKey("afterRead")) {
                    afterRead = extras.getString("afterRead")!!
                }
                if (extras.containsKey("loadSuccess")) {
                    loadSuccess = extras.getString("loadSuccess")!!
                }
                if (extras.containsKey("loadFail")) {
                    loadFail = extras.getString("loadFail")!!
                }

                if (extras.containsKey("autoRunItemId")) {
                    autoRun = extras.getString("autoRunItemId")!!
                }

                if (pageConfig.isEmpty() && pageConfigSh.isEmpty()) {
                    setResult(2)
                    finish()
                }
            }
        }
    }

    private var actionShortClickHandler = object : KrScriptActionHandler {
        override fun onActionCompleted(runnableNode: RunnableNode) {
            if (runnableNode.autoFinish) {
                finishAndRemoveTask()
            } else if (runnableNode.reloadPage) {
                loadPageConfig()
            }
        }

        override fun addToFavorites(clickableNode: ClickableNode, addToFavoritesHandler: KrScriptActionHandler.AddToFavoritesHandler) {
            val intent = Intent()

            intent.component = ComponentName(this@ActionPage.applicationContext, this@ActionPage.javaClass.name)
            intent.putExtra("title", "" + title)
            intent.putExtra("beforeRead", beforeRead)
            intent.putExtra("config", pageConfig)
            intent.putExtra("parentDir", parentDir)
            intent.putExtra("pageConfigSh", pageConfigSh)
            intent.putExtra("afterRead", afterRead)
            intent.putExtra("loadSuccess", loadSuccess)
            intent.putExtra("loadFail", loadFail)
            intent.putExtra("autoRunItemId", clickableNode.key)

            addToFavoritesHandler.onAddToFavorites(clickableNode, intent)
        }

        override fun onSubPageClick(pageNode: PageNode) {
            _openPage(pageNode)
        }

        override fun openFileChooser(fileSelectedInterface: FileChooserRender.FileSelectedInterface): Boolean {
            return chooseFilePath(fileSelectedInterface)
        }
    }

    private var fileSelectedInterface: FileChooserRender.FileSelectedInterface? = null
    private val ACTION_FILE_PATH_CHOOSER = 65400
    private fun chooseFilePath(fileSelectedInterface: FileChooserRender.FileSelectedInterface): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2);
            Toast.makeText(this, getString(R.string.kr_write_external_storage), Toast.LENGTH_LONG).show()
            return false
        } else {
            return try {
                val intent = Intent(Intent.ACTION_GET_CONTENT);
                intent.type = "*/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, ACTION_FILE_PATH_CHOOSER);
                this.fileSelectedInterface = fileSelectedInterface
                true;
            } catch (ex: java.lang.Exception) {
                false
            }
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

    private fun getPath(uri: Uri): String? {
        return try {
            FilePathResolver().getPath(this, uri)
        } catch (ex: java.lang.Exception) {
            null
        }
    }

    override fun onResume() {
        super.onResume()

        if (!actionsLoaded) {
            loadPageConfig()
        }
    }

    private fun showDialog(msg: String) {
        handler.post {
            progressBarDialog.showDialog(msg)
        }
    }
    private fun hideDialog() {
        handler.post {
            progressBarDialog.hideDialog()
        }
    }

    private fun loadPageConfig() {
        val activity = this

        Thread(Runnable {
            if (beforeRead.isNotEmpty()) {
                showDialog(getString(R.string.kr_page_before_load))
                ScriptEnvironmen.executeResultRoot(activity, beforeRead)
            }

            showDialog(getString(R.string.kr_page_loading))
            var items: ArrayList<NodeInfoBase>? = null

            if (pageConfigSh.isNotEmpty()) {
                items = PageConfigSh(this, pageConfigSh).execute()
            }

            if (items == null && pageConfig.isNotEmpty()) {
                items = PageConfigReader(this.applicationContext, pageConfig, parentDir).readConfigXml()
            }

            if (afterRead.isNotEmpty()) {
                showDialog(getString(R.string.kr_page_after_load))
                ScriptEnvironmen.executeResultRoot(activity, afterRead)
            }

            if (items != null && items.size != 0) {
                if (loadSuccess.isNotEmpty()) {
                    showDialog(getString(R.string.kr_page_load_success))
                    ScriptEnvironmen.executeResultRoot(activity, loadSuccess)
                }

                handler.post {
                    val autoRunTask = if (actionsLoaded) null else object : AutoRunTask {
                        override val key = autoRun
                        override fun onCompleted(result: Boolean?) {
                            if (result != true) {
                                Toast.makeText(this@ActionPage, getString(R.string.kr_auto_run_item_losted), Toast.LENGTH_SHORT).show()
                            }
                        }
                    };

                    val fragment = ActionListFragment.create(items, actionShortClickHandler, autoRunTask, themeMode)
                    supportFragmentManager.beginTransaction().replace(R.id.main_list, fragment).commitAllowingStateLoss()
                    hideDialog()
                    actionsLoaded = true
                }
            } else {
                if (loadFail.isNotEmpty()) {
                    showDialog(getString(R.string.kr_page_load_fail))
                    ScriptEnvironmen.executeResultRoot(activity, loadFail)
                    hideDialog()
                }

                handler.post {
                    Toast.makeText(this@ActionPage, getString(R.string.kr_page_load_fail), Toast.LENGTH_SHORT).show()
                }
                hideDialog()
                finish()
            }
        }).start()
    }

    fun _openPage(pageInfo: PageNode) {
        OpenPageHelper(this).openPage(pageInfo)
    }
}
