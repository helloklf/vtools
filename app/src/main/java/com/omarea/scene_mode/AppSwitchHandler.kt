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
import com.omarea.data_collection.EventBus
import com.omarea.data_collection.EventReceiver
import com.omarea.data_collection.EventTypes
import com.omarea.data_collection.GlobalStatus
import com.omarea.store.SceneConfigStore
import com.omarea.store.SpfConfig
import com.omarea.utils.CommonCmds
import com.omarea.vtools.R
import java.util.*
import kotlin.collections.ArrayList

/**
 *
 * Created by helloklf on 2016/10/1.
 */
class AppSwitchHandler(private var context: AccessibilityService) : ModeSwitcher(), EventReceiver {
    private var systemScene = SystemScene(context)
    private var lastPackage: String? = null
    private var lastModePackage: String? = "com.system.ui"
    private var lastMode = ""
    private var spfPowercfg = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
    private var sceneBlackList = context.getSharedPreferences(SpfConfig.SCENE_BLACK_LIST, Context.MODE_PRIVATE)
    private var spfGlobal = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
    private var ignoredList = ArrayList<String>()
    private var dyamicCore = false
    private var firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, BALANCE)
    private var batteryMonitro = spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_BATTERY_MONITORY, false)
    private var screenOn = false
    private var lastScreenOnOff: Long = 0
    //屏幕关闭后切换网络延迟（ms）
    private val SCREEN_OFF_SWITCH_NETWORK_DELAY: Long = 25000
    private var handler = Handler(Looper.getMainLooper())
    private var notifyHelper = AlwaysNotification(context, spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_NOTIFY, true))
    private val sceneMode = SceneMode.getInstanceOrInit(context, SceneConfigStore(context))!!
    private var timer: Timer? = null
    private var sceneConfigChanged: BroadcastReceiver? = null
    private var sceneAppChanged: BroadcastReceiver? = null
    private var screenState = ScreenState(context)
    private var configInstaller = CpuConfigInstaller()

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
            timer = Timer(true).apply {
                val interval = if (batteryMonitro) 2 else 6
                scheduleAtFixedRate(object : TimerTask() {
                    private var ticks = 0
                    override fun run() {
                        updateModeNofity(true) // 耗电统计 定时更新通知显示

                        ticks += interval
                        ticks %= 60
                        if (ticks == 0) {
                            sceneMode.clearFreezeAppTimeLimit()
                        }
                    }
                }, 0, interval * 1000L)
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
                    updateModeNofity() //
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
        updateModeNofity() // 屏幕点亮后更新通知

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
    private fun updateModeNofity(saveLog:Boolean = false) {
        if (screenOn) {
            notifyHelper.notify(saveLog)
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
                updateModeNofity() // 模式切换后更新通知
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
        if (configInstaller.configInstalled() || CpuConfigInstaller().installOfficialConfig(context, "")) {
            if (screenOn) {
                executePowercfgMode(context, mode)
            } else {
                executePowercfgMode(context, POWERSAVE)
                updateModeNofity() //
            }
            lastMode = mode
        } else {
            dyamicCore = false
            spfGlobal.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, false).apply()
            Toast.makeText(context, context.getString(R.string.dynamic_auto_disabled), Toast.LENGTH_LONG).show()
        }
    }
    //#endregion

    override fun onReceive(eventType: EventTypes) {
        when (eventType) {
            EventTypes.APP_SWITCH ->
                onFocusAppChanged(GlobalStatus.lastPackageName)
            EventTypes.SCREEN_ON -> {
                // if (!screenOn && screenState.isScreenOn()) {
                    _onScreenOn()
                // }
            }
            EventTypes.SCREEN_OFF -> {
                if (ScreenState(context).isScreenLocked()) {
                    _onScreenOff()
                }
            }
            else -> return
        }
    }

    override fun eventFilter(eventType: EventTypes): Boolean {
        return when (eventType) {
            EventTypes.APP_SWITCH -> true
            else -> false
        }
    }

    /**
     * 焦点应用改变
     */
    fun onFocusAppChanged(packageName: String) {
        if (!screenOn && screenState.isScreenOn()) {
            _onScreenOn() // 如果切换应用时发现屏幕出于开启状态 而记录的状态是关闭，通知开启
        }

        if (lastPackage == packageName || ignoredList.contains(packageName) || sceneBlackList.contains(packageName)) return
        if (lastPackage == null) lastPackage = "com.android.systemui"

        autoToggleMode(packageName)
        sceneMode.onAppEnter(packageName)
        lastPackage = packageName
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
        EventBus.unsubscibe(this)
    }

    @SuppressLint("ApplySharedPref")
    private fun initConfig() {
        ignoredList.clear()
        // 添加强制忽略列表
        ignoredList.addAll(context.resources.getStringArray(R.array.powercfg_force_igoned))
        // 添加输入法到忽略列表
        ignoredList.addAll(InputMethodHelper(context).getInputMethods())

        if (spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)) {
            // 是否已经完成性能调节配置安装或自定义
            if (configInstaller.configInstalled() || allModeReplaced(context)) {
                dyamicCore = true
                CpuConfigInstaller().configCodeVerify()
                KeepShellPublic.doCmdSync(CommonCmds.ExecuteConfig)
            } else {
                dyamicCore = false
            }
            spfGlobal.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG, "").commit()
        } else {
            dyamicCore = false
        }
    }

    init {
        screenState = ScreenState(context)

        updateModeNofity() // 服务启动后 更新通知

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
                Toast.makeText(context, "性能调节配置参数已更新，将在下次切换应用时生效！", Toast.LENGTH_SHORT).show()
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
