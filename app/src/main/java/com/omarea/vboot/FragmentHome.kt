package com.omarea.vboot

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.omarea.shared.AppShared
import com.omarea.shared.BootService
import com.omarea.shared.Consts
import com.omarea.shared.SpfConfig
import com.omarea.shell.*
import kotlinx.android.synthetic.main.layout_home.*
import java.io.File


class FragmentHome : Fragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
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

        globalSPF = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        if (DynamicConfig().DynamicSupport(context)) {
            powermode_toggles.visibility = View.VISIBLE
        }

        btn_powersave.setOnClickListener {
            installConfig(Consts.TogglePowersaveMode)
            showMsg("已切换为省电模式，适合长时间媒体播放或阅读，综合使用时并不效率也不会省电太多！")
        }
        btn_defaultmode.setOnClickListener {
            installConfig(Consts.ToggleDefaultMode)
            showMsg("已切换为均衡模式，适合日常使用，速度与耗电平衡！")
        }
        btn_gamemode.setOnClickListener {
            installConfig(Consts.ToggleGameMode)
            showMsg("已切换为游戏（性能）模式，但受温度影响并不一定会更快，你可以考虑删除温控！")
        }
        btn_fastmode.setOnClickListener {
            installConfig(Consts.ToggleFastMode)
            showMsg("已切换为极速模式，这会大幅增加发热，如果不删除温控性能并不稳定！")
        }
        home_hide_in_recents.setOnCheckedChangeListener({
            _,checked ->
            globalSPF.edit().putBoolean(SpfConfig.GLOBAL_SPF_AUTO_REMOVE_RECENT, checked).commit()
        })
    }

    override fun onResume() {
        super.onResume()

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
        if (File("${Consts.POWER_CFG_PATH}").exists()) {
            SuDo(context).execCmdSync(after)
            //SuDo(context).execCmdSync(Consts.ExecuteConfig + "\n" + after)
        } else {
            //TODO：选取配置
            AppShared.WriteFile(context.assets, Platform().GetCPUName() + "/powercfg-default.sh", "powercfg.sh")
            //SuDo(context).execCmdSync(Consts.InstallPowerToggleConfigToCache + "\n\n" + Consts.ExecuteConfig + "\n" + after)
            SuDo(context).execCmdSync(Consts.InstallPowerToggleConfigToCache + "\n\n" + "\n" + after)
        }
        setModeState();
    }
}
