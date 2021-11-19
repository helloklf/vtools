package com.omarea.scene_mode

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.omarea.Scene
import com.omarea.common.shell.KeepShellPublic
import com.omarea.data.EventBus
import com.omarea.data.EventType
import com.omarea.data.GlobalStatus
import com.omarea.data.IEventReceiver
import com.omarea.library.basic.InputMethodApp
import com.omarea.library.basic.ScreenState
import com.omarea.store.SceneConfigStore
import com.omarea.store.SpfConfig
import com.omarea.utils.CommonCmds
import com.omarea.vtools.AccessibilityScenceMode
import com.omarea.vtools.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

/**
 *
 * Created by helloklf on 2016/10/1.
 */
class AppSwitchHandler(private var context: AccessibilityScenceMode, override val isAsync: Boolean = false) : ModeSwitcher(), IEventReceiver {
    private var lastPackage: String? = null
    private var lastModePackage: String? = "com.system.ui"
    private var lastMode = ""
    private var spfPowercfg = context.getSharedPreferences(SpfConfig.POWER_CONFIG_SPF, Context.MODE_PRIVATE)
    private var sceneBlackList = context.getSharedPreferences(SpfConfig.SCENE_BLACK_LIST, Context.MODE_PRIVATE)
    private val spfGlobal: SharedPreferences
        get() {
            return Scene.globalConfig
        }
    private var ignoredList = ArrayList<String>()
    private val dynamicCore: Boolean
        get() {
            return spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)
        }
    private var firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, BALANCE)
    private var screenOn = false
    private var lastScreenOnOff: Long = 0

    //屏幕关闭后切换网络延迟（ms）
    private val SCREEN_OFF_SWITCH_NETWORK_DELAY: Long = 25000
    private var handler = Handler(Looper.getMainLooper())
    private var notifyHelper = AlwaysNotification(context, true)
    private val sceneMode = SceneMode.getNewInstance(context, SceneConfigStore(context))!!
    private var timer: Timer? = null
    private var sceneAppChanged: BroadcastReceiver? = null
    private var screenState = ScreenState(context)

    /**
     * 更新设置
     */
    private fun updateConfig() {
        clearInitedState()
        lastMode = ""
        firstMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_FIRST_MODE, BALANCE)

        initConfig()
        notifyHelper.setNotify(true)
        stopTimer()
        startTimer()
    }

    private fun startTimer() {
        if (timer == null && screenOn && screenState.isScreenOn()) {
            if (screenOn) {
                timer = Timer(true).apply {
                    val interval = 6
                    scheduleAtFixedRate(object : TimerTask() {
                        private var ticks = 0
                        override fun run() {
                            updateModeNoitfy(true) // 耗电统计 定时更新通知显示

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
        if (!screenOn)
            return

        screenOn = false
        lastScreenOnOff = System.currentTimeMillis()
        sceneMode.onScreenOff()

        handler.postDelayed({
            onScreenOffCloseNetwork()
        }, SCREEN_OFF_SWITCH_NETWORK_DELAY + 1000)

        handler.postDelayed({
            if (!screenOn) {
                notifyHelper.hideNotify()
                stopTimer()
                setDelayFreezeApps()

                // 息屏后自动切换为省电模式
                if (dynamicCore && lastMode.isNotEmpty()) {
                    val sleepMode = spfGlobal.getString(SpfConfig.GLOBAL_SPF_POWERCFG_SLEEP_MODE, POWERSAVE)
                    if (sleepMode != null && sleepMode != IGONED) {
                        toggleConfig(sleepMode, context.packageName)
                    }
                }
            }
        }, 10000)
    }

    /**
     * 屏幕关闭后 - 关闭网络
     */
    private fun onScreenOffCloseNetwork() {
        if (!screenOn) {
            if (System.currentTimeMillis() - lastScreenOnOff >= SCREEN_OFF_SWITCH_NETWORK_DELAY) {
                sceneMode.onScreenOffDelay()
                System.gc()
            }
        }
    }

    /**
     * 点亮屏幕且解锁后执行
     */
    private fun onScreenOn() {
        lastScreenOnOff = System.currentTimeMillis()

        handler.postDelayed({
            if (dynamicCore && lastMode.isNotEmpty()) {
                lastPackage = null
                lastModePackage = null
                EventBus.publish(EventType.STATE_RESUME)
                // toggleConfig(lastMode, context.packageName)
                sceneMode.cancelFreezeAppThread()
            }
        }, 1000)
        sceneMode.onScreenOn()

        if (!screenOn) {
            screenOn = true
            startTimer() // 屏幕开启后开始定时更新通知
            updateModeNoitfy() // 屏幕点亮后更新通知
        }
    }

    private fun setDelayFreezeApps() {
        val delay = spfGlobal.getInt(SpfConfig.GLOBAL_SPF_FREEZE_DELAY, 0)
        SceneMode.FreezeAppThread(context.applicationContext, true, if (delay > 0) (delay * 60) else 0).start()
    }

    /**
     * 更新通知
     */
    private fun updateModeNoitfy(saveLog: Boolean = false) {
        if (screenOn) {
            notifyHelper.notify(saveLog)
        }
    }

    //自动切换模式
    private fun autoToggleMode(packageName: String?) {
        if (packageName != null && packageName != lastModePackage) {
            lastModePackage = packageName
            if (dynamicCore) {
                val mode = spfPowercfg.getString(packageName, firstMode)!!
                if (
                        mode != IGONED && (lastMode != mode || spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_STRICT, false))
                ) {
                    if (spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DELAY, false)) {
                        delayToggleConfig(mode, packageName)
                    } else {
                        toggleConfig(mode, packageName)
                    }
                }
            }
            setCurrentPowercfgApp(packageName)
            updateModeNoitfy() // 应用改变后更新通知
        }
    }

    private fun toggleConfig(mode: String, packageName: String) {
        lastMode = mode
        executePowercfgMode(mode, packageName)
    }

    private fun delayToggleConfig(mode: String, packageName: String) {
        handler.postDelayed({
            if (lastMode == mode) {
                executePowercfgMode(mode, packageName)
            }
        }, 5000)
        lastMode = mode
    }
    //#endregion

    override fun onReceive(eventType: EventType, data: HashMap<String, Any>?) {
        when (eventType) {
            EventType.APP_SWITCH ->
                onFocusedAppChanged(GlobalStatus.lastPackageName)
            EventType.SCREEN_ON -> {
                onScreenOn()
            }
            EventType.SCREEN_OFF -> {
                if (ScreenState(context).isScreenLocked()) {
                    onScreenOff()
                }
            }
            EventType.SCENE_CONFIG -> {
                updateConfig()
                Scene.toast("性能调节配置参数已更新，将在下次切换应用时生效！", Toast.LENGTH_SHORT)
            }
            EventType.SCENE_APP_CONFIG -> {
                data?.run {
                    if (containsKey("app")) {
                        if (dynamicCore && screenOn && containsKey("mode")) {
                            val mode = get("mode")?.toString()
                            val app = get("app")?.toString()
                            if (mode != null && app != null && app == lastModePackage) {
                                toggleConfig(mode, app)
                            }
                        }
                        sceneMode.updateAppConfig()
                    }
                }
            }
            else -> return
        }
    }

    override fun eventFilter(eventType: EventType): Boolean {
        return when (eventType) {
            EventType.APP_SWITCH, EventType.SCREEN_OFF, EventType.SCREEN_ON, EventType.SCENE_CONFIG, EventType.SCENE_APP_CONFIG -> true
            else -> false
        }
    }

    override fun onSubscribe() {

    }

    override fun onUnsubscribe() {
        sceneMode.clearState()
        notifyHelper.hideNotify()
        stopTimer()
        if (sceneAppChanged != null) {
            context.unregisterReceiver(sceneAppChanged)
            sceneAppChanged = null
        }
        EventBus.unsubscribe(notifyHelper)
        EventBus.unsubscribe(this)
    }

    /**
     * 焦点应用改变
     */
    private fun onFocusedAppChanged(packageName: String) {
        if (!screenOn && screenState.isScreenOn()) {
            onScreenOn() // 如果切换应用时发现屏幕出于开启状态 而记录的状态是关闭，通知开启
        }

        if (lastPackage == packageName || ignoredList.contains(packageName) || sceneBlackList.contains(packageName)) return
        if (lastPackage == null) lastPackage = "com.android.systemui"

        autoToggleMode(packageName)
        sceneMode.onAppEnter(packageName)
        lastPackage = packageName
    }

    @SuppressLint("ApplySharedPref")
    private fun initConfig() {
        ignoredList.clear()
        // 添加强制忽略列表
        ignoredList.addAll(context.resources.getStringArray(R.array.powercfg_force_igoned))
        // 添加输入法到忽略列表
        ignoredList.addAll(InputMethodApp(context).getInputMethods())

        if (spfPowercfg.all.isEmpty()) {
            for (item in context.resources.getStringArray(R.array.powercfg_igoned)) {
                spfPowercfg.edit().putString(item, IGONED).apply()
            }
            for (item in context.resources.getStringArray(R.array.powercfg_fast)) {
                spfPowercfg.edit().putString(item, FAST).apply()
            }
            for (item in context.resources.getStringArray(R.array.powercfg_game)) {
                spfPowercfg.edit().putString(item, PERFORMANCE).apply()
            }
            for (item in context.resources.getStringArray(R.array.powercfg_powersave)) {
                spfPowercfg.edit().putString(item, POWERSAVE).apply()
            }
        }

        if (spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL_DEFAULT)) {
            // 是否已经完成性能调节配置安装或自定义
            if (modeConfigCompleted()) {
                val installer = CpuConfigInstaller()
                if (installer.outsideConfigInstalled()) {
                    installer.configCodeVerify()
                }
                initPowerCfg()
            } else {
                spfGlobal.edit().putBoolean(SpfConfig.GLOBAL_SPF_DYNAMIC_CONTROL, false).apply()
            }
            spfGlobal.edit().putString(SpfConfig.GLOBAL_SPF_POWERCFG, "").commit()
        }
    }

    init {
        screenState = ScreenState(context)

        updateModeNoitfy() // 服务启动后 更新通知

        // 禁用SeLinux
        if (spfGlobal.getBoolean(SpfConfig.GLOBAL_SPF_DISABLE_ENFORCE, false)) {
            KeepShellPublic.doCmdSync(CommonCmds.DisableSELinux)
        }

        GlobalScope.launch(Dispatchers.IO) {
            initConfig()
        }

        EventBus.subscribe(notifyHelper)
        EventBus.subscribe(this)
    }
}
