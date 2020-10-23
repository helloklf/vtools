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
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.omarea.Scene
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.RootFile
import com.omarea.common.ui.DialogHelper
import com.omarea.data.GlobalStatus
import com.omarea.library.shell.CpuFrequencyUtil
import com.omarea.library.shell.CpuLoadUtils
import com.omarea.library.shell.GpuUtils
import com.omarea.library.shell.SwapUtils
import com.omarea.model.CpuCoreInfo
import com.omarea.scene_mode.CpuConfigInstaller
import com.omarea.scene_mode.ModeSwitcher
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

class FragmentHome : androidx.fragment.app.Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    private var CpuFrequencyUtil = CpuFrequencyUtil()
    private lateinit var globalSPF: SharedPreferences
    private var timer: Timer? = null
    private fun showMsg(msg: String) {
        this.view?.let { Snackbar.make(it, msg, Snackbar.LENGTH_LONG).show() }
    }

    private lateinit var spf: SharedPreferences
    private var myHandler = Handler(Looper.getMainLooper())
    private var cpuLoadUtils = CpuLoadUtils()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityManager = context!!.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        batteryManager = context!!.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        if (ModeSwitcher().modeConfigCompleted() || CpuConfigInstaller().dynamicSupport(Scene.context)) {
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

        if (!GlobalStatus.homeMessage.isNullOrEmpty()) {
            home_message.visibility = View.VISIBLE
            home_message.text = GlobalStatus.homeMessage
        }

        spf = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        home_clear_ram.setOnClickListener {
            home_raminfo_text.text = getString(R.string.please_wait)
            Thread(Runnable {
                KeepShellPublic.doCmdSync("sync\n" +
                        "echo 3 > /proc/sys/vm/drop_caches\n" +
                        "echo 1 > /proc/sys/vm/compact_memory")
                myHandler.post {
                    Scene.toast("缓存已清理...", Toast.LENGTH_SHORT)
                }
            }).start()
        }

        home_clear_swap.setOnClickListener {
            if (RootFile.fileExists("/proc/1/reclaim")) {
                Thread {
                    KeepShellPublic.doCmdSync(
                            "sync\n" +
                            "echo 1 > /proc/sys/vm/compact_memory")
                    myHandler.post {
                        Scene.toast("已对RAM中的碎片进行整理\n如需强制压缩RAM，请长按", Toast.LENGTH_SHORT)
                    }
                }.start()
                home_zramsize_text.text = getText(R.string.please_wait)
            }
        }

        home_clear_swap.setOnLongClickListener {
            home_zramsize_text.text = getText(R.string.please_wait)
            Thread(Runnable {
                val result = SwapUtils(context!!).forceKswapd()
                myHandler.post {
                    Scene.toast(result, Toast.LENGTH_SHORT)
                }
            }).start()
            true
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
            DialogElectricityUnit().showDialog(context!!)
        }

        // 点击CPU核心 查看详细参数
        cpu_core_list.setOnItemClickListener { _, _, position, _ ->
            CpuFrequencyUtil.getCoregGovernorParams(position)?.run {
                val msg = StringBuilder()
                for (param in this) {
                    msg.append("\n")
                    msg.append(param.key)
                    msg.append("：")
                    msg.append(param.value)
                    msg.append("\n")
                }
                DialogHelper.animDialog(AlertDialog.Builder(context)
                        .setTitle("调度器参数")
                        .setMessage(msg.toString())
                        .setPositiveButton(R.string.btn_confirm) { _, _ ->
                        })
            }
        }

        home_device_name.text = when (Build.VERSION.SDK_INT) {
            30 -> "Android 11"
            29 -> "Android 10"
            28 -> "Android 9"
            27 -> "Android 8.1"
            26 -> "Android 8.0"
            25 -> "Android 7.0"
            24 -> "Android 7.0"
            23 -> "Android 6.0"
            22 -> "Android 5.1"
            21 -> "Android 5.0"
            else -> "SDK(" + Build.VERSION.SDK_INT + ")"
        } // (Build.MANUFACTURER + " " + Build.MODEL + " (SDK" + Build.VERSION.SDK_INT + ")").trim()
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        if (isDetached) {
            return
        }
        activity!!.title = getString(R.string.app_name)
        setModeState()
        maxFreqs.clear()
        minFreqs.clear()
        stopTimer()
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                updateInfo()
            }
        }, 0, 1500)
        updateRamInfo()
    }

    private var coreCount = -1
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
                    if (Regex("[\\d]+[\\s]{1,}[\\d]{1,}").matches(swapInfos)) {
                        val total = swapInfos.substring(0, swapInfos.indexOf(" ")).trim().toInt()
                        val use = swapInfos.substring(swapInfos.indexOf(" ")).trim().toInt()
                        val free = total - use
                        home_swapstate_chat.setData(total.toFloat(), free.toFloat())
                        if (total > 99) {
                            home_zramsize_text.text = "${(use * 100.0 / total).toInt()}% (${format1(total / 1024.0)}GB)"
                        } else {
                            home_zramsize_text.text = "${(use * 100.0 / total).toInt()}% (${total}MB)"
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

    private fun elapsedRealtimeStr(): String {
        val timer = SystemClock.elapsedRealtime() / 1000
        return String.format("%02d:%02d:%02d", timer / 3600, timer % 3600 / 60, timer % 60)
    }

    private var updateTick = 0

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
        for (coreIndex in 0 until coreCount) {
            val core = CpuCoreInfo(coreIndex)

            core.currentFreq = CpuFrequencyUtil.getCurrentFrequency("cpu$coreIndex")
            if (!maxFreqs.containsKey(coreIndex) || (core.currentFreq != "" && maxFreqs[coreIndex].isNullOrEmpty())) {
                maxFreqs[coreIndex] = CpuFrequencyUtil.getCurrentMaxFrequency("cpu$coreIndex")
            }
            core.maxFreq = maxFreqs[coreIndex]

            if (!minFreqs.containsKey(coreIndex) || (core.currentFreq != "" && minFreqs[coreIndex].isNullOrEmpty())) {
                minFreqs.put(coreIndex, CpuFrequencyUtil.getCurrentMinFrequency("cpu$coreIndex"))
            }
            core.minFreq = minFreqs[coreIndex]
            cores.add(core)
        }
        val loads = cpuLoadUtils.cpuLoad
        for (core in cores) {
            if (loads.containsKey(core.coreIndex)) {
                core.loadRatio = loads[core.coreIndex]!!
            }
        }

        val gpuFreq = GpuUtils.getGpuFreq() + "Mhz"
        val gpuLoad = GpuUtils.getGpuLoad()

        // 电池电流
        batteryCurrentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        // 电量
        val batteryCapacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        myHandler.post {
            try {
                updateRamInfo()
                home_running_time.text = elapsedRealtimeStr()
                home_battery_now.text = (batteryCurrentNow / globalSPF.getInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT)).toString() + "mA"
                home_battery_capacity.text = "$batteryCapacity%"
                home_battery_temperature.text = "${GlobalStatus.batteryTemperature}°C"

                home_gpu_freq.text = gpuFreq
                home_gpu_load.text = "负载：$gpuLoad%"
                if (gpuLoad > -1) {
                    home_gpu_chat.setData(100.toFloat(), (100 - gpuLoad).toFloat())
                }
                if (loads.containsKey(-1)) {
                    cpu_core_total_load.text = "负载：" + loads[-1]!!.toInt().toString() + "%"
                    home_cpu_chat.setData(100.toFloat(), (100 - loads[-1]!!.toInt()).toFloat())
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
        when (ModeSwitcher().getCurrentPowerMode()) {
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
        val modeSwitcher = ModeSwitcher()
        val dynamic = AccessibleServiceHelper().serviceRunning(context!!) && spf.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)
        if (!dynamic && modeSwitcher.getCurrentPowerMode() == action) {
            modeSwitcher.setCurrent("", "")
            globalSPF.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG, "").apply()
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle("提示")
                    .setMessage("需要重启手机才能恢复默认调度，是否立即重启？")
                    .setNegativeButton(R.string.btn_cancel) { _, _ ->
                    }
                    .setPositiveButton(R.string.btn_confirm) { _, _ ->
                        KeepShellPublic.doCmdSync("sync\nsleep 1\nreboot")
                    })
            setModeState()
            return
        }
        if (modeSwitcher.modeConfigCompleted()) {
            modeSwitcher.executePowercfgMode(action, context!!.packageName)
        } else {
            CpuConfigInstaller().installOfficialConfig(context!!)
            modeSwitcher.executePowercfgMode(action)
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
                            .setMessage("“场景模式-性能调节”已被激活，你手动选择的模式随时可能被覆盖。\n\n如果你需要长期使用手动控制，请前往“场景模式”的设置界面关闭“性能调节”！")
                            .setNegativeButton(R.string.btn_confirm) { _, _ ->
                            }
                            .setCancelable(false))
        } else {
            globalSPF.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG, action).apply()
            if (!globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_POWERCFG_FRIST_NOTIFY, false)) {
                DialogHelper.animDialog(
                        AlertDialog
                                .Builder(context)
                                .setTitle("提示")
                                .setMessage("如果你已允许Scene自启动，手机重启后，Scene还会自动激活刚刚选择的模式。\n\n如果需要恢复系统默认调度，请再次点击，然后重启手机！")
                                .setNegativeButton(R.string.btn_confirm) { _, _ ->
                                }
                                .setPositiveButton(R.string.btn_dontshow) { _, _ ->
                                    globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_POWERCFG_FRIST_NOTIFY, true).apply()
                                }
                                .setCancelable(false))
            }
        }
    }
}
