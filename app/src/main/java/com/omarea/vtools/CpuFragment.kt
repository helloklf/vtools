package com.omarea.vtools

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.omarea.shared.model.CpuCoreInfo
import com.omarea.shell.cpucontrol.CpuFrequencyUtils
import com.omarea.ui.AdapterCpuCores
import kotlinx.android.synthetic.main.fragment_cpu.*
import java.util.*

class CpuFragment : Fragment() {
    private var myHandler = Handler()
    private var currentContext: Context? = null
    private var timer:Timer? = null

    // Runtime.getRuntime().availableProcessors()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_cpu, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.currentContext = context
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //updateInfo()
    }

    override fun onResume() {
        super.onResume()

        stopTimer()
        timer = Timer()
        timer!!.schedule(object: TimerTask() {
            override fun run() {
                updateInfo()
            }
        },0, 1000)
    }

    /*
    # cat /proc/stat | grep '^cpu'
    cpu (user   nice    system  idle    iowait  irq   softirq stealstolen guest)
    buf=cpu  15824 100 13772 879622 11014 40 720 0 0
    buf=cpu  15837 100 13790 879731 11014 40 720 0 0
    all=140
    ilde=31
    cpu use = 22.14%
    =======================
    buf=cpu  15837 100 13790 879731 11014 40 720 0 0
    buf=cpu  15857 100 13824 879786 11014 40 721 0 0
    all=110
    ilde=55
    cpu use = 50.00%


    cpu  187286 29798 90650 3195682 1326 5 12194 0 0 0
    cpu0 75926 14050 35582 363729 284 3 1973 0 0 0
    cpu1 27901 9214 17671 439477 283 0 5871 0 0 0
    cpu2 20170 1714 14784 457689 123 0 1931 0 0 0
    cpu3 15847 899 10630 467426 134 1 1940 0 0 0
    cpu4 17443 1326 5323 475286 177 0 280 0 0 0
    cpu5 13349 952 2672 366863 231 0 81 0 0 0
    cpu6 8869 760 1856 312524 34 0 66 0 0 0
    cpu7 7780 880 2128 312684 58 0 49 0 0 0
    */
    private var coreCount = -1;
    private fun updateInfo() {
        if (coreCount < 1) {
            coreCount = CpuFrequencyUtils.getCoreCount()
        }
        val cores = ArrayList<CpuCoreInfo>()
        val loads = CpuFrequencyUtils.getCpuLoad()
        for (coreIndex in 0..coreCount - 1) {
            val core = CpuCoreInfo()
            core.maxFreq = CpuFrequencyUtils.getCurrentMaxFrequency("cpu" + coreIndex)
            core.minFreq = CpuFrequencyUtils.getCurrentMinFrequency("cpu" + coreIndex)
            core.currentFreq = CpuFrequencyUtils.getCurrentFrequency("cpu" + coreIndex)
            core.cpuGovernor = CpuFrequencyUtils.getCurrentScalingGovernor("cpu" + coreIndex)
            if (loads.containsKey(coreIndex)) {
                core.loadRatio = loads.get(coreIndex)!!
            }
            cores.add(core)
        }
        myHandler.post {
            try {
                cpu_core_count.text = "核心数：" + coreCount
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
    }

    private fun stopTimer() {
        if (this.timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun onPause() {
        this.stopTimer()
        super.onPause()
    }

    override fun onDetach() {
        this.stopTimer()
        super.onDetach()
    }
}
