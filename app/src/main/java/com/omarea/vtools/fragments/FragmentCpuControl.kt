package com.omarea.vtools.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.omarea.shared.CpuConfigStorage
import com.omarea.shared.model.CpuStatus
import com.omarea.shell.KernelProrp
import com.omarea.shell.cpucontrol.CpuFrequencyUtils
import com.omarea.shell.cpucontrol.ThermalControlUtils
import com.omarea.ui.ProgressBarDialog
import com.omarea.ui.StringAdapter
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.fragment_cpu_control.*
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList


class FragmentCpuControl : Fragment() {
    private var hasBigCore = false
    private var littleFreqs = arrayOf("")
    private var littleGovernor = arrayOf("")
    private var bigFreqs = arrayOf("")
    private var bigGovernor = arrayOf("")
    private var handler = Handler()
    private var coreCount = 0
    private var cores = arrayListOf<CheckBox>()
    private var exynosCpuhotplug = false;
    private var exynosHMP = false;
    private var adrenoGPU = false;
    private var adrenoFreqs = arrayOf("")
    private var adrenoGovernors = arrayOf("")
    private var adrenoPLevels = arrayOf("")
    private var inited = false
    private var statusOnBoot: CpuStatus? = null

    private fun initData() {
        hasBigCore = CpuFrequencyUtils.getClusterInfo().size > 1
        littleFreqs = CpuFrequencyUtils.getAvailableFrequencies(0)
        littleGovernor = CpuFrequencyUtils.getAvailableGovernors(0)
        bigFreqs = CpuFrequencyUtils.getAvailableFrequencies(1)
        bigGovernor = CpuFrequencyUtils.getAvailableGovernors(1)
        coreCount = CpuFrequencyUtils.getCoreCount()
        exynosCpuhotplug = CpuFrequencyUtils.exynosCpuhotplugSupport()
        exynosHMP = CpuFrequencyUtils.exynosHMP()
        adrenoGPU = CpuFrequencyUtils.isAdrenoGPU()
        if (adrenoGPU) {
            adrenoGovernors = CpuFrequencyUtils.getAdrenoGPUGovernors()
            adrenoFreqs = CpuFrequencyUtils.adrenoGPUFreqs()
            adrenoPLevels = CpuFrequencyUtils.getAdrenoGPUPowerLevels()
        }

        handler.post {
            try {
                if (!hasBigCore) {
                    big_core_configs.visibility = View.GONE
                }

                exynos_cpuhotplug.isEnabled = exynosCpuhotplug
                exynos_hmp_up.isEnabled = exynosHMP
                exynos_hmp_down.isEnabled = exynosHMP
                exynos_hmp_booster.isEnabled = exynosHMP
                if (!adrenoGPU) {
                    adreno_gpu.visibility = View.GONE
                }

                cores = arrayListOf<CheckBox>(core_0, core_1, core_2, core_3, core_4, core_5, core_6, core_7)
                if (coreCount > cores.size) coreCount = cores.size;
                for (i in 0 until cores.size) {
                    if (i >= coreCount) {
                        cores[i].isEnabled = false
                    }
                }

                bindEvent()
                inited = true
            } catch (ex: Exception) {

            }
        }
    }

    /*
    * 获得近似值
    */
    private fun getApproximation(arr: Array<String>, value: String): String {
        try {
            if (arr.contains(value)) {
                return value
            } else {
                var approximation = if (arr.isNotEmpty()) arr[0] else ""
                for (item in arr) {
                    if (item.toInt() <= value.toInt()) {
                        approximation = item
                    } else {
                        break
                    }
                }

                return approximation
            }
        } catch (ex: Exception) {
            return value
        }
    }

    @SuppressLint("InflateParams")
    private fun bindEvent() {
        try {
            thermal_core_control.setOnClickListener {
                ThermalControlUtils.setCoreControlState((it as CheckBox).isChecked, this.context)
            }
            thermal_vdd.setOnClickListener {
                ThermalControlUtils.setVDDRestrictionState((it as CheckBox).isChecked, this.context)
            }
            thermal_paramters.setOnClickListener {
                ThermalControlUtils.setTheramlState((it as CheckBox).isChecked, this.context)
            }
            cpu_sched_boost.setOnClickListener {
                CpuFrequencyUtils.setSechedBoostState((it as CheckBox).isChecked, this.context)
            }

            cluster_little_min_freq.setOnClickListener {
                var currentIndex = littleFreqs.indexOf(status.cluster_little_min_freq)
                if (currentIndex < 0) {
                    currentIndex = 0
                }
                var index = currentIndex

                AlertDialog.Builder(context)
                        .setTitle("选择Cluster0最小频率")
                        .setSingleChoiceItems(parseFreqList(littleFreqs), currentIndex, { dialog, which ->
                            index = which
                        })
                        .setPositiveButton(R.string.btn_confirm, { _, _ ->
                            if (index != currentIndex) {
                                val g = littleFreqs[index]
                                if (CpuFrequencyUtils.getCurrentMinFrequency(0) == g) {
                                    return@setPositiveButton
                                }
                                CpuFrequencyUtils.setMinFrequency(g, 0, context)
                                status.cluster_little_min_freq = g
                                setText(it as TextView?, subFreqStr(g))
                            }
                        })
                        .create()
                        .show()
            }
            cluster_little_max_freq.setOnClickListener {
                var currentIndex = littleFreqs.indexOf(status.cluster_little_max_freq)
                if (currentIndex < 0) {
                    currentIndex = 0
                }
                var index = currentIndex
                AlertDialog.Builder(context)
                        .setTitle("选择Cluster0最大频率")
                        .setSingleChoiceItems(parseFreqList(littleFreqs), currentIndex, { dialog, which ->
                            index = which
                        })
                        .setPositiveButton(R.string.btn_confirm, { _, _ ->
                            if (index != currentIndex) {
                                val g = littleFreqs[index]
                                if (CpuFrequencyUtils.getCurrentMinFrequency(0) == g) {
                                    return@setPositiveButton
                                }
                                CpuFrequencyUtils.setMaxFrequency(g, 0, context)
                                status.cluster_little_max_freq = g
                                setText(it as TextView?, subFreqStr(g))
                            }
                        })
                        .create()
                        .show()
            }
            // cluster_little_governor.onItemSelectedListener = ItemSelected(R.id.cluster_little_governor, next)
            cluster_little_governor.setOnClickListener {
                var currentIndex = littleGovernor.indexOf(status.cluster_little_governor)
                if (currentIndex < 0) {
                    currentIndex = 0
                }
                var index = currentIndex
                AlertDialog.Builder(context)
                        .setTitle("选择Cluster0调度模式")
                        .setSingleChoiceItems((littleGovernor), currentIndex, { dialog, which ->
                            index = which
                        })
                        .setPositiveButton(R.string.btn_confirm, { _, _ ->
                            if (index != currentIndex) {
                                val v = littleGovernor[index]
                                if (CpuFrequencyUtils.getCurrentScalingGovernor(0) == v) {
                                    return@setPositiveButton
                                }
                                CpuFrequencyUtils.setGovernor(v, 0, context)
                                status.cluster_little_governor = v
                                setText(it as TextView?, v)
                            }
                        })
                        .create()
                        .show()
                return@setOnClickListener
            }
            cluster_little_governor_params.setOnClickListener {
                if (status.cluster_little_governor_params != null) {
                    val msg = StringBuilder()
                    for (param in status.cluster_little_governor_params) {
                        msg.append("\n")
                        msg.append(param.key)
                        msg.append("：")
                        msg.append(param.value)
                        msg.append("\n")
                    }
                    AlertDialog.Builder(context)
                            .setTitle("Cluster0 调度器参数")
                            .setMessage(msg.toString())
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                            })
                            .create()
                            .show()
                }
            }

            if (hasBigCore) {
                cluster_big_min_freq.setOnClickListener {
                    var currentIndex = bigFreqs.indexOf(status.cluster_big_min_freq)
                    if (currentIndex < 0) {
                        currentIndex = 0
                    }
                    var index = currentIndex
                    AlertDialog.Builder(context)
                            .setTitle("选择Cluster1最小频率")
                            .setSingleChoiceItems(parseFreqList(bigFreqs), currentIndex, { dialog, which ->
                                index = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (index != currentIndex) {
                                    val v = bigFreqs[index]
                                    if (CpuFrequencyUtils.getCurrentMinFrequency(1) == v) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setMinFrequency(v, 1, context)
                                    status.cluster_big_min_freq = v
                                    setText(it as TextView?, subFreqStr(v))
                                }
                            })
                            .create()
                            .show()
                }
                cluster_big_max_freq.setOnClickListener {
                    var currentIndex = bigFreqs.indexOf(status.cluster_big_max_freq)
                    if (currentIndex < 0) {
                        currentIndex = 0
                    }
                    var index = currentIndex
                    AlertDialog.Builder(context)
                            .setTitle("选择Cluster1最大频率")
                            .setSingleChoiceItems(parseFreqList(bigFreqs), currentIndex, { dialog, which ->
                                index = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (index != currentIndex) {
                                    val v = bigFreqs[index]
                                    if (CpuFrequencyUtils.getCurrentMaxFrequency(1) == v) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setMaxFrequency(v, 1, context)
                                    status.cluster_big_max_freq = v
                                    setText(it as TextView?, subFreqStr(v))
                                }
                            })
                            .create()
                            .show()
                }
                cluster_big_governor.setOnClickListener {
                    var currentIndex = bigGovernor.indexOf(status.cluster_big_governor)
                    if (currentIndex < 0) {
                        currentIndex = 0
                    }
                    var governor = currentIndex
                    AlertDialog.Builder(context)
                            .setTitle("选择Cluster1调度模式")
                            .setSingleChoiceItems(bigGovernor, currentIndex, { dialog, which ->
                                governor = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (governor != currentIndex) {
                                    val g = bigGovernor[governor]
                                    if (CpuFrequencyUtils.getCurrentScalingGovernor(1) == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setGovernor(g, 1, context)
                                    status.cluster_big_governor = g
                                    setText(it as TextView?, g)
                                }
                            })
                            .create()
                            .show()
                }
                cluster_big_governor_params.setOnClickListener {
                    if (status.cluster_big_governor_params != null) {
                        val msg = StringBuilder()
                        for (param in status.cluster_big_governor_params) {
                            msg.append(param.key)
                            msg.append("：")
                            msg.append(param.value)
                            msg.append("\n\n")
                        }
                        AlertDialog.Builder(context)
                                .setTitle("Cluster1 调度器参数")
                                .setMessage(msg.toString())
                                .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                })
                                .create()
                                .show()
                    }
                }
            }

            if (adrenoGPU) {
                adreno_gpu_min_freq.setOnClickListener {
                    var currentIndex = adrenoFreqs.indexOf(status.adrenoMinFreq)
                    if (currentIndex < 0) {
                        currentIndex = 0
                    }
                    var index = currentIndex
                    AlertDialog.Builder(context)
                            .setTitle("选择Adreno GPU最小频率")
                            .setSingleChoiceItems(parseGPUFreqList(adrenoFreqs), currentIndex, { dialog, which ->
                                index = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (index != currentIndex) {
                                    val g = adrenoFreqs[index]
                                    if (CpuFrequencyUtils.getAdrenoGPUMinFreq() == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setAdrenoGPUMinFreq(g)
                                    status.adrenoMinFreq = g
                                    setText(it as TextView?, subGPUFreqStr(g))
                                }
                            })
                            .create()
                            .show()
                }
                adreno_gpu_max_freq.setOnClickListener {
                    var currentIndex = adrenoFreqs.indexOf(status.adrenoMaxFreq)
                    if (currentIndex < 0) {
                        currentIndex = 0
                    }
                    var index = currentIndex
                    AlertDialog.Builder(context)
                            .setTitle("选择Adreno GPU最大频率")
                            .setSingleChoiceItems(parseGPUFreqList(adrenoFreqs), currentIndex, { dialog, which ->
                                index = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (index != currentIndex) {
                                    val g = adrenoFreqs[index]
                                    if (CpuFrequencyUtils.getAdrenoGPUMaxFreq() == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setAdrenoGPUMaxFreq(g)
                                    status.adrenoMaxFreq = g
                                    setText(it as TextView?, subGPUFreqStr(g))
                                }
                            })
                            .create()
                            .show()
                }
                adreno_gpu_governor.setOnClickListener {
                    var currentIndex = adrenoGovernors.indexOf(status.adrenoGovernor)
                    if (currentIndex < 0) {
                        currentIndex = 0
                    }
                    var governor = currentIndex
                    AlertDialog.Builder(context)
                            .setTitle("选择Adreno GPU调度")
                            .setSingleChoiceItems((adrenoGovernors), currentIndex, { dialog, which ->
                                governor = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (governor != currentIndex) {
                                    val g = adrenoGovernors[governor]
                                    if (CpuFrequencyUtils.getAdrenoGPUGovernor() == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setAdrenoGPUGovernor(g)
                                    status.adrenoGovernor = g
                                    setText(it as TextView?, g)
                                }
                            })
                            .create()
                            .show()
                }
                adreno_gpu_min_pl.setOnClickListener {
                    var currentIndex = adrenoPLevels.indexOf(status.adrenoMinPL)
                    if (currentIndex < 0) {
                        currentIndex = 0
                    }
                    var index = currentIndex
                    AlertDialog.Builder(context)
                            .setTitle("选择GPU最小功耗级别")
                            .setSingleChoiceItems((adrenoPLevels), currentIndex, { dialog, which ->
                                index = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (index != currentIndex) {
                                    val g = adrenoPLevels[index]
                                    if (CpuFrequencyUtils.getAdrenoGPUMinPowerLevel() == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setAdrenoGPUMinPowerLevel(g)
                                    status.adrenoMinPL = g
                                    setText(it as TextView?, g)
                                }
                            })
                            .create()
                            .show()
                }
                adreno_gpu_max_pl.setOnClickListener {
                    var currentIndex = adrenoPLevels.indexOf(status.adrenoMaxPL)
                    if (currentIndex < 0) {
                        currentIndex = 0
                    }
                    var index = currentIndex
                    AlertDialog.Builder(context)
                            .setTitle("选择GPU最大功耗级别")
                            .setSingleChoiceItems((adrenoPLevels), currentIndex, { dialog, which ->
                                index = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (index != currentIndex) {
                                    val g = adrenoPLevels[index]
                                    if (CpuFrequencyUtils.getAdrenoGPUMaxPowerLevel() == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setAdrenoGPUMaxPowerLevel(g)
                                    status.adrenoMaxPL = g
                                    setText(it as TextView?, g)
                                }
                            })
                            .create()
                            .show()
                }
                adreno_gpu_default_pl.setOnClickListener {
                    var currentIndex = adrenoPLevels.indexOf(status.adrenoDefaultPL)
                    if (currentIndex < 0) {
                        currentIndex = 0
                    }
                    var index = currentIndex
                    AlertDialog.Builder(context)
                            .setTitle("选择GPU默认功耗级别")
                            .setSingleChoiceItems((adrenoPLevels), currentIndex, { dialog, which ->
                                index = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (index != currentIndex) {
                                    val g = adrenoPLevels[index]
                                    if (CpuFrequencyUtils.getAdrenoGPUDefaultPowerLevel() == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setAdrenoGPUDefaultPowerLevel(g)
                                    status.adrenoDefaultPL = g
                                    updateUI()
                                }
                            })
                            .create()
                            .show()
                }
            }

            cpu_inputboost_freq.setOnClickListener({ view ->
                val dialogView = layoutInflater.inflate(R.layout.cpu_inputboost_dialog, null)
                val currentValues = cpu_inputboost_freq.text.toString().split(" ")

                val clusterInfos = CpuFrequencyUtils.getClusterInfo()
                if (clusterInfos.size == 0)
                    return@setOnClickListener

                val cluster0 = dialogView.findViewById<Spinner>(R.id.boost_cluster0)
                val cluster1 = dialogView.findViewById<Spinner>(R.id.boost_cluster1)
                cluster0.adapter = StringAdapter(context, littleFreqs)
                for (value in currentValues) {
                    for (cpu in clusterInfos.get(0)) {
                        if (value.startsWith("$cpu:")) {
                            cluster0.setSelection(littleFreqs.indexOf(value.substring(2, value.length)), true)
                        }
                    }
                }

                if (clusterInfos.size == 1) {
                    cluster1.isEnabled = false
                } else {
                    cluster1.adapter = StringAdapter(context, bigFreqs)
                    for (value in currentValues) {
                        for (cpu in clusterInfos.get(1)) {
                            if (value.startsWith("$cpu:")) {
                                cluster1.setSelection(bigFreqs.indexOf(value.substring(2, value.length)), true)
                            }
                        }
                    }
                }

                AlertDialog.Builder(context)
                        .setTitle("input_boost_freq")
                        .setView(dialogView)
                        .setNegativeButton("确定", { dialog, which ->
                            val config = StringBuilder()
                            val c0 = cluster0.selectedItem.toString()
                            val c1 = cluster1.selectedItem.toString()
                            for (core in clusterInfos.get(0)) {
                                config.append(core)
                                config.append(":")
                                config.append(c0)
                                config.append(" ")
                            }
                            if (clusterInfos.size > 1) {
                                for (core in clusterInfos.get(1)) {
                                    config.append(core)
                                    config.append(":")
                                    config.append(c1)
                                    config.append(" ")
                                }
                            }
                            CpuFrequencyUtils.setInputBoosterFreq(config.toString().trim())
                        })
                        .create()
                        .show()
            })
            cpu_inputboost_time.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    CpuFrequencyUtils.setInputBoosterTime(v.text.toString())
                    val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    // 得到InputMethodManager的实例
                    if (imm.isActive()) {
                        // 如果开启
                        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS)
                    }
                }
                true
            }
            for (i in 0 until cores.size) {
                val core = i
                cores[core].setOnClickListener {
                    CpuFrequencyUtils.setCoreOnlineState(core, (it as CheckBox).isChecked)
                }
            }

            exynos_cpuhotplug.setOnClickListener {
                CpuFrequencyUtils.setExynosHotplug((it as CheckBox).isChecked)
            }
            exynos_hmp_booster.setOnClickListener {
                CpuFrequencyUtils.setExynosBooster((it as CheckBox).isChecked)
            }
            exynos_hmp_up.setOnSeekBarChangeListener(OnSeekBarChangeListener(true))
            exynos_hmp_down.setOnSeekBarChangeListener(OnSeekBarChangeListener(false))

            cpuset_bg.setOnClickListener {
                if (status.cpusetBackground.isNotEmpty()) {
                    val coreState = parsetCpuset(status.cpusetBackground)
                    AlertDialog.Builder(context)
                            .setTitle("选择要使用的核心")
                            .setMultiChoiceItems(getCoreList(), coreState, { dialog, which, isChecked ->
                                coreState[which] = isChecked
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                status.cpusetBackground = parsetCpuset(coreState)
                                KernelProrp.setProp("/dev/cpuset/background/cpus", status.cpusetBackground)
                                updateUI()
                            })
                            .setNegativeButton(R.string.btn_cancel, { _, _ ->
                            })
                            .create().show()
                }
            }
            cpuset_system_bg.setOnClickListener {
                if (status.cpusetSysBackground.isNotEmpty()) {
                    val coreState = parsetCpuset(status.cpusetSysBackground)
                    AlertDialog.Builder(context)
                            .setTitle("选择要使用的核心")
                            .setMultiChoiceItems(getCoreList(), coreState, { dialog, which, isChecked ->
                                coreState[which] = isChecked
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                status.cpusetSysBackground = parsetCpuset(coreState)
                                KernelProrp.setProp("/dev/cpuset/system-background/cpus", status.cpusetSysBackground)
                                updateUI()
                            })
                            .setNegativeButton(R.string.btn_cancel, { _, _ ->
                            })
                            .create().show()
                }
            }
            cpuset_foreground.setOnClickListener {
                if (status.cpusetForeground.isNotEmpty()) {
                    val coreState = parsetCpuset(status.cpusetForeground)
                    AlertDialog.Builder(context)
                            .setTitle("选择要使用的核心")
                            .setMultiChoiceItems(getCoreList(), coreState, { dialog, which, isChecked ->
                                coreState[which] = isChecked
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                status.cpusetForeground = parsetCpuset(coreState)
                                KernelProrp.setProp("/dev/cpuset/foreground/cpus", status.cpusetForeground)
                                updateUI()
                            })
                            .setNegativeButton(R.string.btn_cancel, { _, _ ->
                            })
                            .create().show()
                }
            }
            cpuset_top_app.setOnClickListener {
                if (status.cpusetTopApp.isNotEmpty()) {
                    val coreState = parsetCpuset(status.cpusetTopApp)
                    AlertDialog.Builder(context)
                            .setTitle("选择要使用的核心")
                            .setMultiChoiceItems(getCoreList(), coreState, { dialog, which, isChecked ->
                                coreState[which] = isChecked
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                status.cpusetTopApp = parsetCpuset(coreState)
                                KernelProrp.setProp("/dev/cpuset/top-app/cpus", status.cpusetTopApp)
                                updateUI()
                            })
                            .setNegativeButton(R.string.btn_cancel, { _, _ ->
                            })
                            .create().show()
                }
            }
            cpuset_boost.setOnClickListener {
                if (status.cpusetForegroundBoost.isNotEmpty()) {
                    val coreState = parsetCpuset(status.cpusetForegroundBoost)
                    AlertDialog.Builder(context)
                            .setTitle("选择要使用的核心")
                            .setMultiChoiceItems(getCoreList(), coreState, { dialog, which, isChecked ->
                                coreState[which] = isChecked
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                status.cpusetForegroundBoost = parsetCpuset(coreState)
                                KernelProrp.setProp("/dev/cpuset/foreground/boost/cpus", status.cpusetForegroundBoost)
                                updateUI()
                            })
                            .setNegativeButton(R.string.btn_cancel, { _, _ ->
                            })
                            .create().show()
                }
            }

            cpu_apply_onboot.setOnClickListener {
                saveBootConfig()
            }
        } catch (ex: Exception) {
            Log.e("bindEvent", ex.message)
        }
    }

    private fun parsetCpuset(booleanArray: BooleanArray): String {
        val stringBuilder = StringBuilder()
        for (index in 0..booleanArray.size - 1) {
            if (booleanArray.get(index)) {
                if (stringBuilder.length > 0) {
                    stringBuilder.append(",")
                }
                stringBuilder.append(index)
            }
        }
        return stringBuilder.toString()
    }

    private fun parsetCpuset(value: String): BooleanArray {
        val cores = ArrayList<Boolean>();
        for (coreIndex in 0..coreCount - 1) {
            cores.add(false)
        }
        if (value.isEmpty() || value == "error") {
        } else {
            val valueGroups = value.split(",")
            for (valueGroup in valueGroups) {
                if (valueGroup.contains("-")) {
                    try {
                        val range = valueGroup.split("-")
                        val min = range[0].toInt()
                        val max = range[1].toInt()
                        for (coreIndex in min..max) {
                            if (coreIndex < cores.size) {
                                cores[coreIndex] = true
                            }
                        }
                    } catch (ex: Exception) {
                    }
                } else {
                    try {
                        val coreIndex = valueGroup.toInt()
                        if (coreIndex < cores.size) {
                            cores[coreIndex] = true
                        }
                    } catch (ex: Exception) {
                    }
                }
            }
        }
        return cores.toBooleanArray()
    }

    private fun getCoreList(): Array<String> {
        val cores = ArrayList<String>();
        for (coreIndex in 0..coreCount - 1) {
            cores.add("Cpu" + coreIndex)
        }
        return cores.toTypedArray()
    }

    class OnSeekBarChangeListener(private var up: Boolean) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            if (seekBar != null) {
                if (up)
                    CpuFrequencyUtils.setExynosHmpUP(seekBar.progress)
                else
                    CpuFrequencyUtils.setExynosHmpDown(seekBar.progress)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        @SuppressLint("ApplySharedPref")
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        }
    }

    private var status = CpuStatus()

    private fun updateState() {
        Thread(Runnable {
            try {
                status.cluster_little_min_freq = getApproximation(littleFreqs, CpuFrequencyUtils.getCurrentMinFrequency(0))
                status.cluster_little_max_freq = getApproximation(littleFreqs, CpuFrequencyUtils.getCurrentMaxFrequency(0))
                status.cluster_little_governor = CpuFrequencyUtils.getCurrentScalingGovernor(0)
                status.cluster_little_governor_params = CpuFrequencyUtils.getCurrentScalingGovernorParams(0)
                status.coreControl = ThermalControlUtils.getCoreControlState()
                status.vdd = ThermalControlUtils.getVDDRestrictionState()
                status.msmThermal = ThermalControlUtils.getTheramlState()
                status.boost = CpuFrequencyUtils.getSechedBoostState()
                status.boostFreq = CpuFrequencyUtils.getInputBoosterFreq()
                status.boostTime = CpuFrequencyUtils.getInputBoosterTime()
                status.exynosHmpUP = CpuFrequencyUtils.getExynosHmpUP()
                status.exynosHmpDown = CpuFrequencyUtils.getExynosHmpDown()
                status.exynosHmpBooster = CpuFrequencyUtils.getExynosBooster()
                status.exynosHotplug = CpuFrequencyUtils.getExynosHotplug()

                if (adrenoGPU) {
                    status.adrenoDefaultPL = CpuFrequencyUtils.getAdrenoGPUDefaultPowerLevel()
                    status.adrenoMinPL = CpuFrequencyUtils.getAdrenoGPUMinPowerLevel()
                    status.adrenoMaxPL = CpuFrequencyUtils.getAdrenoGPUMaxPowerLevel()
                    status.adrenoMinFreq = getApproximation(adrenoFreqs, CpuFrequencyUtils.getAdrenoGPUMinFreq())
                    status.adrenoMaxFreq = getApproximation(adrenoFreqs, CpuFrequencyUtils.getAdrenoGPUMaxFreq())
                    status.adrenoGovernor = CpuFrequencyUtils.getAdrenoGPUGovernor()
                }

                if (hasBigCore) {
                    status.cluster_big_min_freq = getApproximation(bigFreqs, CpuFrequencyUtils.getCurrentMinFrequency(1))
                    status.cluster_big_max_freq = getApproximation(bigFreqs, CpuFrequencyUtils.getCurrentMaxFrequency(1))
                    status.cluster_big_governor = CpuFrequencyUtils.getCurrentScalingGovernor(1)
                    status.cluster_big_governor_params = CpuFrequencyUtils.getCurrentScalingGovernorParams(1)
                }

                status.coreOnline = arrayListOf<Boolean>()
                try {
                    mLock.lockInterruptibly()
                    for (i in 0 until coreCount) {
                        status.coreOnline.add(CpuFrequencyUtils.getCoreOnlineState(i))
                    }
                } catch (ex: Exception) {
                } finally {
                    mLock.unlock()
                }
                status.cpusetBackground = KernelProrp.getProp("/dev/cpuset/background/cpus")
                status.cpusetSysBackground = KernelProrp.getProp("/dev/cpuset/system-background/cpus")
                status.cpusetForeground = KernelProrp.getProp("/dev/cpuset/foreground/cpus")
                status.cpusetForegroundBoost = KernelProrp.getProp("/dev/cpuset/foreground/boost/cpus")
                status.cpusetTopApp = KernelProrp.getProp("/dev/cpuset/top-app/cpus")

                handler.post {
                    updateUI()
                }
            } catch (ex: Exception) {
            }
        }).start()
    }

    private val mLock = ReentrantLock()
    private fun subFreqStr(freq: String): String {
        if (freq.length > 3) {
            return freq.substring(0, freq.length - 3) + " Mhz"
        } else {
            return freq
        }
    }

    private fun subGPUFreqStr(freq: String): String {
        if (freq.isNullOrEmpty()) {
            return ""
        }
        if (freq.length > 6) {
            return freq.substring(0, freq.length - 6) + " Mhz"
        } else {
            return freq
        }
    }

    private fun parseFreqList(arr: Array<String>): Array<String> {
        val arrMhz = ArrayList<String>()
        for (item in arr) {
            arrMhz.add(subFreqStr(item))
        }
        return arrMhz.toTypedArray<String>()
    }


    private fun parseGPUFreqList(arr: Array<String>): Array<String> {
        val arrMhz = ArrayList<String>()
        for (item in arr) {
            arrMhz.add(subGPUFreqStr(item))
        }
        return arrMhz.toTypedArray<String>()
    }

    private fun setText(view: TextView?, text: String) {
        if (view != null && view.text != text) {
            view.setText(text)
        }
    }

    private fun updateUI() {
        progressBarDialog.hideDialog()
        try {
            setText(cluster_little_min_freq, subFreqStr(status.cluster_little_min_freq))
            setText(cluster_little_max_freq, subFreqStr(status.cluster_little_max_freq))
            setText(cluster_little_governor, status.cluster_little_governor)

            if (hasBigCore) {
                cluster_big_min_freq.setText(subFreqStr(status.cluster_big_min_freq))
                cluster_big_max_freq.setText(subFreqStr(status.cluster_big_max_freq))
                cluster_big_governor.setText(status.cluster_big_governor)
            }

            if (status.coreControl.isEmpty()) {
                thermal_core_control.isEnabled = false
            }
            thermal_core_control.isChecked = status.coreControl == "1"

            if (status.vdd.isEmpty()) {
                thermal_vdd.isEnabled = false
            }
            thermal_vdd.isChecked = status.vdd == "1"


            if (status.msmThermal.isEmpty()) {
                thermal_paramters.isEnabled = false
            }
            thermal_paramters.isChecked = status.msmThermal == "Y"

            if (status.boost.isEmpty()) {
                cpu_sched_boost.isEnabled = false
            }
            cpu_sched_boost.isChecked = status.boost == "1"

            exynos_hmp_down.setProgress(status.exynosHmpDown)
            exynos_hmp_down_text.setText(status.exynosHmpDown.toString())
            exynos_hmp_up.setProgress(status.exynosHmpUP)
            exynos_hmp_up_text.setText(status.exynosHmpUP.toString())
            exynos_cpuhotplug.isChecked = status.exynosHotplug
            exynos_hmp_booster.isChecked = status.exynosHmpBooster

            if (adrenoGPU) {
                adreno_gpu_default_pl.setText(status.adrenoDefaultPL)
                adreno_gpu_min_pl.setText(status.adrenoMinPL)
                adreno_gpu_max_pl.setText(status.adrenoMaxPL)
                adreno_gpu_min_freq.setText(subGPUFreqStr(status.adrenoMinFreq))
                adreno_gpu_max_freq.setText(subGPUFreqStr(status.adrenoMaxFreq))
                adreno_gpu_governor.setText(status.adrenoGovernor)
            }

            if (status.boostFreq.isEmpty()) {
                cpu_inputboost_freq.isEnabled = false
            }
            cpu_inputboost_freq.setText(status.boostFreq)

            if (status.boostTime.isEmpty()) {
                cpu_inputboost_time.isEnabled = false
            }
            if (!cpu_inputboost_time.isFocused)
                cpu_inputboost_time.setText(status.boostTime)

            for (i in 0 until coreCount) {
                cores[i].isChecked = status.coreOnline.get(i)
            }

            cpuset_bg.text = status.cpusetBackground
            cpuset_system_bg.text = status.cpusetSysBackground
            cpuset_foreground.text = status.cpusetForeground
            cpuset_top_app.text = status.cpusetTopApp
            cpuset_boost.text = status.cpusetForegroundBoost
        } catch (ex: Exception) {
        }
    }

    private lateinit var progressBarDialog: ProgressBarDialog
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBarDialog = ProgressBarDialog(this.context!!)
        Thread(Runnable {
            initData()
        }).start()
    }

    private fun loadBootConfig() {
        statusOnBoot = CpuConfigStorage().loadBootConfig(context!!)
        cpu_apply_onboot.isChecked = statusOnBoot != null
    }

    private fun saveBootConfig() {
        if (!CpuConfigStorage().saveBootConfig(context!!, if (cpu_apply_onboot.isChecked) status else null)) {
            Toast.makeText(context!!, "更新配置为启动设置失败！", Toast.LENGTH_SHORT).show()
            cpu_apply_onboot.isChecked = false
        }
    }

    private var timer: Timer? = null
    override fun onResume() {
        super.onResume()
        if (isDetached) {
            return
        }
        progressBarDialog.showDialog("正在读取信息...")
        loadBootConfig()
        if (timer == null) {
            timer = Timer()
            timer!!.schedule(object : TimerTask() {
                override fun run() {
                    handler.post({
                        if (!inited) {
                            return@post
                        }
                        updateState()
                    })
                }
            }, 1000, 3000)
        }
    }

    override fun onPause() {
        super.onPause()
        saveBootConfig()
        cancel()
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }

    private fun cancel() {
        try {
            if (timer != null) {
                timer!!.cancel()
                timer = null
            }
        } catch (ex: Exception) {

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_cpu_control, container, false)
    }

    companion object {
        fun newInstance(): FragmentCpuControl {
            val fragment = FragmentCpuControl()
            return fragment
        }
    }
}