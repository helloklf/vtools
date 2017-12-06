package com.omarea.vboot

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.TextView
import com.omarea.shared.*
import com.omarea.shell.DynamicConfig
import kotlinx.android.synthetic.main.activity_accessibility_settings.*


class accessibility_settings : AppCompatActivity() {
    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()

        val serviceState = ServiceHelper.serviceIsRunning(applicationContext)
        vbootserviceSettings!!.visibility = if (serviceState) View.VISIBLE else View.GONE

        vbootservice_state.text = if (serviceState) application.getString(R.string.accessibility_running) else application.getString(R.string.accessibility_stoped)

        settings_autoinstall.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_INSTALL, false)
        settings_autobooster.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_BOOSTER, false)
        settings_dynamic.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, false)
        settings_debugmode.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_DEBUG, false)
        settings_delaystart.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_DELAY, false)
    }

    lateinit var spf: SharedPreferences;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accessibility_settings)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (!DynamicConfig().DynamicSupport(this)) {
            settings_dynamic.isEnabled = false
            settings_dynamic.isChecked = false
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, false).commit()
        }

        vbootservice_state.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        settings_delaystart.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DELAY, settings_delaystart.isChecked).commit()
        }
        settings_debugmode.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DEBUG, settings_debugmode.isChecked).commit()
        }
        settings_autoinstall.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_AUTO_INSTALL, settings_autoinstall.isChecked).commit()
        }
        settings_autobooster.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_AUTO_BOOSTER, settings_autobooster.isChecked).commit()
        }
        settings_dynamic.setOnClickListener {
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, settings_dynamic.isChecked).commit()
        }
    }

    public override fun onPause() {
        super.onPause()
    }
}
