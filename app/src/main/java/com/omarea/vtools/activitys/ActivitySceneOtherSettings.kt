package com.omarea.vtools.activitys

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import com.omarea.shared.helper.AccessibleServiceHelper
import com.omarea.shared.CommonCmds
import com.omarea.shared.SpfConfig
import com.omarea.shell.KeepShellPublic
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_other_settings.*

class ActivitySceneOtherSettings : AppCompatActivity() {
    private lateinit var spf: SharedPreferences
    private var myHandler = Handler()

    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()

        home_hide_in_recents.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT, false)

        val serviceState = AccessibleServiceHelper().serviceIsRunning(this)
        vtoolsserviceSettings!!.visibility = if (serviceState) View.VISIBLE else View.GONE

        settings_autoinstall.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_INSTALL, false)
        settings_delaystart.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_DELAY, false)
        settings_disable_selinux.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)
    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        ThemeSwitch.switchTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        home_hide_in_recents.setOnCheckedChangeListener({ _, checked ->
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT, checked).commit()
        })

        settings_delaystart.setOnCheckedChangeListener({ _, checked ->
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DELAY, checked).commit()
        })
        settings_autoinstall.setOnCheckedChangeListener({ _, checked ->
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_AUTO_INSTALL, checked).commit()
        })
        settings_disable_selinux.setOnClickListener {
            if (settings_disable_selinux.isChecked) {
                KeepShellPublic.doCmdSync(CommonCmds.DisableSELinux)
                myHandler.postDelayed({
                    spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, settings_disable_selinux.isChecked).commit()
                }, 10000)
            } else {
                KeepShellPublic.doCmdSync(CommonCmds.ResumeSELinux)
                spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, settings_disable_selinux.isChecked).commit()
            }
        }
    }

    fun onThemeClick(view: View) {
        val tag = view.tag.toString().toInt()

        spf.edit().putInt(SpfConfig.GLOBAL_SPF_THEME, tag).commit()
        this.recreate()
    }

    override fun onDestroy() {
        super.onDestroy()

        spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, settings_disable_selinux.isChecked).commit()
    }

    public override fun onPause() {
        super.onPause()
    }
}
