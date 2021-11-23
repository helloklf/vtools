package com.omarea.vtools.fragments

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.*
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.omarea.Scene
import com.omarea.common.model.SelectItem
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.ShellTranslation
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.DialogItemChooser
import com.omarea.data.GlobalStatus
import com.omarea.library.device.GpuInfo
import com.omarea.library.shell.*
import com.omarea.model.CpuCoreInfo
import com.omarea.model.ProcessInfo
import com.omarea.store.SpfConfig
import com.omarea.ui.AdapterCpuCores
import com.omarea.ui.AdapterProcessMini
import com.omarea.vtools.R
import com.omarea.vtools.activities.*
import com.omarea.vtools.dialogs.DialogElectricityUnit
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FragmentHome : androidx.fragment.app.Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    private var CpuFrequencyUtil = CpuFrequencyUtils()
    private lateinit var globalSPF: SharedPreferences
    private var timer: Timer? = null

    private lateinit var spf: SharedPreferences
    private var myHandler = Handler(Looper.getMainLooper())
    private var cpuLoadUtils = CpuLoadUtils()
    private val memoryUtils = MemoryUtils()
    private var mGpuInfo: GpuInfo? = null

    private suspend fun forceKSWAPD(mode: Int): String {
        return withContext(Dispatchers.Default) {
            ShellTranslation(context!!).resolveRow(SwapUtils(context!!).forceKswapd(mode))
        }
    }

    private suspend fun dropCaches() {
        return withContext(Dispatchers.Default) {
            KeepShellPublic.doCmdSync(
                    "sync\n" +
                            "echo 3 > /proc/sys/vm/drop_caches\n" +
                            "echo 1 > /proc/sys/vm/compact_memory")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityManager = context!!.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        batteryManager = context!!.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        spf = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        home_memory_clear.setOnClickListener {
            home_raminfo_text.text = getString(R.string.please_wait)
            GlobalScope.launch(Dispatchers.Main) {
                dropCaches()
                Scene.toast(getString(R.string.home_cache_cleared), Toast.LENGTH_SHORT)
            }
        }

        home_memory_compact.setOnClickListener {
            home_zramsize_text.text = getText(R.string.please_wait)
            Toast.makeText(context!!, R.string.home_shell_begin, Toast.LENGTH_SHORT).show()
            GlobalScope.launch(Dispatchers.Main) {
                val result = forceKSWAPD(1)
                Scene.toast(result, Toast.LENGTH_SHORT)
            }
        }

        home_memory_compact.setOnLongClickListener {
            home_zramsize_text.text = getText(R.string.please_wait)
            GlobalScope.launch(Dispatchers.Main) {
                val result = forceKSWAPD(2)
                Scene.toast(result, Toast.LENGTH_SHORT)
            }
            true
        }

        home_help.setOnClickListener {
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("http://vtools.omarea.com/"))
                )
            } catch (ex: Exception) {
                Toast.makeText(context!!, R.string.home_browser_error, Toast.LENGTH_SHORT).show()
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
                DialogHelper.helpInfo(activity!!, "Governor Params", msg.toString())
            }
        }

        GlobalScope.launch(Dispatchers.Main) {
            // 获取GPU信息
            GpuInfo.getGpuInfo(home_gpu_info) { gpuInfo ->
                home_gpu.removeView(home_gpu_info)
                mGpuInfo = gpuInfo
            }
        }

        // 进程列表
        home_process_list.run {
            adapter = AdapterProcessMini(context!!).apply {
                updateFilterMode(AdapterProcessMini.FILTER_ANDROID)
            }
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    home_root.requestDisallowInterceptTouchEvent(false)
                } else {
                    home_root.requestDisallowInterceptTouchEvent(true) //屏蔽父控件的拦截事件
                }
                false
            }
            setOnItemClickListener { parent, _, index, _ ->
                val item = parent.getItemAtPosition(index) as ProcessInfo?
                val intent = Intent(context, ActivityProcess::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("name", item?.name)
                }
                startActivity(intent)
            }
        }

        home_device_name.text = when (Build.VERSION.SDK_INT) {
            31 -> "Android 12"
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

        // 点击内存信息
        home_memory.setOnClickListener {
            startActivity(Intent(context, ActivitySwap::class.java))
        }
        // 点击电池信息
        home_battery.setOnClickListener {
            if (GlobalStatus.batteryStatus == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                startActivity(Intent(context, ActivityPowerUtilization::class.java))
            } else {
                startActivity(Intent(context, ActivityCharge::class.java))
            }
        }
        // 点击CPU
        home_cpu.setOnClickListener {
            setCpuOnline()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isDetached) {
            return
        }
        activity!!.title = getString(R.string.app_name)

        maxFreqList.clear()
        minFreqList.clear()
        stopTimer()
        updateTick = 0
        timer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    updateInfo()
                }
            }, 0, 1500)
        }
    }

    private val coreCount = object : TripleCacheValue(Scene.context, "CoreCount") {
        override fun initValue(): String {
            return "" + CpuFrequencyUtil.coreCount
        }
    }.toInt()

    private lateinit var batteryManager: BatteryManager
    private lateinit var activityManager: ActivityManager
    private val platformUtils = PlatformUtils()
    private val processUtils = ProcessUtilsSimple(Scene.context)

    private var minFreqList = HashMap<Int, String>()
    private var maxFreqList = HashMap<Int, String>()
    private fun formatNumber(value: Double): String {
        var bd = BigDecimal(value)
        bd = bd.setScale(1, RoundingMode.HALF_UP)
        return bd.toString()
    }

    @SuppressLint("SetTextI18n")
    private fun updateRamInfo() {
        try {
            val info = ActivityManager.MemoryInfo().apply {
                activityManager.getMemoryInfo(this)
            }
            val totalMem = (info.totalMem / 1024 / 1024f).toInt()
            val availMem = (info.availMem / 1024 / 1024f).toInt()

            val swapInfo = KeepShellPublic.doCmdSync("free -m | grep Swap")
            var swapTotal = 0
            var swapUsed = 0
            if (swapInfo.contains("Swap")) {
                try {
                    val swapInfos = swapInfo.substring(swapInfo.indexOf(" "), swapInfo.lastIndexOf(" ")).trim()
                    if (Regex("[\\d]+[\\s]+[\\d]+").matches(swapInfos)) {
                        swapTotal = swapInfos.substring(0, swapInfos.indexOf(" ")).trim().toInt()
                        swapUsed = swapInfos.substring(swapInfos.indexOf(" ")).trim().toInt()
                    }
                } catch (ex: java.lang.Exception) {
                }
            }

            myHandler.post {
                home_raminfo_text?.text = "${((totalMem - availMem) * 100 / totalMem)}% (${totalMem / 1024 + 1}GB)"
                home_ramstat?.setData(totalMem.toFloat(), availMem.toFloat())
                home_swapstat?.setData(swapTotal.toFloat(), (swapTotal - swapUsed).toFloat())
                home_memory_total?.setData(
                        (totalMem + swapTotal).toFloat(), availMem + (swapTotal - swapUsed).toFloat(), totalMem.toFloat()
                )
                home_zramsize_text?.text = (
                        if (swapTotal > 99) {
                            "${(swapUsed * 100.0 / swapTotal).toInt()}% (${formatNumber(swapTotal / 1024.0)}GB)"
                        } else {
                            "${(swapUsed * 100.0 / swapTotal).toInt()}% (${swapTotal}MB)"
                        }
                        )
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
        val cores = ArrayList<CpuCoreInfo>()
        for (coreIndex in 0 until coreCount) {
            val core = CpuCoreInfo(coreIndex)

            core.currentFreq = CpuFrequencyUtil.getCurrentFrequency("cpu$coreIndex")
            if (!maxFreqList.containsKey(coreIndex) || (core.currentFreq != "" && maxFreqList[coreIndex].isNullOrEmpty())) {
                maxFreqList[coreIndex] = CpuFrequencyUtil.getCurrentMaxFrequency("cpu$coreIndex")
            }
            core.maxFreq = maxFreqList[coreIndex]

            if (!minFreqList.containsKey(coreIndex) || (core.currentFreq != "" && minFreqList[coreIndex].isNullOrEmpty())) {
                minFreqList.put(coreIndex, CpuFrequencyUtil.getCurrentMinFrequency("cpu$coreIndex"))
            }
            core.minFreq = minFreqList[coreIndex]
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
        // 电压
        val batteryVoltage = (GlobalStatus.batteryVoltage * 10).toInt() / 10.0
        // 电池温度
        val temperature = GlobalStatus.updateBatteryTemperature()

        updateRamInfo()
        val memInfo = memoryUtils.memoryInfo
        val platform = platformUtils.getCPUName()
        if (updateTick == 0 || updateTick == 3) {
            GlobalScope.launch(Dispatchers.IO) {
                val processList = processUtils.allProcess
                myHandler.post {
                    (home_process_list?.adapter as AdapterProcessMini?)?.setList(processList)
                }
            }
        }

        myHandler.post {
            try {
                home_swap_cached.text = "" + (memInfo.swapCached / 1024) + "MB"
                home_dirty.text = "" + (memInfo.dirty / 1024) + "MB"

                home_running_time.text = elapsedRealtimeStr()
                if (batteryCurrentNow != Long.MIN_VALUE && batteryCurrentNow != Long.MAX_VALUE) {
                    home_battery_now.text = (batteryCurrentNow / globalSPF.getInt(SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT, SpfConfig.GLOBAL_SPF_CURRENT_NOW_UNIT_DEFAULT)).toString() + "mA"
                } else {
                    home_battery_now.text = "--"
                }
                home_battery_capacity.text = "$batteryCapacity%  ${batteryVoltage}v"
                home_battery_temperature.text = "${temperature}°C"

                home_gpu_freq.text = gpuFreq
                home_gpu_load.text = getString(R.string.home_utilization) + "$gpuLoad%"
                if (gpuLoad > -1) {
                    home_gpu_chat.setData(100.toFloat(), (100 - gpuLoad).toFloat())
                }
                if (loads.containsKey(-1)) {
                    cpu_core_total_load.text = getString(R.string.home_utilization) + loads[-1]!!.toInt().toString() + "%"
                    home_cpu_chat.setData(100.toFloat(), (100 - loads[-1]!!.toInt()).toFloat())
                }
                if (cpu_core_list.adapter == null) {
                    val layoutParams = cpu_core_list.layoutParams
                    if (cores.size < 6) {
                        layoutParams.height = dp2px(85 * 2F)
                        cpu_core_list.numColumns = 2
                    } else if (cores.size > 12) {
                        layoutParams.height = dp2px(85 * 4F)
                    } else if (cores.size > 8) {
                        layoutParams.height = dp2px(85 * 3F)
                    } else {
                        layoutParams.height = dp2px(85 * 2F)
                    }
                    cpu_core_list.layoutParams = layoutParams
                    cpu_core_list.adapter = AdapterCpuCores(context!!, cores)
                } else {
                    (cpu_core_list.adapter as AdapterCpuCores).setData(cores)
                }
                mGpuInfo?.run {
                    home_gpu_info_text.text = "$glVendor $glRender\n$glVersion"
                }
                cpu_soc_platform?.text = platform.toUpperCase(Locale.getDefault()) + " (" + coreCount + " Cores)"
            } catch (ex: Exception) {

            }
        }
        updateTick++
        if (updateTick > 5) {
            updateTick = 0
            minFreqList.clear()
            maxFreqList.clear()
        }
    }

    private fun stopTimer() {
        if (this.timer != null) {
            updateTick = 0
            timer!!.cancel()
            timer = null
        }
    }

    // 选择开关核心
    private fun setCpuOnline() {
        val activity = (activity as ActivityBase?)
        if (activity != null) {
            val options = ArrayList<SelectItem>().apply {
                for (i in 0 until coreCount) {
                    add(SelectItem().apply {
                        title = "CPU $i"
                        value = "" + i
                        selected = CpuFrequencyUtil.getCoreOnlineState(i)
                    })
                }
            }
            DialogItemChooser(activity.themeMode.isDarkMode, options, true, object : DialogItemChooser.Callback {
                override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                    if (status.isNotEmpty() && status.find { it } != null) {
                        status.forEachIndexed { index, b ->
                            CpuFrequencyUtil.setCoreOnlineState(index, b)
                            updateInfo()
                        }
                    } else {
                        Toast.makeText(activity,  getString(R.string.home_core_required), Toast.LENGTH_SHORT).show()
                    }
                }
            }, true)
            .setTitle(getString(R.string.home_core_switch))
            .show(activity.supportFragmentManager, "home-cpu-control")
        }
    }

    override fun onPause() {
        stopTimer()
        super.onPause()
    }
}
