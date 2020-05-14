package com.omarea.vtools.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.omarea.common.shell.KernelProrp
import com.omarea.common.ui.DialogHelper
import com.omarea.model.CpuClusterStatus
import com.omarea.model.CpuStatus
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.shell_utils.CpuFrequencyUtil
import com.omarea.shell_utils.GpuUtils
import com.omarea.shell_utils.ThermalControlUtils
import com.omarea.store.CpuConfigStorage
import com.omarea.store.SpfConfig
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.fragment_cpu_control.*
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FragmentCpuControl : androidx.fragment.app.Fragment() {
    // 应用到指定的配置模式
    private var cpuModeName: String? = null

    private var clusterCount = 0
    private var handler = Handler()
    private var coreCount = 0
    private var cores = arrayListOf<CheckBox>()
    private var exynosHMP = false
    private var supportedGPU = false
    private var adrenoGPU = false
    private var adrenoFreqs = arrayOf("")
    private var adrenoGovernors = arrayOf("")
    private var adrenoPLevels = arrayOf("")
    private var inited = false
    private var statusOnBoot: CpuStatus? = null

    val cluterFreqs: HashMap<Int, Array<String>> = HashMap()
    val cluterGovernors: HashMap<Int, Array<String>> = HashMap()

    private fun initData() {
        clusterCount = CpuFrequencyUtil.getClusterInfo().size
        for (cluster in 0 until clusterCount) {
            cluterFreqs.put(cluster, CpuFrequencyUtil.getAvailableFrequencies(cluster))
            cluterGovernors.put(cluster, CpuFrequencyUtil.getAvailableGovernors(cluster))
        }

        coreCount = CpuFrequencyUtil.getCoreCount()

        val exynosCpuhotplugSupport = CpuFrequencyUtil.exynosCpuhotplugSupport()
        exynosHMP = CpuFrequencyUtil.exynosHMP()

        supportedGPU = GpuUtils.supported()
        adrenoGPU = GpuUtils.isAdrenoGPU()

        if (supportedGPU) {
            adrenoGovernors = GpuUtils.getGovernors()
            adrenoFreqs = GpuUtils.getFreqs()
            adrenoPLevels = GpuUtils.getAdrenoGPUPowerLevels()
        }

        handler.post {
            try {
                if (exynosHMP || exynosCpuhotplugSupport) {
                    cpu_exynos.visibility = View.VISIBLE
                    exynos_cpuhotplug.isEnabled = exynosCpuhotplugSupport
                    exynos_hmp_up.isEnabled = exynosHMP
                    exynos_hmp_down.isEnabled = exynosHMP
                    exynos_hmp_booster.isEnabled = exynosHMP
                } else {
                    cpu_exynos.visibility = View.GONE
                }

                if (supportedGPU) {
                    gpu_params.visibility = View.VISIBLE
                    if (adrenoGPU) {
                        adreno_gpu_power.visibility = View.VISIBLE
                    } else {
                        adreno_gpu_power.visibility = View.GONE
                    }
                } else {
                    gpu_params.visibility = View.GONE
                    adreno_gpu_power.visibility = View.GONE
                }

                for (i in 0 until coreCount) {
                    val checkBox = CheckBox(context)
                    checkBox.text = "CPU$i"
                    cores.add(checkBox)
                    val params = GridLayout.LayoutParams()
                    params.height = GridLayout.LayoutParams.WRAP_CONTENT
                    params.width = GridLayout.LayoutParams.MATCH_PARENT
                    cpu_cores.addView(checkBox, params)
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
                ThermalControlUtils.setCoreControlState((it as CheckBox).isChecked)
            }
            thermal_vdd.setOnClickListener {
                ThermalControlUtils.setVDDRestrictionState((it as CheckBox).isChecked)
            }
            thermal_paramters.setOnClickListener {
                ThermalControlUtils.setTheramlState((it as CheckBox).isChecked)
            }
            cpu_sched_boost.setOnClickListener {
                CpuFrequencyUtil.setSechedBoostState((it as CheckBox).isChecked, this.context)
            }

            for (cluster in 0 until clusterCount) {
                handler.post {
                    bindClusterConfig(cluster)
                }
            }

            bindGPUConfig()

            for (i in 0 until cores.size) {
                val core = i
                cores[core].setOnClickListener {
                    CpuFrequencyUtil.setCoreOnlineState(core, (it as CheckBox).isChecked)
                }
            }

            bindExynosConfig()
            bindCpusetConfig()

            cpu_apply_onboot.setOnClickListener {
                saveBootConfig()
            }
        } catch (ex: Exception) {
        }
    }

    private fun bindGPUConfig() {
        if (supportedGPU) {
            gpu_min_freq.setOnClickListener {
                var currentIndex = adrenoFreqs.indexOf(status.adrenoMinFreq)
                if (currentIndex < 0) {
                    currentIndex = 0
                }
                var index = currentIndex
                DialogHelper.animDialog(AlertDialog.Builder(context)
                        .setTitle("选择GPU最小频率")
                        .setSingleChoiceItems(parseGPUFreqList(adrenoFreqs), currentIndex) { dialog, which ->
                            index = which
                        }
                        .setPositiveButton(R.string.btn_confirm) { _, _ ->
                            if (index != currentIndex) {
                                val g = adrenoFreqs[index]
                                if (GpuUtils.getMinFreq() == g) {
                                    return@setPositiveButton
                                }
                                GpuUtils.setMinFreq(g)
                                status.adrenoMinFreq = g
                                setText(it as TextView?, subGPUFreqStr(g))
                            }
                        })
            }
            gpu_max_freq.setOnClickListener {
                var currentIndex = adrenoFreqs.indexOf(status.adrenoMaxFreq)
                if (currentIndex < 0) {
                    currentIndex = 0
                }
                var index = currentIndex
                DialogHelper.animDialog(AlertDialog.Builder(context)
                        .setTitle("选择GPU最大频率")
                        .setSingleChoiceItems(parseGPUFreqList(adrenoFreqs), currentIndex) { dialog, which ->
                            index = which
                        }
                        .setPositiveButton(R.string.btn_confirm) { _, _ ->
                            if (index != currentIndex) {
                                val g = adrenoFreqs[index]
                                if (GpuUtils.getMaxFreq() == g) {
                                    return@setPositiveButton
                                }
                                GpuUtils.setMaxFreq(g)
                                status.adrenoMaxFreq = g
                                setText(it as TextView?, subGPUFreqStr(g))
                            }
                        })
            }
            gpu_governor.setOnClickListener {
                var currentIndex = adrenoGovernors.indexOf(status.adrenoGovernor)
                if (currentIndex < 0) {
                    currentIndex = 0
                }
                var governor = currentIndex
                DialogHelper.animDialog(AlertDialog.Builder(context)
                        .setTitle("选择GPU调度")
                        .setSingleChoiceItems((adrenoGovernors), currentIndex) { _, which ->
                            governor = which
                        }
                        .setPositiveButton(R.string.btn_confirm) { _, _ ->
                            if (governor != currentIndex) {
                                val g = adrenoGovernors[governor]
                                if (GpuUtils.getGovernor() == g) {
                                    return@setPositiveButton
                                }
                                GpuUtils.setGovernor(g)
                                status.adrenoGovernor = g
                                setText(it as TextView?, g)
                            }
                        })
            }
            if(adrenoGPU) {
                adreno_gpu_min_pl.setOnClickListener {
                    var currentIndex = adrenoPLevels.indexOf(status.adrenoMinPL)
                    if (currentIndex < 0) {
                        currentIndex = 0
                    }
                    var index = currentIndex
                    DialogHelper.animDialog(AlertDialog.Builder(context)
                            .setTitle("选择GPU最小功耗级别")
                            .setSingleChoiceItems((adrenoPLevels), currentIndex) { dialog, which ->
                                index = which
                            }
                            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                                if (index != currentIndex) {
                                    val g = adrenoPLevels[index]
                                    if (GpuUtils.getAdrenoGPUMinPowerLevel() == g) {
                                        return@setPositiveButton
                                    }
                                    GpuUtils.setAdrenoGPUMinPowerLevel(g)
                                    status.adrenoMinPL = g
                                    setText(it as TextView?, g)
                                }
                            })
                }
                adreno_gpu_max_pl.setOnClickListener {
                    var currentIndex = adrenoPLevels.indexOf(status.adrenoMaxPL)
                    if (currentIndex < 0) {
                        currentIndex = 0
                    }
                    var index = currentIndex
                    DialogHelper.animDialog(AlertDialog.Builder(context)
                            .setTitle("选择GPU最大功耗级别")
                            .setSingleChoiceItems((adrenoPLevels), currentIndex) { dialog, which ->
                                index = which
                            }
                            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                                if (index != currentIndex) {
                                    val g = adrenoPLevels[index]
                                    if (GpuUtils.getAdrenoGPUMaxPowerLevel() == g) {
                                        return@setPositiveButton
                                    }
                                    GpuUtils.setAdrenoGPUMaxPowerLevel(g)
                                    status.adrenoMaxPL = g
                                    setText(it as TextView?, g)
                                }
                            })
                }
                adreno_gpu_default_pl.setOnClickListener {
                    var currentIndex = adrenoPLevels.indexOf(status.adrenoDefaultPL)
                    if (currentIndex < 0) {
                        currentIndex = 0
                    }
                    var index = currentIndex
                    DialogHelper.animDialog(AlertDialog.Builder(context)
                            .setTitle("选择GPU默认功耗级别")
                            .setSingleChoiceItems((adrenoPLevels), currentIndex) { _, which ->
                                index = which
                            }
                            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                                if (index != currentIndex) {
                                    val g = adrenoPLevels[index]
                                    if (GpuUtils.getAdrenoGPUDefaultPowerLevel() == g) {
                                        return@setPositiveButton
                                    }
                                    GpuUtils.setAdrenoGPUDefaultPowerLevel(g)
                                    status.adrenoDefaultPL = g
                                    updateUI()
                                }
                            })
                }
            }
        }
    }

    private fun bindExynosConfig() {
        exynos_cpuhotplug.setOnClickListener {
            CpuFrequencyUtil.setExynosHotplug((it as CheckBox).isChecked)
        }
        exynos_hmp_booster.setOnClickListener {
            CpuFrequencyUtil.setExynosBooster((it as CheckBox).isChecked)
        }
        exynos_hmp_up.setOnSeekBarChangeListener(OnSeekBarChangeListener(true))
        exynos_hmp_down.setOnSeekBarChangeListener(OnSeekBarChangeListener(false))
    }

    private fun bindCpusetConfig() {
        cpuset_bg.setOnClickListener {
            if (status.cpusetBackground.isNotEmpty()) {
                val coreState = parsetCpuset(status.cpusetBackground)
                DialogHelper.animDialog(AlertDialog.Builder(context)
                        .setTitle("选择要使用的核心")
                        .setMultiChoiceItems(getCoreList(), coreState) { _, which, isChecked ->
                            coreState[which] = isChecked
                        }
                        .setPositiveButton(R.string.btn_confirm) { _, _ ->
                            status.cpusetBackground = parsetCpuset(coreState)
                            KernelProrp.setProp("/dev/cpuset/background/cpus", status.cpusetBackground)
                            updateUI()
                        }
                        .setNegativeButton(R.string.btn_cancel) { _, _ ->
                        })
            }
        }
        cpuset_system_bg.setOnClickListener {
            if (status.cpusetSysBackground.isNotEmpty()) {
                val coreState = parsetCpuset(status.cpusetSysBackground)
                DialogHelper.animDialog(AlertDialog.Builder(context)
                        .setTitle("选择要使用的核心")
                        .setMultiChoiceItems(getCoreList(), coreState) { dialog, which, isChecked ->
                            coreState[which] = isChecked
                        }
                        .setPositiveButton(R.string.btn_confirm) { _, _ ->
                            status.cpusetSysBackground = parsetCpuset(coreState)
                            KernelProrp.setProp("/dev/cpuset/system-background/cpus", status.cpusetSysBackground)
                            updateUI()
                        }
                        .setNegativeButton(R.string.btn_cancel) { _, _ ->
                        })
            }
        }
        cpuset_foreground.setOnClickListener {
            if (status.cpusetForeground.isNotEmpty()) {
                val coreState = parsetCpuset(status.cpusetForeground)
                DialogHelper.animDialog(AlertDialog.Builder(context)
                        .setTitle("选择要使用的核心")
                        .setMultiChoiceItems(getCoreList(), coreState) { dialog, which, isChecked ->
                            coreState[which] = isChecked
                        }
                        .setPositiveButton(R.string.btn_confirm) { _, _ ->
                            status.cpusetForeground = parsetCpuset(coreState)
                            KernelProrp.setProp("/dev/cpuset/foreground/cpus", status.cpusetForeground)
                            updateUI()
                        }
                        .setNegativeButton(R.string.btn_cancel) { _, _ ->
                        })
            }
        }
        cpuset_top_app.setOnClickListener {
            if (status.cpusetTopApp.isNotEmpty()) {
                val coreState = parsetCpuset(status.cpusetTopApp)
                DialogHelper.animDialog(AlertDialog.Builder(context)
                        .setTitle("选择要使用的核心")
                        .setMultiChoiceItems(getCoreList(), coreState) { dialog, which, isChecked ->
                            coreState[which] = isChecked
                        }
                        .setPositiveButton(R.string.btn_confirm) { _, _ ->
                            status.cpusetTopApp = parsetCpuset(coreState)
                            KernelProrp.setProp("/dev/cpuset/top-app/cpus", status.cpusetTopApp)
                            updateUI()
                        }
                        .setNegativeButton(R.string.btn_cancel) { _, _ ->
                        })
            }
        }
        cpuset_boost.setOnClickListener {
            if (status.cpusetForegroundBoost.isNotEmpty()) {
                val coreState = parsetCpuset(status.cpusetForegroundBoost)
                DialogHelper.animDialog(AlertDialog.Builder(context)
                        .setTitle("选择要使用的核心")
                        .setMultiChoiceItems(getCoreList(), coreState) { dialog, which, isChecked ->
                            coreState[which] = isChecked
                        }
                        .setPositiveButton(R.string.btn_confirm) { _, _ ->
                            status.cpusetForegroundBoost = parsetCpuset(coreState)
                            KernelProrp.setProp("/dev/cpuset/foreground/boost/cpus", status.cpusetForegroundBoost)
                            updateUI()
                        }
                        .setNegativeButton(R.string.btn_cancel) { _, _ ->
                        })
            }
        }
    }

    private fun bindClusterConfig(cluster: Int) {
        val view = View.inflate(this.context!!, R.layout.fragment_cpu_cluster, null)
        cpu_cluster_list.addView(view)
        view.findViewById<TextView>(R.id.cluster_title).text = "Cluster $cluster"
        view.tag = "cluster_$cluster"

        val cluster_min_freq = view.findViewById<TextView>(R.id.cluster_min_freq)
        val cluster_max_freq = view.findViewById<TextView>(R.id.cluster_max_freq)
        val cluster_governor = view.findViewById<TextView>(R.id.cluster_governor)
        val cluster_governor_params = view.findViewById<TextView>(R.id.cluster_governor_params)

        cluster_min_freq.setOnClickListener {
            var currentIndex = cluterFreqs[cluster]!!.indexOf(getApproximation(cluterFreqs[cluster]!!, status.cpuClusterStatuses[cluster].min_freq))
            if (currentIndex < 0) {
                currentIndex = 0
            }
            var index = currentIndex

            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle("选择最小频率")
                    .setSingleChoiceItems(parseFreqList(cluterFreqs[cluster]!!), currentIndex) { dialog, which ->
                        index = which
                    }
                    .setPositiveButton(R.string.btn_confirm) { _, _ ->
                        if (index != currentIndex) {
                            val g = cluterFreqs[cluster]!![index]
                            if (CpuFrequencyUtil.getCurrentMinFrequency(cluster) == g) {
                                return@setPositiveButton
                            }
                            CpuFrequencyUtil.setMinFrequency(g, cluster)
                            status.cpuClusterStatuses[cluster].min_freq = g
                            setText(it as TextView?, subFreqStr(g))
                        }
                    })
        }
        cluster_max_freq.setOnClickListener {
            var currentIndex = cluterFreqs[cluster]!!.indexOf(getApproximation(cluterFreqs[cluster]!!, status.cpuClusterStatuses[cluster].max_freq))
            if (currentIndex < 0) {
                currentIndex = 0
            }
            var index = currentIndex
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle("选择最大频率")
                    .setSingleChoiceItems(parseFreqList(cluterFreqs[cluster]!!), currentIndex) { dialog, which ->
                        index = which
                    }
                    .setPositiveButton(R.string.btn_confirm) { _, _ ->
                        if (index != currentIndex) {
                            val g = cluterFreqs[cluster]!![index]
                            if (CpuFrequencyUtil.getCurrentMinFrequency(cluster) == g) {
                                return@setPositiveButton
                            }
                            CpuFrequencyUtil.setMaxFrequency(g, cluster)
                            status.cpuClusterStatuses[cluster].max_freq = g
                            setText(it as TextView?, subFreqStr(g))
                        }
                    })
        }
        // cluster_little_governor.onItemSelectedListener = ItemSelected(R.id.cluster_little_governor, next)
        cluster_governor.setOnClickListener {
            var currentIndex = cluterGovernors[cluster]!!.indexOf(status.cpuClusterStatuses[cluster].governor)
            if (currentIndex < 0) {
                currentIndex = 0
            }
            var index = currentIndex
            DialogHelper.animDialog(AlertDialog.Builder(context)
                    .setTitle("选择调度模式")
                    .setSingleChoiceItems((cluterGovernors[cluster]!!), currentIndex) { dialog, which ->
                        index = which
                    }
                    .setPositiveButton(R.string.btn_confirm) { _, _ ->
                        if (index != currentIndex) {
                            val v = cluterGovernors[cluster]!![index]
                            if (CpuFrequencyUtil.getCurrentScalingGovernor(cluster) == v) {
                                return@setPositiveButton
                            }
                            CpuFrequencyUtil.setGovernor(v, cluster)
                            status.cpuClusterStatuses[cluster].governor = v
                            setText(it as TextView?, v)
                        }
                    })
            return@setOnClickListener
        }
        cluster_governor_params.setOnClickListener {
            status.cpuClusterStatuses[cluster].governor_params = CpuFrequencyUtil.getCurrentScalingGovernorParams(cluster)

            if (status.cpuClusterStatuses[cluster].governor_params != null) {
                val msg = StringBuilder()
                for (param in status.cpuClusterStatuses[cluster].governor_params) {
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
    }

    private fun parsetCpuset(booleanArray: BooleanArray): String {
        val stringBuilder = StringBuilder()
        for (index in booleanArray.indices) {
            if (booleanArray.get(index)) {
                if (stringBuilder.isNotEmpty()) {
                    stringBuilder.append(",")
                }
                stringBuilder.append(index)
            }
        }
        return stringBuilder.toString()
    }

    private fun parsetCpuset(value: String): BooleanArray {
        val cores = ArrayList<Boolean>();
        for (coreIndex in 0 until coreCount) {
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
        for (coreIndex in 0 until coreCount) {
            cores.add("Cpu$coreIndex")
        }
        return cores.toTypedArray()
    }

    class OnSeekBarChangeListener(private var up: Boolean) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            if (seekBar != null) {
                if (up)
                    CpuFrequencyUtil.setExynosHmpUP(seekBar.progress)
                else
                    CpuFrequencyUtil.setExynosHmpDown(seekBar.progress)
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
        try {
            for (cluster in 0 until clusterCount) {
                if (status.cpuClusterStatuses.size < cluster + 1) {
                    status.cpuClusterStatuses.add(CpuClusterStatus())
                }
                val config = status.cpuClusterStatuses.get(cluster)
                config.min_freq = CpuFrequencyUtil.getCurrentMinFrequency(cluster)
                config.max_freq = CpuFrequencyUtil.getCurrentMaxFrequency(cluster)
                config.governor = CpuFrequencyUtil.getCurrentScalingGovernor(cluster)
                // TODO: 要不要加载 config.governor_params = CpuFrequencyUtil.getCurrentScalingGovernorParams(cluster)
            }

            status.coreControl = ThermalControlUtils.getCoreControlState()
            status.vdd = ThermalControlUtils.getVDDRestrictionState()
            status.msmThermal = ThermalControlUtils.getTheramlState()

            status.boost = CpuFrequencyUtil.getSechedBoostState()
            status.boostFreq = CpuFrequencyUtil.getInputBoosterFreq()
            status.boostTime = CpuFrequencyUtil.getInputBoosterTime()

            status.exynosHmpUP = CpuFrequencyUtil.getExynosHmpUP()
            status.exynosHmpDown = CpuFrequencyUtil.getExynosHmpDown()
            status.exynosHmpBooster = CpuFrequencyUtil.getExynosBooster()
            status.exynosHotplug = CpuFrequencyUtil.getExynosHotplug()

            if (supportedGPU) {
                if (adrenoGPU) {
                    status.adrenoDefaultPL = GpuUtils.getAdrenoGPUDefaultPowerLevel()
                    status.adrenoMinPL = GpuUtils.getAdrenoGPUMinPowerLevel()
                    status.adrenoMaxPL = GpuUtils.getAdrenoGPUMaxPowerLevel()
                }
                status.adrenoMinFreq = getApproximation(adrenoFreqs, GpuUtils.getMinFreq())
                status.adrenoMaxFreq = getApproximation(adrenoFreqs, GpuUtils.getMaxFreq())
                status.adrenoGovernor = GpuUtils.getGovernor()
            }

            status.coreOnline = arrayListOf<Boolean>()
            try {
                mLock.lockInterruptibly()
                for (i in 0 until coreCount) {
                    status.coreOnline.add(CpuFrequencyUtil.getCoreOnlineState(i))
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
        return if (freq.length > 6) {
            freq.substring(0, freq.length - 6) + " Mhz"
        } else {
            freq
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
        try {
            for (cluster in 0 until clusterCount) {
                if (status.cpuClusterStatuses.size > cluster) {
                    val cluster_view = view!!.findViewWithTag<View>("cluster_" + cluster)
                    val cluster_min_freq = cluster_view.findViewById<TextView>(R.id.cluster_min_freq)
                    val cluster_max_freq = cluster_view.findViewById<TextView>(R.id.cluster_max_freq)
                    val cluster_governor = cluster_view.findViewById<TextView>(R.id.cluster_governor)
                    val status = status.cpuClusterStatuses[cluster]!!
                    setText(cluster_min_freq, subFreqStr(status.min_freq))
                    setText(cluster_max_freq, subFreqStr(status.max_freq))
                    setText(cluster_governor, status.governor)
                }
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

            exynos_hmp_down.progress = status.exynosHmpDown
            exynos_hmp_down_text.text = status.exynosHmpDown.toString()
            exynos_hmp_up.progress = status.exynosHmpUP
            exynos_hmp_up_text.text = status.exynosHmpUP.toString()
            exynos_cpuhotplug.isChecked = status.exynosHotplug
            exynos_hmp_booster.isChecked = status.exynosHmpBooster

            if (supportedGPU) {
                if (adrenoGPU) {
                    adreno_gpu_default_pl.text = status.adrenoDefaultPL
                    adreno_gpu_min_pl.text = status.adrenoMinPL
                    adreno_gpu_max_pl.text = status.adrenoMaxPL
                }
                gpu_min_freq.text = subGPUFreqStr(status.adrenoMinFreq)
                gpu_max_freq.text = subGPUFreqStr(status.adrenoMaxFreq)
                gpu_governor.text = status.adrenoGovernor
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Thread(Runnable {
            initData()
        }).start()

        val globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        val dynamic = AccessibleServiceHelper().serviceRunning(context!!) && globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)
        if (dynamic && (cpuModeName == null)) {
            DialogHelper.animDialog(AlertDialog.Builder(context!!)
                    .setTitle("请注意")
                    .setMessage("检测到你已开启“性能调节”，你手动对CPU、GPU的修改随时可能被覆盖。\n\n同时，手动调整参数还可能对“性能调节”的工作造成不利影响！")
                    .setPositiveButton(R.string.btn_confirm) { _, _ ->
                    }
                    .setCancelable(false))
        }
    }

    private fun loadBootConfig() {
        val storage = CpuConfigStorage(context!!)
        statusOnBoot = storage.load(cpuModeName)
        cpu_apply_onboot.isChecked = statusOnBoot != null

        if (cpuModeName != null) {
            ModeSwitcher().executePowercfgMode(cpuModeName!!)

            val modeName = ModeSwitcher.getModName(cpuModeName!!)
            cpu_apply_onboot.setText("自定义 $modeName ")
            cpu_apply_onboot_desc.setText("自定义 [$modeName] 的具体参数，覆盖Scene原始设定")
            cpu_help_text.visibility = View.GONE
        }
    }

    private fun saveBootConfig() {
        if (!CpuConfigStorage(context!!).saveCpuConfig(if (cpu_apply_onboot.isChecked) status else null, cpuModeName)) {
            Toast.makeText(context!!, "保存配置文件失败！", Toast.LENGTH_SHORT).show()
            cpu_apply_onboot.isChecked = false
        }
    }

    private var timer: Timer? = null
    override fun onResume() {
        super.onResume()
        if (isDetached) {
            return
        }
        if (cpuModeName == null) {
            activity!!.title = getString(R.string.menu_core_control)
        }

        loadBootConfig()
        if (timer == null) {
            timer = Timer()
            timer!!.schedule(object : TimerTask() {
                override fun run() {
                    if (!inited) {
                        return
                    }
                    updateState()
                }
            }, 1000, 1000)
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

        fun newInstance(mode: String): FragmentCpuControl {
            val fragment = FragmentCpuControl()
            fragment.cpuModeName = mode
            return fragment
        }
    }
}