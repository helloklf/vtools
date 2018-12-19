package com.omarea.vtools

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.omarea.shared.*
import com.omarea.shared.model.CpuCoreInfo
import com.omarea.shell.KeepShellPublic
import com.omarea.shell.Platform
import com.omarea.shell.Props
import com.omarea.shell.RootFile
import com.omarea.shell.cpucontrol.CpuFrequencyUtils
import com.omarea.shell.cpucontrol.GpuUtils
import com.omarea.ui.AdapterCpuCores
import kotlinx.android.synthetic.main.layout_home.*
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.collections.HashMap


class FragmentHome : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_home, container, false)
    }

    private lateinit var globalSPF: SharedPreferences
    private var timer: Timer? = null
    private fun showMsg(msg: String) {
        this.view?.let { Snackbar.make(it, msg, Snackbar.LENGTH_LONG).show() }
    }

    private lateinit var spf: SharedPreferences
    private var modeList = ModeList()
    private var myHandler = Handler()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        if (Platform().dynamicSupport(context!!) || File(CommonCmds.POWER_CFG_PATH).exists()) {
            powermode_toggles.visibility = View.VISIBLE
        } else {
            powermode_toggles.visibility = View.GONE
        }

        btn_powersave.setOnClickListener {
            installConfig(ModeList.POWERSAVE, getString(R.string.power_change_powersave))
        }
        btn_defaultmode.setOnClickListener {
            installConfig(ModeList.BALANCE, getString(R.string.power_change_default))
        }
        btn_gamemode.setOnClickListener {
            installConfig(ModeList.PERFORMANCE, getString(R.string.power_change_game))
        }
        btn_fastmode.setOnClickListener {
            installConfig(ModeList.FAST, getString(R.string.power_chagne_fast))
        }

        spf = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        home_clear_ram.setOnClickListener {
            home_raminfo_text.text = "稍等一下"
            Thread(Runnable {
                KeepShellPublic.doCmdSync("sync\n" +
                        "echo 3 > /proc/sys/vm/drop_caches")
                myHandler.postDelayed({
                    try {
                        updateRamInfo()
                        Toast.makeText(context, "缓存已清理...", Toast.LENGTH_SHORT).show()
                    } catch (ex: java.lang.Exception) {}
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
        updateInfo()

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
    private var activityManager:ActivityManager? = null

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
            if (activityManager == null) {
                activityManager = context!!.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            }
            activityManager!!.getMemoryInfo(info)
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
                            home_zramsize.text = "${(use * 100.0 / total).toInt().toString()}% (${format1(total / 1024.0)}GB)"
                        } else {
                            home_zramsize.text = "${(use * 100.0 / total).toInt().toString()}% (${total}MB)"
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

    private var updateTick = 0;

    @SuppressLint("SetTextI18n")
    private fun updateInfo() {
        if (coreCount < 1) {
            coreCount = CpuFrequencyUtils.getCoreCount()
            myHandler.post {
                try {
                    cpu_core_count.text = "$coreCount 核心"
                } catch (ex: Exception) {}
            }
        }
        val cores = ArrayList<CpuCoreInfo>()
        val loads = CpuFrequencyUtils.getCpuLoad()
        for (coreIndex in 0 until coreCount) {
            val core = CpuCoreInfo()

            core.currentFreq = CpuFrequencyUtils.getCurrentFrequency("cpu$coreIndex")
            if (!maxFreqs.containsKey(coreIndex) || (core.currentFreq != "" && maxFreqs.get(coreIndex).isNullOrEmpty())) {
                maxFreqs.put(coreIndex, CpuFrequencyUtils.getCurrentMaxFrequency("cpu" + coreIndex))
            }
            core.maxFreq = maxFreqs.get(coreIndex)

            if (!minFreqs.containsKey(coreIndex) || (core.currentFreq != "" && minFreqs.get(coreIndex).isNullOrEmpty())) {
                minFreqs.put(coreIndex, CpuFrequencyUtils.getCurrentMinFrequency("cpu" + coreIndex))
            }
            core.minFreq = minFreqs.get(coreIndex)

            if (loads.containsKey(coreIndex)) {
                core.loadRatio = loads.get(coreIndex)!!
            }
            cores.add(core)
        }
        val gpuFreq = GpuUtils.getGpuFreq() + "Mhz"
        val gpuLoad = GpuUtils.getGpuLoad()
        myHandler.post {
            try {
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
        val cfg = Props.getProp("vtools.powercfg")
        when (cfg) {
            ModeList.BALANCE -> {
                btn_defaultmode.setTextColor(Color.WHITE)
            }
            ModeList.PERFORMANCE -> {
                btn_gamemode.setTextColor(Color.WHITE)
            }
            ModeList.POWERSAVE -> {
                btn_powersave.setTextColor(Color.WHITE)
            }
            ModeList.FAST -> {
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

    @SuppressLint("ApplySharedPref")
    private fun installConfig(action: String, message: String) {
        if (Build.BRAND.toLowerCase() == "meizu" || Build.MANUFACTURER.toLowerCase() == "meizu") {
            val soc = Platform().getCPUName()
            if (soc == "sdm845" || soc == "710") {
                AlertDialog.Builder(context)
                        .setTitle("暂不支持该设备")
                        .setMessage("根据许多魅族16th/16th Plus用户反馈，使用性能调度模式以后会无法正常开机（原因不详）。为了避免更多用户被坑，该功能现在将直接不再允许Meizu的sdm845、sdm710系列手机使用！")
                        .setPositiveButton(R.string.btn_iknow, { _,_ -> })
                        .create()
                        .show()
                return
            }
        }
        val dynamic = AccessibleServiceHelper().serviceIsRunning(context!!)
        if (!dynamic && modeList.getCurrentPowerMode() == action) {
            modeList.setCurrent("", "")
            globalSPF.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG, "").commit()
            Toast.makeText(context, "已取消开机后自动设置模式，你现在需要重启手机才能恢复系统默认调度！", Toast.LENGTH_LONG).show()
            setModeState()
            return
        }
        if (RootFile.fileExists(CommonCmds.POWER_CFG_PATH)) {
            modeList.executePowercfgMode(action, context!!.packageName)
        } else {
            val stringBuilder = StringBuilder()
            stringBuilder.append(String.format(CommonCmds.ToggleMode, action))
            ConfigInstaller().installPowerConfig(context!!, stringBuilder.toString());
        }
        setModeState()
        showMsg(message)
        maxFreqs.clear()
        minFreqs.clear()
        updateInfo()
        if (dynamic) {
            globalSPF.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG, "").commit()
        } else {
            globalSPF.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG, action).commit()
            Toast.makeText(context, "重启手机后Scene会尝试自动设置为当前选中的模式，如需取消自动设置请再次点击！", Toast.LENGTH_LONG).show()
        }
    }
}
