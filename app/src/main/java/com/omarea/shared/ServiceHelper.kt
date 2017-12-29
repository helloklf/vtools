package com.omarea.shared

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.omarea.vboot.R
import java.io.BufferedWriter
import java.io.IOException
import java.util.*
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.widget.TextView
import android.content.Intent
import com.omarea.vboot.ActivityMain


/**
 * Created by helloklf on 2016/10/1.
 */
class ServiceHelper(private var context: Context) {
    private var lastPackage: String? = null
    private var lastModePackage: String? = null
    private var lastMode = Configs.None
    private var p: Process? = null
    private val serviceCreatedTime = Date().time
    private var out: BufferedWriter? = null
    private var spfPowercfg: SharedPreferences = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
    private var spfBooster: SharedPreferences = context.getSharedPreferences(SpfConfig.BOOSTER_CONFIG_SPF, Context.MODE_PRIVATE)
    private var spfGlobal: SharedPreferences = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
    //标识是否已经加载完设置
    private var settingsLoaded = false
    private var ignoredList = arrayListOf<String>("com.miui.securitycenter", "android", "com.android.systemui", "com.omarea.vboot", "com.miui.touchassistant", "com.miui.contentextension", "com.miui.systemAdSolution")
    private var autoBooster = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_BOOSTER, false)
    private var dyamicCore = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, false)
    private var debugMode = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DEBUG, false)
    private var delayStart = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DELAY, false)
    private var showNofity = spfBooster.getBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, true)

    private var listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == SpfConfig.GLOBAL_SPF_AUTO_BOOSTER) {
            if (this.lastPackage != null) {
                autoBoosterApp(this.lastPackage)
            }
            autoBooster = sharedPreferences.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_BOOSTER, false)
        } else if (key == SpfConfig.GLOBAL_SPF_DYNAMIC_CPU || key == SpfConfig.GLOBAL_SPF_DYNAMIC_CPU_CONFIG) {
            dyamicCore = sharedPreferences.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, false)
            doCmd(Consts.ExecuteConfig)
            if (this.lastModePackage != null) {
                autoToggleMode(this.lastModePackage)
            } else if (dyamicCore) {
                toggleConfig(Configs.Default)
            }
        } else if (key == SpfConfig.GLOBAL_SPF_DEBUG) {
            debugMode = sharedPreferences.getBoolean(SpfConfig.GLOBAL_SPF_DEBUG, false)
        } else if (key == SpfConfig.GLOBAL_SPF_NOTIFY) {
            showNofity = sharedPreferences.getBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, true)
            if (!showNofity) {
                hideNotify()
            } else if(notification == null) {
                showNotify()
            }
        }
    }

    init {
        spfGlobal.registerOnSharedPreferenceChangeListener(listener)

        //添加输入法到忽略列表
        Thread(Runnable {
            showNotify("辅助服务已启动")

            val im = (context.getSystemService(Context.INPUT_METHOD_SERVICE)) as InputMethodManager?
            if (im == null)
                return@Runnable

            val inputList = im.inputMethodList
            for (input in inputList) {
                ignoredList.add(input.packageName)
            }
        }).start()
    }

    private var notification: Notification? = null
    private var notificationManager: NotificationManager? = null

    //显示通知
    private fun showNotify(msg: String = "辅助服务正在后台运行") {
        if (!showNofity) {
            return
        }
        //获取PendingIntent
        val mainIntent = Intent(context, ActivityMain::class.java)
        val mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        notification =
                Notification.Builder(context)
                        .setSmallIcon(R.drawable.linux)
                        .setContentTitle("微工具箱")
                        .setContentText(msg)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true)
                        //.setDefaults(Notification.DEFAULT_SOUND)
                        .setContentIntent(mainPendingIntent)
                        .build()

        notification!!.flags = Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        notificationManager?.notify(0x100, notification)
    }

    //隐藏通知
    private fun hideNotify() {
        if (notification != null) {
            notificationManager?.cancel(0x100)
            notification = null
            notificationManager = null
        }
    }

    //加载设置
    private fun settingsLoad(): Boolean {
        if (delayStart && Date().time - serviceCreatedTime < 20000)
            return false

        doCmd(Consts.DisableSELinux)

        settingsLoaded = true

        return true
    }

    internal var myHandler = Handler()

    private fun tryExit() {
        try {
            if (out != null)
                out!!.close()
            out = null
        } catch (ex: Exception) {
        }
        out = null
        try {
            p!!.destroy()
        } catch (ex: Exception) {
        }
        p = null
    }

    private fun doCmd(cmd: String, isRedo: Boolean = false) {
        Thread(Runnable {
            try {
                //tryExit()
                if (p == null || isRedo || out == null) {
                    tryExit()
                    p = Runtime.getRuntime().exec("su")
                    out = p!!.outputStream.bufferedWriter()
                }

                out!!.write(cmd)
                out!!.write("\n\n")
                out!!.flush()
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
            showMsg("$modeName \n$packageName")
        showNotify("$modeName \n$packageName")
    }

    //自动切换模式
    private fun autoToggleMode(packageName: String?) {
        if (!dyamicCore || packageName == null)
            return

        //如果没有切换应用
        if (packageName == lastModePackage)
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
                } else return
            }
            "game" -> {
                if (lastMode != Configs.Game) {
                    try {
                        toggleConfig(Configs.Game)
                        showModeToggleMsg(packageName, "性能模式")
                    } catch (ex: Exception) {
                        showModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！")
                    }
                } else return
            }
            "fast" -> {
                if (lastMode != Configs.Fast) {
                    try {
                        toggleConfig(Configs.Fast)
                        showModeToggleMsg(packageName, "极速模式")
                    } catch (ex: Exception) {
                        showModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！")
                    }
                } else return
            }
            else -> {
                if (lastMode != Configs.Default) {
                    try {
                        toggleConfig(Configs.Default)
                        showModeToggleMsg(packageName, "均衡模式")
                    } catch (ex: Exception) {
                        showModeToggleMsg(packageName, "切换模式失败，请允许本应用使用ROOT权限！")
                    }
                } else return
            }
        }
        lastModePackage = packageName
    }

    //终止进程
    private fun autoBoosterApp(packageName: String?) {
        if (!autoBooster || lastPackage == null || packageName == null)
            return

        if (lastPackage == "android" || lastPackage == "com.android.systemui" || lastPackage == "com.omarea.vboot" || lastPackage.equals(packageName))
            return

        if (spfBooster.getBoolean(SpfConfig.BOOSTER_SPF_CLEAR_CACHE, false)) {
            doCmd(Consts.ClearCache)
        }

        if (spfBooster.contains(lastPackage)) {
            if (spfBooster.getBoolean(SpfConfig.BOOSTER_SPF_DOZE_MOD, true)) {
                try {
                    doCmd("dumpsys deviceidle enable; am set-inactive $lastPackage true")
                    //am set-idle com.tencent.mobileqq true
                    if (debugMode)
                        showMsg("休眠 " + lastPackage)
                } catch (ex: Exception) {
                }
                return
            }

            //android.os.Process.killProcess(android.os.Process.myPid());//自杀
            try {
                doCmd("killall -9 $lastPackage;pkill -9 $lastPackage;pgrep $lastPackage |xargs kill -9;")
                if (debugMode)
                    showMsg("结束 " + lastPackage)
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

    fun onFocusAppChanged(pkgName: String) {
        val packageName = pkgName
        if (!settingsLoaded && !settingsLoad())
            return

        if (lastPackage == packageName || ignoredList.contains(packageName))
            return

        if (lastPackage == null)
            lastPackage = "com.android.systemui"

        autoBoosterApp(packageName)
        autoToggleMode(packageName.toLowerCase())

        lastPackage = packageName
    }

    fun onInterrupt() {
        hideNotify()
    }

    companion object {
        //判断服务是否激活
        fun serviceIsRunning(context: Context): Boolean {
            val m = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val serviceInfos = m.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            for (serviceInfo in serviceInfos) {
                if (serviceInfo.id == "${Consts.PACKAGE_NAME}/.AccessibilityServiceVTools") {
                    return true
                }
            }
            return false
        }
    }
}
