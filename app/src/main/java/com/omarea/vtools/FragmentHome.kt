package com.omarea.vtools

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ScrollView
import android.widget.Toast
import com.omarea.shared.*
import com.omarea.shared.model.CpuCoreInfo
import com.omarea.shell.Files
import com.omarea.shell.KeepShellPublic
import com.omarea.shell.Platform
import com.omarea.shell.Props
import com.omarea.shell.cpucontrol.CpuFrequencyUtils
import com.omarea.ui.AdapterCpuCores
import kotlinx.android.synthetic.main.layout_home.*
import java.io.File
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

    @SuppressLint("ApplySharedPref")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        if (!globalSPF.getBoolean("faq_readed_001", false)) {
            AlertDialog.Builder(context!!)
                    .setTitle("重要说明！！！")
                    .setMessage("使用场景模式-动态响应的时，同时使用其它调频优化脚本或调度模式优化软件！\n\n由于部分调频优化脚本或应用，会锁定一些重要的调度参数，导致无法Scene动态修改，并引发一些冲突导致的错误。\n\n如：最低频率被锁定在某个值不再下降。\n\n已知的冲突：卡刷或Magisk模块形式的Project WIPE（yc）调频\nSkymi集成的模式脚本\n\n此外，更换不同作者的动态响应配置脚本需要重启生效！！！\n\n例如：从[保守/激进]更换成[在线获取]的配置脚本")
                    .setCancelable(false)
                    .setPositiveButton(R.string.btn_iknow, {
                        _, _ ->
                    })
                    .setNeutralButton(R.string.btn_dontshow, {
                        _,_ ->
                        globalSPF.edit().putBoolean("faq_readed_001", true).apply()
                    })
                    .create()
                    .show()
        }
        if (!globalSPF.getBoolean("faq_readed_002", false)) {
            AlertDialog.Builder(context!!)
                    .setTitle("重要说明！！！")
                    .setMessage("如果你在使用场景模式过程中出现触摸失灵或按键没反应，可到【场景模式 - 设置 - 专家选项】中关闭按键事件捕获选项。\n\n但是，关闭此选项的同时，场景模式中的“按键屏蔽”功能将会失效！")
                    .setCancelable(false)
                    .setPositiveButton(R.string.btn_iknow, {
                        _, _ ->
                    })
                    .setNeutralButton(R.string.btn_dontshow, {
                        _,_ ->
                        globalSPF.edit().putBoolean("faq_readed_002", true).apply()
                    })
                    .create()
                    .show()
        }
        if (!globalSPF.getBoolean("faq_readed_003", false)) {
            AlertDialog.Builder(context!!)
                    .setTitle("重要说明！！！")
                    .setMessage("为了确保你隐私和财产安全，建议不要从其它不可信的渠道下载本应用！\n\n目前，Scene只在酷安市场和[Scene实验室]发布！！！\n\n")
                    .setCancelable(false)
                    .setPositiveButton(R.string.btn_iknow, {
                        _, _ ->
                    })
                    .setNeutralButton(R.string.btn_dontshow, {
                        _,_ ->
                        globalSPF.edit().putBoolean("faq_readed_003", true).apply()
                    })
                    .create()
                    .show()
        }

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
                    updateRamInfo()
                    Toast.makeText(context, "缓存已清理...", Toast.LENGTH_SHORT).show()
                }, 600)
            }).start()
        }

        val globalLayoutListener  = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val h = home_mainview.measuredHeight
                if (h > 0) {
                    val lp1 = home_mainview1.layoutParams
                    lp1.height = h
                    home_mainview1.layoutParams = lp1

                    val lp = home_mainview2.layoutParams
                    lp.height = h
                    home_mainview2.layoutParams = lp
                    home_mainview_scroll.fullScroll(ScrollView.FOCUS_UP)
                    home_mainview.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        }
        home_mainview.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
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

    private fun updateRamInfo() {
        sdfree.text = "SDCard：" + Files.getDirFreeSizeMB(Environment.getExternalStorageDirectory().absolutePath) + " MB"
        datafree.text = "Data：" + Files.getDirFreeSizeMB(Environment.getDataDirectory().absolutePath) + " MB"
        val info = ActivityManager.MemoryInfo()
        if (activityManager == null) {
            activityManager = context!!.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        }
        activityManager!!.getMemoryInfo(info)
        val totalMem = (info.totalMem / 1024 / 1024f).toInt()
        val availMem = (info.availMem / 1024 / 1024f).toInt()
        home_raminfo_text.text = "${availMem} / ${totalMem}MB"
        home_raminfo.setData(totalMem.toFloat(), availMem.toFloat())
    }

    private var updateTick = 0;

    @SuppressLint("SetTextI18n")
    private fun updateInfo() {
        if (coreCount < 1) {
            coreCount = CpuFrequencyUtils.getCoreCount()
            myHandler.post {
                try {
                    cpu_core_count.text = "核心数：$coreCount"
                } catch (ex: Exception) {}
            }
        }
        val cores = ArrayList<CpuCoreInfo>()
        val loads = CpuFrequencyUtils.getCpuLoad()
        /*
        for (coreIndex in loads.keys.sorted()) {
            if (coreIndex != -1) {
                val core = CpuCoreInfo()
                if (!maxFreqs.containsKey(coreIndex)) {
                    maxFreqs.put(coreIndex, CpuFrequencyUtils.getCurrentMaxFrequency("cpu" + coreIndex))
                }
                core.maxFreq = maxFreqs.get(coreIndex)

                if (!minFreqs.containsKey(coreIndex)) {
                    minFreqs.put(coreIndex, CpuFrequencyUtils.getCurrentMinFrequency("cpu" + coreIndex))
                }
                core.minFreq = minFreqs.get(coreIndex)

                core.currentFreq = CpuFrequencyUtils.getCurrentFrequency("cpu$coreIndex")
                if (loads.containsKey(coreIndex)) {
                    core.loadRatio = loads.get(coreIndex)!!
                }
                cores.add(core)
            }
        }
        */
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
        myHandler.post {
            try {
                if (loads.containsKey(-1)) {
                    cpu_core_total_load.text = "负载：" + loads.get(-1)!!.toInt().toString() + "%"
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

    private fun installConfig(action: String, message: String) {
        if (File(CommonCmds.POWER_CFG_PATH).exists()) {
            modeList.executePowercfgModeOnce(action, context!!.packageName)
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
    }
}
