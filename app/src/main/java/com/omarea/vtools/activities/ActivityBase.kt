package com.omarea.vtools.activities

import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.omarea.Scene
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.ThemeMode
import com.omarea.vtools.R

open class ActivityBase : AppCompatActivity() {
    public lateinit var themeMode: ThemeMode
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        this.themeMode = ThemeSwitch.switchTheme(this)
        super.onCreate(savedInstanceState, persistentState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.themeMode = ThemeSwitch.switchTheme(this)
        super.onCreate(savedInstanceState)
    }

    protected val context: Context
        get() {
            return this
        }

    protected fun setBackArrow() {
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        // 显示返回按钮
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            this.onBackPressed()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Scene.postDelayed({
            System.gc()
        }, 500)
        if (isTaskRoot) {
            Scene.postDelayed({
                KeepShellPublic.doCmdSync("dumpsys meminfo " + context.packageName + " > /dev/null")
            }, 100)
        }
    }
}