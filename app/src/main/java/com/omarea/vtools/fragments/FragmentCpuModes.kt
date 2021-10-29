package com.omarea.vtools.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.omarea.Scene
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ThemeMode
import com.omarea.data.EventBus
import com.omarea.data.EventType
import com.omarea.krscript.model.PageNode
import com.omarea.library.shell.ThermalDisguise
import com.omarea.permissions.CheckRootStatus
import com.omarea.scene_mode.CpuConfigInstaller
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.SpfConfig
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.vtools.R
import com.omarea.vtools.activities.*
import com.projectkr.shell.OpenPageHelper
import kotlinx.android.synthetic.main.fragment_cpu_modes.*

class FragmentCpuModes : Fragment() {
    private var author: String = ""
    private var configFileInstalled: Boolean = false
    private lateinit var modeSwitcher: ModeSwitcher
    private lateinit var globalSPF: SharedPreferences
    private lateinit var themeMode: ThemeMode

    companion object {
        fun createPage(themeMode: ThemeMode): Fragment {
            val fragment = FragmentCpuModes()
            fragment.themeMode = themeMode;
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_cpu_modes, container, false)

    private fun startService() {
        Scene.toast(getString(R.string.accessibility_please_activate), Toast.LENGTH_SHORT)
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        modeSwitcher = ModeSwitcher()

        bindMode(cpu_config_p0, ModeSwitcher.POWERSAVE)
        bindMode(cpu_config_p1, ModeSwitcher.BALANCE)
        bindMode(cpu_config_p2, ModeSwitcher.PERFORMANCE)
        bindMode(cpu_config_p3, ModeSwitcher.FAST)

        dynamic_control.setOnClickListener {
            val value = (it as Switch).isChecked
            if (value && !(modeSwitcher.modeConfigCompleted())) {
                it.isChecked = false
                DialogHelper.alert(context!!, getString(R.string.sorry), getString(R.string.schedule_unfinished))
            } else if (value && !AccessibleServiceHelper().serviceRunning(context!!)) {
                it.isChecked = false
                startService()
            } else {
                globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, value).apply()
                reStartService()
            }

        }
        dynamic_control.setOnCheckedChangeListener { _, isChecked ->
            dynamic_control_opts.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        strict_mode.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_STRICT, false)
        strict_mode.setOnClickListener {
            val checked = (it as CompoundButton).isChecked
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_STRICT, checked).apply()
        }

        delay_switch.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DELAY, false)
        delay_switch.setOnClickListener {
            val checked = (it as CompoundButton).isChecked
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DELAY, checked).apply()
        }

        first_mode.run {
            when (globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, ModeSwitcher.BALANCE)) {
                ModeSwitcher.POWERSAVE -> setSelection(0)
                ModeSwitcher.BALANCE -> setSelection(1)
                ModeSwitcher.PERFORMANCE -> setSelection(2)
                ModeSwitcher.FAST -> setSelection(3)
                ModeSwitcher.IGONED -> setSelection(4)
            }

            onItemSelectedListener = ModeOnItemSelectedListener(globalSPF) {
                reStartService()
            }
        }

        sleep_mode.run {
            when (globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_SLEEP_MODE, ModeSwitcher.POWERSAVE)) {
                ModeSwitcher.POWERSAVE -> setSelection(0)
                ModeSwitcher.BALANCE -> setSelection(1)
                ModeSwitcher.PERFORMANCE -> setSelection(2)
                ModeSwitcher.IGONED -> setSelection(3)
            }
            onItemSelectedListener = ModeOnItemSelectedListener2(globalSPF) {
            }
        }

        val sourceClick = object : View.OnClickListener {
            override fun onClick(it: View) {
                if (configInstaller.outsideConfigInstalled()) {
                    if (configInstaller.dynamicSupport(context!!)) {
                        DialogHelper.warning(
                            activity!!,
                            getString(R.string.make_choice),
                            getString(R.string.schedule_remove_outside),
                            {
                                configInstaller.removeOutsideConfig()
                                reStartService()
                                updateState()
                                chooseConfigSource()
                            })
                    } else {
                        Scene.toast(getString(R.string.schedule_unofficial), Toast.LENGTH_LONG)
                    }
                } else if (configInstaller.dynamicSupport(context!!)) {
                    chooseConfigSource()
                } else {
                    Scene.toast(getString(R.string.schedule_unsupported), Toast.LENGTH_LONG)
                }
            }
        }
        config_author_icon.setOnClickListener(sourceClick)
        config_author.setOnClickListener(sourceClick)

        nav_battery_stats.setOnClickListener {
            val intent = Intent(context, ActivityBatteryStats::class.java)
            startActivity(intent)
        }
        nav_app_scene.setOnClickListener {
            if (!AccessibleServiceHelper().serviceRunning(context!!)) {
                startService()
            } else if (dynamic_control.isChecked) {
                val intent = Intent(context, ActivityAppConfig2::class.java)
                startActivity(intent)
            } else {
                DialogHelper.warning(
                        activity!!,
                        getString(R.string.please_notice),
                        getString(R.string.schedule_dynamic_off), {
                    val intent = Intent(context, ActivityAppConfig2::class.java)
                    startActivity(intent)
                })
            }
        }
        // 激活辅助服务按钮
        nav_scene_service_not_active.setOnClickListener {
            startService()
        }
        // 自动跳过广告
        nav_skip_ad.setOnClickListener {
            if (AccessibleServiceHelper().serviceRunning(context!!)) {
                val intent = Intent(context, ActivityAutoClick::class.java)
                startActivity(intent)
            } else {
                startService()
            }
        }
        if (CheckRootStatus.lastCheckResult) {
            nav_more.visibility = View.VISIBLE
            nav_thermal.setOnClickListener {
                val pageNode = PageNode("").apply {
                    title = "温控配置"
                    pageConfigPath = "file:///android_asset/kr-script/miui/miui.xml"
                }
                OpenPageHelper(activity!!).openPage(pageNode)
            }
            nav_processes.setOnClickListener {
                val intent = Intent(context, ActivityProcess::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            nav_freeze.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setClassName(
                        "com.omarea.vtools", "com.omarea.vtools.activities.ActivityFreezeApps2")
                startActivity(intent)
            }
        }

        if (!modeSwitcher.modeConfigCompleted() && configInstaller.dynamicSupport(context!!)) {
            installConfig(false)
        }
        // 卓越性能 目前仅限888处理器开放
        extreme_performance.visibility = if (ThermalDisguise().supported()) View.VISIBLE else View.GONE
        extreme_performance_on.setOnClickListener {
            val isChecked = (it as CompoundButton).isChecked
            if (isChecked) {
                ThermalDisguise().disableMessage()
            } else {
                ThermalDisguise().resumeMessage()
            }
        }
    }

    // 选择配置来源
    private fun chooseConfigSource() {
        val view = layoutInflater.inflate(R.layout.dialog_powercfg_source, null)
        val dialog = DialogHelper.customDialog(activity!!, view)

        val conservative = view.findViewById<View>(R.id.source_official_conservative)
        val active = view.findViewById<View>(R.id.source_official_active)

        val cpuConfigInstaller = CpuConfigInstaller()
        if (cpuConfigInstaller.dynamicSupport(context!!)) {
            conservative.setOnClickListener {
                if (configInstaller.outsideConfigInstalled()) {
                    configInstaller.removeOutsideConfig()
                }
                installConfig(false)

                dialog.dismiss()
            }
            active.setOnClickListener {
                if (configInstaller.outsideConfigInstalled()) {
                    configInstaller.removeOutsideConfig()
                }
                installConfig(true)

                dialog.dismiss()
            }
        } else {
            conservative.visibility = View.GONE
            active.visibility = View.GONE
        }
    }

    private fun bindSPF(checkBox: CompoundButton, spf: SharedPreferences, prop: String, defValue: Boolean = false, restartService: Boolean = false) {
        checkBox.isChecked = spf.getBoolean(prop, defValue)
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            spf.edit().putBoolean(prop, isChecked).apply()
            if (restartService) {
                reStartService()
            }
        }
    }

    private class ModeOnItemSelectedListener(private var globalSPF: SharedPreferences, private var runnable: Runnable) : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
        }

        @SuppressLint("ApplySharedPref")
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            var mode = ModeSwitcher.DEFAULT
            when (position) {
                0 -> mode = ModeSwitcher.POWERSAVE
                1 -> mode = ModeSwitcher.BALANCE
                2 -> mode = ModeSwitcher.PERFORMANCE
                3 -> mode = ModeSwitcher.FAST
                4 -> mode = ModeSwitcher.IGONED
            }
            if (globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, ModeSwitcher.DEFAULT) != mode) {
                globalSPF.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, mode).commit()
                runnable.run()
            }
        }
    }

    private class ModeOnItemSelectedListener2(private var globalSPF: SharedPreferences, private var runnable: Runnable) : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
        }

        @SuppressLint("ApplySharedPref")
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            var mode = ModeSwitcher.POWERSAVE
            when (position) {
                0 -> mode = ModeSwitcher.POWERSAVE
                1 -> mode = ModeSwitcher.BALANCE
                2 -> mode = ModeSwitcher.PERFORMANCE
                3 -> mode = ModeSwitcher.IGONED
            }
            if (globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_SLEEP_MODE, ModeSwitcher.POWERSAVE) != mode) {
                globalSPF.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG_SLEEP_MODE, mode).commit()
                runnable.run()
            }
        }
    }

    private fun bindMode(button: View, mode: String) {
        button.setOnClickListener {
            if (mode == ModeSwitcher.FAST && ModeSwitcher.getCurrentSource() == ModeSwitcher.SOURCE_OUTSIDE_UPERF) {
                DialogHelper.warning(
                        activity!!,
                        getString(R.string.please_notice),
                        getString(R.string.schedule_uperf_fast),
                        {
                            modeSwitcher.executePowercfgMode(mode, context!!.packageName)
                            updateState(cpu_config_p3, ModeSwitcher.FAST)
                        }
                )
            } else {
                modeSwitcher.executePowercfgMode(mode, context!!.packageName)
                updateState(cpu_config_p0, ModeSwitcher.POWERSAVE)
                updateState(cpu_config_p1, ModeSwitcher.BALANCE)
                updateState(cpu_config_p2, ModeSwitcher.PERFORMANCE)
                updateState(cpu_config_p3, ModeSwitcher.FAST)
            }
        }
    }

    private fun updateState() {
        val outsideInstalled = configInstaller.outsideConfigInstalled()
        configFileInstalled = outsideInstalled || configInstaller.insideConfigInstalled()
        author = ModeSwitcher.getCurrentSource()

        config_author.text = ModeSwitcher.getCurrentSourceName()

        updateState(cpu_config_p0, ModeSwitcher.POWERSAVE)
        updateState(cpu_config_p1, ModeSwitcher.BALANCE)
        updateState(cpu_config_p2, ModeSwitcher.PERFORMANCE)
        updateState(cpu_config_p3, ModeSwitcher.FAST)
        val serviceState = AccessibleServiceHelper().serviceRunning(context!!)
        val dynamicControl = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)
        dynamic_control.isChecked = dynamicControl && serviceState
        nav_scene_service_not_active.visibility = if (serviceState) View.GONE else View.VISIBLE

        if (dynamicControl && !modeSwitcher.modeConfigCompleted()) {
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, false).apply()
            dynamic_control.isChecked = false
            reStartService()
        }
        dynamic_control_opts.visibility = if (dynamic_control.isChecked) View.VISIBLE else View.GONE
        extreme_performance_on.isChecked = ThermalDisguise().isDisabled()
    }

    private fun updateState(button: View, mode: String) {
        val isCurrent = ModeSwitcher.getCurrentPowerMode() == mode
        button.alpha = if (configFileInstalled && isCurrent) 1f else 0.4f
    }

    override fun onResume() {
        super.onResume()

        val currentAuthor = author
        updateState()

        // 如果开启了动态响应 并且配置作者变了，重启后台服务
        if (dynamic_control.isChecked && !currentAuthor.isEmpty() && currentAuthor != author) {
            reStartService()
        }
    }

    private val configInstaller = CpuConfigInstaller()

    private fun openUrl(link: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (ex: Exception) {
        }
    }

    //安装调频文件
    private fun installConfig(active: Boolean) {
        if (!configInstaller.dynamicSupport(context!!)) {
            Scene.toast(R.string.not_support_config, Toast.LENGTH_LONG)
            return
        }

        configInstaller.installOfficialConfig(context!!, "", active)
        configInstalled()
    }

    private fun configInstalled() {
        updateState()
        reStartService()
    }

    /**
     * 重启辅助服务
     */
    private fun reStartService() {
        EventBus.publish(EventType.SERVICE_UPDATE)
    }
}