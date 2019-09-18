package com.omarea.vtools.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.SpannableString
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.shared.FilePathResolver
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.config.PageConfigReader
import com.omarea.krscript.model.*
import com.omarea.krscript.shortcut.ActionShortcutManager
import com.omarea.krscript.ui.ActionListFragment
import com.omarea.krscript.ui.FileChooserRender
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_action_page2.*

class ActionPage2 : AppCompatActivity() {
    private val progressBarDialog = ProgressBarDialog(this)
    private var actionsLoaded = false
    private var handler = Handler()
    private var pageConfig: String = ""
    private var autoRun: String = ""
    private var pageTitle = ""
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeSwitch.switchTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action_page2)

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
                if (extras.containsKey("autoRunItemId")) {
                    autoRun = extras.getString("autoRunItemId")!!
                }
                if (pageConfig.isEmpty()) {
                    setResult(2)
                    finish()
                }
            }
        }

        action_page_tabhost.setup()
        action_page_tabhost.addTab(action_page_tabhost.newTabSpec("a").setContent(R.id.main_list).setIndicator(""))
        action_page_tabhost.addTab(action_page_tabhost.newTabSpec("b").setContent(R.id.action_params).setIndicator(""))
        action_page_tabhost.addTab(action_page_tabhost.newTabSpec("c").setContent(R.id.action_log).setIndicator(""))
        action_page_tabhost.setOnTabChangedListener {
            if (action_page_tabhost.currentTab == 0) {
                title = pageTitle
            }
        }
    }

    /**
     *  “添加收藏”功能实现
     */
    private var addToFavorites = object : ActionLongClickHandler {
        fun addToFavorites(configItemBase: ConfigItemBase) {
            val context = this@ActionPage2

            if (configItemBase.id.isEmpty()) {
                DialogHelper.animDialog(AlertDialog.Builder(context).setTitle(R.string.shortcut_create_fail)
                        .setMessage(R.string.ushortcut_nsupported)
                        .setNeutralButton(R.string.btn_cancel) { _, _ ->
                        }
                )
            } else {
                DialogHelper.animDialog(AlertDialog.Builder(context)
                        .setTitle(getString(R.string.shortcut_create))
                        .setMessage(String.format(getString(R.string.shortcut_create_desc), configItemBase.title))
                        .setPositiveButton(R.string.btn_confirm) { _, _ ->
                            val intent = Intent()
                            intent.component = ComponentName(context.applicationContext, context.javaClass.name)
                            intent.putExtra("config", pageConfig)
                            intent.putExtra("title", "" + title)
                            intent.putExtra("autoRunItemId", configItemBase.id)
                            val result = ActionShortcutManager(context)
                                    .addShortcut(intent, getDrawable(R.drawable.linux)!!, configItemBase)
                            if (!result) {
                                Toast.makeText(context, R.string.shortcut_create_fail, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, getString(R.string.shortcut_create_success), Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton(R.string.btn_cancel) { _, _ ->
                        })
            }
        }
        override fun addToFavorites(switchInfo: SwitchInfo) {
            addToFavorites(switchInfo as ConfigItemBase)
        }

        override fun addToFavorites(actionInfo: ActionInfo) {
            addToFavorites(actionInfo as ConfigItemBase)
        }
    }

    private var actionShortClickHandler = object : ActionShortClickHandler {
        override fun onParamsView(actionInfo: ActionInfo, view: View, onCancel: Runnable, onComplete: Runnable): Boolean {
            if (actionInfo.params!!.size > 3) {
                action_params_editor.removeAllViews()
                action_params_editor.addView(view)
                action_page_tabhost.currentTab = 1
                action_cancel.setOnClickListener {
                    action_page_tabhost.currentTab = 0
                    onCancel.run()
                }
                btn_confirm.setOnClickListener {
                    action_params_editor.removeAllViews()
                    onComplete.run()
                }
                title = actionInfo.title
                return true
            }
            return false
        }

        override fun onExecute(configItem: ConfigItemBase, onExit: Runnable): ShellHandlerBase? {
            var forceStopRunnable: Runnable? = null

            btn_hide.setOnClickListener {
                action_page_tabhost.currentTab = 0
            }
            btn_exit.setOnClickListener {
                action_page_tabhost.currentTab = 0
                if (running && forceStopRunnable != null) {
                    forceStopRunnable!!.run()
                }
            }
            btn_copy.setOnClickListener {
                try {
                    val myClipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val myClip: ClipData = ClipData.newPlainText("text", shell_output.text.toString())
                    myClipboard.primaryClip = myClip
                    Toast.makeText(this@ActionPage2, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
                } catch (ex: Exception) {
                    Toast.makeText(this@ActionPage2, getString(R.string.copy_fail), Toast.LENGTH_SHORT).show()
                }
            }

            if (configItem.interruptible) {
                btn_hide.visibility = View.VISIBLE
                btn_exit.visibility = View.VISIBLE
            } else {
                btn_hide.visibility = View.GONE
                btn_exit.visibility = View.GONE
            }

            action_page_tabhost.currentTab = 2
            title = configItem.title
            action_progress.visibility = View.VISIBLE
            return MyShellHandler(object : IActionEventHandler {
                override fun onExit() {
                    running = false

                    onExit.run()
                    btn_hide.visibility = View.GONE
                    btn_exit.visibility = View.VISIBLE
                    action_progress.visibility = View.GONE

                    if(configItem.autoOff) {
                        action_page_tabhost.currentTab = 0
                    }
                }

                override fun onStart(forceStop: Runnable?) {
                    running = true

                    if (configItem.interruptible && forceStop != null) {
                        btn_exit.visibility = View.VISIBLE
                    } else {
                        btn_exit.visibility = View.GONE
                    }
                    forceStopRunnable = forceStop
                }

            }, shell_output, action_progress)
        }
    }

    @FunctionalInterface
    private interface IActionEventHandler {
        fun onStart(forceStop: Runnable?)
        fun onExit()
    }

    private class MyShellHandler(private var actionEventHandler: IActionEventHandler, private var logView: TextView, private var shellProgress: ProgressBar) : ShellHandlerBase() {
        override fun onStart(forceStop: Runnable?) {
            actionEventHandler.onStart(forceStop)
        }

        override fun onProgress(current: Int, total: Int) {
            if (current == -1) {
                this.shellProgress.visibility = View.VISIBLE
                this.shellProgress.isIndeterminate = true
            } else if (current == total) {
                this.shellProgress.visibility = View.GONE
            } else {
                this.shellProgress.visibility = View.VISIBLE
                this.shellProgress.isIndeterminate = false
                this.shellProgress.max = total
                this.shellProgress.progress = current
            }
        }

        override fun cleanUp() {
        }

        override fun onStart(msg: Any?) {
            this.logView.text = ""
            updateLog(msg, Color.GRAY)
        }

        override fun onExit(msg: Any?) {
            updateLog("\n\n脚本运行结束\n\n", Color.BLUE)
            actionEventHandler.onExit()
        }

        override fun updateLog(msg: SpannableString?) {
            if (msg != null) {
                this.logView.post {
                    logView.append(msg)
                    (logView.parent as ScrollView).fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && action_page_tabhost.currentTab != 0) {
            if (action_page_tabhost.currentTab == 1) {
                action_page_tabhost.currentTab = 0
            } else if (action_page_tabhost.currentTab == 2 && !running) {
                action_page_tabhost.currentTab = 0
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()

        if (!actionsLoaded) {
            progressBarDialog.showDialog(getString(R.string.please_wait))
            Thread(Runnable {
                val items = PageConfigReader(this.applicationContext).readConfigXml(pageConfig)
                handler.post {
                    if (items != null && items.size != 0) {
                        val fragment = ActionListFragment()
                        getFragmentManager().beginTransaction()
                                .add(R.id.main_list, fragment as android.app.Fragment)        //.addToBackStack("fname")
                                .commit()

                        fragment.setListData(
                                this,
                                items,
                                object : FileChooserRender.FileChooserInterface {
                                    override fun openFileChooser(fileSelectedInterface: FileChooserRender.FileSelectedInterface): Boolean {
                                        return chooseFilePath(fileSelectedInterface)
                                    }
                                },
                                actionShortClickHandler
                        )
                        if (autoRun.isNotEmpty()) {
                            if (!fragment.triggerAction(autoRun, Runnable{})) {
                                Toast.makeText(this, "指定项已丢失", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    progressBarDialog.hideDialog()
                }
                actionsLoaded = true
            }).start()
        }
    }


    private var fileSelectedInterface:FileChooserRender.FileSelectedInterface? = null
    private val ACTION_FILE_PATH_CHOOSER = 65400
    private fun chooseFilePath(fileSelectedInterface: FileChooserRender.FileSelectedInterface): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2)
            Toast.makeText(this, getString(R.string.kr_write_external_storage), Toast.LENGTH_LONG).show()
            false
        } else try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(intent, ACTION_FILE_PATH_CHOOSER)
            this.fileSelectedInterface = fileSelectedInterface
            true
        } catch (ex: java.lang.Exception) {
            false
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
}
