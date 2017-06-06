package com.omarea.vboot

import android.content.Context
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.omarea.shared.AppShared
import com.omarea.shared.cmd_shellTools
import java.io.UnsupportedEncodingException

class rom2zip : AppCompatActivity() {
    lateinit internal var thisview: AppCompatActivity
    lateinit internal var progressBar: ProgressBar
    lateinit internal var cmdshellTools: cmd_shellTools
    lateinit internal var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this

        setContentView(R.layout.activity_rom2zip)
        val toolbar = findViewById(R.id.rom2ziptoolbar1) as Toolbar
        setSupportActionBar(toolbar)
        thisview = this
        progressBar = findViewById(R.id.rom2zipprogressBar) as ProgressBar
        cmdshellTools = cmd_shellTools(thisview, progressBar)

        val assetManager = assets
        AppShared.WriteFile(assetManager, "rom.zip", true)
        AppShared.WriteFile(assetManager, "romvboot.zip", true)
        AppShared.WriteFile(assetManager, "zip.zip", false)

        val rom2zip_boot = findViewById(R.id.rom2zip_boot) as CheckBox
        val rom2zip_sys = findViewById(R.id.rom2zip_sys) as CheckBox
        val rom2zip_rec = findViewById(R.id.rom2zip_rec) as CheckBox
        val rom2zip_other = findViewById(R.id.rom2zip_other) as CheckBox
        val rom2zip_name = findViewById(R.id.rom2zip_name) as EditText
        val rom2zip_needsize = findViewById(R.id.rom2zip_needsize) as TextView

        val clickListener = View.OnClickListener {
            var size = 0
            if (rom2zip_boot.isChecked) {
                size += 100
            }
            if (rom2zip_rec.isChecked) {
                size += 100
            }
            if (rom2zip_sys.isChecked) {
                size += 2750
            }
            if (rom2zip_other.isChecked) {
                size += 150
            }
            rom2zip_needsize.text = "大约需要空间：" + size + "MB"
        }
        rom2zip_boot.setOnClickListener(clickListener)
        rom2zip_sys.setOnClickListener(clickListener)
        rom2zip_rec.setOnClickListener(clickListener)
        rom2zip_other.setOnClickListener(clickListener)

        val btn = findViewById(R.id.rom2zipCommitBtn) as FloatingActionButton
        btn.setOnClickListener(View.OnClickListener {
            btn.isEnabled = false
            try {
                cmdshellTools.Rom2Zip(rom2zip_boot.isChecked, rom2zip_sys.isChecked, rom2zip_rec.isChecked, rom2zip_other.isChecked, rom2zip_name.text.toString().trim { it <= ' ' })
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        })
    }

}
