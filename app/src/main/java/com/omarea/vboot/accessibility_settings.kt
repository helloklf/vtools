package com.omarea.vboot

import android.content.Intent
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

        servicceState.text = if (serviceState) application.getString(R.string.accessibility_running) else application.getString(R.string.accessibility_stoped)
    }

    lateinit var servicceState: TextView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accessibility_settings)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        servicceState = findViewById(R.id.vbootservice_state) as TextView

        servicceState.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }


        settings_autoinstall.isChecked = ConfigInfo.getConfigInfo().AutoInstall
        settings_autobooster.isChecked = ConfigInfo.getConfigInfo().AutoBooster
        settings_dynamic.isChecked = ConfigInfo.getConfigInfo().DyamicCore
        if (!DynamicConfig().DynamicSupport(ConfigInfo.getConfigInfo().CPUName)) {
            settings_dynamic.isEnabled = false
            settings_dynamic.isChecked = false
        }

        settings_debugmode.isChecked = ConfigInfo.getConfigInfo().DebugMode
        settings_qc.isChecked = ConfigInfo.getConfigInfo().QcMode
        settings_delaystart.isChecked = ConfigInfo.getConfigInfo().DelayStart
        settings_bp.isChecked = ConfigInfo.getConfigInfo().BatteryProtection

        settings_bp.setOnClickListener {
            ConfigInfo.getConfigInfo().BatteryProtection = settings_bp.isChecked;
            if (!settings_bp.isChecked)
                ShellRuntime().execute(Consts.BPReset)
        }
        settings_delaystart.setOnClickListener { ConfigInfo.getConfigInfo().DelayStart = settings_delaystart.isChecked }
        settings_debugmode.setOnClickListener { ConfigInfo.getConfigInfo().DebugMode = settings_debugmode.isChecked }
        settings_autoinstall.setOnClickListener { ConfigInfo.getConfigInfo().AutoInstall = settings_autoinstall.isChecked }
        settings_autobooster.setOnClickListener { ConfigInfo.getConfigInfo().AutoBooster = settings_autobooster.isChecked }
        settings_dynamic.setOnClickListener {
            ConfigInfo.getConfigInfo().DyamicCore = settings_dynamic.isChecked
            EventBus.publish(Events.DyamicCoreConfigChanged)
        }
        settings_qc.setOnClickListener { ConfigInfo.getConfigInfo().QcMode = settings_qc.isChecked }

        if (cmd_shellTools(null, null).GetProp("/sys/class/power_supply/battery/battery_charging_enabled") == null) {
            settings_bp.visibility = View.GONE
            settings_bp_desc.visibility = View.GONE
            ConfigInfo.getConfigInfo().BatteryProtection = false
        }
    }

    public override fun onPause() {
        ConfigInfo.getConfigInfo().saveChange()

        super.onPause()
    }

    override fun onDestroy() {
        ConfigInfo.getConfigInfo().saveChange()

        super.onDestroy()
    }
}
