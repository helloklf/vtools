package com.omarea.shared

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.WindowManager
import android.widget.Toast
import com.omarea.shared.helper.InputHelper
import com.omarea.shared.helper.NotifyHelper
import com.omarea.shared.helper.ReciverLock
import com.omarea.shared.helper.ScreenEventHandler
import com.omarea.shell.DumpTopAppliction
import com.omarea.shell.KeepShellAsync
import com.omarea.shell.Platform
import com.omarea.shell.RootFile
import java.io.File
import java.util.*

/**
 *
 * Created by helloklf on 2016/10/1.
 */
class ServiceHelper(private var context: AccessibilityService) : ModeList(context) {
    private var systemScene: SystemScene = SystemScene(context)
    private var lastPackage: String? = null
    private var lastModePackage: String? = "com.system.ui"
    private var lastMode = ""
    private var spfPowercfg: SharedPreferences = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
    private var spfGlobal: SharedPreferences = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
    private var dumpTopAppliction: DumpTopAppliction = DumpTopAppliction()
    private var ignoredList = arrayListOf(
            "com.miui.securitycenter",
            "android",
            "com.android.systemui",
            "com.omarea.vtools",
            "com.miui.touchassistant",
            "com.miui.contentextension",
            "com.miui.systemAdSolution")
    private var dyamicCore = false
    private var debugMode = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DEBUG, false)
    private var firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, BALANCE)
    private var accuSwitch: Boolean = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_ACCU_SWITCH, false)
    private var batteryMonitro: Boolean = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, false)
    private var screenOn: Boolean = true
    private var lastScreenOnOff: Long = 0
    //屏幕关闭后切换网络延迟（ms）
    private val SCREEN_OFF_SWITCH_NETWORK_DELAY: Long = 30000
    private var screenHandler = ScreenEventHandler({ onScreenOff() }, { onScreenOn() })
    private var handler = Handler(Looper.getMainLooper())
    private var notifyHelper: NotifyHelper = NotifyHelper(context, spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, true))
    private val sceneMode: SceneMode = SceneMode.getInstanceOrInit(context.contentResolver, AppConfigStore(context))!!
    private var timer: Timer? = null

    private fun startTimer() {
        if (timer == null) {
            val windowManager = this.context.getSystemService(WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            this.screenOn = display.state == Display.STATE_ON
            if (!screenOn) return
            val time = if (batteryMonitro) 2000L else 10000L
            timer = Timer(true)
            timer!!.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    notifyHelper.notify()
                }
            }, 0, time)
        }
    }

    private fun stopTimer() {
        try {
            if (timer != null) {
                timer!!.cancel()
                timer!!.purge()
                timer = null
            }
        } catch (ex: Exception) {
        }
    }

    /**
     * 屏幕关闭时执行
     */
    private fun onScreenOff() {
        if (screenOn == false)
            return
        screenOn = false
        notifyHelper.hideNotify()
        lastScreenOnOff = System.currentTimeMillis()

        screenHandler.postDelayed({
            if (!screenOn)
                onScreenOffCloseNetwork()
        }, SCREEN_OFF_SWITCH_NETWORK_DELAY)
        // TODO: 关闭屏幕后清理后台
        screenHandler.postDelayed({
            if (!screenOn) stopTimer()
        }, 10000)
    }

    /**
     * 屏幕关闭后 - 关闭网络
     */
    private fun onScreenOffCloseNetwork() {
        if (dyamicCore && !screenOn) {
            toggleConfig(POWERSAVE)
            updateModeNofity()
        }
        if (System.currentTimeMillis() - lastScreenOnOff >= SCREEN_OFF_SWITCH_NETWORK_DELAY && !screenOn) {
            systemScene.onScreenOff()
        }
    }

    /**
     * 点亮屏幕且解锁后执行
     */
    private fun onScreenOn() {
        if (debugMode)
            showMsg("屏幕开启！")

        lastScreenOnOff = System.currentTimeMillis()
        if (screenOn == true) return

        screenOn = true
        startTimer()
        notifyHelper.notify()

        if (dyamicCore) {
            if (this.lastModePackage != null && !this.lastModePackage.isNullOrEmpty()) {
                handler.postDelayed({
                    if (screenOn)
                        forceToggleMode(this.lastModePackage)
                }, 5000)
            }
        }
        if (screenOn == true)
            systemScene.onScreenOn()
    }

    private var keepShellAsync2: KeepShellAsync = KeepShellAsync(context)

    /**
     * 显示消息
     */
    private fun showMsg(msg: String) {
        screenHandler.post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 显示模式切换通知
     */
    private fun showModeToggleMsg(packageName: String, modeName: String) {
        if (debugMode)
            showMsg("$modeName \n$packageName")
    }

    /**
     * 更新通知
     */
    private fun updateModeNofity() {
        notifyHelper.notify()
    }

    //#region 模式切换
    //强制执行模式切换，无论当前应用是什么模式是什么
    private fun forceToggleMode(packageName: String?) {
        if (packageName == null || packageName.isNullOrEmpty())
            return
        val mode = spfPowercfg.getString(packageName, firstMode)
        when (mode) {
            IGONED -> return
            else -> {
                toggleConfig(mode)
                showModeToggleMsg(packageName, getModName(mode))
                lastModePackage = packageName
                updateModeNofity()
            }
        }
    }

    //自动切换模式
    private fun autoToggleMode(packageName: String?) {
        if (!dyamicCore || packageName == null || packageName == lastModePackage)
            return

        val mode = spfPowercfg.getString(packageName, firstMode)
        when (mode) {
            IGONED -> return
            else -> {
                if (lastMode != mode) {
                    toggleConfig(mode)
                    showModeToggleMsg(packageName, getModName(mode))
                }
                lastModePackage = packageName
            }
        }
        setCurrentPowercfgApp(packageName)
        updateModeNofity()
    }

    private fun toggleConfig(mode: String) {
        if (!screenOn) {
            executePowercfgMode(POWERSAVE)
        }
        if (!File(Consts.POWER_CFG_PATH).exists()) {
            ConfigInstaller().installPowerConfig(context, "")
        }
        executePowercfgMode(mode)
        lastMode = mode
    }
    //#endregion

    private fun dumpSuccess(packageName: String) {
        autoToggleMode(packageName)
        sceneMode.onFocusdAppChange(packageName)
        lastPackage = packageName
    }

    //焦点应用改变
    fun onFocusAppChanged(packageName: String) {
        if (screenOn) startTimer()
        if (lastPackage == packageName || ignoredList.contains(packageName)) return
        if (lastPackage == null) lastPackage = "com.android.systemui"

        if (accuSwitch)
            Thread(Runnable {
                if (dumpTopAppliction.dumpsysTopActivity(packageName) == packageName)
                    dumpSuccess(packageName)
            }).start()
        else
            dumpSuccess(packageName)
    }

    fun onKeyDown(): Boolean {
        return sceneMode.onKeyDown()
    }

    fun onInterrupt() {
        sceneMode.clearState()
        notifyHelper.hideNotify()
        ReciverLock.unRegister(context)
        densityKeepShell()
        keepShellAsync2.tryExit()
        stopTimer()
    }

    init {
        notifyHelper.notify()

        val windowManager = this.context.getSystemService(WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        this.screenOn = display.state == Display.STATE_ON

        // 监听锁屏状态变化
        ReciverLock.autoRegister(context, screenHandler)
        // 禁用SeLinux
        if (spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, true))
            keepShellAsync2.doCmd(Consts.DisableSELinux)

        Thread(Runnable {
            if (!RootFile.fileExists(Consts.POWER_CFG_PATH)) {
                if (Platform().dynamicSupport(context)) {
                    ConfigInstaller().installPowerConfig(context, Consts.ExecuteConfig, false)
                    dyamicCore = true
                } else {
                    dyamicCore = false
                }
            } else {
                dyamicCore = true
                ConfigInstaller().configCodeVerify(context)
                keepShellAsync2.doCmd(Consts.ExecuteConfig)
            }
            // 添加输入法到忽略列表
            ignoredList.addAll(InputHelper(context).getInputMethods())
            // 启动完成后初始化模式状态
            Thread.sleep(5 * 1000)
            if (dyamicCore && lastMode.isEmpty()) {
                if (!this.screenOn)
                    toggleConfig(POWERSAVE)
                else {
                    startTimer()
                    toggleConfig(DEFAULT)
                }
            } else
                toggleConfig(lastMode)
        }).start()
    }
}
