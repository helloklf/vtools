package com.omarea.vboot

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import com.omarea.shared.Consts
import com.omarea.shared.ServiceHelper
import com.omarea.shared.SpfConfig
import com.omarea.shell.DynamicConfig
import kotlinx.android.synthetic.main.activity_accessibility_settings.*
import java.io.File

class ActivityAccessibilitySettings : AppCompatActivity() {
    private lateinit var spf: SharedPreferences

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
        accessbility_notify.isChecked = spf.getBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, true)
    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accessibility_settings)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        spf = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (!DynamicConfig().DynamicSupport(this) && !File(Consts.POWER_CFG_PATH).exists()) {
            settings_dynamic.isEnabled = false
            settings_dynamic.isChecked = false
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, false).commit()
        }

        vbootservice_state.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        settings_delaystart.setOnCheckedChangeListener({
            _,checked ->
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DELAY, checked).commit()
        })
        settings_debugmode.setOnCheckedChangeListener({
            _,checked ->
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DEBUG, checked).commit()
        })
        settings_autoinstall.setOnCheckedChangeListener({
            _,checked ->
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_AUTO_INSTALL, checked).commit()
        })
        settings_autobooster.setOnCheckedChangeListener({
            _,checked ->
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_AUTO_BOOSTER, checked).commit()
        })
        settings_dynamic.setOnCheckedChangeListener({
            _,checked ->
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, checked).commit()
        })
        accessbility_notify.setOnCheckedChangeListener({
            _,checked ->
            spf.edit().putBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, checked).commit()
        })
    }

    public override fun onPause() {
        super.onPause()
    }
}
