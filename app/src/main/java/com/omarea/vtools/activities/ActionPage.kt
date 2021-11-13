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
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.omarea.common.shared.FilePathResolver
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.TryOpenActivity
import com.omarea.krscript.config.IconPathAnalysis
import com.omarea.krscript.config.PageConfigReader
import com.omarea.krscript.config.PageConfigSh
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.*
import com.omarea.krscript.shortcut.ActionShortcutManager
import com.omarea.krscript.ui.ActionListFragment
import com.omarea.krscript.ui.DialogLogFragment
import com.omarea.krscript.ui.PageMenuLoader
import com.omarea.krscript.ui.ParamsFileChooserRender
import com.omarea.vtools.R
import com.projectkr.shell.OpenPageHelper
import kotlinx.android.synthetic.main.activity_action_page.*

class ActionPage : ActivityBase() {
    private val progressBarDialog = ProgressBarDialog(this)
    private var actionsLoaded = false
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var currentPageConfig: PageNode
    private var autoRunItemId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action_page)
        setBackArrow()

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
            if (extras != null && (extras.containsKey("page") || extras.containsKey("shortcutId"))) {
                val page = if (extras.containsKey("page")) {
                    extras.getSerializable("page") as PageNode?
                } else {
                    ActionShortcutManager(this@ActionPage).getShortcutTarget("" + extras.getString("shortcutId"))
                }

                if (page != null) {
                    autoRunItemId = if (extras.containsKey("autoRunItemId")) ("" + extras.getString("autoRunItemId")) else ""

                    if (page.activity.isNotEmpty()) {
                        if (TryOpenActivity(this, page.activity).tryOpen()) {
                            finish()
                            return
                        }
                    }

                    if (page.onlineHtmlPage.isNotEmpty()) {
                        try {
                            startActivity(Intent(this, ActionPageOnline::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra("config", page.onlineHtmlPage)
                            })
                        } catch (ex: Exception) {
                        }
                    }

                    if (page.title.isNotEmpty()) {
                        title = page.title
                    }
                    currentPageConfig = page
                } else {
                    Toast.makeText(this, "页面信息无效", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        if (currentPageConfig.pageConfigPath.isEmpty() && currentPageConfig.pageConfigSh.isEmpty()) {
            setResult(2)
            finish()
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
            val page = if (clickableNode is PageNode) {
                clickableNode
            } else if (clickableNode is RunnableNode) {
                currentPageConfig
            } else {
                return
            }

            val intent = Intent()

            intent.component = ComponentName(this@ActionPage.applicationContext, ActionPage::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            if (clickableNode is RunnableNode) {
                intent.putExtra("autoRunItemId", clickableNode.key)
            }
            intent.putExtra("page", page)

            addToFavoritesHandler.onAddToFavorites(clickableNode, intent)
        }

        override fun onSubPageClick(pageNode: PageNode) {
            _openPage(pageNode)
        }

        override fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
            return chooseFilePath(fileSelectedInterface)
        }
    }

    private var fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface? = null
    private val ACTION_FILE_PATH_CHOOSER = 65400
    private val ACTION_FILE_PATH_CHOOSER_INNER = 65300

    private fun chooseFilePath(extension: String) {
        try {
            val intent = Intent(this, ActivityFileSelector::class.java)
            intent.putExtra("extension", extension)
            intent.putExtra("mode", ActivityFileSelector.MODE_FILE)
            startActivityForResult(intent, ACTION_FILE_PATH_CHOOSER_INNER)
        } catch (ex: Exception) {
            Toast.makeText(this, "启动内置文件选择器失败！", Toast.LENGTH_SHORT).show()
        }
    }

    private fun chooseFolderPath() {
        try {
            val intent = Intent(this, ActivityFileSelector::class.java)
            intent.putExtra("mode", ActivityFileSelector.MODE_FOLDER)
            startActivityForResult(intent, ACTION_FILE_PATH_CHOOSER_INNER)
        } catch (ex: Exception) {
            Toast.makeText(this, "启动内置文件选择器失败！", Toast.LENGTH_SHORT).show()
        }
    }

    private var menuOptions: ArrayList<PageMenuOption>? = null

    // 右上角菜单的创建
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (menuOptions == null) {
            menuOptions = PageMenuLoader(applicationContext, currentPageConfig).load()
        }

        if (menuOptions != null && menu != null) {
            for (i in 0 until menuOptions!!.size) {
                val menuOption = menuOptions!![i]
                if (menuOption.isFab) {
                    addFab(menuOption)
                } else {
                    menu.add(-1, i, i, menuOption.title)
                }
            }
        }

        return true // super.onCreateOptionsMenu(menu)
    }

    private fun addFab(menuOption: PageMenuOption) {
        action_page_fab.run {
            visibility = View.VISIBLE
            setOnClickListener {
                onMenuItemClick(menuOption)
            }

            if (menuOption.type == "file" && menuOption.iconPath.isEmpty()) {
                setImageDrawable(getDrawable(R.drawable.kr_folder))
            } else if (menuOption.iconPath.isNotEmpty()) {
                val icon = IconPathAnalysis().loadLogo(context, menuOption, false)
                if (icon != null) {
                    setImageDrawable(icon)
                } else {
                    setImageDrawable(getDrawable(R.drawable.kr_fab))
                }
            } else {
                setImageDrawable(getDrawable(R.drawable.kr_fab))
            }
        }
    }

    // 右上角菜单的点击操作
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (menuOptions == null) {
            return false
        }

        onMenuItemClick(menuOptions!![item.itemId])

        return true
    }

    private fun onMenuItemClick(menuOption: PageMenuOption) {
        when (menuOption.type) {
            "refresh", "reload" -> {
                recreate()
            }
            "exit", "finish", "close" -> {
                finish()
            }
            "file" -> {
                menuItemChooseFile(menuOption)
            }
            else -> {
                menuItemExecute(menuOption, HashMap<String, String>().apply {
                    put("state", menuOption.key)
                    put("menu_id", menuOption.key)
                })
            }
        }
    }

    private fun menuItemExecute(menuOption: PageMenuOption, params: HashMap<String, String>) {
        val onDismiss = Runnable {
            if (menuOption.autoFinish) {
                finish()
            } else if (menuOption.reloadPage) {
                recreate()
            } else if (menuOption.updateBlocks != null) {
                // TODO rootGroup.triggerUpdateByKey(item.updateBlocks!!)
            }
        }

        val darkMode = themeMode.isDarkMode
        val dialog = DialogLogFragment.create(
                menuOption,
                { },
                onDismiss,
                currentPageConfig.pageHandlerSh,
                params,
                darkMode)
        dialog.show(supportFragmentManager, "")
        dialog.isCancelable = false
    }

    private fun menuItemChooseFile(menuOption: PageMenuOption) {
        chooseFilePath(object : ParamsFileChooserRender.FileSelectedInterface {
            override fun onFileSelected(path: String?) {
                if (path != null) {
                    handler.post {
                        menuItemExecute(menuOption, HashMap<String, String>().apply {
                            put("state", menuOption.key)
                            put("menu_id", menuOption.key)
                            put("file", path)
                            put("folder", path)
                        })
                    }
                }
            }

            // TODO:文件类型过滤
            override fun mimeType(): String? {
                return if (menuOption.mime.isEmpty()) null else menuOption.mime
            }

            override fun suffix(): String? {
                return if (menuOption.suffix.isEmpty()) null else menuOption.suffix
            }

            override fun type(): Int {
                return when (menuOption.type) {
                    "folder" -> ParamsFileChooserRender.FileSelectedInterface.TYPE_FOLDER
                    "file" -> ParamsFileChooserRender.FileSelectedInterface.TYPE_FILE
                    else -> ParamsFileChooserRender.FileSelectedInterface.TYPE_FILE
                }
            }
        })
    }

    private fun chooseFilePath(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2)
            Toast.makeText(this, getString(R.string.kr_write_external_storage), Toast.LENGTH_LONG).show()
            return false
        } else {
            return try {
                if (fileSelectedInterface.type() == ParamsFileChooserRender.FileSelectedInterface.TYPE_FOLDER) {
                    chooseFolderPath()
                } else {
                    val suffix = fileSelectedInterface.suffix()
                    if (!suffix.isNullOrEmpty()) {
                        chooseFilePath(suffix)
                    } else {
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        val mimeType = fileSelectedInterface.mimeType()
                        if (mimeType != null) {
                            intent.type = mimeType
                        } else {
                            intent.type = "*/*"
                        }
                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                        startActivityForResult(intent, ACTION_FILE_PATH_CHOOSER)
                    }
                }
                this.fileSelectedInterface = fileSelectedInterface
                true
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
        } else if (requestCode == ACTION_FILE_PATH_CHOOSER_INNER) {
            val absPath = if (data == null || resultCode != Activity.RESULT_OK) null else data.getStringExtra("file")
            fileSelectedInterface?.onFileSelected(absPath)
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

    override fun onResume() {
        super.onResume()

        if (!actionsLoaded) {
            loadPageConfig()
        }
    }

    private fun loadPageConfig() {
        val activity = this

        Thread {
            currentPageConfig.run {
                if (beforeRead.isNotEmpty()) {
                    showDialog(getString(R.string.kr_page_before_load))
                    ScriptEnvironmen.executeResultRoot(activity, beforeRead, currentPageConfig)
                }

                showDialog(getString(R.string.kr_page_loading))
                var items: ArrayList<NodeInfoBase>? = null

                if (pageConfigSh.isNotEmpty()) {
                    items = PageConfigSh(this@ActionPage, pageConfigSh, this).execute()
                }

                if (items == null && pageConfigPath.isNotEmpty()) {
                    items = PageConfigReader(applicationContext, pageConfigPath, pageConfigDir).readConfigXml()
                }

                if (afterRead.isNotEmpty()) {
                    showDialog(getString(R.string.kr_page_after_load))
                    ScriptEnvironmen.executeResultRoot(activity, afterRead, this)
                }

                if (items != null && items.size != 0) {
                    if (loadSuccess.isNotEmpty()) {
                        showDialog(getString(R.string.kr_page_load_success))
                        ScriptEnvironmen.executeResultRoot(activity, loadSuccess, this)
                    }

                    handler.post {
                        val autoRunTask = if (actionsLoaded) null else object : AutoRunTask {
                            override val key = autoRunItemId
                            override fun onCompleted(result: Boolean?) {
                                if (result != true) {
                                    Toast.makeText(this@ActionPage, getString(R.string.kr_auto_run_item_losted), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        val fragment = ActionListFragment.create(items, actionShortClickHandler, autoRunTask, themeMode)
                        supportFragmentManager.beginTransaction().replace(R.id.main_list, fragment).commitAllowingStateLoss()
                        hideDialog()
                        actionsLoaded = true
                    }
                } else {
                    if (loadFail.isNotEmpty()) {
                        showDialog(getString(R.string.kr_page_load_fail))
                        ScriptEnvironmen.executeResultRoot(activity, loadFail, this)
                        hideDialog()
                    }

                    handler.post {
                        Toast.makeText(this@ActionPage, getString(R.string.kr_page_load_fail), Toast.LENGTH_SHORT).show()
                    }
                    hideDialog()
                    finish()
                }
            }
        }.start()
    }

    fun _openPage(pageNode: PageNode) {
        OpenPageHelper(this).openPage(pageNode)
    }

    override fun onDestroy() {
        if (isTaskRoot) {
            excludeFromRecent()
        }
        super.onDestroy()
    }
}
