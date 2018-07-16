package com.omarea.vboot

import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.KeyEvent
import android.widget.Toast
import com.omarea.shared.Consts
import com.omarea.shared.SpfConfig
import com.omarea.ui.AdapterFileSelector
import com.omarea.ui.ProgressBarDialog
import kotlinx.android.synthetic.main.activity_file_selector.*
import java.io.File

class ActivityFileSelector : AppCompatActivity() {

    private var adapterFileSelector: AdapterFileSelector? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        val spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (spf.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false))
            this.setTheme(R.style.AppTheme_NoActionBarNight)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_selector)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && adapterFileSelector != null && adapterFileSelector!!.goParent()) {
            return true
        } else {
            setResult(-1)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        val sdcard = File(Consts.SDCardDir)
        if (sdcard.exists() && sdcard.isDirectory) {
            val list = sdcard.listFiles()
            if (list == null) {
                Toast.makeText(this, "没有读取文件的权限！", Toast.LENGTH_LONG).show()
                return
            }
            adapterFileSelector = AdapterFileSelector(sdcard, Runnable {
                val file:File? = adapterFileSelector!!.selectedFile
                if (file != null) {
                    this.setResult(1, android.content.Intent().putExtra("file", file))
                    this.finish()
                }
            }, ProgressBarDialog(this))
            file_selector_list.adapter = adapterFileSelector
        } else {

        }
    }
}
