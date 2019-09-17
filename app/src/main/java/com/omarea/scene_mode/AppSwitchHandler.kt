package com.omarea.scene_mode

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.omarea.common.shell.KeepShellPublic
import com.omarea.store.AppConfigStore
import com.omarea.store.SpfConfig
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R
import java.util.*
import kotlin.collections.ArrayList

/**
 *
 * Created by helloklf on 2016/10/1.
 */
class AppSwitchHandler(private var context: AccessibilityService) : ModeSwitcher() {
    private var systemScene = SystemScene(context)
    private var lastPackage: String? = null
    private var lastModePackage: String? = "com.system.ui"
    private var lastMode = ""
    private var spfPowercfg = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
    private var spfGlobal = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
    private var ignoredList = ArrayList<String>()
    private var dyamicCore = false
    private var firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, BALANCE)
    private var batteryMonitro = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, false)
    private var screenOn = false
    private var lastScreenOnOff: Long = 0
    //屏幕关闭后切换网络延迟（ms）
    private val SCREEN_OFF_SWITCH_NETWORK_DELAY: Long = 25000
    private var reciverLock = LockScreenReciver(context, object : IScreenEventHandler {
        override fun onScreenOff() {
            _onScreenOff()
        }

        override fun onScreenOn() {
            _onScreenOn()
        }
    })
    private var handler = Handler(Looper.getMainLooper())
    private var notifyHelper = AlwaysNotification(context, spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, true))
    private val sceneMode = SceneMode.getInstanceOrInit(context.contentResolver, AppConfigStore(context))!!
    private var timer: Timer? = null
    private var sceneConfigChanged: BroadcastReceiver? = null
    private var sceneAppChanged: BroadcastReceiver? = null
    private var screenState = ScreenState(context)
    private var configInstaller = ModeConfigInstaller()

    /**
     * 更新设置
     */
    private fun updateConfig() {
        lastMode = ""
        firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, BALANCE)
        batteryMonitro = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, false)

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
    private fun _onScreenOff() {
        if (screenOn == false)
            return
        screenOn = false
        lastScreenOnOff = System.currentTimeMillis()

        handler.postDelayed({
            onScreenOffCloseNetwork()
        }, SCREEN_OFF_SWITCH_NETWORK_DELAY + 1000)

        handler.postDelayed({
            if (!screenOn) {
                stopTimer()
                notifyHelper.hideNotify()
            }
        }, 10000)
    }

    /**
     * 屏幕关闭后 - 关闭网络
     */
    private fun onScreenOffCloseNetwork() {
        if (!screenOn) {
            if (dyamicCore && !screenOn) {
                if (spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_LOCK_MODE, false)) {
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
    private fun _onScreenOn() {
        lastScreenOnOff = System.currentTimeMillis()
        if (screenOn == true) return

        screenOn = true
        startTimer() // 屏幕开启后开始定时更新通知
        notifyHelper.notify()

        // if (dyamicCore && spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_LOCK_MODE, false) && !this.lastModePackage.isNullOrEmpty()) {
        if (dyamicCore && !this.lastModePackage.isNullOrEmpty()) {
            handler.postDelayed({
                if (screenOn) {
                    forceToggleMode(this.lastModePackage)
                }
            }, 2000)
        }
        systemScene.onScreenOn()
    }

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
        if (packageName != null && packageName != lastModePackage) {
            if (dyamicCore) {
                val mode = spfPowercfg.getString(packageName, firstMode)
                if (mode != IGONED) {
                    if (lastMode != mode) {
                        toggleConfig(mode)
                    }

                    lastModePackage = packageName
                    setCurrentPowercfgApp(packageName)
                    updateModeNofity()
                }
            } else {
                setCurrentPowercfgApp(packageName)
                updateModeNofity()
            }
        }
    }

    private fun toggleConfig(mode: String) {
        if (!configInstaller.configInstalled()) {
            ModeConfigInstaller().installPowerConfig(context, "")
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
            _onScreenOn() // 如果切换应用时发现屏幕出于开启状态 而记录的状态是关闭，通知开启
        }

        if (lastPackage == packageName || ignoredList.contains(packageName)) return
        if (lastPackage == null) lastPackage = "com.android.systemui"

        dumpSuccess(packageName)
    }

    fun isIgnoredApp(packageName: String, isLandscapf: Boolean): Boolean {
        // 排除列表 || 横屏时屏蔽 QQ、微信事件，因为游戏模式下通常会在横屏使用悬浮窗打开QQ 微信
        return ignoredList.contains(packageName) || (isLandscapf && (packageName == "com.tencent.mobileqq" || packageName == "com.tencent.mm"))
    }

    fun onKeyDown(): Boolean {
        return sceneMode.onKeyDown()
    }

    fun onInterrupt() {
        sceneMode.clearState()
        notifyHelper.hideNotify()
        reciverLock.unRegister()
        destroyKeepShell()
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
        ignoredList.addAll(InputMethodHelper(context).getInputMethods())

        if (spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, true)) {
            if (configInstaller.configInstalled()) {
                dyamicCore = true
                ModeConfigInstaller().configCodeVerify()
                KeepShellPublic.doCmdSync(CommonCmds.ExecuteConfig)
            } else {
                if (configInstaller.dynamicSupport(context)) {
                    ModeConfigInstaller().installPowerConfig(context, CommonCmds.ExecuteConfig, false)
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
        reciverLock.autoRegister()
        // 禁用SeLinux
        if (spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)) {
            KeepShellPublic.doCmdSync(CommonCmds.DisableSELinux)
        }

        Thread(Runnable {
            initConfig()
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
                    if (extras.containsKey("mode")) {
                        val mode = intent.getStringExtra("mode")
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
