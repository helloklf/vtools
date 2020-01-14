package com.omarea.vtools.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.omarea.permissions.CheckRootStatus
import com.omarea.scene_mode.CpuConfigInstaller
import com.omarea.scene_mode.ModeSwitcher
import com.omarea.store.SpfConfig
import com.omarea.ui.NavItem
import com.omarea.vtools.R
import kotlinx.android.synthetic.main.fragment_cpu_modes.*
import kotlinx.android.synthetic.main.fragment_not_root.*


class FragmentCpuModes : Fragment() {
    private lateinit var author: String
    private var configInstalled: Boolean = false
    private lateinit var modeSwitcher: ModeSwitcher

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_cpu_modes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        configInstalled = CpuConfigInstaller().configInstalled()
        modeSwitcher = ModeSwitcher()

        if (configInstalled) {
            val globalSpf = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
            author = globalSpf.getString(SpfConfig.GLOBAL_SPF_CPU_CONFIG_AUTHOR, "unknown")!!
        } else {
            author = "none"
        }

        cpu_config_p4.setOnClickListener {
            Toast.makeText(context!!, "该模式暂未开放使用", Toast.LENGTH_SHORT).show()
        }
        bindMode(cpu_config_p0, ModeSwitcher.POWERSAVE)
        bindMode(cpu_config_p1, ModeSwitcher.BALANCE)
        bindMode(cpu_config_p2, ModeSwitcher.PERFORMANCE)
        bindMode(cpu_config_p3, ModeSwitcher.FAST)
    }

    private fun bindMode(button: View, mode: String) {
        button.setOnClickListener {
            modifyCpuConfig(mode)
        }
    }

    private fun updateState(button: View, mode: String) {
        val authorView = button.findViewWithTag<TextView>("author")
        val replaced = modeSwitcher.modeReplaced(context!!, mode) != null
        authorView.setText("Author : " + (if (replaced) "custom" else author))
        button.alpha = if (configInstalled || replaced) 1f else 0.5f
    }

    override fun onResume() {
        super.onResume()
        activity!!.title = getString(R.string.menu_cpu_modes)

        updateState(cpu_config_p0, ModeSwitcher.POWERSAVE)
        updateState(cpu_config_p1, ModeSwitcher.BALANCE)
        updateState(cpu_config_p2, ModeSwitcher.PERFORMANCE)
        updateState(cpu_config_p3, ModeSwitcher.FAST)
    }

    private fun modifyCpuConfig(mode: String) {
        val transaction = activity!!.supportFragmentManager.beginTransaction()
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        // transaction.setCustomAnimations(R.animator.fragment_enter, R.animator.fragment_exit)
        var fragment = FragmentCpuControl.newInstance(mode)

        val pageTitle = ModeSwitcher.getModName(mode)
        // transaction.disallowAddToBackStack()
        transaction.replace(R.id.app_more, fragment)
        transaction.addToBackStack(pageTitle);
        transaction.commitAllowingStateLoss()
        activity!!.title = pageTitle
    }

    companion object {
        fun createPage(): Fragment {
            val fragment = FragmentCpuModes()
            return fragment
        }
    }
}
