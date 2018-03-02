package com.omarea.vboot

import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.AdapterView
import com.omarea.cpucontrol.CpuFrequencyUtils
import com.omarea.cpucontrol.ThermalControlUtils
import com.omarea.ui.StringAdapter
import kotlinx.android.synthetic.main.activity_core_contrl.*

class ActivityCoreContrl : AppCompatActivity() {
    private val hasBigCore = CpuFrequencyUtils.getClusterInfo().size > 1
    private val littleFreqs = CpuFrequencyUtils.getAvailableFrequencies(0)
    private val littleGovernor = CpuFrequencyUtils.getAvailableGovernors(0)
    private val bigFreqs = CpuFrequencyUtils.getAvailableFrequencies(1)
    private val bigGovernor = CpuFrequencyUtils.getAvailableGovernors(1)
    private var handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_core_contrl)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        if (!hasBigCore) {
            big_core_configs.visibility = View.GONE
        }

        initData()
        updateState()
        handler.postDelayed({
            bindEvent()
        }, 1000)
    }


    override fun onResume() {
        super.onResume()

        updateState()
    }

    private fun initData() {
        cluster_little_min_freq.adapter = StringAdapter(this ,littleFreqs)
        cluster_little_max_freq.adapter = StringAdapter(this ,littleFreqs)
        cluster_little_governor.adapter = StringAdapter(this, littleGovernor)

        if (hasBigCore) {
            cluster_big_min_freq.adapter = StringAdapter(this ,bigFreqs)
            cluster_big_max_freq.adapter = StringAdapter(this ,bigFreqs)
            cluster_big_governor.adapter = StringAdapter(this, bigGovernor)
        }
    }

    private fun bindEvent() {
        thermal_core_control.setOnCheckedChangeListener({
            buttonView, isChecked ->
            ThermalControlUtils.setCoreControlState(isChecked, this)
            updateState()
        })
        thermal_vdd.setOnCheckedChangeListener({
            buttonView, isChecked ->
            ThermalControlUtils.setVDDRestrictionState(isChecked, this)
            updateState()
        })
        thermal_paramters.setOnCheckedChangeListener({
            buttonView, isChecked ->
            ThermalControlUtils.setTheramlState(isChecked, this)
            updateState()
        })

        cpu_sched_boost.setOnCheckedChangeListener({
            buttonView, isChecked ->
            CpuFrequencyUtils.setSechedBoostState(isChecked, this)
            updateState()
        })

        val next = Runnable {
            updateState()
        }
        cluster_little_min_freq.onItemSelectedListener = ItemSelected(R.id.cluster_little_min_freq, next)
        cluster_little_max_freq.onItemSelectedListener = ItemSelected(R.id.cluster_little_max_freq, next)
        cluster_little_governor.onItemSelectedListener = ItemSelected(R.id.cluster_little_governor, next)

        if (hasBigCore) {
            cluster_big_min_freq.onItemSelectedListener = ItemSelected(R.id.cluster_big_min_freq, next)
            cluster_big_max_freq.onItemSelectedListener = ItemSelected(R.id.cluster_big_max_freq, next)
            cluster_big_governor.onItemSelectedListener = ItemSelected(R.id.cluster_big_governor, next)
        }
    }

    class ItemSelected(private val spinner: Int, next: Runnable): AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (parent == null)
                return

            val freq = parent.selectedItem.toString()
            when (spinner) {
                R.id.cluster_little_min_freq -> {
                    CpuFrequencyUtils.setMinFrequency(freq, 0, parent.context)
                }
                R.id.cluster_little_max_freq -> {
                    CpuFrequencyUtils.setMaxFrequency(freq, 0, parent.context)
                }
                R.id.cluster_little_governor -> {
                    CpuFrequencyUtils.setGovernor(freq, 0, parent.context)
                }
                R.id.cluster_big_min_freq -> {
                    CpuFrequencyUtils.setMinFrequency(freq, 1, parent.context)
                }
                R.id.cluster_big_max_freq -> {
                    CpuFrequencyUtils.setMaxFrequency(freq, 1, parent.context)
                }
                R.id.cluster_big_governor -> {
                    CpuFrequencyUtils.setGovernor(freq, 1, parent.context)
                }
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    private fun updateState() {
        cluster_little_min_freq.setSelection(littleFreqs.indexOf(CpuFrequencyUtils.getCurrentMinFrequency(0)), true)
        cluster_little_max_freq.setSelection(littleFreqs.indexOf(CpuFrequencyUtils.getCurrentMaxFrequency(0)), true)
        cluster_little_governor.setSelection(littleGovernor.indexOf(CpuFrequencyUtils.getCurrentScalingGovernor(0)))
        if (hasBigCore) {
            cluster_big_min_freq.setSelection(bigFreqs.indexOf(CpuFrequencyUtils.getCurrentMinFrequency(1)), true)
            cluster_big_max_freq.setSelection(bigFreqs.indexOf(CpuFrequencyUtils.getCurrentMaxFrequency(1)), true)
            cluster_big_governor.setSelection(bigGovernor.indexOf(CpuFrequencyUtils.getCurrentScalingGovernor(1)))
        }

        val coreControl = ThermalControlUtils.getCoreControlState()
        if (coreControl == null || coreControl.isEmpty()) {
            thermal_core_control.isEnabled = false
        }
        thermal_core_control.isChecked = coreControl == "1"

        val vdd = ThermalControlUtils.getVDDRestrictionState()
        if (vdd == null || vdd.isEmpty()) {
            thermal_vdd.isEnabled = false
        }
        thermal_vdd.isChecked = vdd == "1"


        val msmThermal = ThermalControlUtils.getTheramlState()
        if (msmThermal == null || msmThermal.isEmpty()) {
            thermal_paramters.isEnabled = false
        }
        thermal_paramters.isChecked = msmThermal == "Y"

        val boost = CpuFrequencyUtils.getSechedBoostState()
        if (boost == null || boost.isEmpty()) {
            cpu_sched_boost.isEnabled  = false
        }
        cpu_sched_boost.isChecked = CpuFrequencyUtils.getSechedBoostState() == "1"
    }
}
