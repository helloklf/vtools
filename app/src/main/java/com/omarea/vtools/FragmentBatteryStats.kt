package com.omarea.vtools

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.SimpleAdapter
import android.widget.Toast
import com.omarea.shared.BatteryHistoryStore
import com.omarea.shared.CommonCmds
import com.omarea.shell.units.BackupRestoreUnit
import com.omarea.ui.AdapterBatteryStats
import kotlinx.android.synthetic.main.layout_battery_stats.*
import kotlinx.android.synthetic.main.layout_img.*
import java.io.File
import java.util.*


class FragmentBatteryStats : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_battery_stats, container, false)

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
