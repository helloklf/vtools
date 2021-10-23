package com.omarea.vtools.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.CompoundButton
import com.omarea.common.ui.AdapterAppChooser
import com.omarea.common.ui.DialogAppChooser
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.data.EventBus
import com.omarea.data.EventType
import com.omarea.store.SpfConfig
import com.omarea.utils.AppListHelper
import com.omarea.utils.AutoSkipCloudData
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_auto_click.*


class ActivityAutoClick : ActivityBase() {
    private lateinit var processBarDialog: ProgressBarDialog
    private lateinit var globalSPF: SharedPreferences
    internal val myHandler: Handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto_click)

        setBackArrow()
        processBarDialog = ProgressBarDialog(this)

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

        ad_skip_blacklist.setOnClickListener {
            adBlackListConfig()
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
            EventBus.publish(EventType.SERVICE_UPDATE)
        }
    }


    // 跳过广告黑名单应用
    private fun adBlackListConfig() {
        processBarDialog.showDialog()
        Thread {
            val configFile = context.getSharedPreferences(SpfConfig.AUTO_SKIP_BLACKLIST, Context.MODE_PRIVATE)
            val options = AppListHelper(context).getBootableApps(null, true).sortedBy {
                it.packageName
            }.map {
                it.apply {
                    selected = configFile.getBoolean(packageName, false)
                }
            }

            myHandler.post {
                processBarDialog.hideDialog()

                DialogAppChooser(
                        themeMode.isDarkMode,
                        ArrayList(options),
                        true,
                        object : DialogAppChooser.Callback {
                    override fun onConfirm(apps: List<AdapterAppChooser.AppInfo>) {
                        val items = apps.map { it.packageName }
                        options.forEach {
                            it.selected = items.contains(it.packageName)
                        }
                        configFile.edit().clear().run {
                            apps.forEach {
                                if (it.selected) {
                                    putBoolean(it.packageName, true)
                                }
                            }
                            apply()
                        }

                    }
                }).show(supportFragmentManager, "standby_apps")
            }
        }.start()
    }

}
