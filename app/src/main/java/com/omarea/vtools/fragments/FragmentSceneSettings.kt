package com.omarea.vtools.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.omarea.store.SpfConfig
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.utils.AutoSkipCloudData
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.fragment_scene_settings.*

class FragmentSceneSettings : androidx.fragment.app.Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scene_settings, container, false)
    }

    private lateinit var globalSPF: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        bindSPF(settings_auto_install, globalSPF, SpfConfig.GLOBAL_SPF_AUTO_INSTALL, false)
        bindSPF(settings_skip_ad, globalSPF, SpfConfig.GLOBAL_SPF_SKIP_AD, false)
        bindSPF(settings_skip_ad_precise, globalSPF, SpfConfig.GLOBAL_SPF_SKIP_AD_PRECISE, false)
        bindSPF(settings_skip_ad_delay, globalSPF, SpfConfig.GLOBAL_SPF_SKIP_AD_DELAY, true)

        settings_skip_ad_precise.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AutoSkipCloudData().updateConfig(context!!, true)
            }
        }
    }

    private fun bindSPF(checkBox: CompoundButton, spf: SharedPreferences, prop: String, defValue: Boolean = false) {
        checkBox.isChecked = spf.getBoolean(prop, defValue)
        checkBox.setOnClickListener { view ->
            spf.edit().putBoolean(prop, (view as CompoundButton).isChecked).apply()
            if (AccessibleServiceHelper().serviceRunning(context!!)) {
                context!!.sendBroadcast(Intent(getString(R.string.scene_service_config_change_action)))
            }
        }
    }
}
