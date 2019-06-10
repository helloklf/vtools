package com.omarea.vtools.activitys

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Toast
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.config.PageConfigReader
import com.omarea.krscript.model.ActionInfo
import com.omarea.krscript.model.ActionLongClickHandler
import com.omarea.krscript.model.SwitchInfo
import com.omarea.krscript.shortcut.ActionShortcutManager
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_action_page.*

class ActionPage : AppCompatActivity() {
    val progressBarDialog = ProgressBarDialog(this)
    private var actionsLoaded = false
    private var handler = Handler()
    private var pageConfig: String = ""
    private var autoRun: String = ""

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
                    setTitle(extras.getString("title"))
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
    }

    /**
     *  “添加收藏”功能实现
     */
    private var addToFavorites = object : ActionLongClickHandler {
        override fun addToFavorites(switchInfo: SwitchInfo) {
            if (switchInfo.id.isEmpty()) {
                DialogHelper.animDialog(AlertDialog.Builder(this@ActionPage).setTitle("添加快捷方式失败")
                        .setMessage("该功能不支持添加快捷方式")
                        .setNeutralButton(R.string.btn_cancel, { _, _ ->
                        })
                )
            } else {
                DialogHelper.animDialog(AlertDialog.Builder(this@ActionPage).setTitle("添加快捷方式")
                        .setMessage("你希望将“" + switchInfo.title + "”添加到桌面，方便快速使用吗？")
                        .setPositiveButton(R.string.btn_confirm, { _, _ ->
                            val intent = Intent()
                            intent.component = ComponentName(this@ActionPage.applicationContext, this@ActionPage.javaClass.name)
                            intent.putExtra("config", pageConfig)
                            intent.putExtra("title", "" + title)
                            intent.putExtra("autoRunItemId", switchInfo.id)
                            val result = ActionShortcutManager(this@ActionPage).addShortcut(intent, getDrawable(R.drawable.linux)!!, switchInfo)
                            if (!result) {
                                Toast.makeText(this@ActionPage, "快捷方式创建失败", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@ActionPage, "已发送创建快捷方式申请", Toast.LENGTH_SHORT).show()
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, { _, _ ->
                        }))
            }
        }

        override fun addToFavorites(actionInfo: ActionInfo) {
            if (actionInfo.id.isEmpty()) {
                DialogHelper.animDialog(AlertDialog.Builder(this@ActionPage).setTitle("添加快捷方式失败")
                        .setMessage("该功能不支持添加快捷方式")
                        .setNeutralButton(R.string.btn_cancel, { _, _ ->
                        })
                )
            } else if (actionInfo.params != null && actionInfo.params!!.size > 0) {
                DialogHelper.animDialog(AlertDialog.Builder(this@ActionPage).setTitle("添加快捷方式")
                        .setMessage("不支持为带有参数的附加功能项添加快捷方式")
                        .setNeutralButton(R.string.btn_cancel, { _, _ ->
                        })
                )
            } else {
                DialogHelper.animDialog(AlertDialog.Builder(this@ActionPage).setTitle("添加快捷方式")
                        .setMessage("你希望将“" + actionInfo.title + "”添加到桌面，方便快速使用吗？")
                        .setPositiveButton(R.string.btn_confirm, { _, _ ->
                            val intent = Intent()
                            intent.component = ComponentName(this@ActionPage.applicationContext, this@ActionPage.javaClass.name)
                            intent.putExtra("config", pageConfig)
                            intent.putExtra("title", "" + title)
                            intent.putExtra("autoRunItemId", actionInfo.id)
                            val result = ActionShortcutManager(this@ActionPage).addShortcut(intent, getDrawable(R.drawable.linux)!!, actionInfo)
                            if (!result) {
                                Toast.makeText(this@ActionPage, "快捷方式创建失败", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@ActionPage, "已发送创建快捷方式申请", Toast.LENGTH_SHORT).show()
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, { _, _ ->
                        }))
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!actionsLoaded) {
            progressBarDialog.showDialog(getString(R.string.please_wait))
            Thread(Runnable {
                val items = PageConfigReader(this.applicationContext).readConfigXml(pageConfig)
                handler.post {
                    if (items != null && items.size != 0) {
                        main_list.setListData(items, addToFavorites)
                        if (autoRun.isNotEmpty()) {
                            val onCompleted = Runnable {
                                // finish()
                            }
                            if(!main_list.triggerAction(autoRun, onCompleted)) {
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
