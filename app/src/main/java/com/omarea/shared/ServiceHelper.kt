package com.omarea.shared

import android.accessibilityservice.AccessibilityService
import android.content.*
import android.content.Context.WINDOW_SERVICE
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
import com.omarea.vtools.R
import java.io.File
import java.util.*
import android.content.Context.KEYGUARD_SERVICE
import android.app.KeyguardManager




/**
 *
 * Created by helloklf on 2016/10/1.
 */
class ServiceHelper(private var context: AccessibilityService) : ModeList(context) {
    private var systemScene = SystemScene(context)
    private var lastPackage: String? = null
    private var lastModePackage: String? = "com.system.ui"
    private var lastMode = ""
    private var spfPowercfg = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
    private var spfGlobal = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
    private var dumpTopAppliction = DumpTopAppliction()
    private var ignoredList = arrayListOf(
            "com.miui.securitycenter",
            "android",
            "com.android.systemui",
            "com.omarea.vtools",
            "com.miui.touchassistant",
            "com.miui.contentextension",
            "com.miui.systemAdSolution")
    private var dyamicCore = false
    private var firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, BALANCE)
    private var accuSwitch = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_ACCU_SWITCH, false)
    private var batteryMonitro = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, false)
    private var screenOn = false
    private var lastScreenOnOff: Long = 0
    //屏幕关闭后切换网络延迟（ms）
    private val SCREEN_OFF_SWITCH_NETWORK_DELAY: Long = 30000
    private var screenHandler = ScreenEventHandler({ onScreenOff() }, { onScreenOn() })
    private var handler = Handler(Looper.getMainLooper())
    private var notifyHelper = NotifyHelper(context, spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, true))
    private val sceneMode = SceneMode.getInstanceOrInit(context.contentResolver, AppConfigStore(context))!!
    private var timer: Timer? = null
    private var sceneConfigChanged:BroadcastReceiver? = null
    private var sceneAppChanged:BroadcastReceiver? = null

    /**
     * 更新设置
     */
    private fun updateConfig() {
        firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, BALANCE)
        accuSwitch = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_ACCU_SWITCH, false)
        batteryMonitro = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, false)
        val windowManager = this.context.getSystemService(WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        screenOn = display.state == Display.STATE_ON
        dyamicCore = RootFile.fileExists(Consts.POWER_CFG_PATH)
        notifyHelper.setNotify(spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, true))
        stopTimer()
        startTimer() // 配置更新后开始定时更新任务
    }

    /**
     * 判断是否黑屏
     * @param c
     * @return
     */
    fun isScreenLocked(): Boolean {
        val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        if (display.state == Display.STATE_ON) {
            return false
        }

        val mKeyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return mKeyguardManager.inKeyguardRestrictedInputMode()
    }

    private fun startTimer() {
        if (!screenOn) {
            return
        }
        if (timer == null) {
            this.screenOn = !isScreenLocked()
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
        lastScreenOnOff = System.currentTimeMillis()
        if (screenOn == true) return

        screenOn = true
        startTimer() // 屏幕开启后开始定时更新通知
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
     * 更新通知
     */
    private fun updateModeNofity() {
        if (screenOn) {
            notifyHelper.notify()
        }
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
        startTimer() // 前台应用改变后开始定时更新通知
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
        if (sceneConfigChanged != null) {
            context.unregisterReceiver(sceneConfigChanged)
            sceneConfigChanged = null
        }
        if (sceneAppChanged != null) {
            context.unregisterReceiver(sceneAppChanged)
            sceneAppChanged = null
        }
    }

    init {
        notifyHelper.notify()

        this.screenOn = !isScreenLocked()

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
                    startTimer() // 服务启动后开启定时更新通知任务
                    toggleConfig(DEFAULT)
                }
            } else
                toggleConfig(lastMode)
        }).start()

        sceneConfigChanged = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateConfig()
                Toast.makeText(context, "动态响应配置参数已更新，将在下次切换应用时生效！", Toast.LENGTH_SHORT).show()
            }
        }
        sceneAppChanged = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.extras != null) {
                    if (intent.extras.containsKey("mode")) {
                        lastMode = intent.getStringExtra("mode")
                    }
                    if (intent.extras.containsKey("app")) {
                        //
                    }
                }
            }
        }
        context.registerReceiver(sceneConfigChanged, IntentFilter(context.getString(R.string.scene_change_action)))
        context.registerReceiver(sceneAppChanged, IntentFilter(context.getString(R.string.scene_appchange_action)))
    }
}
