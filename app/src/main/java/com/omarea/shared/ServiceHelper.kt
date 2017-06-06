package com.omarea.shared

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Message
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import com.omarea.shell.DynamicConfig
import com.omarea.vboot.reciver_batterychanged
import java.io.DataOutputStream
import java.io.IOException
import java.util.*

/**
 * Created by helloklf on 2016/10/1.
 */
class ServiceHelper(context: Context) {
    private var batteryChangedReciver: reciver_batterychanged? = null
    private var context: Context?= null
    private var lastPackage: String? = null
    private var lastMode = Configs.None
    private var onChanger: Boolean = false//是否正在充电
    private var configInstalled: Boolean = false
    private var batteryLevel: Int = 0 //电量级别
    private var p: Process? = null
    private val serviceCreatedTime = Date().time
    //标识是否已经加载完设置
    private var SettingsLoaded = false

    init {
        EventBus.subscribe(Events.DyamicCoreConfigChanged, object : IEventSubscribe {
            override fun messageRecived(message: Any?) {
                if (ConfigInfo.getConfigInfo().DyamicCore) {
                    InstallConfig()
                    onAccessibilityEvent()
                } else
                    UnInstallConfig()
            }
        })
        EventBus.subscribe(Events.PowerDisConnection, object : IEventSubscribe {
            override fun messageRecived(message: Any?) {
                onChanger = false
                onAccessibilityEvent()
            }
        })
        EventBus.subscribe(Events.PowerConnection, object : IEventSubscribe {
            override fun messageRecived(message: Any?) {
                onChanger = true
                onAccessibilityEvent()
            }
        })
        EventBus.subscribe(Events.BatteryChanged, object : IEventSubscribe {
            override fun messageRecived(message: Any?) {
                batteryLevel = message as Int
                onAccessibilityEvent()
            }
        })
        EventBus.subscribe(Events.CoreConfigChanged, object : IEventSubscribe {
            override fun messageRecived(message: Any?) {
                configInstalled = false
                onAccessibilityEvent()
            }
        })

        this.context = context
    }

    //加载设置
    private fun SettingsLoad(): Boolean {
        if (ConfigInfo.getConfigInfo().DelayStart && !AppShared.system_inited && Date().time - serviceCreatedTime < 20000)
            return false

        DoCmd(Consts.DisableSELinux)

        if (!DynamicConfig().DynamicSupport(ConfigInfo.getConfigInfo().CPUName)) {
            ConfigInfo.getConfigInfo().DyamicCore = false
        }

        ShowMsg("微工具箱增强服务已启动")

        SettingsLoaded = true
        AppShared.system_inited = true

        try {
            if (batteryChangedReciver == null) {
                //监听电池改变
                batteryChangedReciver = reciver_batterychanged()
                reciver_batterychanged.serviceHelper = this
                //启动完成
                val ACTION_BOOT_COMPLETED = IntentFilter(Intent.ACTION_BOOT_COMPLETED)
                context!!.registerReceiver(batteryChangedReciver, ACTION_BOOT_COMPLETED)
                //电源连接
                val ACTION_POWER_CONNECTED = IntentFilter(Intent.ACTION_POWER_CONNECTED)
                context!!.registerReceiver(batteryChangedReciver, ACTION_POWER_CONNECTED)
                //电源断开
                val ACTION_POWER_DISCONNECTED = IntentFilter(Intent.ACTION_POWER_DISCONNECTED)
                context!!.registerReceiver(batteryChangedReciver, ACTION_POWER_DISCONNECTED)
                //电量变化
                val ACTION_BATTERY_CHANGED = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                context!!.registerReceiver(batteryChangedReciver, ACTION_BATTERY_CHANGED)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return true
    }

    internal var myHandler: android.os.Handler = object : android.os.Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }

    internal fun tryExit() {
        try {
            if (out != null)
                out!!.close()
        } catch (ex: Exception) {
        }

        try {
            p!!.destroy()
        } catch (ex: Exception) {
        }

    }

    internal var out: DataOutputStream? = null

    @JvmOverloads internal fun DoCmd(cmd: String, isRedo: Boolean = false) {
        Thread(Runnable {
            try {
                tryExit()
                if (p == null || isRedo || out == null) {
                    tryExit()
                    p = Runtime.getRuntime().exec("su")
                    out = DataOutputStream(p!!.outputStream)
                }
                out!!.writeBytes(cmd)
                out!!.writeBytes("\n")
                out!!.flush()
                //out!!.close()
            } catch (e: IOException) {
                //重试一次
                if (!isRedo)
                    DoCmd(cmd, true)
                else
                    ShowMsg("Failed execution action!\nError message : " + e.message + "\n\n\ncommand : \r\n" + cmd)
            }
        }).start()
    }

    private fun ShowMsg(msg: String) {
        if (context != null)
            myHandler.post { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
        else {
            //XposedBridge.log("微工具箱 Message：" + msg);
        }
    }

    internal var msgTemplate = "Package: %s\n Mode: %s"

    private fun ShowModeToggleMsg(packageName: String, modeName: String) {
        if (ConfigInfo.getConfigInfo().DebugMode)
            ShowMsg(String.format(msgTemplate, packageName, modeName))
    }

    //自动切换模式
    private fun autoToggleMode(packageName: String) {
        //打包安装程序速度优化-使用游戏模式
        if (packageName == "com.android.packageinstaller") {
            if (lastMode != Configs.Game) {
                try {
                    ToggleConfig(Configs.Game)
                    ShowModeToggleMsg("com.android.packageinstaller", "performance mode")
                } catch (ex: Exception) {
                    ShowModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！")
                }

            }
            return
        }

        for (item in ConfigInfo.getConfigInfo().gameList) {
            if (item["packageName"].toString() == packageName) {
                if (lastMode != Configs.Game) {
                    try {
                        ToggleConfig(Configs.Game)
                        ShowModeToggleMsg(packageName, "performance mode")
                    } catch (ex: Exception) {
                        ShowModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！")
                    }

                }
                return
            }
        }


        for (item in ConfigInfo.getConfigInfo().powersaveList) {
            if (item["packageName"].toString() == packageName) {
                if (lastMode != Configs.PowerSave) {
                    try {
                        ToggleConfig(Configs.PowerSave)
                        ShowModeToggleMsg(packageName, "powersave mode")
                    } catch (ex: Exception) {
                        ShowModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！")
                    }

                }
                return
            }
        }

        if (lastMode != Configs.Default) {
            try {
                ToggleConfig(Configs.Default)
                ShowModeToggleMsg(packageName, "balance mode")
            } catch (ex: Exception) {
                ShowModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！")
            }

        }
    }

    //终止进程
    internal fun autoBoosterApp(packageName: String) {
        if (lastPackage == "android" || lastPackage == "com.android.systemui")
            return

        if (ConfigInfo.getConfigInfo().blacklist.contains(packageName)) {
            if (ConfigInfo.getConfigInfo().UsingDozeMod) {
                try {
                    DoCmd("am set-inactive $packageName true")
                    //am set-idle com.tencent.mobileqq true
                    if (ConfigInfo.getConfigInfo().DebugMode)
                        ShowMsg("force doze: " + packageName)
                } catch (ex: Exception) {

                }

                return
            }

            //android.os.Process.killProcess(android.os.Process.myPid());//自杀
            try {
                DoCmd("pgrep $packageName |xargs kill -9")
                if (ConfigInfo.getConfigInfo().DebugMode)
                    ShowMsg("force kill: " + packageName)
            } catch (ex: Exception) {

            }

        }
    }

    //切换配置
    @Throws(IOException::class, InterruptedException::class)
    fun ToggleConfig(mode: Configs) {
        val cmd = StringBuilder()
        when (mode) {
            ServiceHelper.Configs.Game -> {
                cmd.append(Consts.ToggleGameMode)
            }
            ServiceHelper.Configs.PowerSave -> {
                cmd.append(Consts.TogglePowersaveMode)
            }
            ServiceHelper.Configs.Fast -> {
                cmd.append(Consts.ToggleFastMode)
            }
            else -> {
                cmd.append(Consts.ToggleDefaultMode)
            }
        }
        DoCmd(cmd.toString())

        lastMode = mode
    }

    //安装调频文件
    internal fun InstallConfig() {
        if (context == null) return

        if (!DynamicConfig().DynamicSupport(ConfigInfo.getConfigInfo().CPUName)) {
            ShowMsg("Unsupported device!(Support only msm8992, msm8996,and other devices create your own configuration files)")
            return
        }

        try {
            val ass = context!!.assets

            val cpuNumber = ConfigInfo.getConfigInfo().CPUName.replace("msm", "")

            if (ConfigInfo.getConfigInfo().UseBigCore) {
                AppShared.WriteFile(ass, ConfigInfo.getConfigInfo().CPUName + "/thermal-engine-bigcore.conf", "thermal-engine.conf")
                AppShared.WriteFile(ass, ConfigInfo.getConfigInfo().CPUName + "/init.qcom.post_boot-bigcore.sh", "init.qcom.post_boot.sh")
                AppShared.WriteFile(ass, ConfigInfo.getConfigInfo().CPUName + "/powercfg-bigcore.sh", "powercfg.sh")
            } else {
                AppShared.WriteFile(ass, ConfigInfo.getConfigInfo().CPUName + "/thermal-engine-default.conf", "thermal-engine.conf")
                AppShared.WriteFile(ass, ConfigInfo.getConfigInfo().CPUName + "/init.qcom.post_boot-default.sh", "init.qcom.post_boot.sh")
                AppShared.WriteFile(ass, ConfigInfo.getConfigInfo().CPUName + "/powercfg-default.sh", "powercfg.sh")
            }


            val cmd = StringBuilder().append(Consts.InstallConfig).append(Consts.ExecuteConfig)
                    .toString().replace("cpuNumber", cpuNumber)
            DoCmd(cmd)

            ToggleConfig(Configs.Default)
            configInstalled = true

            if (ConfigInfo.getConfigInfo().DebugMode)
                ShowMsg("The configuration file is installed!")
        } catch (ex: Exception) {
            ShowMsg("The configuration file installation failed!")
        }

    }

    fun UnInstallConfig() {
        if (ConfigInfo.getConfigInfo().CPUName == null || ConfigInfo.getConfigInfo().CPUName.trim { it <= ' ' } == "") return

        try {
            ToggleConfig(Configs.Game)
            DoCmd(StringBuilder().append(Consts.ExecuteConfig)
                    .toString().replace("cpuNumber", ConfigInfo.getConfigInfo().CPUName.replace("msm", ""))
            )
            ShowMsg("Restore complete, you may need to restart your phone!")
        } catch (ex: Exception) {
            ShowMsg("You may need to restart your phone！")
        }

    }

    enum class Configs {
        None,
        Default,
        Game,
        PowerSave,
        Fast
    }

    fun onAccessibilityEvent() {
        try {
            onAccessibilityEvent(lastPackage)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    fun onAccessibilityEvent(packageName: String?) {
        var packageName = packageName
        if (!SettingsLoaded && !SettingsLoad() || !ConfigInfo.getConfigInfo().DyamicCore)
            return

        if (!configInstalled || lastPackage == null) {
            lastPackage = "com.android.systemui"
            InstallConfig()
        }

        if (packageName == null) {
            packageName = lastPackage
        }
        if (ConfigInfo.getConfigInfo().AutoClearCache && lastPackage !== packageName) {
            DoCmd(Consts.ClearCache)
        }

        /*
        //开启电源适配且电量充足
        if (onChanger && ConfigInfo.getConfigInfo().PowerAdapter && ConfigInfo.getConfigInfo().CPUName == "msm8996" && batteryLevel > 29) {
            try {
                if (lastPackage !== packageName && lastMode == Configs.Fast)
                    return

                ToggleConfig(Configs.Fast)
                lastPackage = packageName
                if (ConfigInfo.getConfigInfo().DebugMode)
                    ShowMsg("Power enough to turn on fast mode...")
            } catch (ex: Exception) {

            }

            return
        }

        if (!onChanger && ConfigInfo.getConfigInfo().PowerAdapter && batteryLevel < 20 && batteryLevel > 0) {
            try {
                if (lastMode != Configs.PowerSave) {
                    ToggleConfig(Configs.PowerSave)
                    lastPackage = packageName
                    ShowMsg("Low power, turn on power saving mode...")
                }
            } catch (ex: Exception) {

            }

            return
        }
        */

        //如果没有切换应用
        if (packageName == lastPackage && lastMode != Configs.Fast)
            return

        if (packageName == "android" || packageName == "com.android.systemui" || packageName == "com.omarea.vboot" || packageName!!.contains("inputmethod"))
            return

        autoToggleMode(packageName)

        if (ConfigInfo.getConfigInfo().AutoBooster)
            autoBoosterApp(lastPackage!!)

        lastPackage = packageName
    }

    fun onInterrupt() {
        if (batteryChangedReciver != null) {
            DoCmd(Consts.BPReset)
            context!!.unregisterReceiver(batteryChangedReciver)
            reciver_batterychanged.serviceHelper = null
            batteryChangedReciver = null
        }
        UnInstallConfig()
    }

    companion object {

        //判断服务是否激活
        fun serviceIsRunning(context: Context): Boolean {
            val m = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val serviceInfos = m.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            for (serviceInfo in serviceInfos) {
                if (serviceInfo.id == "com.omarea.vboot/.vtools_accessibility") {
                    return true
                }
            }
            return false
        }
    }

}
