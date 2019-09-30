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
import android.util.Log
import android.view.View
import android.widget.Toast
import com.omarea.common.shared.FilePathResolver
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.common.ui.ThemeMode
import com.omarea.krscript.config.PageConfigReader
import com.omarea.krscript.model.AutoRunTask
import com.omarea.krscript.model.ConfigItemBase
import com.omarea.krscript.model.KrScriptActionHandler
import com.omarea.krscript.model.PageInfo
import com.omarea.krscript.ui.ActionListFragment
import com.omarea.krscript.ui.FileChooserRender
import com.omarea.vtools.R

class ActionPage : AppCompatActivity() {
    private val progressBarDialog = ProgressBarDialog(this)
    private var actionsLoaded = false
    private var handler = Handler()
    private var pageConfig: String = ""
    private var autoRun: String = ""
    private var pageTitle = ""
    private lateinit var themeMode: ThemeMode

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

    private var actionShortClickHandler = object : KrScriptActionHandler {
        override fun addToFavorites(configItemBase: ConfigItemBase, addToFavoritesHandler: KrScriptActionHandler.AddToFavoritesHandler) {
            val intent = Intent()

            intent.component = ComponentName(this@ActionPage.applicationContext, this@ActionPage.javaClass.name)
            intent.putExtra("config", pageConfig)
            intent.putExtra("title", "" + title)
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
            progressBarDialog.showDialog(getString(R.string.please_wait))
            Thread(Runnable {
                val items = PageConfigReader(this.applicationContext).readConfigXml(pageConfig)
                handler.post {
                    if (items != null && items.size != 0) {
                        val fragment = ActionListFragment.create(items, actionShortClickHandler, object : AutoRunTask {
                            override val key = autoRun
                            override fun onCompleted(result: Boolean?) {
                                if (result != true) {
                                    Toast.makeText(this@ActionPage, "指定项已丢失", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }, themeMode)
                        supportFragmentManager.beginTransaction().add(R.id.main_list, fragment).commit()
                    }
                    progressBarDialog.hideDialog()
                }
                actionsLoaded = true
            }).start()
        }
    }

    fun _openPage(pageInfo: PageInfo) {
        try {
            if (!pageInfo.pageConfigPath.isEmpty()) {
                val intent = Intent(this, ActionPage::class.java)
                intent.putExtra("config", pageInfo.pageConfigPath)
                intent.putExtra("title", pageInfo.title)
                startActivity(intent)
            } else if (!pageInfo.onlineHtmlPage.isEmpty()) {
                // TODO:OnlinePage
                // val intent = Intent(this, ActionPageOnline::class.java)
                // intent.putExtra("config", pageInfo.onlineHtmlPage)
                // intent.putExtra("title", pageInfo.title)
                // startActivity(intent)
            }
        } catch (ex: java.lang.Exception) {
            Log.e("_openPage", "" + ex.message)
        }
    }
}
