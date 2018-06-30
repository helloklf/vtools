package com.omarea.vboot

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
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import com.omarea.shell.cpucontrol.CpuFrequencyUtils
import com.omarea.shell.cpucontrol.ThermalControlUtils
import com.omarea.ui.StringAdapter
import kotlinx.android.synthetic.main.fragment_cpu_control.*
import java.util.*
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
                for (i in 0..cores.size - 1) {
                    if (i >= coreCount) {
                        cores[i].isEnabled = false
                    }
                }

                bindEvent()
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
                var approximation = if (arr.size > 0) arr[0] else ""
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
                var index = status.cluster_little_min_freq
                AlertDialog.Builder(context)
                        .setTitle("选择Cluster0最小频率")
                        .setSingleChoiceItems(parseFreqList(littleFreqs), status.cluster_little_min_freq, { dialog, which ->
                            index = which
                        })
                        .setPositiveButton(R.string.btn_confirm, { _, _ ->
                            if (index != status.cluster_little_min_freq) {
                                val g = littleFreqs[index]
                                if (CpuFrequencyUtils.getCurrentMinFrequency(0) == g) {
                                    return@setPositiveButton
                                }
                                CpuFrequencyUtils.setMinFrequency(g, 0, context)
                                status.cluster_little_min_freq = index
                                updateUI()
                            }
                        })
                        .create()
                        .show()
            }
            cluster_little_max_freq.setOnClickListener {
                var index = status.cluster_little_max_freq
                AlertDialog.Builder(context)
                        .setTitle("选择Cluster0最大频率")
                        .setSingleChoiceItems(parseFreqList(littleFreqs), status.cluster_little_max_freq, { dialog, which ->
                            index = which
                        })
                        .setPositiveButton(R.string.btn_confirm, { _, _ ->
                            if (index != status.cluster_little_max_freq) {
                                val g = littleFreqs[index]
                                if (CpuFrequencyUtils.getCurrentMinFrequency(0) == g) {
                                    return@setPositiveButton
                                }
                                CpuFrequencyUtils.setMaxFrequency(g, 0, context)
                                status.cluster_little_max_freq = index
                                updateUI()
                            }
                        })
                        .create()
                        .show()
            }
            // cluster_little_governor.onItemSelectedListener = ItemSelected(R.id.cluster_little_governor, next)
            cluster_little_governor.setOnClickListener {
                var governor = status.cluster_little_governor
                AlertDialog.Builder(context)
                        .setTitle("选择Cluster0调度模式")
                        .setSingleChoiceItems((littleGovernor), status.cluster_little_governor, { dialog, which ->
                            governor = which
                        })
                        .setPositiveButton(R.string.btn_confirm, { _, _ ->
                            if (governor != status.cluster_little_governor) {
                                val g = littleGovernor[governor]
                                if (CpuFrequencyUtils.getCurrentScalingGovernor(0) == g) {
                                    return@setPositiveButton
                                }
                                CpuFrequencyUtils.setGovernor(g, 0, context)
                                status.cluster_little_governor = governor
                                updateUI()
                            }
                        })
                        .create()
                        .show()
                return@setOnClickListener
            }

            if (hasBigCore) {
                cluster_big_min_freq.setOnClickListener {
                    var index = status.cluster_big_min_freq
                    AlertDialog.Builder(context)
                            .setTitle("选择Cluster1最小频率")
                            .setSingleChoiceItems(parseFreqList(bigFreqs), status.cluster_big_min_freq, { dialog, which ->
                                index = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (index != status.cluster_big_min_freq) {
                                    val g = bigFreqs[index]
                                    if (CpuFrequencyUtils.getCurrentMinFrequency(1) == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setMinFrequency(g, 1, context)
                                    status.cluster_big_min_freq = index
                                    updateUI()
                                }
                            })
                            .create()
                            .show()
                }
                cluster_big_max_freq.setOnClickListener {
                    var index = status.cluster_big_max_freq
                    AlertDialog.Builder(context)
                            .setTitle("选择Cluster1最大频率")
                            .setSingleChoiceItems(parseFreqList(bigFreqs), status.cluster_big_max_freq, { dialog, which ->
                                index = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (index != status.cluster_big_max_freq) {
                                    val g = bigFreqs[index]
                                    if (CpuFrequencyUtils.getCurrentMaxFrequency(1) == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setMaxFrequency(g, 1, context)
                                    status.cluster_big_max_freq = index
                                    updateUI()
                                }
                            })
                            .create()
                            .show()
                }
                cluster_big_governor.setOnClickListener {
                    var governor = status.cluster_big_governor
                    AlertDialog.Builder(context)
                            .setTitle("选择Cluster1调度模式")
                            .setSingleChoiceItems((bigGovernor), status.cluster_big_governor, { dialog, which ->
                                governor = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (governor != status.cluster_big_governor) {
                                    val g = bigGovernor[governor]
                                    if (CpuFrequencyUtils.getCurrentScalingGovernor(1) == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setGovernor(g, 1, context)
                                    status.cluster_big_governor = governor
                                    updateUI()
                                }
                            })
                            .create()
                            .show()
                }
            }

            if (adrenoGPU) {
                adreno_gpu_min_freq.setOnClickListener {
                    var index = status.adrenoMinFreq
                    AlertDialog.Builder(context)
                            .setTitle("选择Adreno GPU最小频率")
                            .setSingleChoiceItems(parseGPUFreqList(adrenoFreqs), status.adrenoMinFreq, { dialog, which ->
                                index = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (index != status.adrenoMinFreq) {
                                    val g = adrenoFreqs[index]
                                    if (CpuFrequencyUtils.getAdrenoGPUMinFreq() == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setAdrenoGPUMinFreq(g)
                                    status.adrenoMinFreq = index
                                    updateUI()
                                }
                            })
                            .create()
                            .show()
                }
                adreno_gpu_max_freq.setOnClickListener {
                    var index = status.adrenoMaxFreq
                    AlertDialog.Builder(context)
                            .setTitle("选择Adreno GPU最大频率")
                            .setSingleChoiceItems(parseGPUFreqList(adrenoFreqs), status.adrenoMaxFreq, { dialog, which ->
                                index = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (index != status.adrenoMaxFreq) {
                                    val g = adrenoFreqs[index]
                                    if (CpuFrequencyUtils.getAdrenoGPUMaxFreq() == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setAdrenoGPUMaxFreq(g)
                                    status.adrenoMaxFreq = index
                                    updateUI()
                                }
                            })
                            .create()
                            .show()
                }
                adreno_gpu_governor.setOnClickListener {
                    var governor = status.adrenoGovernor
                    AlertDialog.Builder(context)
                            .setTitle("选择Adreno GPU调度")
                            .setSingleChoiceItems((adrenoGovernors), status.adrenoGovernor, { dialog, which ->
                                governor = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (governor != status.adrenoGovernor) {
                                    val g = adrenoGovernors[governor]
                                    if (CpuFrequencyUtils.getAdrenoGPUGovernor() == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setAdrenoGPUGovernor(g)
                                    status.adrenoGovernor = governor
                                    updateUI()
                                }
                            })
                            .create()
                            .show()
                }
                adreno_gpu_min_pl.setOnClickListener {
                    var index = status.adrenoMinPL
                    AlertDialog.Builder(context)
                            .setTitle("选择GPU最小功耗级别")
                            .setSingleChoiceItems((adrenoPLevels), status.adrenoMinPL, { dialog, which ->
                                index = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (index != status.adrenoMinPL) {
                                    val g = adrenoPLevels[index]
                                    if (CpuFrequencyUtils.getAdrenoGPUMinPowerLevel() == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setAdrenoGPUMinPowerLevel(g)
                                    status.adrenoMinPL = index
                                    updateUI()
                                }
                            })
                            .create()
                            .show()
                }
                adreno_gpu_max_pl.setOnClickListener {
                    var index = status.adrenoMaxPL
                    AlertDialog.Builder(context)
                            .setTitle("选择GPU最大功耗级别")
                            .setSingleChoiceItems((adrenoPLevels), status.adrenoMaxPL, { dialog, which ->
                                index = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (index != status.adrenoMaxPL) {
                                    val g = adrenoPLevels[index]
                                    if (CpuFrequencyUtils.getAdrenoGPUMaxPowerLevel() == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setAdrenoGPUMaxPowerLevel(g)
                                    status.adrenoMaxPL = index
                                    updateUI()
                                }
                            })
                            .create()
                            .show()
                }
                adreno_gpu_default_pl.setOnClickListener {
                    var index = status.adrenoDefaultPL
                    AlertDialog.Builder(context)
                            .setTitle("选择GPU默认功耗级别")
                            .setSingleChoiceItems((adrenoPLevels), status.adrenoDefaultPL, { dialog, which ->
                                index = which
                            })
                            .setPositiveButton(R.string.btn_confirm, { _, _ ->
                                if (index != status.adrenoDefaultPL) {
                                    val g = adrenoPLevels[index]
                                    if (CpuFrequencyUtils.getAdrenoGPUDefaultPowerLevel() == g) {
                                        return@setPositiveButton
                                    }
                                    CpuFrequencyUtils.setAdrenoGPUDefaultPowerLevel(g)
                                    status.adrenoDefaultPL = index
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
                        if (value.startsWith(cpu + ":")) {
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
                            if (value.startsWith(cpu + ":")) {
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
            for (i in 0..cores.size - 1) {
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
        } catch (ex: Exception) {
            Log.e("bindEvent", ex.message)
        }
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

    class Status {
        var cluster_little_min_freq = -1;
        var cluster_little_max_freq = -1;
        var cluster_little_governor = -1;

        var cluster_big_min_freq = -1;
        var cluster_big_max_freq = -1;
        var cluster_big_governor = -1;

        var coreControl = ""
        var vdd = ""
        var msmThermal = ""
        var boost = ""
        var boostFreq = ""
        var boostTime = ""
        var coreOnline = arrayListOf<Boolean>()

        var exynosHmpUP = 0;
        var exynosHmpDown = 0;
        var exynosHmpBooster = false;
        var exynosHotplug = false;

        var adrenoMinFreq = -1
        var adrenoMaxFreq = -1
        var adrenoMinPL = -1
        var adrenoMaxPL = -1
        var adrenoDefaultPL = -1
        var adrenoGovernor = -1
    }

    private var status = Status()

    private fun updateState() {
        Thread(Runnable {
            try {
                status.cluster_little_min_freq = littleFreqs.indexOf(getApproximation(littleFreqs, CpuFrequencyUtils.getCurrentMinFrequency(0)))
                status.cluster_little_max_freq = littleFreqs.indexOf(getApproximation(littleFreqs, CpuFrequencyUtils.getCurrentMaxFrequency(0)))
                status.cluster_little_governor = littleGovernor.indexOf(CpuFrequencyUtils.getCurrentScalingGovernor(0))
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
                    status.adrenoDefaultPL = adrenoPLevels.indexOf(CpuFrequencyUtils.getAdrenoGPUDefaultPowerLevel())
                    status.adrenoMinPL = adrenoPLevels.indexOf(CpuFrequencyUtils.getAdrenoGPUMinPowerLevel())
                    status.adrenoMaxPL = adrenoPLevels.indexOf(CpuFrequencyUtils.getAdrenoGPUMaxPowerLevel())
                    status.adrenoMinFreq = adrenoFreqs.indexOf(getApproximation(adrenoFreqs, CpuFrequencyUtils.getAdrenoGPUMinFreq()))
                    status.adrenoMaxFreq = adrenoFreqs.indexOf(getApproximation(adrenoFreqs, CpuFrequencyUtils.getAdrenoGPUMaxFreq()))
                    status.adrenoGovernor = adrenoGovernors.indexOf(CpuFrequencyUtils.getAdrenoGPUGovernor())
                }

                if (hasBigCore) {
                    status.cluster_big_min_freq = bigFreqs.indexOf(getApproximation(bigFreqs, CpuFrequencyUtils.getCurrentMinFrequency(1)))
                    status.cluster_big_max_freq = bigFreqs.indexOf(getApproximation(bigFreqs, CpuFrequencyUtils.getCurrentMaxFrequency(1)))
                    status.cluster_big_governor = bigGovernor.indexOf(CpuFrequencyUtils.getCurrentScalingGovernor(1))
                }

                status.coreOnline = arrayListOf()
                for (i in 0..coreCount - 1) {
                    status.coreOnline.add(CpuFrequencyUtils.getCoreOnlineState(i))
                }

                handler.post {
                    updateUI()
                }
            } catch (ex: Exception) {
            }
        }).start()
    }

    private fun subFreqStr(freq: String): String {
        if (freq.length > 3) {
            return freq.substring(0, freq.length - 3) + " Mhz"
        } else {
            return freq
        }
    }

    private fun subGPUFreqStr(freq: String): String {
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

    private fun setText(view: TextView, text: String) {
        if (view.text != text) {
            view.setText(text)
        }
    }

    private fun updateUI() {
        try {
            setText(cluster_little_min_freq, subFreqStr(littleFreqs[status.cluster_little_min_freq]))
            setText(cluster_little_max_freq, subFreqStr(littleFreqs[status.cluster_little_max_freq]))
            setText(cluster_little_governor, littleGovernor[status.cluster_little_governor])

            if (hasBigCore) {
                cluster_big_min_freq.setText(subFreqStr(bigFreqs[status.cluster_big_min_freq]))
                cluster_big_max_freq.setText(subFreqStr(bigFreqs[status.cluster_big_max_freq]))
                cluster_big_governor.setText(bigGovernor[status.cluster_big_governor])
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
                if (status.adrenoDefaultPL != -1) {
                    adreno_gpu_default_pl.setText(adrenoPLevels[status.adrenoDefaultPL])
                }
                if (status.adrenoMinPL != -1) {
                    adreno_gpu_min_pl.setText(adrenoPLevels[status.adrenoMinPL])
                }
                if (status.adrenoMaxPL != -1) {
                    adreno_gpu_max_pl.setText(adrenoPLevels[status.adrenoMaxPL])
                }
                if (status.adrenoMinFreq != -1) {
                    adreno_gpu_min_freq.setText(subGPUFreqStr(adrenoFreqs[status.adrenoMinFreq]))
                }
                if (status.adrenoMaxFreq != -1) {
                    adreno_gpu_max_freq.setText(subGPUFreqStr(adrenoFreqs[status.adrenoMaxFreq]))
                }
                if (status.adrenoGovernor != -1) {
                    adreno_gpu_governor.setText(adrenoGovernors[status.adrenoGovernor])
                }
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

            for (i in 0..coreCount - 1) {
                cores[i].isChecked = status.coreOnline.get(i)
            }
        } catch (ex: Exception) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Thread(Runnable {
            initData()
        }).start()
    }

    private var timer: Timer? = null
    override fun onResume() {
        super.onResume()
        if (timer == null) {
            timer = Timer()
            timer!!.schedule(object : TimerTask() {
                override fun run() {
                    handler.post({
                        updateState()
                    })
                }
            }, 3000, 3000)
        }
    }

    override fun onPause() {
        super.onPause()
        cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
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