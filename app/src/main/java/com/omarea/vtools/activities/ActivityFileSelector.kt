package com.omarea.vtools.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.ui.AdapterFileSelector
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_file_selector.*
import java.io.File

class ActivityFileSelector : ActivityBase() {
    companion object {
        val MODE_FILE = 0
        val MODE_FOLDER = 1
    }

    private var adapterFileSelector: AdapterFileSelector? = null
    var extension = ""
    var mode = MODE_FILE
    var start = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_selector)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        // setTitle(R.string.app_name)

        // 显示返回按钮
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { _ ->
            finish()
        }

        intent.extras?.run {
            if (containsKey("extension")) {
                extension = "" + intent.extras!!.getString("extension")
                if (!extension.startsWith(".")) {
                    extension = ".$extension"
                }
                if (extension.isNotEmpty()) {
                    title = title.toString() + "($extension)"
                }
            }

            if (containsKey("mode")) {
                mode = getInt("mode")
                if (mode == MODE_FOLDER) {
                    title = getString(R.string.title_activity_folder_selector)
                }
            }

            if (containsKey("start")) {
                start = getString("start")!!
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && adapterFileSelector != null && adapterFileSelector!!.goParent()) {
            return true
        } else {
            setResult(Activity.RESULT_CANCELED, Intent())
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        val startDir = if (start.isNotEmpty()) File(start) else {
            try {
                val file = File(CommonCmds.SDCardDir)
                if (file.exists() && file.canRead()) {
                    file
                } else {
                    File("/sdcard")
                }
            } catch (ex: Exception) {
                File("/sdcard")
            }
        }
        if (startDir.exists() && startDir.isDirectory) {
            val list = startDir.listFiles()
            if (list == null) {
                Toast.makeText(applicationContext, "没有读取文件的权限！", Toast.LENGTH_LONG).show()
                return
            }
            val onSelected = Runnable {
                val file: File? = adapterFileSelector!!.selectedFile
                if (file != null) {
                    this.setResult(Activity.RESULT_OK, Intent().putExtra("file", file.absolutePath))
                    this.finish()
                }
            }
            adapterFileSelector = if (mode == MODE_FOLDER) {
                AdapterFileSelector.FolderChooser(startDir, onSelected, ProgressBarDialog(this))
            } else {
                AdapterFileSelector.FileChooser(startDir, onSelected, ProgressBarDialog(this), extension)
            }
            file_selector_list.adapter = adapterFileSelector
        }
    }
}
