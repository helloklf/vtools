package com.omarea.vtools.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.omarea.store.BatteryHistoryStore
import com.omarea.ui.AdapterBatteryStats
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.fragment_battery_stats.*


class FragmentBatteryStats : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_battery_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        battery_stats_delete.setOnClickListener {
            BatteryHistoryStore(context!!).clearData()
            Toast.makeText(context!!, "统计记录已清理", Toast.LENGTH_SHORT).show()
            loadData()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isDetached) {
            return
        }
        loadData()
    }

    private fun loadData() {
        val data = BatteryHistoryStore(context!!).getAvgData(System.currentTimeMillis())
        data.sortBy {
            -(it.io * it.count)
        }
        battery_stats.adapter = AdapterBatteryStats(context!!, data)
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentBatteryStats()
            return fragment
        }
    }
}
