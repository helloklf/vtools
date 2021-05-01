package com.omarea.vtools.activities

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import com.omarea.common.model.SelectItem
import com.omarea.common.shell.KernelProrp
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.DialogItemChooser
import com.omarea.common.ui.DialogItemChooser2
import com.omarea.library.shell.CpuFrequencyUtils
import com.omarea.library.shell.GpuUtils
import com.omarea.library.shell.ThermalControlUtils
import com.omarea.model.CpuClusterStatus
import com.omarea.model.CpuStatus
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.CpuConfigStorage
import com.omarea.store.SpfConfig
import com.omarea.utils.AccessibleServiceHelper
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.activity_cpu_control.*
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ActivityCpuControl : ActivityBase() {
    // 应用到指定的配置模式
    private var cpuModeName: String? = null

    private var clusterCount = 0
    private var handler = Handler(Looper.getMainLooper())
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

    private val thermalControlUtils = ThermalControlUtils()
    private val CpuFrequencyUtil = CpuFrequencyUtils()
    var qualcommThermalSupported: Boolean = false

    private fun initData() {
        clusterCount = CpuFrequencyUtil.getClusterInfo().size
        for (cluster in 0 until clusterCount) {
            cluterFreqs.put(cluster, CpuFrequencyUtil.getAvailableFrequencies(cluster))
            cluterGovernors.put(cluster, CpuFrequencyUtil.getAvailableGovernors(cluster))
        }

        coreCount = CpuFrequencyUtil.coreCount

        val exynosCpuhotplugSupport = CpuFrequencyUtil.exynosCpuhotplugSupport()
        exynosHMP = CpuFrequencyUtil.exynosHMP()

        supportedGPU = GpuUtils.supported()
        adrenoGPU = GpuUtils.isAdrenoGPU()
        qualcommThermalSupported = thermalControlUtils.isSupported()

        if (supportedGPU) {
            adrenoGovernors = GpuUtils.getGovernors()
            adrenoFreqs = GpuUtils.getAvailableFreqs()
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
                thermalControlUtils.setCoreControlState((it as CheckBox).isChecked)
            }
            thermal_vdd.setOnClickListener {
                thermalControlUtils.setVDDRestrictionState((it as CheckBox).isChecked)
            }
            thermal_paramters.setOnClickListener {
                thermalControlUtils.setTheramlState((it as CheckBox).isChecked)
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
            bindCpuSetConfig()

            cpu_apply_onboot.setOnClickListener {
                saveBootConfig()
            }
        } catch (ex: Exception) {
        }
    }

    interface PickerCallback {
        fun onSelected(result: String)
    }

    interface PickerCallback2 {
        fun onSelected(result: BooleanArray)
    }

    private fun openMultiplePicker(dialogTitle: String, options: ArrayList<SelectItem>, selectedIndex: Int, pickerCallback: PickerCallback) {
        val selected = (ArrayList<SelectItem>().apply {
            if (selectedIndex > -1) {
                add(options.get(selectedIndex))
            }
        })
        DialogItemChooser2(themeMode.isDarkMode, options, selected, false, object : DialogItemChooser2.Callback {
            override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                if (selected.isNotEmpty()) {
                    pickerCallback.onSelected("" + selected.first().value)
                }
            }
        }).setTitle(dialogTitle).show(supportFragmentManager, "cpu-control")
    }

    private fun openMultiplePicker(dialogTitle: String, options: ArrayList<SelectItem>, pickerCallback: PickerCallback2) {
        DialogItemChooser(themeMode.isDarkMode, options,true, object : DialogItemChooser.Callback {
            override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                if (status.isNotEmpty()) {
                    pickerCallback.onSelected(status)
                }
            }
        }).setTitle(dialogTitle).show(supportFragmentManager, "cpu-control")
    }

    private fun string2SelectItem(items: Array<String>): ArrayList<SelectItem> {
        return ArrayList(items.map {
            SelectItem().apply {
                title = it
                value = it
            }
        })
    }

    private fun bindGPUConfig() {
        if (supportedGPU) {
            gpu_min_freq.setOnClickListener {
                openMultiplePicker("选择GPU最小频率",
                        parseGPUFreqList(adrenoFreqs),
                        adrenoFreqs.indexOf(status.adrenoMinFreq),
                        object : PickerCallback {
                            override fun onSelected(freq: String) {
                                if (GpuUtils.getMinFreq() != freq) {
                                    GpuUtils.setMinFreq(freq)
                                    status.adrenoMinFreq = freq
                                    setText(it as TextView?, subGPUFreqStr(freq))
                                }
                            }
                        })
            }
            gpu_max_freq.setOnClickListener {
                openMultiplePicker("选择GPU最大频率",
                        parseGPUFreqList(adrenoFreqs),
                        adrenoFreqs.indexOf(status.adrenoMaxFreq),
                        object : PickerCallback {
                            override fun onSelected(result: String) {
                                if (GpuUtils.getMaxFreq() != result) {
                                    GpuUtils.setMaxFreq(result)
                                    status.adrenoMaxFreq = result
                                    setText(it as TextView?, subGPUFreqStr(result))
                                }
                            }
                        })
            }
            gpu_governor.setOnClickListener {
                openMultiplePicker("选择GPU调度",
                        string2SelectItem(adrenoGovernors),
                        adrenoGovernors.indexOf(status.adrenoGovernor),
                        object : PickerCallback {
                            override fun onSelected(result: String) {
                                if (GpuUtils.getGovernor() != result) {
                                    GpuUtils.setGovernor(result)
                                    status.adrenoGovernor = result
                                    setText(it as TextView?, result)
                                }
                            }
                        })
            }
            if (adrenoGPU) {
                adreno_gpu_min_pl.setOnClickListener {
                    openMultiplePicker("选择GPU最小功耗级别",
                            string2SelectItem(adrenoPLevels),
                            adrenoPLevels.indexOf(status.adrenoMinPL),
                            object : PickerCallback {
                                override fun onSelected(result: String) {
                                    if (GpuUtils.getAdrenoGPUMinPowerLevel() != result) {
                                        GpuUtils.setAdrenoGPUMinPowerLevel(result)
                                        status.adrenoMinPL = result
                                        setText(it as TextView?, result)
                                    }
                                }
                            })
                }
                adreno_gpu_max_pl.setOnClickListener {
                    openMultiplePicker("选择GPU最大功耗级别",
                            string2SelectItem(adrenoPLevels),
                            adrenoPLevels.indexOf(status.adrenoMaxPL),
                            object : PickerCallback {
                                override fun onSelected(result: String) {
                                    if (GpuUtils.getAdrenoGPUMaxPowerLevel() != result) {
                                        GpuUtils.setAdrenoGPUMaxPowerLevel(result)
                                        status.adrenoMaxPL = result
                                        setText(it as TextView?, result)
                                    }
                                }
                            })
                }
                adreno_gpu_default_pl.setOnClickListener {
                    openMultiplePicker("选择GPU默认功耗级别",
                            string2SelectItem(adrenoPLevels),
                            adrenoPLevels.indexOf(status.adrenoDefaultPL),
                            object : PickerCallback {
                                override fun onSelected(result: String) {
                                    if (GpuUtils.getAdrenoGPUDefaultPowerLevel() != result) {
                                        GpuUtils.setAdrenoGPUDefaultPowerLevel(result)
                                        status.adrenoDefaultPL = result
                                        updateUI()
                                    }
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
        exynos_hmp_up.setOnSeekBarChangeListener(OnSeekBarChangeListener(true, CpuFrequencyUtil))
        exynos_hmp_down.setOnSeekBarChangeListener(OnSeekBarChangeListener(false, CpuFrequencyUtil))
    }

    private fun bindCpuSetConfig(currentState: String, callback: PickerCallback2) {
        if (currentState.isNotEmpty()) {
            val coreState = parsetCpuset(currentState)
            openMultiplePicker("选择要使用的核心",
                    getCoreList(coreState),
                    object: PickerCallback2 {
                        override fun onSelected(result: BooleanArray) {
                            callback.onSelected(result)
                            updateUI()
                        }
                    })
        }
    }

    private fun bindCpuSetConfig() {
        cpuset_bg.setOnClickListener {
            bindCpuSetConfig(status.cpusetBackground, object: PickerCallback2 {
                override fun onSelected(result: BooleanArray) {
                    status.cpusetBackground = parsetCpuset(result)
                    KernelProrp.setProp("/dev/cpuset/background/cpus", status.cpusetBackground)
                }
            })
        }
        cpuset_system_bg.setOnClickListener {
            bindCpuSetConfig(status.cpusetSysBackground, object: PickerCallback2 {
                override fun onSelected(result: BooleanArray) {
                    status.cpusetSysBackground = parsetCpuset(result)
                    KernelProrp.setProp("/dev/cpuset/system-background/cpus", status.cpusetSysBackground)
                }
            })
        }
        cpuset_foreground.setOnClickListener {
            bindCpuSetConfig(status.cpusetForeground, object: PickerCallback2 {
                override fun onSelected(result: BooleanArray) {
                    status.cpusetForeground = parsetCpuset(result)
                    KernelProrp.setProp("/dev/cpuset/foreground/cpus", status.cpusetForeground)
                }
            })
        }
        cpuset_top_app.setOnClickListener {
            bindCpuSetConfig(status.cpusetTopApp, object: PickerCallback2 {
                override fun onSelected(result: BooleanArray) {
                    status.cpusetTopApp = parsetCpuset(result)
                    KernelProrp.setProp("/dev/cpuset/top-app/cpus", status.cpusetTopApp)
                }
            })
        }
    }

    private fun getClusterFreqs(cluster: Int): Array<String> {
        val freqs = cluterFreqs[cluster]
        if (freqs == null || freqs.size < 2) {
            cluterFreqs[cluster] = CpuFrequencyUtil.getAvailableFrequencies(cluster)
        }
        return cluterFreqs[cluster]!!
    }

    private fun getClusterGovernors(cluster: Int): Array<String> {
        val freqs = cluterGovernors[cluster]
        if (freqs == null || freqs.size < 2) {
            cluterFreqs[cluster] = CpuFrequencyUtil.getAvailableGovernors(cluster)
        }
        return cluterGovernors[cluster]!!
    }

    private fun bindClusterConfig(cluster: Int) {
        val view = View.inflate(context, R.layout.fragment_cpu_cluster, null)
        cpu_cluster_list.addView(view)
        view.findViewById<TextView>(R.id.cluster_title).text = "CPU - Cluster $cluster"
        view.tag = "cluster_$cluster"

        val cluster_min_freq = view.findViewById<TextView>(R.id.cluster_min_freq)
        val cluster_max_freq = view.findViewById<TextView>(R.id.cluster_max_freq)
        val cluster_governor = view.findViewById<TextView>(R.id.cluster_governor)
        val cluster_governor_params = view.findViewById<TextView>(R.id.cluster_governor_params)

        cluster_min_freq.setOnClickListener {
            val freqs = getClusterFreqs(cluster)
            openMultiplePicker("选择最小频率",
                    parseFreqList(freqs),
                    freqs.indexOf(getApproximation(freqs, status.cpuClusterStatuses[cluster].min_freq)),
                    object: PickerCallback {
                        override fun onSelected(result: String) {
                            if (CpuFrequencyUtil.getCurrentMinFrequency(cluster) != result) {
                                CpuFrequencyUtil.setMinFrequency(result, cluster)
                                status.cpuClusterStatuses[cluster].min_freq = result
                                setText(it as TextView?, subFreqStr(result))
                            }
                        }
                    })
        }

        cluster_max_freq.setOnClickListener {
            val freqs = getClusterFreqs(cluster)
            openMultiplePicker("选择最大频率",
                    parseFreqList(freqs),
                    freqs.indexOf(getApproximation(freqs, status.cpuClusterStatuses[cluster].max_freq)),
                    object: PickerCallback {
                        override fun onSelected(result: String) {
                            if (CpuFrequencyUtil.getCurrentMinFrequency(cluster) != result) {
                                CpuFrequencyUtil.setMaxFrequency(result, cluster)
                                status.cpuClusterStatuses[cluster].max_freq = result
                                setText(it as TextView?, subFreqStr(result))
                            }
                        }
                    })
        }

        // cluster_little_governor.onItemSelectedListener = ItemSelected(R.id.cluster_little_governor, next)
        cluster_governor.setOnClickListener {
            val governors = getClusterGovernors(cluster)
            openMultiplePicker("选择调度模式",
                    string2SelectItem(governors),
                    governors.indexOf(status.cpuClusterStatuses[cluster].governor),
                    object: PickerCallback {
                        override fun onSelected(result: String) {
                            if (CpuFrequencyUtil.getCurrentScalingGovernor(cluster) != result) {
                                CpuFrequencyUtil.setGovernor(result, cluster)
                                status.cpuClusterStatuses[cluster].governor = result
                                setText(it as TextView?, result)
                            }
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
                DialogHelper.alert(this, "调度器参数", msg.toString())
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
        val cores = ArrayList<Boolean>()
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

    private fun getCoreList(coreState: BooleanArray): ArrayList<SelectItem> {
        val cores = ArrayList<SelectItem>()
        for (coreIndex in 0 until coreCount) {
            cores.add(SelectItem().apply {
                title = "Cpu$coreIndex"
                if (coreIndex < coreState.size) {
                    selected = coreState[coreIndex]
                }
            })
        }
        return cores
    }

    class OnSeekBarChangeListener(private var up: Boolean, private var cpuFrequencyUtils: CpuFrequencyUtils) : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            if (seekBar != null) {
                if (up)
                    cpuFrequencyUtils.exynosHmpUP = seekBar.progress
                else
                    cpuFrequencyUtils.exynosHmpDown = seekBar.progress
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

            if (qualcommThermalSupported) {
                status.coreControl = thermalControlUtils.getCoreControlState()
                status.vdd = thermalControlUtils.getVDDRestrictionState()
                status.msmThermal = thermalControlUtils.getTheramlState()
            }

            status.exynosHmpUP = CpuFrequencyUtil.exynosHmpUP
            status.exynosHmpDown = CpuFrequencyUtil.exynosHmpDown
            status.exynosHmpBooster = CpuFrequencyUtil.exynosBooster
            status.exynosHotplug = CpuFrequencyUtil.exynosHotplug

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
            status.cpusetRestricted = KernelProrp.getProp("/dev/cpuset/restricted/cpus")
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

    private fun parseFreqList(arr: Array<String>): ArrayList<SelectItem> {
        val arrMhz = ArrayList<SelectItem>()
        for (item in arr) {
            arrMhz.add(SelectItem().apply {
                title = subFreqStr(item)
                value = item
            })
        }
        return arrMhz
    }


    private fun parseGPUFreqList(arr: Array<String>): ArrayList<SelectItem> {
        val arrMhz = ArrayList<SelectItem>()
        for (item in arr) {
            arrMhz.add(
                    SelectItem().apply {
                        title = subGPUFreqStr(item)
                        value = item
                    }
            )
        }
        return arrMhz
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
                    val cluster_view = cpu_cluster_list.findViewWithTag<View>("cluster_" + cluster)
                    val cluster_min_freq = cluster_view.findViewById<TextView>(R.id.cluster_min_freq)
                    val cluster_max_freq = cluster_view.findViewById<TextView>(R.id.cluster_max_freq)
                    val cluster_governor = cluster_view.findViewById<TextView>(R.id.cluster_governor)
                    val status = status.cpuClusterStatuses[cluster]!!
                    setText(cluster_min_freq, subFreqStr(status.min_freq))
                    setText(cluster_max_freq, subFreqStr(status.max_freq))
                    setText(cluster_governor, status.governor)
                }
            }

            if (qualcommThermalSupported) {
                qualcomm_thermal.visibility = View.VISIBLE
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
            } else {
                qualcomm_thermal.visibility = View.GONE
            }

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

            for (i in 0 until coreCount) {
                cores[i].isChecked = status.coreOnline[i]
            }

            cpuset_bg.text = status.cpusetBackground
            cpuset_system_bg.text = status.cpusetSysBackground
            cpuset_foreground.text = status.cpusetForeground
            cpuset_top_app.text = status.cpusetTopApp
        } catch (ex: Exception) {
        }
    }

    private fun onViewCreated() {
        if (intent.hasExtra("cpuModeName")) {
            cpuModeName = intent.getStringExtra("cpuModeName")
        }

        Thread {
            initData()
        }.start()

        val globalSPF = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        val dynamic = AccessibleServiceHelper().serviceRunning(context) && globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)
        if (dynamic && (cpuModeName == null)) {
            DialogHelper.alert(this,
                    "请注意",
                    "检测到你已开启“动态响应”，你手动对CPU、GPU的修改随时可能被覆盖。\n\n同时，手动调整参数还可能对“动态响应”的工作造成不利影响！").setCancelable(false)
        }
    }

    private fun loadBootConfig() {
        val storage = CpuConfigStorage(context)
        statusOnBoot = storage.load(cpuModeName)
        cpu_apply_onboot.isChecked = statusOnBoot != null

        if (cpuModeName != null) {
            cpu_apply_boot.visibility = View.GONE

            ModeSwitcher().executePowercfgMode(cpuModeName!!, packageName)

            cpu_help_text.visibility = View.GONE
        }
    }

    private fun saveBootConfig() {
        if (cpuModeName != null) {
            if (!CpuConfigStorage(context).saveCpuConfig(status, cpuModeName)) {
                Toast.makeText(context, "保存配置文件失败！", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (!CpuConfigStorage(context).saveCpuConfig(if (cpu_apply_onboot.isChecked) status else null)) {
                Toast.makeText(context, "保存配置文件失败！", Toast.LENGTH_SHORT).show()
                cpu_apply_onboot.isChecked = false
            }
        }
    }

    private var timer: Timer? = null
    override fun onResume() {
        super.onResume()
        if (this.cpuModeName == null) {
            title = getString(R.string.menu_core_control)
        } else {
            title = "自定义[" + ModeSwitcher.getModName("" + cpuModeName) + "]"
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
        stopStatusUpdate()
    }

    override fun onDestroy() {
        stopStatusUpdate()
        super.onDestroy()
    }

    private fun stopStatusUpdate() {
        try {
            if (timer != null) {
                timer!!.cancel()
                timer = null
            }
        } catch (ex: Exception) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cpu_control)

        setBackArrow()
        this.onViewCreated()
    }
}