package com.omarea.shared

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import java.io.DataOutputStream
import java.io.IOException
import java.util.*


/**
 * Created by helloklf on 2016/10/1.
 */
class ServiceHelper(private var context: Context) {
    private var lastPackage: String? = null
    private var lastMode = Configs.None
    private var p: Process? = null
    private val serviceCreatedTime = Date().time
    private var out: DataOutputStream? = null
    private var spfPowercfg: SharedPreferences = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
    private var spfBooster: SharedPreferences = context.getSharedPreferences(SpfConfig.BOOSTER_CONFIG_SPF, Context.MODE_PRIVATE)
    private var spfGlobal: SharedPreferences = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
    //标识是否已经加载完设置
    private var settingsLoaded = false
    private var ignoredList = arrayListOf<String>("com.miui.securitycenter", "android", "com.android.systemui", "com.omarea.vboot", "com.miui.touchassistant")
    private var autoBooster = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_BOOSTER, false)
    private var dyamicCore = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, false)
    private var debugMode = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DEBUG, false)
    private var delayStart = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DELAY, false)

    private var listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == SpfConfig.GLOBAL_SPF_AUTO_BOOSTER) {
            if (this.lastPackage != null) {
                autoBoosterApp(this.lastPackage!!)
            }
            autoBooster = sharedPreferences.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_BOOSTER, false)
        } else if (key == SpfConfig.GLOBAL_SPF_DYNAMIC_CPU || key == SpfConfig.GLOBAL_SPF_DYNAMIC_CPU_CONFIG) {
            doCmd(Consts.ExecuteConfig)
            if (this.lastPackage != null) {
                autoToggleMode(this.lastPackage!!)
            } else {
                toggleConfig(Configs.Default)
            }
            dyamicCore = sharedPreferences.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, false)
        } else if (key == SpfConfig.GLOBAL_SPF_DEBUG) {
            debugMode = sharedPreferences.getBoolean(SpfConfig.GLOBAL_SPF_DEBUG, false)
        }
    }

    init {
        val crashHandler = CrashHandler.instance
        crashHandler.init(context)

        spfGlobal.registerOnSharedPreferenceChangeListener(listener)
    }

    //加载设置
    private fun settingsLoad(): Boolean {
        if (delayStart && Date().time - serviceCreatedTime < 20000)
            return false

        doCmd(Consts.DisableSELinux)
        doCmd(Consts.ExecuteConfig)
        showMsg("微工具箱增强服务已启动")

        settingsLoaded = true

        return true
    }

    internal var myHandler = Handler()

    private fun tryExit() {
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

    private fun doCmd(cmd: String, isRedo: Boolean = false) {
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
                    doCmd(cmd, true)
                else
                    showMsg("Failed execution action!\nError message : " + e.message + "\n\n\ncommand : \r\n" + cmd)
            }
        }).start()
    }

    private fun showMsg(msg: String) {
        myHandler.post { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }

    private fun showModeToggleMsg(packageName: String, modeName: String) {
        if (debugMode)
            showMsg("Package: $packageName\n Mode: $modeName")
    }

    //自动切换模式
    private fun autoToggleMode(packageName: String) {
        if (!dyamicCore)
            return

        if (packageName == "android" || packageName == "com.android.systemui" || packageName == "com.omarea.vboot" || packageName.contains("inputmethod"))
            return

        //如果没有切换应用
        if (packageName == lastPackage && lastMode != Configs.Fast)
            return

        var mod = spfPowercfg.getString(packageName, "default")
        when (mod) {
            "igoned" -> {
                return
            }
            "powersave" -> {
                if (lastMode != Configs.PowerSave) {
                    try {
                        toggleConfig(Configs.PowerSave)
                        showModeToggleMsg(packageName, "节电模式")
                    } catch (ex: Exception) {
                        showModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！")
                    }
                }
                return
            }
            "game" -> {
                if (lastMode != Configs.Game) {
                    try {
                        toggleConfig(Configs.Game)
                        showModeToggleMsg(packageName, "性能模式")
                    } catch (ex: Exception) {
                        showModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！")
                    }
                }
                return
            }
            "fast" -> {
                if (lastMode != Configs.Fast) {
                    try {
                        toggleConfig(Configs.Fast)
                        showModeToggleMsg(packageName, "极速模式")
                    } catch (ex: Exception) {
                        showModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！")
                    }
                }
                return
            }
            else -> {
                if (lastMode != Configs.Default) {
                    try {
                        toggleConfig(Configs.Default)
                        showModeToggleMsg(packageName, "均衡模式")
                    } catch (ex: Exception) {
                        showModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！")
                    }
                }
            }
        }
    }

    //终止进程
    internal fun autoBoosterApp(packageName: String) {
        if (!autoBooster)
            return

        if (lastPackage == "android" || lastPackage == "com.android.systemui" || lastPackage == "com.omarea.vboot" || lastPackage.equals(packageName))
            return

        if (spfBooster.getBoolean(SpfConfig.BOOSTER_SPF_CLEAR_CACHE, false)) {
            doCmd(Consts.ClearCache)
        }

        if (spfBooster.contains(lastPackage)) {
            if (spfBooster.getBoolean(SpfConfig.BOOSTER_SPF_DOZE_MOD, false)) {
                try {
                    doCmd("dumpsys deviceidle enable; am set-inactive $lastPackage true")
                    //am set-idle com.tencent.mobileqq true
                    if (debugMode)
                        showMsg("休眠： " + lastPackage)
                } catch (ex: Exception) {
                }
                return
            }

            //android.os.Process.killProcess(android.os.Process.myPid());//自杀
            try {
                doCmd("killall -9 $lastPackage;pkill -9 $lastPackage;pgrep $lastPackage |xargs kill -9;")
                if (debugMode)
                    showMsg("结束运行: " + lastPackage)
            } catch (ex: Exception) {
            }
        }
    }

    //切换配置
    private fun toggleConfig(mode: Configs) {
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
        doCmd(cmd.toString())

        lastMode = mode
    }

    enum class Configs {
        None,
        Default,
        Game,
        PowerSave,
        Fast
    }

    fun onAccessibilityEvent(pkgName: String?) {
        var packageName = pkgName
        if (!settingsLoaded && !settingsLoad())
            return

        if (dyamicCore) {
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
        autoToggleMode(packageName)

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
                if (serviceInfo.id == "${Consts.PACKAGE_NAME}/.vtools_accessibility") {
                    return true
                }
            }
            return false
        }
    }
}
