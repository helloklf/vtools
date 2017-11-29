package com.omarea.shared

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.SharedPreferences
import android.os.Message
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import java.io.DataOutputStream
import java.io.IOException
import java.util.*
import android.app.ActivityManager



/**
 * Created by helloklf on 2016/10/1.
 */
class ServiceHelper(context: Context) {
    private var context: Context? = null
    private var lastPackage: String? = null
    private var lastMode = Configs.None
    private var p: Process? = null
    private val serviceCreatedTime = Date().time
    internal var out: DataOutputStream? = null
    private var spfPowercfg: SharedPreferences
    private var spfBooster: SharedPreferences
    private var spfGlobal: SharedPreferences
    //标识是否已经加载完设置
    private var SettingsLoaded = false
    var ignoredList = arrayListOf<String>("com.miui.securitycenter", "android", "com.android.systemui", "com.omarea.vboot", "com.miui.touchassistant")


    var AutoBooster = true;
    var DyamicCore = false;
    var DebugMode = false;
    var DelayStart = false;
    var listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == SpfConfig.GLOBAL_SPF_AUTO_BOOSTER) {
            if (this.lastPackage != null) {
                autoBoosterApp(this.lastPackage!!)
            }
            AutoBooster = sharedPreferences.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_BOOSTER, false)
        } else if (key == SpfConfig.GLOBAL_SPF_DYNAMIC_CPU || key == SpfConfig.GLOBAL_SPF_DYNAMIC_CPU_CONFIG) {
            DoCmd(Consts.ExecuteConfig)
            if (this.lastPackage != null) {
                autoToggleMode(this.lastPackage!!)
            } else {
                ToggleConfig(Configs.Default)
            }
            DyamicCore = sharedPreferences.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, false)
        } else if (key == SpfConfig.GLOBAL_SPF_DEBUG) {
            DebugMode = sharedPreferences.getBoolean(SpfConfig.GLOBAL_SPF_DEBUG, false)
        }
    }

    init {
        this.context = context
        spfPowercfg = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
        spfBooster = context.getSharedPreferences(SpfConfig.BOOSTER_CONFIG_SPF, Context.MODE_PRIVATE)
        spfGlobal = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

        DebugMode = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DEBUG, false)
        AutoBooster = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_BOOSTER, false)
        DyamicCore = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, false)
        DelayStart = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DELAY, false)

        spfGlobal.registerOnSharedPreferenceChangeListener(listener)
    }

    //加载设置
    private fun SettingsLoad(): Boolean {
        if (DelayStart && Date().time - serviceCreatedTime < 20000)
            return false

        DoCmd(Consts.DisableSELinux)
        DoCmd(Consts.ExecuteConfig)
        ShowMsg("微工具箱增强服务已启动")

        SettingsLoaded = true

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
                //tryExit()
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
        if (DebugMode)
            ShowMsg(String.format(msgTemplate, packageName, modeName))
    }

    //自动切换模式
    private fun autoToggleMode(packageName: String) {
        if (!DyamicCore)
            return

        if (packageName == "android" || packageName == "com.android.systemui" || packageName == "com.omarea.vboot" || packageName!!.contains("inputmethod"))
            return

        //如果没有切换应用
        if (packageName == lastPackage && lastMode != Configs.Fast)
            return

        var mod = spfPowercfg.getString(packageName, "default")
        when(mod){
            "igoned" -> {
                return
            }
            "powersave" -> {
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
            "game" -> {
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
            "fast" -> {
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
            else -> {
                if (lastMode != Configs.Default) {
                    try {
                        ToggleConfig(Configs.Default)
                        ShowModeToggleMsg(packageName, "balance mode")
                    } catch (ex: Exception) {
                        ShowModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！")
                    }
                }
            }
        }
    }

    //终止进程
    internal fun autoBoosterApp(packageName: String) {
        if (!AutoBooster)
            return

        if (lastPackage == "android" || lastPackage == "com.android.systemui" || lastPackage == "com.omarea.vboot" || lastPackage.equals(packageName))
            return

        if (spfBooster.getBoolean(SpfConfig.BOOSTER_SPF_CLEAR_CACHE, false)) {
            DoCmd(Consts.ClearCache)
        }

        if (spfBooster.contains(lastPackage)) {
            if (spfBooster.getBoolean(SpfConfig.BOOSTER_SPF_DOZE_MOD, false)) {
                try {
                    DoCmd("dumpsys deviceidle enable; am set-inactive $lastPackage true")
                    //am set-idle com.tencent.mobileqq true
                    if (DebugMode)
                        ShowMsg("force doze: " + lastPackage)
                } catch (ex: Exception) {
                }
                return
            }

            //android.os.Process.killProcess(android.os.Process.myPid());//自杀
            try {
                DoCmd("killall -9 $lastPackage;pkill -9 $lastPackage;pgrep $lastPackage |xargs kill -9;")
                if (DebugMode)
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

    enum class Configs {
        None,
        Default,
        Game,
        PowerSave,
        Fast
    }

    fun onAccessibilityEvent(packageName: String?) {
        var packageName = packageName
        if (!SettingsLoaded && !SettingsLoad())
            return

        if (DyamicCore) {
            if (lastPackage != null) {
                lastPackage = "com.android.systemui"
            }
        } else if (lastPackage == null)
            lastPackage = "com.android.systemui"

        if (packageName == null)
            packageName = lastPackage

        if (lastPackage == packageName || ignoredList.contains(packageName))
            return

        autoBoosterApp(packageName!!)
        autoToggleMode(packageName!!)

        lastPackage = packageName
    }

    fun onInterrupt() {
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
