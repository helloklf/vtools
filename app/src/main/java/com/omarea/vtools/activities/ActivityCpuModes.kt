package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.omarea.Scene
import com.omarea.common.ui.DialogHelper
import com.omarea.scene_mode.CpuConfigInstaller
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.SpfConfig
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_cpu_modes.*
import java.io.File
import java.nio.charset.Charset
import java.util.*


class ActivityCpuModes : ActivityBase() {
    private var author: String = ""
    private var configFileInstalled: Boolean = false
    private lateinit var modeSwitcher: ModeSwitcher
    private lateinit var globalSPF: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cpu_modes)

        setBackArrow()
        onViewCreated()
    }

    private fun onViewCreated() {
        globalSPF = getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        modeSwitcher = ModeSwitcher()

        bindMode(cpu_config_p0, ModeSwitcher.POWERSAVE)
        bindMode(cpu_config_p1, ModeSwitcher.BALANCE)
        bindMode(cpu_config_p2, ModeSwitcher.PERFORMANCE)
        bindMode(cpu_config_p3, ModeSwitcher.FAST)

        config_customer_powercfg.setOnClickListener {
            if (!outsideOverrided()) {
                chooseLocalConfig()
            }
        }
        config_customer_powercfg_online.setOnClickListener {
            if (!outsideOverrided()) {
                getOnlineConfig()
            }
        }
        checkConfig()
        dynamic_control.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)
        dynamic_control_opts.visibility = if (dynamic_control.isChecked) View.VISIBLE else View.GONE
        dynamic_control.setOnClickListener {
            val value = (it as Switch).isChecked
            if (value && !(modeSwitcher.modeConfigCompleted())) {
                it.isChecked = false
                DialogHelper.helpInfo(context,
                        "设置向导",
                        "在使用此功能前，你需要先完成一些配置~") {
                    chooseConfigSource()
                }
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
            DialogHelper.animDialog(AlertDialog.Builder(context).setTitle("确定删除?")
                    .setMessage("确定删除安装在 /data/powercfg.sh 的外部配置脚本吗？\n它可能是Scene2遗留下来的，也可能是其它优化模块创建的")
                    .setPositiveButton(R.string.btn_confirm) { _, _ ->
                        configInstaller.removeOutsideConfig()
                        cpu_mode_outside.visibility = View.GONE
                        reStartService()
                        updateState()
                    }.setNegativeButton(R.string.btn_cancel, { _, _ -> }))
        }

        val modeValue = globalSPF.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, "balance")
        when (modeValue) {
            ModeSwitcher.POWERSAVE -> first_mode.setSelection(0)
            ModeSwitcher.BALANCE -> first_mode.setSelection(1)
            ModeSwitcher.PERFORMANCE -> first_mode.setSelection(2)
            ModeSwitcher.FAST -> first_mode.setSelection(3)
            ModeSwitcher.IGONED -> first_mode.setSelection(4)
        }
        first_mode.onItemSelectedListener = ModeOnItemSelectedListener(globalSPF) {
            reStartService()
        }

        config_author.setOnClickListener {
            if (configInstaller.outsideConfigInstalled()) {
                Snackbar.make(it, "你需要删除外部配置，才能选择其它配置源", Snackbar.LENGTH_LONG).show()
            } else {
                chooseConfigSource()
            }
        }

        home_quick_switch.isChecked = globalSPF.getBoolean(SpfConfig.HOME_QUICK_SWITCH, true)
        home_quick_switch.setOnClickListener {
            globalSPF.edit().putBoolean(SpfConfig.HOME_QUICK_SWITCH, (it as CompoundButton).isChecked).apply()
        }
    }

    // 选择配置来源
    private fun chooseConfigSource () {
        val view = layoutInflater.inflate(R.layout.layout_powercfg_source, null)
        val dialog = DialogHelper.customDialog(this, view)

        val conservative = view.findViewById<View>(R.id.source_official_conservative)
        val active = view.findViewById<View>(R.id.source_official_active)

        val cpuConfigInstaller = CpuConfigInstaller()
        if (cpuConfigInstaller.dynamicSupport(this)) {
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

        view.findViewById<View>(R.id.source_import).setOnClickListener {
            chooseLocalConfig()
        }
        view.findViewById<View>(R.id.source_download).setOnClickListener {
            // TODO:改为清空此前的所有自定义配置，而不仅仅是外部配置
            if (outsideOverrided()) {
                configInstaller.removeOutsideConfig()
            }

            getOnlineConfig()
        }
        view.findViewById<View>(R.id.source_custom).setOnClickListener {
            // TODO:改为清空此前的所有自定义配置，而不仅仅是外部配置
            if (outsideOverrided()) {
                configInstaller.removeOutsideConfig()
            }
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


    private fun outsideOverrided(): Boolean {
        if (configInstaller.outsideConfigInstalled()) {
            DialogHelper.helpInfo(context, "你需要先删除外部配置，因为Scene3会优先使用它！")
            return true
        }
        return false
    }

    private fun bindMode(button: View, mode: String) {
        button.setOnClickListener {
            if (modeSwitcher.anyModeReplaced()) {
                modifyCpuConfig(mode)
            } else {
                Snackbar.make(it, "如需自定义各个模式的参数，请先将配置源切换为“自定义”", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateState() {
        val outsideInstalled = configInstaller.outsideConfigInstalled()
        configFileInstalled = outsideInstalled || configInstaller.insideConfigInstalled()

        if (ModeSwitcher().anyModeReplaced()) {
            author = "custom"
        } else if (outsideInstalled) {
            author = "outside"
        } else if (configFileInstalled) {
            author = globalSPF.getString(SpfConfig.GLOBAL_SPF_CPU_CONFIG_AUTHOR, "unknown")!!
        } else {
            author = "none"
        }

        if (author == "none") {
            quick_switch.visibility = View.GONE
        } else {
            quick_switch.visibility = View.VISIBLE
        }

        if (outsideInstalled) {
            cpu_mode_outside.visibility = View.VISIBLE
        } else {
            cpu_mode_outside.visibility = View.GONE
        }

        config_author.text = getProviderName(author)

        updateState(cpu_config_p0, ModeSwitcher.POWERSAVE)
        updateState(cpu_config_p1, ModeSwitcher.BALANCE)
        updateState(cpu_config_p2, ModeSwitcher.PERFORMANCE)
        updateState(cpu_config_p3, ModeSwitcher.FAST)

        if (globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT) && !modeSwitcher.modeConfigCompleted()) {
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, false).apply()
            dynamic_control.isChecked = false
            reStartService()
        }
    }

    private fun getProviderName(name: String): String {
        return when (name.toLowerCase(Locale.getDefault())) {
            "scene" -> "Scene自带"
            "downloader" -> "在线下载"
            "outside" -> "外部配置"
            "custom" -> "自定义"
            "import-file" -> "外部导入"
            "none" -> "未配置"
            else -> name
        }
    }

    private fun updateState(button: View, mode: String) {
        button.alpha = if ((configFileInstalled && author != "custom") || modeSwitcher.modeReplaced(mode)) 1f else 0.4f
    }

    override fun onResume() {
        super.onResume()
        title = getString(R.string.menu_cpu_modes)

        val currentAuthor = author

        updateState()

        // 如果开启了动态响应 并且配置作者变了，重启后台服务
        if (dynamic_control.isChecked && !currentAuthor.isNullOrEmpty() && currentAuthor != author) {
            reStartService()
        }
    }

    private fun modifyCpuConfig(mode: String) {
        val intent = Intent(context, ActivityCpuControl::class.java)
        intent.putExtra("cpuModeName", mode)
        startActivity(intent)
    }

    private val REQUEST_POWERCFG_FILE = 1
    private val REQUEST_POWERCFG_ONLINE = 2
    private val configInstaller = CpuConfigInstaller()
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_POWERCFG_FILE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                if (data.extras?.containsKey("file") != true) {
                    return
                }
                val path = data.extras!!.getString("file")!!
                val file = File(path)
                if (file.exists()) {
                    if (file.length() > 200 * 1024) {
                        Toast.makeText(context, "这个文件也太大了，配置脚本大小不能超过200KB！", Toast.LENGTH_LONG).show()
                        return
                    }
                    val lines = file.readText(Charset.defaultCharset()).replace("\r", "")
                    val configStar = lines.split("\n").firstOrNull()
                    if (configStar != null && configStar.startsWith("#!/") && configStar.endsWith("sh")) {
                        if (configInstaller.installCustomConfig(context, lines, "import-file")) {
                            configInstalled()
                        } else {
                            Toast.makeText(context, "由于某些原因，安装配置脚本失败，请重试！", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "这似乎是个无效的脚本文件！", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "所选的文件没找到！", Toast.LENGTH_LONG).show()
                }
            }
            return
        } else if (requestCode == REQUEST_POWERCFG_ONLINE) {
            if (resultCode == Activity.RESULT_OK) {
                configInstalled()
            }
        }
    }

    //检查配置脚本是否已经安装
    private fun checkConfig() {
        val support = configInstaller.dynamicSupport(context)
        if (support) {
            config_cfg_select.visibility = View.VISIBLE
            config_cfg_select_0.setOnClickListener {
                if (!outsideOverrided()) {
                    installConfig(false)
                }
            }
            config_cfg_select_1.setOnClickListener {
                if (!outsideOverrided()) {
                    installConfig(true)
                }
            }
        } else {
            config_cfg_select.visibility = View.GONE
        }
    }

    private fun chooseLocalConfig() {
        try {
            val intent = Intent(this.context, ActivityFileSelector::class.java)
            intent.putExtra("extension", "sh")
            startActivityForResult(intent, REQUEST_POWERCFG_FILE)
        } catch (ex: Exception) {
            Toast.makeText(context, "启动内置文件选择器失败！", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getOnlineConfig() {
        var i = 0
        DialogHelper.animDialog(AlertDialog.Builder(context)
                .setTitle(getString(R.string.config_online_options))
                .setCancelable(true)
                .setSingleChoiceItems(
                        arrayOf(
                                getString(R.string.online_config_v1),
                                getString(R.string.online_config_v2)
                        ), 0) { _, which ->
                    i = which
                }
                .setNegativeButton(R.string.btn_confirm) { _, _ ->
                    if (i == 0) {
                        getOnlineConfigV1()
                    } else if (i == 1) {
                        getOnlineConfigV2()
                    }
                })
    }

    private fun getOnlineConfigV1() {
        try {
            val intent = Intent(this.context, ActivityAddinOnline::class.java)
            intent.putExtra("url", "https://github.com/yc9559/cpufreq-interactive-opt/tree/master/vtools-powercfg")
            startActivityForResult(intent, REQUEST_POWERCFG_ONLINE)
        } catch (ex: Exception) {
            Toast.makeText(context, "启动在线页面失败！", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getOnlineConfigV2() {
        try {
            val intent = Intent(this.context, ActivityAddinOnline::class.java)
            intent.putExtra("url", "https://github.com/yc9559/wipe-v2/releases")
            startActivityForResult(intent, REQUEST_POWERCFG_ONLINE)
        } catch (ex: Exception) {
            Toast.makeText(context, "启动在线页面失败！", Toast.LENGTH_SHORT).show()
        }
    }

    //安装调频文件
    private fun installConfig(active: Boolean) {
        if (!configInstaller.dynamicSupport(context)) {
            Scene.toast(R.string.not_support_config, Toast.LENGTH_LONG)
            return
        }

        configInstaller.installOfficialConfig(context, "", active)
        configInstalled()
    }

    private fun configInstalled() {
        updateState()

        if (globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)) {
            Scene.toast(getString(R.string.config_installed), Toast.LENGTH_LONG)
            reStartService()
        } else {
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setMessage("配置脚本已安装，是否开启 [动态响应] ？")
                    .setPositiveButton(R.string.btn_confirm) { _, _ ->
                        dynamic_control.isChecked = true
                        globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, true).apply()
                        reStartService()
                    }
                    .setNegativeButton(R.string.btn_cancel) { _, _ ->
                    })
        }
    }

    /**
     * 重启辅助服务
     */
    private fun reStartService() {
        if (AccessibleServiceHelper().serviceRunning(context)) {
            context.sendBroadcast(Intent(context.getString(R.string.scene_change_action)))
        }
    }
}
