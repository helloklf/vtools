package com.omarea.vtools

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Toast
import com.omarea.shared.AccessibleServiceHelper
import com.omarea.shared.CommonCmds
import com.omarea.shared.SpfConfig
import com.omarea.shell.KeepShellPublic
import kotlinx.android.synthetic.main.activity_other_settings.*

class ActivitySceneOtherSettings : AppCompatActivity() {
    private lateinit var spf: SharedPreferences
    private var myHandler = Handler()
    private lateinit var globalSPF: SharedPreferences

    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()

        home_app_nightmode.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false)
        home_hide_in_recents.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT, false)

        val serviceState = AccessibleServiceHelper().serviceIsRunning(this)
        vtoolsserviceSettings!!.visibility = if (serviceState) View.VISIBLE else View.GONE

        settings_autoinstall.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_INSTALL, false)
        settings_delaystart.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_DELAY, false)
        settings_disable_selinux.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)
    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (spf.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false))
            this.setTheme(R.style.AppTheme_NoActionBarNight)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        globalSPF = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        home_hide_in_recents.setOnCheckedChangeListener({ _, checked ->
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT, checked).commit()
        })
        home_app_nightmode.setOnCheckedChangeListener({ _, checked ->
            if (globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false) == checked) {
                return@setOnCheckedChangeListener
            }
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, checked).commit()
            Toast.makeText(applicationContext, "此设置将在下次启动Scene时生效！", Toast.LENGTH_SHORT).show()
        })

        spf = this.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

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

    public override fun onPause() {
        super.onPause()
    }
}
