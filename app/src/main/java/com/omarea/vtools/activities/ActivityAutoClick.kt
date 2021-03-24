package com.omarea.vtools.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.CompoundButton
import com.omarea.store.SpfConfig
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.utils.AutoSkipCloudData
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_auto_click.*


class ActivityAutoClick : ActivityBase() {
    private lateinit var globalSPF: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto_click)

        setBackArrow()

        globalSPF = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        bindSPF(settings_auto_install, globalSPF, SpfConfig.GLOBAL_SPF_AUTO_INSTALL, false)
        bindSPF(settings_skip_ad, globalSPF, SpfConfig.GLOBAL_SPF_SKIP_AD, false)
        bindSPF(settings_skip_ad_precise, globalSPF, SpfConfig.GLOBAL_SPF_SKIP_AD_PRECISE, false)

        settings_skip_ad.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_SKIP_AD_PRECISE, false)) {
                    AutoSkipCloudData().updateConfig(context, true)
                }
            }
        }

        settings_skip_ad_precise.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AutoSkipCloudData().updateConfig(context, true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        title = getString(R.string.menu_auto_click)
    }

    private fun bindSPF(checkBox: CompoundButton, spf: SharedPreferences, prop: String, defValue: Boolean = false) {
        checkBox.isChecked = spf.getBoolean(prop, defValue)
        checkBox.setOnClickListener { view ->
            spf.edit().putBoolean(prop, (view as CompoundButton).isChecked).apply()
            if (AccessibleServiceHelper().serviceRunning(context)) {
                sendBroadcast(Intent(getString(R.string.scene_service_config_change_action)))
            }
        }
    }
}
