package com.omarea.vtools.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import com.omarea.common.ui.DialogHelper
import com.omarea.scene_mode.CpuConfigInstaller
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.SpfConfig
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.vtools.R
import com.omarea.vtools.activities.ActivityAddinOnline
import com.omarea.vtools.activities.ActivityFileSelector
import kotlinx.android.synthetic.main.fragment_cpu_modes.*
import java.io.File
import java.nio.charset.Charset


class FragmentCpuModes : androidx.fragment.app.Fragment() {
    private var author: String = ""
    private var configFileInstalled: Boolean = false
    private lateinit var modeSwitcher: ModeSwitcher
    private lateinit var globalSPF: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_cpu_modes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        modeSwitcher = ModeSwitcher()

        cpu_config_p4.setOnClickListener {
            Toast.makeText(context!!, "该模式暂未开放使用", Toast.LENGTH_SHORT).show()
        }
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
        dynamic_control.setOnClickListener {
            val value = (it as Switch).isChecked
            if (value && !(modeSwitcher.modeConfigCompleted())) {
                dynamic_control.isChecked = false
                DialogHelper.helpInfo(context!!, "请先完成四个模式的配置！", "使用Scene自带配置(如果有显示选项)、本地导入、在线下载，均可快速完成四个模式的配置。\n\n如果都没找到适用的配置，不妨试试点击各个模式，自己动手设置参数！")
            } else {
                globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, value).apply()
                reStartService()
            }
        }

        cpu_mode_delete_outside.setOnClickListener {
            DialogHelper.animDialog(AlertDialog.Builder(context!!).setTitle("确定删除?")
                    .setMessage("确定删除安装在 /data/powercfg.sh 的外部配置脚本吗？\n它可能是Scene2遗留下来的，也可能是其它优化模块创建的")
                    .setPositiveButton(R.string.btn_confirm) { _, _ ->
                        configInstaller.removeOutsideConfig()
                        cpu_mode_outside.visibility = View.GONE
                        reStartService()
                        updateState()
                    }.setNegativeButton(R.string.btn_cancel, { _, _ -> }))
        }
    }

    private fun outsideOverrided(): Boolean {
        if (configInstaller.outsideConfigInstalled()) {
            DialogHelper.helpInfo(context!!, "你需要先删除外部配置，因为Scene3会优先使用它！")
            return true
        }
        return false
    }

    private fun bindMode(button: View, mode: String) {
        button.setOnClickListener {
            modifyCpuConfig(mode)
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
        return when (name.toLowerCase()) {
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
        activity!!.title = getString(R.string.menu_cpu_modes)

        val currentAuthor = author

        updateState()

        // 如果开启了动态响应 并且配置作者变了，重启后台服务
        if (dynamic_control.isChecked && !currentAuthor.isNullOrEmpty() && currentAuthor != author) {
            reStartService()
        }
    }

    private fun modifyCpuConfig(mode: String) {
        val transaction = activity!!.supportFragmentManager.beginTransaction()
        transaction.setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        // transaction.setCustomAnimations(R.animator.fragment_enter, R.animator.fragment_exit)
        val fragment = FragmentCpuControl.newInstance(mode)

        val pageTitle = ModeSwitcher.getModName(mode)
        // transaction.disallowAddToBackStack()
        transaction.replace(R.id.app_more, fragment)
        transaction.addToBackStack(pageTitle);
        transaction.commitAllowingStateLoss()
        activity!!.title = pageTitle
    }

    private val REQUEST_POWERCFG_FILE = 1
    private val REQUEST_POWERCFG_ONLINE = 2
    private val configInstaller = CpuConfigInstaller()
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (this.isDetached) {
            return
        }
        if (requestCode == REQUEST_POWERCFG_FILE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                if (data.extras == null || !data.extras.containsKey("file")) {
                    return
                }
                val path = data.extras.getString("file")
                val file = File(path)
                if (file.exists()) {
                    if (file.length() > 200 * 1024) {
                        Toast.makeText(context, "这个文件也太大了，配置脚本大小不能超过200KB！", Toast.LENGTH_LONG).show()
                        return
                    }
                    val lines = file.readText(Charset.defaultCharset()).replace("\r", "")
                    val configStar = lines.split("\n").firstOrNull()
                    if (configStar != null && configStar.startsWith("#!/") && configStar.endsWith("sh")) {
                        if (configInstaller.installCustomConfig(context!!, lines, "import-file")) {
                            configInstalled()
                        } else {
                            Toast.makeText(context, "由于某些原因，安装配置脚本失败，请重试！", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "这似乎是个无效的脚本文件！", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context!!, "所选的文件没找到！", Toast.LENGTH_LONG).show()
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
        val support = configInstaller.dynamicSupport(context!!)
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
            Toast.makeText(context!!, "启动内置文件选择器失败！", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context!!, "启动在线页面失败！", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getOnlineConfigV2() {
        try {
            val intent = Intent(this.context, ActivityAddinOnline::class.java)
            intent.putExtra("url", "https://github.com/yc9559/wipe-v2/releases")
            startActivityForResult(intent, REQUEST_POWERCFG_ONLINE)
        } catch (ex: Exception) {
            Toast.makeText(context!!, "启动在线页面失败！", Toast.LENGTH_SHORT).show()
        }
    }

    //安装调频文件
    private fun installConfig(useBigCore: Boolean) {
        if (!configInstaller.dynamicSupport(context!!)) {
            Snackbar.make(view!!, R.string.not_support_config, Snackbar.LENGTH_LONG).show()
            return
        }

        configInstaller.installOfficialConfig(context!!, "", useBigCore)
        configInstalled()
    }

    private fun configInstalled() {
        updateState()

        if (globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)) {
            Snackbar.make(view!!, getString(R.string.config_installed), Snackbar.LENGTH_LONG).show()
            reStartService()
        } else {
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setMessage("配置脚本已安装，是否开启 [性能调节] ？")
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
        if (AccessibleServiceHelper().serviceRunning(context!!)) {
            context!!.sendBroadcast(Intent(context!!.getString(R.string.scene_change_action)))
        }
    }

    companion object {
        fun createPage(): androidx.fragment.app.Fragment {
            val fragment = FragmentCpuModes()
            return fragment
        }
    }
}
