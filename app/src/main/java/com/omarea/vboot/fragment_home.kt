package com.omarea.vboot

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

import com.omarea.shared.AppShared
import com.omarea.shared.ConfigInfo
import com.omarea.shared.Consts
import com.omarea.shared.EventBus
import com.omarea.shared.Events
import com.omarea.shared.IEventSubscribe
import com.omarea.shared.cmd_shellTools
import com.omarea.shell.DynamicConfig
import com.omarea.shell.Files
import com.omarea.shell.KernelProrp
import com.omarea.shell.Props
import kotlinx.android.synthetic.main.layout_home.*


class fragment_home : Fragment() {

    /*
    void toggleIcon() {
        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(this, "com.smartmadsoft.xposed.aio.Launch");
        int newComponentState = 0x2;
        if(packageManager.getComponentEnabledSetting(componentName) == newComponentState) {
            newComponentState = 0x1;
            String text = "Launcher icon has been restored";
        }
        packageManager.setComponentEnabledSetting(componentName, newComponentState, 0x1);
        Toast.makeText(this, text, 0x0).show();
    }
    */

    internal lateinit var view: View

    internal var eventSubscribe: IEventSubscribe = object : IEventSubscribe {
        override fun messageRecived(message: Any?) {
            setModeState()
        }
    }

    internal lateinit var cmdshellTools: cmd_shellTools

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        view = inflater!!.inflate(R.layout.layout_home, container, false)

        cmdshellTools = cmd_shellTools(null, null)
        if (DynamicConfig().DynamicSupport(context)) {
            view.findViewById(R.id.powermode_toggles).visibility = View.VISIBLE
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        btn_powersave.setOnClickListener{
            installConfig(Consts.TogglePowersaveMode)
            Snackbar.make(this.view, "已切换为省电模式，适合长时间媒体播放或阅读，综合使用时并不效率也不会省电太多！", Snackbar.LENGTH_LONG).show()
        }
        btn_defaultmode.setOnClickListener{
            installConfig(Consts.ToggleDefaultMode)
            Snackbar.make(this.view, "已切换为均衡模式，适合日常使用，速度与耗电平衡！", Snackbar.LENGTH_LONG).show()
        }
        btn_gamemode.setOnClickListener{
            installConfig(Consts.ToggleGameMode)
            Snackbar.make(this.view, "已切换为游戏（性能）模式，但受温度影响并不一定会更快，你可以考虑删除温控！", Snackbar.LENGTH_LONG).show()
        }
        btn_fastmode.setOnClickListener{
            installConfig(Consts.ToggleFastMode)
            Snackbar.make(this.view, "已切换为极速模式，这会大幅增加发热，如果不删除温控性能并不稳定！", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        EventBus.subscribe(Events.ModeToggle, eventSubscribe)
        setModeState()
        val sdfree = view.findViewById(R.id.sdfree) as TextView
        val datafree = view.findViewById(R.id.datafree) as TextView
        sdfree.text = "共享存储：" + Files.GetDirFreeSizeMB("/sdcard") + " MB"
        datafree.text = "应用存储：" + Files.GetDirFreeSizeMB("/data") + " MB"
        super.onResume()
    }

    private fun setModeState() {
        btn_powersave!!.text = "省电"
        btn_defaultmode!!.text = "均衡"
        btn_gamemode!!.text = "游戏"
        btn_fastmode!!.text = "极速"
        val cfg = Props.getProp("vtools.powercfg")
        when (cfg) {
            "default" -> {
                btn_defaultmode!!.text = "均衡 √"
            }
            "game" -> {
                btn_gamemode!!.text = "游戏 √"
            }
            "powersave" -> {
                btn_powersave!!.text = "省电 √"
            }
            "fast" -> {
                btn_fastmode!!.text = "极速 √"
            }
        }
    }

    private fun installConfig(after:String) {
        if (ConfigInfo.getConfigInfo().UseBigCore)
            AppShared.WriteFile(context.assets, ConfigInfo.getConfigInfo().CPUName + "/powercfg-bigcore.sh", "powercfg.sh")
        else
            AppShared.WriteFile(context.assets, ConfigInfo.getConfigInfo().CPUName + "/powercfg-default.sh", "powercfg.sh")
        cmdshellTools.DoCmdSync(Consts.InstallPowerToggleConfigToCache + "\n\n" + after)
        setModeState();
    }

    override fun onPause() {
        super.onPause()
        try {
            EventBus.unSubscribe(Events.ModeToggle, eventSubscribe)
        } catch (ex: Exception) {
        }
    }

    override fun onDestroy() {
        try {
            EventBus.unSubscribe(Events.ModeToggle, eventSubscribe)
        } catch (ex: Exception) {
        }
        super.onDestroy()
    }
}
