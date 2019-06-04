package com.omarea.shared

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.omarea.common.shell.KeepShellAsync
import com.omarea.shared.helper.*
import com.omarea.shell.DumpTopAppliction
import com.omarea.vtools.R
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList


/**
 *
 * Created by helloklf on 2016/10/1.
 */
class ServiceHelper(private var context: AccessibilityService) : ModeList() {
    private var systemScene = SystemScene(context)
    private var lastPackage: String? = null
    private var lastModePackage: String? = "com.system.ui"
    private var lastMode = ""
    private var spfPowercfg = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
    private var spfGlobal = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
    private var dumpTopAppliction = DumpTopAppliction()
    private var ignoredList = ArrayList<String>()
    private var dyamicCore = false
    private var lockMode = false
    private var firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, BALANCE)
    private var accuSwitch = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_ACCU_SWITCH, false)
    private var batteryMonitro = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, false)
    private var screenOn = false
    private var lastScreenOnOff: Long = 0
    //屏幕关闭后切换网络延迟（ms）
    private val SCREEN_OFF_SWITCH_NETWORK_DELAY: Long = 25000
    private var screenHandler = ScreenEventHandler({ onScreenOff() }, { onScreenOn() })
    private var handler = Handler(Looper.getMainLooper())
    private var notifyHelper = NotifyHelper(context, spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, true))
    private val sceneMode = SceneMode.getInstanceOrInit(context.contentResolver, AppConfigStore(context))!!
    private var timer: Timer? = null
    private var sceneConfigChanged: BroadcastReceiver? = null
    private var sceneAppChanged: BroadcastReceiver? = null
    private var screenState = ScreenState(context)
    private var configInstaller = ConfigInstaller()

    /**
     * 更新设置
     */
    private fun updateConfig() {
        lastMode = ""
        firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, BALANCE)
        accuSwitch = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_ACCU_SWITCH, false)
        batteryMonitro = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, false)
        lockMode = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_LOCK_MODE, true)

        initConfig()
        notifyHelper.setNotify(spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, true))
        stopTimer()
        if (screenState.isScreenOn()) {
            startTimer() // 如果屏幕出于开启状态 启动定时器
        }
    }


    private fun startTimer() {
        if (timer == null && screenOn) {
            var ticks = 0
            timer = Timer(true)
            if (batteryMonitro) {
                timer!!.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        notifyHelper.notify()
                        ticks %= 30
                        if (ticks == 0) {
                            SceneMode.clearFreezeAppTimeLimit()
                        }
                    }
                }, 0, 2000L)
            } else {
                timer!!.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        notifyHelper.notify()
                        ticks %= 6
                        if (ticks == 0) {
                            SceneMode.clearFreezeAppTimeLimit()
                        }
                    }
                }, 0, 10000L)
            }
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
        lastScreenOnOff = System.currentTimeMillis()

        screenHandler.postDelayed({
            onScreenOffCloseNetwork()
        }, SCREEN_OFF_SWITCH_NETWORK_DELAY + 1000)
        // TODO: 关闭屏幕后清理后台
        screenHandler.postDelayed({
            if (!screenOn)
                stopTimer()
            notifyHelper.hideNotify()
        }, 10000)
    }

    /**
     * 屏幕关闭后 - 关闭网络
     */
    private fun onScreenOffCloseNetwork() {
        if (!screenOn) {
            if (dyamicCore && !screenOn) {
                if (lockMode) {
                    updateModeNofity()
                    toggleConfig(POWERSAVE)
                }
            }
            if (System.currentTimeMillis() - lastScreenOnOff >= SCREEN_OFF_SWITCH_NETWORK_DELAY) {
                sceneMode.onScreenOff()
                systemScene.onScreenOff()
                System.gc()
            }
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

        if (dyamicCore && lockMode && this.lastModePackage.isNullOrEmpty()) {
            handler.postDelayed({
                if (screenOn)
                    forceToggleMode(this.lastModePackage)
            }, 2000)
        }
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
        if (packageName == null || packageName == lastModePackage)
            return
        if (!dyamicCore) {
            setCurrentPowercfgApp(packageName)
            updateModeNofity()
            return
        }

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
        if (!configInstaller.configInstalled()) {
            ConfigInstaller().installPowerConfig(context, "")
        }
        if (screenOn) {
            executePowercfgMode(mode)
        } else {
            executePowercfgMode(POWERSAVE)
        }
        lastMode = mode
    }
    //#endregion

    private fun dumpSuccess(packageName: String) {
        autoToggleMode(packageName)
        sceneMode.onAppEnter(packageName)
        lastPackage = packageName
    }

    /**
     * 焦点应用改变
     */
    fun onFocusAppChanged(packageName: String) {
        if (!screenOn && screenState.isScreenOn()) {
            onScreenOn() // 如果切换应用时发现屏幕出于开启状态 而记录的状态是关闭，通知开启
        }

        if (lastPackage == packageName || ignoredList.contains(packageName)) return
        if (lastPackage == null) lastPackage = "com.android.systemui"

        if (accuSwitch)
            DumpTopApplictionThread(dumpTopAppliction, packageName, this).start()
        else
            dumpSuccess(packageName)
    }

    /**
     * 获取上层应用
     */
    private class DumpTopApplictionThread(
            dumpTopAppliction: DumpTopAppliction,
            packageName: String,
            serviceHelper: ServiceHelper
    ) : Thread() {
        private var dumpTopAppliction: WeakReference<DumpTopAppliction>
        private var packageName: WeakReference<String>
        private var serviceHelper: WeakReference<ServiceHelper>
        override fun run() {
            val packageName = this.packageName.get()!!
            if (dumpTopAppliction.get()!!.fromDumpsysWindow(packageName) == packageName) {
                val serviceHelper = this.serviceHelper.get()
                if (serviceHelper != null) {
                    serviceHelper.dumpSuccess(packageName)
                }
            }
        }

        init {
            this.dumpTopAppliction = WeakReference(dumpTopAppliction)
            this.packageName = WeakReference(packageName)
            this.serviceHelper = WeakReference(serviceHelper)
        }
    }

    fun onKeyDown(): Boolean {
        return sceneMode.onKeyDown()
    }

    fun onInterrupt() {
        sceneMode.clearState()
        notifyHelper.hideNotify()
        ReciverLock.unRegister(context)
        destroyKeepShell()
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

    @SuppressLint("ApplySharedPref")
    private fun initConfig() {
        ignoredList.clear()
        // 添加强制忽略列表
        ignoredList.addAll(context.resources.getStringArray(R.array.powercfg_force_igoned))
        // 添加输入法到忽略列表
        ignoredList.addAll(InputHelper(context).getInputMethods())

        if (spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, true)) {
            if (configInstaller.configInstalled()) {
                dyamicCore = true
                ConfigInstaller().configCodeVerify()
                keepShellAsync2.doCmd(CommonCmds.ExecuteConfig)
            } else {
                if (configInstaller.dynamicSupport(context)) {
                    ConfigInstaller().installPowerConfig(context, CommonCmds.ExecuteConfig, false)
                    dyamicCore = true
                } else {
                    dyamicCore = false
                }
            }
            spfGlobal.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG, "").commit()
        } else {
            dyamicCore = false
        }
    }

    init {
        screenState = ScreenState(context)

        notifyHelper.notify()

        // 监听锁屏状态变化
        ReciverLock.autoRegister(context, screenHandler)
        // 禁用SeLinux
        if (spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)) {
            keepShellAsync2.doCmd(CommonCmds.DisableSELinux)
        }

        Thread(Runnable {
            initConfig()
            // 启动完成后初始化模式状态
            Thread.sleep(5 * 1000)
        }).start()

        sceneConfigChanged = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateConfig()
                Toast.makeText(context, "动态响应配置参数已更新，将在下次切换应用时生效！", Toast.LENGTH_SHORT).show()
            }
        }

        sceneAppChanged = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val extras = intent.extras
                if (extras != null && extras.containsKey("app")) {
                    if (extras.containsKey("brightnessMode")) {
                        val mode = intent.getStringExtra("brightnessMode")
                        val app = intent.getStringExtra("app")
                        if (dyamicCore && screenOn && app == lastModePackage) {
                            toggleConfig(mode)
                        }
                    }
                    sceneMode.updateAppConfig()
                }
            }
        }

        context.registerReceiver(sceneConfigChanged, IntentFilter(context.getString(R.string.scene_change_action)))
        context.registerReceiver(sceneAppChanged, IntentFilter(context.getString(R.string.scene_appchange_action)))
    }
}
