package com.omarea.vtools.activitys

import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.config.PageConfigReader
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_action_page.*

class ActionPage : AppCompatActivity() {
    val progressBarDialog = ProgressBarDialog(this)
    private var actionsLoaded = false
    private var handler = Handler()
    private var pageConfig:String = ""

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
        toolbar.setNavigationOnClickListener({
            _ ->
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
                if (pageConfig.isEmpty()) {
                    setResult(2)
                    finish()
                }
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
                        main_list.setListData(items)
                    }
                    progressBarDialog.hideDialog()
                }
                actionsLoaded = true
            }).start()
        }
    }
}
