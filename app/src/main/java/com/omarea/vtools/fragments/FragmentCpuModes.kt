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
import com.omarea.scene_mode.CpuConfigInstaller
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.SpfConfig
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.vtools.R
import com.omarea.vtools.activities.ActivityAppConfig2
import com.omarea.vtools.activities.ActivityBatteryStats
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
        Scene.toast("请在系统设置里激活[Scene 辅助服务]选项", Toast.LENGTH_SHORT)
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
                DialogHelper.alert(context!!, "提示", "在安装有效的调度配置之前，你不能开启此功能~")
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

        cpu_mode_delete_outside.setOnClickListener {
            DialogHelper.confirm(activity!!,
                    "确定删除?",
                    "确定删除安装在 /data/powercfg.sh 的外部配置脚本吗？\n它可能是Scene2遗留下来的，也可能是其它优化模块创建的\n（删除后建议重启手机一次）",
                    {
                        configInstaller.removeOutsideConfig()
                        cpu_mode_outside.visibility = View.GONE
                        reStartService()
                        updateState()
                    })
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
                    Scene.toast("你需要删除外部配置，才能选择其它配置源", Toast.LENGTH_LONG)
                } else if (configInstaller.dynamicSupport(context!!)) {
                    chooseConfigSource()
                } else {
                    Scene.toast("Scene Core Edition自带的调度策略尚未适配当前SOC~", Toast.LENGTH_LONG)
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
                DialogHelper.warning(activity!!, "请注意", "你未开启[动态响应]，Scene将根据前台应用变化调节设备性能！", {
                    val intent = Intent(context, ActivityAppConfig2::class.java)
                    startActivity(intent)
                })
            }
        }
        // 激活辅助服务按钮
        nav_scene_service_not_active.setOnClickListener {
            startService()
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
                // TODO:改为清空此前的所有自定义配置，而不仅仅是外部配置
                if (outsideOverrided()) {
                    configInstaller.removeOutsideConfig()
                }
                installConfig(false)

                dialog.dismiss()
            }
            active.setOnClickListener {
                // TODO:改为清空此前的所有自定义配置，而不仅仅是外部配置
                if (outsideOverrided()) {
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


    private fun outsideOverrided(): Boolean {
        if (configInstaller.outsideConfigInstalled()) {
            DialogHelper.alert(activity!!, "提示", "你需要先删除外部配置，因为Scene会优先使用它！")
            return true
        }
        return false
    }

    private fun bindMode(button: View, mode: String) {
        button.setOnClickListener {
            if (mode == ModeSwitcher.FAST && ModeSwitcher.getCurrentSource() == ModeSwitcher.SOURCE_OUTSIDE_UPERF) {
                DialogHelper.warning(
                        activity!!,
                        "提示说明",
                        "Uperf调度仅提供了[卡顿/均衡/费电]三种模式，分别于Scene的[省电/均衡/性能]对应。\n而在Scene里选择[极速]模式时，实际生效为[费电]，即与选择 [性能]模式相同",
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

        if (outsideInstalled && configInstaller.dynamicSupport(context!!)) {
            cpu_mode_outside.visibility = View.VISIBLE
        } else {
            cpu_mode_outside.visibility = View.GONE
        }

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
    }

    private fun updateState(button: View, mode: String) {
        val isCurrent = modeSwitcher.getCurrentPowerMode() == mode
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

        if (globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)) {
            Scene.toast(getString(R.string.config_installed), Toast.LENGTH_LONG)
            reStartService()
        } else {
            DialogHelper.confirm(
                    activity!!,
                    "",
                    "配置脚本已安装，是否开启 [动态响应] ？",
                    {
                        dynamic_control.isChecked = true
                        globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, true).apply()
                        reStartService()
                    })
        }
    }

    /**
     * 重启辅助服务
     */
    private fun reStartService() {
        if (AccessibleServiceHelper().serviceRunning(context!!)) {
            context!!.sendBroadcast(Intent(context!!.getString(R.string.scene_change_action)))
        }
    }
}