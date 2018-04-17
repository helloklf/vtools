package com.omarea.vboot

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.omarea.shared.FileWrite
import com.omarea.shared.Consts
import com.omarea.shared.SpfConfig
import com.omarea.shell.*
import kotlinx.android.synthetic.main.layout_home.*
import java.io.File


class FragmentHome : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.layout_home, container, false)
    }

    private lateinit var globalSPF: SharedPreferences

    private fun showMsg(msg:String) {
        this.view?.let { Snackbar.make(it, msg, Snackbar.LENGTH_LONG).show() }
    }

    @SuppressLint("ApplySharedPref")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        globalSPF = context!!.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        if (Platform().dynamicSupport(context!!)) {
            powermode_toggles.visibility = View.VISIBLE
        }

        btn_powersave.setOnClickListener {
            installConfig(Consts.TogglePowersaveMode)
            showMsg(getString(R.string.power_change_powersave))
        }
        btn_defaultmode.setOnClickListener {
            installConfig(Consts.ToggleDefaultMode)
            showMsg(getString(R.string.power_change_default))
        }
        btn_gamemode.setOnClickListener {
            installConfig(Consts.ToggleGameMode)
            showMsg(getString(R.string.power_change_game))
        }
        btn_fastmode.setOnClickListener {
            installConfig(Consts.ToggleFastMode)
            showMsg(getString(R.string.power_chagne_fast))
        }
        home_hide_in_recents.setOnCheckedChangeListener({
            _,checked ->
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT, checked).commit()
        })
        home_app_nightmode.setOnCheckedChangeListener({
            _,checked ->
            if (globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false) == checked) {
                return@setOnCheckedChangeListener
            }
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, checked).commit()
            Snackbar.make(view!!, getString(R.string.change_need_reboot), Snackbar.LENGTH_SHORT).show()
        })
    }

    override fun onResume() {
        super.onResume()

        home_app_nightmode.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_NIGHT_MODE, false)
        home_hide_in_recents.isChecked = globalSPF.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT, false)
        setModeState()
        sdfree.text = "共享存储：" + Files.GetDirFreeSizeMB(Environment.getExternalStorageDirectory().absolutePath) + " MB"
        datafree.text = "应用存储：" + Files.GetDirFreeSizeMB(Environment.getDataDirectory().absolutePath) + " MB"
    }

    private fun setModeState() {
        btn_powersave.text = "省电"
        btn_defaultmode.text = "均衡"
        btn_gamemode.text = "游戏"
        btn_fastmode.text = "极速"
        val cfg = Props.getProp("vtools.powercfg")
        when (cfg) {
            "default" -> btn_defaultmode.text = "均衡 √"
            "game" -> btn_gamemode.text = "游戏 √"
            "powersave" -> btn_powersave!!.text = "省电 √"
            "fast" -> btn_fastmode!!.text = "极速 √"
        }
    }

    private fun installConfig(after: String) {
        if (File(Consts.POWER_CFG_PATH).exists()) {
            SuDo(context).execCmdSync(after)
            //SuDo(context).execCmdSync(Consts.ExecuteConfig + "\n" + after)
        } else {
            //TODO：选取配置
            FileWrite.WritePrivateFile(context!!.assets, Platform().GetCPUName() + "/powercfg-default.sh", "powercfg.sh", context!!)
            FileWrite.WritePrivateFile(context!!.assets, Platform().GetCPUName() + "/init.qcom.post_boot-default", "init.qcom.post_boot.sh", context!!)
            val cmd = StringBuilder()
                    .append("cp ${FileWrite.getPrivateFilePath(context!!, "powercfg.sh")} ${Consts.POWER_CFG_PATH};")
                    .append("chmod 0777 ${Consts.POWER_CFG_PATH};")
                    .append("chmod 0777 ${Consts.POWER_CFG_BASE};")
                    .append(after)
            //SuDo(context).execCmdSync(Consts.InstallPowerToggleConfigToCache + "\n\n" + Consts.ExecuteConfig + "\n" + after)
            SuDo(context).execCmdSync(cmd.toString())
        }
        setModeState();
    }
}
