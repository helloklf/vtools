package com.omarea.vtools

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentStatePagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleAdapter
import android.widget.Toast
import com.omarea.shared.ConfigInstaller
import com.omarea.shared.Consts
import com.omarea.shared.ModeList
import com.omarea.shared.SpfConfig
import com.omarea.shell.Files
import com.omarea.shell.KeepShellSync
import com.omarea.shell.Platform
import com.omarea.shell.Props
import kotlinx.android.synthetic.main.layout_home.*
import java.io.File


class FragmentHome : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_home, container, false)
    }

    private lateinit var globalSPF: SharedPreferences
    private fun showMsg(msg: String) {
        this.view?.let { Snackbar.make(it, msg, Snackbar.LENGTH_LONG).show() }
    }

    private val fragmentList = ArrayList<Fragment>()
    private lateinit var spf: SharedPreferences
    private var modeList = ModeList()
    @SuppressLint("ApplySharedPref")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        if (Platform().dynamicSupport(context!!) || File(Consts.POWER_CFG_PATH).exists()) {
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

        fragmentList.clear()
        fragmentList.add(RamFragment())
        fragmentList.add(CpuFragment())
        home_viewpager.adapter = object : FragmentStatePagerAdapter(fragmentManager) {
            override fun getCount(): Int {
                return fragmentList.size
            }

            override fun getItem(position: Int): Fragment {
                return fragmentList.get(position)
            }
        }
        home_viewpager.adapter!!.notifyDataSetChanged()
        home_viewpager.setCurrentItem(0, true)
    }

    override fun onResume() {
        super.onResume()
        setModeState()
        updateInfo()
        // AppConfigLoader().getAppConfig("de.robv.android.xposed.installer", context!!.contentResolver)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun updateInfo() {
        sdfree.text = "SDCard：" + Files.GetDirFreeSizeMB(Environment.getExternalStorageDirectory().absolutePath) + " MB"
        datafree.text = "Data：" + Files.GetDirFreeSizeMB(Environment.getDataDirectory().absolutePath) + " MB"
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

    private fun installConfig(action: String, message: String) {
        if (File(Consts.POWER_CFG_PATH).exists()) {
            modeList.executePowercfgModeOnce(action, context!!.packageName)
        } else {
            val stringBuilder = StringBuilder()
            stringBuilder.append(String.format(Consts.ToggleMode, action))
            ConfigInstaller().installPowerConfig(context!!, stringBuilder.toString());
        }
        setModeState()
        showMsg(message)
    }
}
