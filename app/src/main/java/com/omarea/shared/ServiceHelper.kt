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
import kotlin.collections.ArrayList

/**
 * Created by helloklf on 2016/10/1.
 */
class ServiceHelper(context: Context) {
    private var context: Context? = null
    private var lastPackage: String? = null
    private var lastMode = Configs.None
    private var configInstalled: Boolean = false
    private var p: Process? = null
    private val serviceCreatedTime = Date().time
    internal var out: DataOutputStream? = null
    //标识是否已经加载完设置
    private var SettingsLoaded = false
    var ignoredList = arrayListOf<String>("com.miui.securitycenter", "android", "com.android.systemui", "com.omarea.vboot", "com.miui.touchassistant")

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

        if (!DynamicConfig().DynamicSupport(context!!)) {
            ConfigInfo.getConfigInfo().DyamicCore = false
        }

        ShowMsg("微工具箱增强服务已启动")

        SettingsLoaded = true
        AppShared.system_inited = true


        if (ConfigInfo.getConfigInfo().AutoStartSwap) {
            var sb = StringBuilder("setenforce 0\n")

            if(ConfigInfo.getConfigInfo().AutoStartZRAM) {
                var zramSize = ConfigInfo.getConfigInfo().AutoStartZRAMSize
                sb.append("if [ `cat /sys/block/zram0/disksize` != '" + zramSize + "000000' ] ; then ")
                sb.append("swapoff /dev/block/zram0 &> /dev/null;")
                sb.append("echo 1 > /sys/block/zram0/reset;")
                sb.append("echo " + zramSize + "000000 > /sys/block/zram0/disksize;")
                sb.append("mkswap /dev/block/zram0 &> /dev/null;")
                sb.append("swapon /dev/block/zram0 &> /dev/null;")
                sb.append("fi;\n")
            }

            if (ConfigInfo.getConfigInfo().AutoStartSwapDisZram) {
                sb.append("swapon /data/swapfile -p 32767\n")
                //sb.append("swapoff /dev/block/zram0\n")
            } else {
                sb.append("swapon /data/swapfile\n")
            }

            sb.append("echo 65 > /proc/sys/vm/swappiness\n")
            sb.append("echo " + ConfigInfo.getConfigInfo().AutoStartSwappiness + " > /proc/sys/vm/swappiness\n")

            DoCmd(sb.toString())
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
        if (!ConfigInfo.getConfigInfo().DyamicCore)
            return

        if (packageName == "android" || packageName == "com.android.systemui" || packageName == "com.omarea.vboot" || packageName!!.contains("inputmethod"))
            return

        //如果没有切换应用
        if (packageName == lastPackage && lastMode != Configs.Fast)
            return

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

        for (item in ConfigInfo.getConfigInfo().fastList) {
            if (item["packageName"].toString() == packageName) {
                if (lastMode != Configs.Fast) {
                    try {
                        ToggleConfig(Configs.Fast)
                        ShowModeToggleMsg(packageName, "fast mode")
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
        if (!ConfigInfo.getConfigInfo().AutoBooster)
            return

        if (lastPackage == "android" || lastPackage == "com.android.systemui" || lastPackage == "com.omarea.vboot")
            return

        if (ConfigInfo.getConfigInfo().AutoClearCache) {
            DoCmd(Consts.ClearCache)
        }

        if (ConfigInfo.getConfigInfo().blacklist.contains(lastPackage)) {
            if (ConfigInfo.getConfigInfo().UsingDozeMod) {
                try {
                    DoCmd("am set-inactive $lastPackage true")
                    //am set-idle com.tencent.mobileqq true
                    if (ConfigInfo.getConfigInfo().DebugMode)
                        ShowMsg("force doze: " + lastPackage)
                } catch (ex: Exception) {
                }
                return
            }

            //android.os.Process.killProcess(android.os.Process.myPid());//自杀
            try {
                DoCmd("pgrep $lastPackage |xargs kill -9")
                if (ConfigInfo.getConfigInfo().DebugMode)
                    ShowMsg("force kill: " + lastPackage)
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

        if (!DynamicConfig().DynamicSupport(context!!)) {
            ShowMsg("未找到对应到当前SOC的调频配置文件！")
            return
        }

        try {
            val ass = context!!.assets

            val cpuNumber = ConfigInfo.getConfigInfo().CPUName.replace("msm", "")

            if (ConfigInfo.getConfigInfo().UseBigCore) {
                AppShared.WriteFile(ass, ConfigInfo.getConfigInfo().CPUName + "/init.qcom.post_boot-bigcore.sh", "init.qcom.post_boot.sh")
                AppShared.WriteFile(ass, ConfigInfo.getConfigInfo().CPUName + "/powercfg-bigcore.sh", "powercfg.sh")
            } else {
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
        if (!SettingsLoaded && !SettingsLoad())
            return

        if (ConfigInfo.getConfigInfo().DyamicCore) {
            if (!configInstalled || lastPackage == null) {
                lastPackage = "com.android.systemui"
                InstallConfig()
            }
        } else if (lastPackage == null)
            lastPackage = "com.android.systemui"

        if (packageName == null)
            packageName = lastPackage

        if(lastPackage==packageName || ignoredList.contains(packageName))
            return

        autoBoosterApp(packageName!!)
        autoToggleMode(packageName!!)

        lastPackage = packageName
    }

    fun onInterrupt() {
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
