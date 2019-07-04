package com.omarea.vtools.activitys

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.SpannableString
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.config.PageConfigReader
import com.omarea.krscript.model.*
import com.omarea.krscript.shortcut.ActionShortcutManager
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_action_page.*

class ActionPage : AppCompatActivity() {
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
        setContentView(R.layout.activity_action_page)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        // setTitle(R.string.app_name)

        // 显示返回按钮
        getSupportActionBar()!!.setHomeButtonEnabled(true);
        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener({ _ ->
            finish()
        });

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
                    setTitle(pageTitle)
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
                setTitle(pageTitle)
            }
        }
    }

    /**
     *  “添加收藏”功能实现
     */
    private var addToFavorites = object : ActionLongClickHandler {
        fun addToFavorites(configItemBase: ConfigItemBase) {
            val context = this@ActionPage

            if (configItemBase.id.isEmpty()) {
                DialogHelper.animDialog(AlertDialog.Builder(context).setTitle(R.string.shortcut_create_fail)
                        .setMessage(R.string.ushortcut_nsupported)
                        .setNeutralButton(R.string.btn_cancel, { _, _ ->
                        })
                )
            } else {
                DialogHelper.animDialog(AlertDialog.Builder(context)
                        .setTitle(getString(R.string.shortcut_create))
                        .setMessage(String.format(getString(R.string.shortcut_create_desc), configItemBase.title))
                        .setPositiveButton(R.string.btn_confirm, { _, _ ->
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
                        })
                        .setNegativeButton(R.string.btn_cancel, { _, _ ->
                        }))
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
                setTitle(actionInfo.title)
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
            if (configItem.interruptible) {
                btn_hide.visibility = View.VISIBLE
                btn_exit.visibility = View.VISIBLE
            } else {
                btn_hide.visibility = View.GONE
                btn_exit.visibility = View.GONE
            }

            action_page_tabhost.currentTab = 2
            setTitle(configItem.title)
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
                        btn_exit.setVisibility(View.VISIBLE)
                    } else {
                        btn_exit.setVisibility(View.GONE)
                    }
                    forceStopRunnable = forceStop
                }

            }, shell_output, action_progress);
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
                this.shellProgress.setVisibility(View.VISIBLE)
                this.shellProgress.setIndeterminate(true)
            } else if (current == total) {
                this.shellProgress.setVisibility(View.GONE)
            } else {
                this.shellProgress.setVisibility(View.VISIBLE)
                this.shellProgress.setIndeterminate(false)
                this.shellProgress.setMax(total)
                this.shellProgress.setProgress(current)
            }
        }

        override fun cleanUp() {
        }

        override fun onStart(msg: Any?) {
            this.logView.text = "";
            updateLog(msg, Color.GRAY)
        }

        override fun onExit(msg: Any?) {
            updateLog("\n\n脚本运行结束\n\n", Color.BLUE)
            actionEventHandler.onExit()
        }

        override fun updateLog(msg: SpannableString?) {
            if (msg != null) {
                this.logView.post({
                    logView.append(msg)
                    (logView.getParent() as ScrollView).fullScroll(ScrollView.FOCUS_DOWN)
                })
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
            return true;
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
                        main_list.setListData(
                                items,
                                actionShortClickHandler,
                                addToFavorites
                        )
                        if (autoRun.isNotEmpty()) {
                            val onCompleted = Runnable {
                                // finish()
                            }
                            if (!main_list.triggerAction(autoRun, onCompleted)) {
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
}
