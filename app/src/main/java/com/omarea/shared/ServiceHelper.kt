package com.omarea.shared

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import com.omarea.shared.helper.*
import com.omarea.shell.AsynSuShellUnit
import com.omarea.shell.SuDo
import java.io.File
import java.util.*

/**
 * Created by helloklf on 2016/10/1.
 */
class ServiceHelper(private var context: Context) {
    private var lastPackage: String? = null
    private var lastModePackage: String? = null
    private var lastMode = ""
    private val serviceCreatedTime = Date().time
    private var spfPowercfg: SharedPreferences = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
    private var spfBlacklist: SharedPreferences = context.getSharedPreferences(SpfConfig.BOOSTER_BLACKLIST_SPF, Context.MODE_PRIVATE)
    private var spfAutoConfig: SharedPreferences = context.getSharedPreferences(SpfConfig.BOOSTER_SPF_CFG_SPF, Context.MODE_PRIVATE)
    private var spfGlobal: SharedPreferences = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
    //标识是否已经加载完设置
    private var settingsLoaded = false
    private var ignoredList = arrayListOf<String>("com.miui.securitycenter", "android", "com.android.systemui", "com.omarea.vboot", "com.miui.touchassistant", "com.miui.contentextension", "com.miui.systemAdSolution")
    private var autoBooster = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_BOOSTER, false)
    private var dyamicCore = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, false)
    private var debugMode = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DEBUG, false)
    private var delayStart = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DELAY, false)
    private var lockScreenOptimize = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_LOCK_SCREEN_OPTIMIZE, false)
    private var firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, "balance")
    private var screenOn: Boolean = true
    private var lastScreenOnOff:Long = 0

    //屏幕关闭后切换网络延迟（ms）
    private val SCREEN_OFF_SWITCH_NETWORK_DELAY:Long = 30000
    //屏幕关闭后清理任务延迟（ms）
    private val SCREEN_OFF_CLEAR_TASKS_DELAY:Long = 60000
    private var screenHandler = ScreenEventHandler({ onScreenOff() }, { onScreenOn() })
    private var handler = Handler()

    private var notifyHelper: NotifyHelper = NotifyHelper(context, spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, true))

    //屏幕关闭时执行
    private fun onScreenOff () {
        screenOn = false
        if (debugMode && autoBooster) {
            handler.postDelayed({
                if(!screenOn) {
                    showMsg("屏幕关闭！")
                    if(debugMode)
                        showMsg("动态响应-锁屏优化 已息屏，自动切换省电模式")
                }
            }, 5000)
        }
        lastScreenOnOff = System.currentTimeMillis()
        if (autoBooster) {
            screenHandler.postDelayed({
                onScreenOffCloseNetwork()
            }, SCREEN_OFF_SWITCH_NETWORK_DELAY)
        }
        clearTasks()
    }

    //屏幕关闭后 - 关闭网络
    private fun onScreenOffCloseNetwork() {
        if((settingsLoaded || settingsLoad()) && dyamicCore && lockScreenOptimize) {
            toggleConfig("powersave")
        }
        if (autoBooster && System.currentTimeMillis() - lastScreenOnOff >= SCREEN_OFF_SWITCH_NETWORK_DELAY && screenOn == false) {
            if (spfAutoConfig.getBoolean(SpfConfig.WIFI + SpfConfig.OFF, false))
                keepShell.doCmd("svc wifi disable")

            if (spfAutoConfig.getBoolean(SpfConfig.NFC + SpfConfig.OFF, false))
                keepShell.doCmd("svc nfc disable")

            if (spfAutoConfig.getBoolean(SpfConfig.DATA + SpfConfig.OFF, false))
                keepShell.doCmd("svc data disable")

            if (spfAutoConfig.getBoolean(SpfConfig.GPS + SpfConfig.OFF, false)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    keepShell.doCmd("settings put secure location_providers_allowed -gps;")
                } else {
                    keepShell.doCmd("settings put secure location_providers_allowed network")
                }
            }

            //if (debugMode)
            //    showMsg("屏幕关闭 - 网络模式已切换！")
        }
    }

    //点亮屏幕且解锁后执行
    private fun onScreenOn () {
        if (debugMode && autoBooster)
            showMsg("屏幕开启！")

        lastScreenOnOff = System.currentTimeMillis()
        if (screenOn == true) return

        screenOn = true

        if((settingsLoaded || settingsLoad()) && dyamicCore && lockScreenOptimize) {
            if(this.lastModePackage != null && !this.lastModePackage.isNullOrEmpty())
            {
                handler.postDelayed({
                    if(screenOn)
                        autoToggleMode(this.lastModePackage)
                    if(debugMode)
                        showMsg("动态响应-锁屏优化 已解锁，自动恢复配置")
                }, 5000)
            }
        }
        if (autoBooster && screenOn == true) {
            keepShell.doCmd("dumpsys deviceidle unforce;dumpsys deviceidle enable all;")
            if (spfAutoConfig.getBoolean(SpfConfig.WIFI + SpfConfig.ON, false))
                keepShell.doCmd("svc wifi enable")

            if (spfAutoConfig.getBoolean(SpfConfig.NFC + SpfConfig.ON, false))
                keepShell.doCmd("svc nfc enable")

            if (spfAutoConfig.getBoolean(SpfConfig.DATA + SpfConfig.ON, false))
                keepShell.doCmd("svc data enable")

            if (spfAutoConfig.getBoolean(SpfConfig.GPS + SpfConfig.ON, false)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    keepShell.doCmd("settings put secure location_providers_allowed -gps;settings put secure location_providers_allowed +gps")
                } else {
                    keepShell.doCmd("settings put secure location_providers_allowed gps,network")
                }
            }

            //if (debugMode)
            //    showMsg("屏幕开启 - 网络模式已切换！")
        }
    }

    private var listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == SpfConfig.GLOBAL_SPF_AUTO_BOOSTER) {
            if (this.lastPackage != null) {
                autoBoosterApp(this.lastPackage)
            }
            autoBooster = sharedPreferences.getBoolean(SpfConfig.GLOBAL_SPF_AUTO_BOOSTER, false)
        } else if (key == SpfConfig.GLOBAL_SPF_DYNAMIC_CPU || key == SpfConfig.GLOBAL_SPF_DYNAMIC_CPU_CONFIG) {
            dyamicCore = sharedPreferences.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CPU, false)
            keepShell.doCmd(Consts.ExecuteConfig)
            handler.postDelayed({
                if (!dyamicCore) {
                    notifyHelper.hideNotify()
                    notifyHelper.notify("辅助服务已启动，动态响应未开启")
                } else {
                    notifyHelper.notify("辅助服务已启动，动态响应已启动")
                    if (dyamicCore && this.lastModePackage != null) {
                        lastMode = ""
                        autoToggleMode(this.lastModePackage)
                    } else if (dyamicCore) {
                        toggleConfig(firstMode)
                    }
                }
            }, 2000)
        } else if (key == SpfConfig.GLOBAL_SPF_DEBUG) {
            debugMode = sharedPreferences.getBoolean(key, false)
        } else if (key == SpfConfig.GLOBAL_SPF_LOCK_SCREEN_OPTIMIZE) {
            lockScreenOptimize = spfGlobal.getBoolean(key, false)
        } else if (key == SpfConfig.GLOBAL_SPF_NOTIFY) {
            notifyHelper.setNotify(sharedPreferences.getBoolean(key, true))
        } else if (key == SpfConfig.BOOSTER_SPF_CFG_SPF_CLEAR_TASKS) {

        } else if (key == SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE) {
            firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, "balance")
        }
    }

    init {
        spfGlobal.registerOnSharedPreferenceChangeListener(listener)

        notifyHelper.notify("辅助服务已启动，" + if(dyamicCore) "动态响应已启动" else "动态响应未开启")

        //添加输入法到忽略列表
        Thread(Runnable {
            ignoredList.addAll(InputHelper(context).getInputMethods())
        }).start()

        ReciverLock.autoRegister(context, screenHandler)
    }

    //加载设置
    private fun settingsLoad(): Boolean {
        if (delayStart && Date().time - serviceCreatedTime < 25000){
            if(dyamicCore)
                notifyHelper.notify("已开启延迟启动，动态响应暂时未生效")
            return false
        }

        if(spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, true))
            keepShell.doCmd(Consts.DisableSELinux)

        if  (dyamicCore)
            keepShell.doCmd(Consts.ExecuteConfig)

        settingsLoaded = true

        return true
    }

    private var keepShell: KeepShell = KeepShell(context)

    //显示消息
    private fun showMsg(msg: String) {
        screenHandler.post { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }

    //显示模式切换通知
    private fun showModeToggleMsg(packageName: String, modeName: String) {
        if (debugMode)
            showMsg("$modeName \n$packageName")
    }

    //更新通知
    private fun updateModeNofity(){
        if (lastModePackage != null && !lastModePackage.isNullOrEmpty()) {
            notifyHelper.notify("${getModName(lastMode)} -> $lastModePackage", lastModePackage!!)
        } else
            notifyHelper.notify("${getModName(lastMode)} -> $lastModePackage")
    }

    private fun getModName(mode:String) : String {
        when(mode) {
            "powersave" ->      return "省电模式"
            "performance" ->      return "性能模式"
            "fast" ->      return "极速模式"
            "balance" ->   return "均衡模式"
            else ->         return "未知模式"
        }
    }

    //自动切换模式
    private fun autoToggleMode(packageName: String?) {
        if (!dyamicCore || packageName == null || packageName == lastModePackage)
            return

        val mode = spfPowercfg.getString(packageName, firstMode)
        when (mode) {
            "igoned" ->     return
            else ->{
                if (lastMode != mode) {
                    toggleConfig(mode)
                    showModeToggleMsg(packageName, getModName(mode))
                }

                lastModePackage = packageName
                updateModeNofity()
            }
        }
    }

    //终止进程
    private fun autoBoosterApp(packageName: String?) {
        if (!autoBooster || lastPackage == null || packageName == null)
            return

        if (lastPackage == "android" || lastPackage == "com.android.systemui" || lastPackage == "com.omarea.vboot" || lastPackage.equals(packageName))
            return

        if (spfAutoConfig.getBoolean(SpfConfig.BOOSTER_SPF_CFG_SPF_CLEAR_CACHE, false)) {
            keepShell.doCmd(Consts.ClearCache)
        }

        if (spfBlacklist.contains(lastPackage)) {
            if (spfAutoConfig.getBoolean(SpfConfig.BOOSTER_SPF_CFG_SPF_DOZE_MOD, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
                dozeApp(lastPackage!!)
            } else {
                //android.os.Process.killProcess(android.os.Process.myPid());//自杀
                killApp(lastPackage!!)
            }
        }
    }

    private fun toggleConfig(mode: String) {
        if (File(Consts.POWER_CFG_PATH).exists()) {
            keepShell.doCmd(String.format(Consts.ToggleMode, mode))
            lastMode = mode
        } else {
            ConfigInstaller().installPowerConfig(context, String.format(Consts.ToggleMode, mode));
            lastMode = mode
        }
    }

    //#region 工具方法
    //休眠指定包名的应用
    private fun dozeApp(packageName: String) {
        keepShell.doCmd("dumpsys deviceidle whitelist -$packageName;\ndumpsys deviceidle enable;\ndumpsys deviceidle enable all;\nam set-inactive $packageName true")
        if (debugMode)
            showMsg("休眠 " + packageName)
    }

    //杀死指定包名的应用
    private fun killApp(packageName: String, showMsg: Boolean = true) {
        //keepShell.doCmd("killall -9 $packageName;pkill -9 $packageName;pgrep $packageName |xargs kill -9;")
        keepShell.doCmd("am stop $packageName;")
        if (debugMode && showMsg)
            showMsg("结束 " + packageName)
    }
    //#endregion

    //清理后台
    private fun clearTasks(timeout: Long =  SCREEN_OFF_CLEAR_TASKS_DELAY) {
        if (!autoBooster) {
            return
        }
        if (timeout == 0L) {
            if (spfAutoConfig.getBoolean(SpfConfig.BOOSTER_SPF_CFG_SPF_CLEAR_CACHE, false)) {
                keepShell.doCmd(Consts.ClearCache)
            }

            if (!screenOn && spfAutoConfig.getBoolean(SpfConfig.BOOSTER_SPF_CFG_SPF_CLEAR_TASKS, true)) {
                val cmds = StringBuilder()
                cmds.append("dumpsys deviceidle enable all;\n")
                cmds.append("dumpsys deviceidle force-idle;\n")


                cmds.append("\n\n")
                val spf = context.getSharedPreferences(SpfConfig.WHITE_LIST_SPF, Context.MODE_PRIVATE)
                for (item in spf.all) {
                    if (item.value == true) {
                        cmds.append("dumpsys deviceidle whitelist +${item.key}")
                    } else {
                        cmds.append("dumpsys deviceidle whitelist -${item.key}")
                    }
                    cmds.append(";\n")
                }
                cmds.append("\n\n")

                for (item in spfBlacklist.all) {
                    cmds.append("am set-inactive ${item.key} true")
                    keepShell.doCmd("am stop ${item.key};")
                    //cmds.append("killall -9 ${item.key};pkill -9 ${item.key};pgrep ${item.key} |xargs kill -9;")
                }
                cmds.append("dumpsys deviceidle step\n")
                cmds.append("dumpsys deviceidle step\n")
                cmds.append("dumpsys deviceidle step\n")
                cmds.append("dumpsys deviceidle step\n")
                var p:Process?

                AsynSuShellUnit(Handler()).exec(cmds.toString()).waitFor()
                if (debugMode)
                    showMsg("后台已自动清理...")
            }
        }
        else {
            //超时时间：1分钟
            screenHandler.postDelayed({
                if (System.currentTimeMillis() - lastScreenOnOff >= SCREEN_OFF_CLEAR_TASKS_DELAY) {
                    clearTasks(0)
                }
            }, SCREEN_OFF_CLEAR_TASKS_DELAY)
        }
    }

    //焦点应用改变
    fun onFocusAppChanged(pkgName: String) {
        val packageName = pkgName
        if (!settingsLoaded && !settingsLoad()) {
            return
        }

        if (lastPackage == packageName || ignoredList.contains(packageName))
            return

        if (lastPackage == null) {
            lastPackage = "com.android.systemui"
        }

        autoBoosterApp(packageName)
        autoToggleMode(packageName.toLowerCase())

        lastPackage = packageName
    }

    fun onInterrupt() {
        notifyHelper.hideNotify()
        ReciverLock.unRegister(context)
        keepShell.tryExit()
    }

    companion object {
        //判断服务是否激活
        fun serviceIsRunning(context: Context): Boolean {
            val m = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val serviceInfos = m.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            for (serviceInfo in serviceInfos) {
                //if (serviceInfo.id == "${Consts.PACKAGE_NAME}/.AccessibilityServiceVTools") {
                if (serviceInfo.id.endsWith("AccessibilityServiceVTools")) {
                    return true
                }
            }
            return false
        }

        fun startServiceUseRoot(context: Context) : Boolean {
            return SuDo(context).execCmdSync(
                "services=`settings get secure enabled_accessibility_services`;\n" +
                        "service='com.omarea.vboot/com.omarea.vboot.AccessibilityServiceVTools';\n" +
                        "echo \"\$services\" |grep -q \"\$service\"\n" +
                        "if [ \$? -gt -1 ]\n" +
                        "then\n" +
                        "\tsettings put secure enabled_accessibility_services \"\$services:\$service\"; \n" +
                        "fi\n" +
                        "settings put secure accessibility_enabled 1;\n" +
                        "am startservice -n \$service;\n"
            )
        }
    }
}
