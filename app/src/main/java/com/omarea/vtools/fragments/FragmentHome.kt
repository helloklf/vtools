package com.omarea.vtools.fragments

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import com.omarea.model.CpuCoreInfo
import com.omarea.scene_mode.ModeConfigInstaller
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.shell_utils.CpuFrequencyUtil
import com.omarea.shell_utils.CpuLoadUtils
import com.omarea.shell_utils.GpuUtils
import com.omarea.shell_utils.PropsUtils
import com.omarea.store.SpfConfig
import com.omarea.ui.AdapterCpuCores
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.vtools.R
import com.omarea.vtools.dialogs.DialogElectricityUnit
import kotlinx.android.synthetic.main.fragment_home.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.collections.HashMap


class FragmentHome : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    private lateinit var globalSPF: SharedPreferences
    private var timer: Timer? = null
    private val configInstaller = ModeConfigInstaller()
    private fun showMsg(msg: String) {
        this.view?.let { Snackbar.make(it, msg, Snackbar.LENGTH_LONG).show() }
    }

    private lateinit var spf: SharedPreferences
    private var modeList = ModeSwitcher()
    private var myHandler = Handler()
    private var cpuLoadUtils = CpuLoadUtils()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityManager = context!!.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        batteryManager = context!!.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        if (configInstaller.dynamicSupport(context!!) || configInstaller.configInstalled()) {
            powermode_toggles.visibility = View.VISIBLE
        } else {
            powermode_toggles.visibility = View.GONE
        }

        btn_powersave.setOnClickListener {
            installConfig(ModeSwitcher.POWERSAVE, getString(R.string.power_change_powersave))
        }
        btn_defaultmode.setOnClickListener {
            installConfig(ModeSwitcher.BALANCE, getString(R.string.power_change_default))
        }
        btn_gamemode.setOnClickListener {
            installConfig(ModeSwitcher.PERFORMANCE, getString(R.string.power_change_game))
        }
        btn_fastmode.setOnClickListener {
            installConfig(ModeSwitcher.FAST, getString(R.string.power_chagne_fast))
        }

        spf = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        home_clear_ram.setOnClickListener {
            home_raminfo_text.text = getString(R.string.please_wait)
            Thread(Runnable {
                KeepShellPublic.doCmdSync("sync\n" +
                        "echo 3 > /proc/sys/vm/drop_caches\n" +
                        "echo 1 > /proc/sys/vm/compact_memory")
                myHandler.postDelayed({
                    try {
                        updateRamInfo()
                        Toast.makeText(context, "缓存已清理...", Toast.LENGTH_SHORT).show()
                    } catch (ex: java.lang.Exception) {
                    }
                }, 600)
            }).start()
        }
        home_clear_swap.setOnClickListener {
            home_zramsize_text.text = getText(R.string.please_wait)
            Thread(Runnable {
                KeepShellPublic.doCmdSync("sync\n" +
                        "echo 1 > /proc/sys/vm/compact_memory")
                myHandler.postDelayed({
                    try {
                        updateRamInfo()
                        Toast.makeText(context, "内存碎片已整理...", Toast.LENGTH_SHORT).show()
                    } catch (ex: java.lang.Exception) {
                    }
                }, 600)
            }).start()
        }
        home_help.setOnClickListener {
            try {
                val uri = Uri.parse("http://vtools.omarea.com/")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(context!!, "启动在线页面失败！", Toast.LENGTH_SHORT).show()
            }
        }

        home_battery_edit.setOnClickListener {
            DialogElectricityUnit().showDialog(this, batteryCurrentNow)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        if (isDetached) {
            return
        }
        setModeState()
        maxFreqs.clear()
        minFreqs.clear()
        stopTimer()
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                updateInfo()
            }
        }, 0, 1000)
        updateRamInfo()
    }

    private var coreCount = -1;
    private lateinit var batteryManager: BatteryManager
    private lateinit var activityManager: ActivityManager

    private var minFreqs = HashMap<Int, String>()
    private var maxFreqs = HashMap<Int, String>()
    fun format1(value: Double): String {
        var bd = BigDecimal(value)
        bd = bd.setScale(1, RoundingMode.HALF_UP)
        return bd.toString()
    }

    @SuppressLint("SetTextI18n")
    private fun updateRamInfo() {
        try {
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            val totalMem = (info.totalMem / 1024 / 1024f).toInt()
            val availMem = (info.availMem / 1024 / 1024f).toInt()
            home_raminfo_text.text = "${((totalMem - availMem) * 100 / totalMem)}% (${totalMem / 1024 + 1}GB)"
            home_raminfo.setData(totalMem.toFloat(), availMem.toFloat())
            val swapInfo = KeepShellPublic.doCmdSync("free -m | grep Swap")
            if (swapInfo.contains("Swap")) {
                try {
                    val swapInfos = swapInfo.substring(swapInfo.indexOf(" "), swapInfo.lastIndexOf(" ")).trim()
                    if (Regex("[\\d]{1,}[\\s]{1,}[\\d]{1,}").matches(swapInfos)) {
                        val total = swapInfos.substring(0, swapInfos.indexOf(" ")).trim().toInt()
                        val use = swapInfos.substring(swapInfos.indexOf(" ")).trim().toInt()
                        val free = total - use
                        home_swapstate_chat.setData(total.toFloat(), free.toFloat())
                        if (total > 99) {
                            home_zramsize_text.text = "${(use * 100.0 / total).toInt().toString()}% (${format1(total / 1024.0)}GB)"
                        } else {
                            home_zramsize_text.text = "${(use * 100.0 / total).toInt().toString()}% (${total}MB)"
                        }
                    }
                } catch (ex: java.lang.Exception) {
                }
                // home_swapstate.text = swapInfo.substring(swapInfo.indexOf(" "), swapInfo.lastIndexOf(" ")).trim()
            } else {
            }
        } catch (ex: Exception) {
        }
    }

    /**
     * dp转换成px
     */
    private fun dp2px(dpValue: Float): Int {
        val scale = context!!.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    private var updateTick = 0;

    private var batteryCurrentNow = 0L
    @SuppressLint("SetTextI18n")
    private fun updateInfo() {
        if (coreCount < 1) {
            coreCount = CpuFrequencyUtil.getCoreCount()
            myHandler.post {
                try {
                    cpu_core_count.text = "$coreCount 核心"
                } catch (ex: Exception) {
                }
            }
        }
        val cores = ArrayList<CpuCoreInfo>()
        val loads = cpuLoadUtils.cpuLoad
        for (coreIndex in 0 until coreCount) {
            val core = CpuCoreInfo()

            core.currentFreq = CpuFrequencyUtil.getCurrentFrequency("cpu$coreIndex")
            if (!maxFreqs.containsKey(coreIndex) || (core.currentFreq != "" && maxFreqs.get(coreIndex).isNullOrEmpty())) {
                maxFreqs.put(coreIndex, CpuFrequencyUtil.getCurrentMaxFrequency("cpu" + coreIndex))
            }
            core.maxFreq = maxFreqs.get(coreIndex)

            if (!minFreqs.containsKey(coreIndex) || (core.currentFreq != "" && minFreqs.get(coreIndex).isNullOrEmpty())) {
                minFreqs.put(coreIndex, CpuFrequencyUtil.getCurrentMinFrequency("cpu" + coreIndex))
            }
            core.minFreq = minFreqs.get(coreIndex)

            if (loads.containsKey(coreIndex)) {
                core.loadRatio = loads.get(coreIndex)!!
            }
            cores.add(core)
        }
        val gpuFreq = GpuUtils.getGpuFreq() + "Mhz"
        val gpuLoad = GpuUtils.getGpuLoad()

        // 电池电流
        batteryCurrentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        // 电量
        val batteryCapacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        myHandler.post {
            try {
                home_battery_now.setText((batteryCurrentNow / globalSPF.getInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT)).toString() + "mA")
                home_battery_capacity.setText(batteryCapacity.toString() + "%")

                home_gpu_freq.text = gpuFreq
                home_gpu_load.text = "负载：" + gpuLoad + "%"
                if (gpuLoad > -1) {
                    home_gpu_chat.setData(100.toFloat(), (100 - gpuLoad).toFloat())
                }
                if (loads.containsKey(-1)) {
                    cpu_core_total_load.text = "负载：" + loads.get(-1)!!.toInt().toString() + "%"
                    home_cpu_chat.setData(100.toFloat(), (100 - loads.get(-1)!!.toInt()).toFloat())
                }
                if (cpu_core_list.adapter == null) {
                    val layoutParams = cpu_core_list.layoutParams
                    if (cores.size < 6) {
                        layoutParams.height = dp2px(105 * 2F)
                        cpu_core_list.numColumns = 2
                    } else if (cores.size > 12) {
                        layoutParams.height = dp2px(105 * 4F)
                    } else if (cores.size > 8) {
                        layoutParams.height = dp2px(105 * 3F)
                    } else {
                        layoutParams.height = dp2px(105 * 2F)
                    }
                    cpu_core_list.layoutParams = layoutParams
                    cpu_core_list.adapter = AdapterCpuCores(context!!, cores)
                } else {
                    (cpu_core_list.adapter as AdapterCpuCores).setData(cores)
                }
            } catch (ex: Exception) {

            }
        }
        updateTick++
        if (updateTick > 5) {
            updateTick = 0
            minFreqs.clear()
            maxFreqs.clear()
        }
    }

    private fun setModeState() {
        btn_powersave.setTextColor(0x66ffffff)
        btn_defaultmode.setTextColor(0x66ffffff)
        btn_gamemode.setTextColor(0x66ffffff)
        btn_fastmode.setTextColor(0x66ffffff)
        val cfg = PropsUtils.getProp("vtools.powercfg")
        when (cfg) {
            ModeSwitcher.BALANCE -> {
                btn_defaultmode.setTextColor(Color.WHITE)
            }
            ModeSwitcher.PERFORMANCE -> {
                btn_gamemode.setTextColor(Color.WHITE)
            }
            ModeSwitcher.POWERSAVE -> {
                btn_powersave.setTextColor(Color.WHITE)
            }
            ModeSwitcher.FAST -> {
                btn_fastmode.setTextColor(Color.WHITE)
            }
        }
    }

    private fun stopTimer() {
        if (this.timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun onPause() {
        stopTimer()
        super.onPause()
    }

    private fun installConfig(action: String, message: String) {
        val dynamic = AccessibleServiceHelper().serviceRunning(context!!) && spf.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, true)
        if (!dynamic && modeList.getCurrentPowerMode() == action) {
            modeList.setCurrent("", "")
            globalSPF.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG, "").apply()
            AlertDialog.Builder(context)
                    .setTitle("提示")
                    .setMessage("需要重启手机才能恢复默认调度，是否立即重启？")
                    .setNegativeButton(R.string.btn_cancel, { _, _ ->
                    })
                    .setPositiveButton(R.string.btn_confirm, { _, _ ->
                        KeepShellPublic.doCmdSync("sync\nsleep 1\nreboot")
                    })
                    .create()
                    .show()
            setModeState()
            return
        }
        if (configInstaller.configInstalled()) {
            modeList.executePowercfgMode(action, context!!.packageName)
        } else {
            ModeConfigInstaller().installPowerConfig(context!!);
            modeList.executePowercfgMode(action)
        }
        setModeState()
        showMsg(message)
        maxFreqs.clear()
        minFreqs.clear()
        updateInfo()
        if (dynamic) {
            globalSPF.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG, "").apply()
            DialogHelper.animDialog(
                    AlertDialog
                            .Builder(context)
                            .setTitle("提示")
                            .setMessage("“场景模式-动态响应”已被激活，你手动选择的模式随时可能被覆盖。\n\n如果你需要长期使用手动控制，请前往“场景模式”的设置界面关闭“动态响应”！")
                            .setNegativeButton(R.string.btn_confirm, { _, _ ->
                            })
                            .setCancelable(false))
        } else {
            globalSPF.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG, action).apply()
            if (!globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_POWERCFG_FRIST_NOTIFY, false)) {
                DialogHelper.animDialog(
                        AlertDialog
                                .Builder(context)
                                .setTitle("提示")
                                .setMessage("如果你允许Scene自启动，下次开机后，Scene还会自动启用你刚刚选择的模式。\n\n如果你需要关闭调度，请再次点击相同的模式取消选中状态，然后重启手机！")
                                .setNegativeButton(R.string.btn_confirm, { _, _ ->
                                })
                                .setPositiveButton(R.string.btn_dontshow, { _, _ ->
                                    globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_POWERCFG_FRIST_NOTIFY, true).apply()
                                })
                                .setCancelable(false))
            }
        }
    }
}
