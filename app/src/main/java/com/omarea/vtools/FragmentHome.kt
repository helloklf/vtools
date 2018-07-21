package com.omarea.vtools

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.github.mikephil.charting.data.PieEntry
import com.omarea.shared.*
import com.omarea.shell.Files
import com.omarea.shell.KeepShellSync
import com.omarea.shell.Platform
import com.omarea.shell.Props
import kotlinx.android.synthetic.main.layout_home.*
import java.io.File
import java.util.*


class FragmentHome : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_home, container, false)
    }

    private var myHandler = Handler()
    private lateinit var globalSPF: SharedPreferences
    private fun showMsg(msg: String) {
        this.view?.let { Snackbar.make(it, msg, Snackbar.LENGTH_LONG).show() }
    }

    private lateinit var spf: SharedPreferences
    private var modeList = ModeList()
    @SuppressLint("ApplySharedPref")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        if (Platform().dynamicSupport(context!!) || File(Consts.POWER_CFG_PATH).exists()) {
            powermode_toggles.visibility = View.VISIBLE
        } else {
            powermode_toggles.visibility = View.GONE
        }

        btn_powersave.setOnClickListener {
            installConfig(modeList.POWERSAVE, getString(R.string.power_change_powersave))
        }
        btn_defaultmode.setOnClickListener {
            installConfig(modeList.DEFAULT, getString(R.string.power_change_default))
        }
        btn_gamemode.setOnClickListener {
            installConfig(modeList.PERFORMANCE, getString(R.string.power_change_game))
        }
        btn_fastmode.setOnClickListener {
            installConfig(modeList.FAST, getString(R.string.power_chagne_fast))
        }

        spf = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        home_clear_ram.setOnClickListener {
            home_raminfo_text.text = "稍等一下"
            Thread(Runnable {
                KeepShellSync.doCmdSync("echo 3 > /proc/sys/vm/drop_caches")
                myHandler.postDelayed({
                    val activityManager = context!!.getSystemService(ACTIVITY_SERVICE) as ActivityManager
                    val info = ActivityManager.MemoryInfo()
                    activityManager.getMemoryInfo(info)
                    val totalMem = (info.totalMem / 1024 / 1024f).toInt()
                    home_raminfo.setData(totalMem.toFloat(), 0f)
                    val availMem = (info.availMem / 1024 / 1024f).toInt()
                    Toast.makeText(context, "缓存已清理...", Toast.LENGTH_SHORT).show()
                    home_raminfo_text.text = "${availMem} / ${totalMem}MB"
                    home_raminfo.setData(totalMem.toFloat(), availMem.toFloat())

                }, 1000)
            }).start()
        }
    }

    override fun onResume() {
        super.onResume()
        setModeState()
        updateInfo()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun updateInfo() {
        sdfree.text = "SDCard：" + Files.GetDirFreeSizeMB(Environment.getExternalStorageDirectory().absolutePath) + " MB"
        datafree.text = "Data：" + Files.GetDirFreeSizeMB(Environment.getDataDirectory().absolutePath) + " MB"
        val activityManager = context!!.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)

        val totalMem = (info.totalMem / 1024 / 1024f).toInt()
        val availMem = (info.availMem / 1024 / 1024f).toInt()

        home_raminfo_text.text = "${availMem} / ${totalMem}MB"
        home_raminfo.setData(totalMem.toFloat(), availMem.toFloat())
    }

    private fun setModeState() {
        btn_powersave.text = "省电"
        btn_defaultmode.text = "均衡"
        btn_gamemode.text = "性能"
        btn_fastmode.text = "极速"
        val cfg = Props.getProp("vtools.powercfg")
        when (cfg) {
            modeList.BALANCE -> btn_defaultmode.text = "均衡 √"
            modeList.PERFORMANCE -> btn_gamemode.text = "性能 √"
            modeList.POWERSAVE -> btn_powersave!!.text = "省电 √"
            modeList.FAST -> btn_fastmode!!.text = "极速 √"
        }
    }

    private fun installConfig(action: String, message: String) {
        if (spf.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, false) && AccessibleServiceHelper().serviceIsRunning(this.context!!)) {
            AlertDialog.Builder(context)
                    .setTitle("")
                    .setMessage("检测到你已经开启“动态响应”，微工具箱将根据你的前台应用，自动调整CPU、GPU性能。\n如果你要更改全局性能，请先关闭“动态响应”！")
                    .setPositiveButton(R.string.btn_confirm, DialogInterface.OnClickListener { dialog, which ->
                    })
                    .show()
                    .create()
        }
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
