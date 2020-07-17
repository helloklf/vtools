package com.omarea.charger_booster

import android.content.Context
import android.content.SharedPreferences
import android.os.BatteryManager
import android.util.Log
import android.widget.Toast
import com.omarea.Scene
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellAsync
import com.omarea.data_collection.EventReceiver
import com.omarea.data_collection.EventType
import com.omarea.data_collection.GlobalStatus
import com.omarea.shell_utils.BatteryUtils
import com.omarea.shell_utils.PropsUtils
import com.omarea.store.SpfConfig
import com.omarea.utils.GetUpTime
import java.util.*

class BatteryReceiver(private var service: Context) : EventReceiver {
    override fun eventFilter(eventType: EventType): Boolean {
        return when (eventType) {
            EventType.BATTERY_CAPACITY_CHANGED, // 电量百分比变化
            EventType.BATTERY_CHANGED,              // 这个执行频率可能有点太高了，耗电
            EventType.BATTERY_LOW,
            EventType.POWER_CONNECTED,
            EventType.POWER_DISCONNECTED -> true
            else -> false
        }
    }

    // 是否启用了充电保护
    val bpAllowed: Boolean
        get() {
            return chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_BP, false)
        }

    // 充电保护电量百分比
    val bpLevel: Int
        get() {
            return chargeConfig.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, SpfConfig.CHARGE_SPF_BP_LEVEL_DEFAULT)
        }

    // 是否正在充电
    val onCharge: Boolean
        get() {
            return GlobalStatus.batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING
        }

    // 是否应该在电池保护状态
    val shouldBP: Boolean
        get() {
            return bpAllowed && (GlobalStatus.batteryCapacity >= bpLevel || (chargeDisabled && GlobalStatus.batteryCapacity > bpLevel - 20))
        }

    override fun onReceive(eventType: EventType) {
        if (GlobalStatus.batteryCapacity < 0) {
            return
        }

        try {
            // 充电保护
            if (shouldBP != chargeDisabled) {
                if (chargeDisabled) {
                    // 恢复充电
                    resumeCharge()
                } else {
                    // 禁止充电
                    disableCharge()
                    return
                }
            }

            if (onCharge) {
                // 夜间慢速充电
                val isSleepTime = sleepChargeMode(GlobalStatus.batteryCapacity, if (bpAllowed) bpLevel else 100, qcLimit)

                // 充电加速
                if (!isSleepTime && chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false)) {
                    autoChangeLimitValue(eventType)
                }
            }
        } catch (ex: Exception) {
        }
    }

    private var chargeDisabled: Boolean = PropsUtils.getProp("vtools.bp").equals("1")
    private var keepShellAsync: KeepShellAsync? = null

    private var chargeConfig: SharedPreferences

    // 电池总容量（mAh）
    private val batteryCapacity = BatteryInfo().getBatteryCapacity(service)

    private var batteryUnits = BatteryUtils()
    private var ResumeCharge = "sh " + FileWrite.writePrivateShellFile("addin/resume_charge.sh", "addin/resume_charge.sh", service)
    private var DisableCharge = "sh " + FileWrite.writePrivateShellFile("addin/disable_charge.sh", "addin/disable_charge.sh", service)

    // 起床时间
    val getUpTime: Int
        get() {
            return chargeConfig.getInt(SpfConfig.CHARGE_SPF_TIME_GET_UP, SpfConfig.CHARGE_SPF_TIME_GET_UP_DEFAULT)
        }

    // 去睡觉的时间
    val goToBedTime: Int
        get() {
            return chargeConfig.getInt(SpfConfig.CHARGE_SPF_TIME_SLEEP, SpfConfig.CHARGE_SPF_TIME_SLEEP_DEFAULT)
        }

    // 现在时间
    val currentTime: Int
        get() {
            val now = Calendar.getInstance()
            return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        }

    val qcLimit: Int
        get() {
            return chargeConfig.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, SpfConfig.CHARGE_SPF_QC_LIMIT_DEFAULT)
        }

    private var lowSpeedMedium = 1000 // 进入慢速阶段的充电限制速度
    private var lowSpeedHigh = 500 // 进入慢速阶段的充电限制速度
    private var lowSpeedExtreme = 100 // 进入慢速阶段的充电限制速度（充电速度控制精确度有限，为了避免控制器精准度和手机自耗电抖动导致的电池冲放循环，最小值不太可能设为 0）

    private var lastLimitValue = -1

    // 判断是否在夜间慢速充电时间
    private fun inSleepTime(): Boolean {
        // 如果开启了夜间充电降速
        if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_NIGHT_MODE, false)) {
            val nowTimeValue = currentTime
            val getUp = getUpTime
            val sleep = goToBedTime

            // 判断是否在夜间慢速充电时间
            val inSleepTime =
                    // 如果【起床时间】比【睡觉时间】要大，如 2:00 睡到 9:00 起床
                    (getUp > sleep && (nowTimeValue >= sleep && nowTimeValue <= getUp)) ||
                            // 正常时间睡觉【睡觉时间】大于【起床时间】，如 23:00 睡到 7:00 起床
                            (getUp < sleep && (nowTimeValue >= sleep || nowTimeValue <= getUp))
            return inSleepTime
        }
        return false
    }

    /**
     * 计算并使用合理的夜间充电速度
     * @param currentCapacityRatio 当前电量百分比（0~100）
     * @param targetRatio 目标充电百分比
     * @param qcLimit 充电速度限制
     */
    private fun sleepChargeMode(currentCapacityRatio: Int, targetRatio: Int, qcLimit: Int): Boolean {
        // 如果开启了夜间充电降速
        if (inSleepTime()) {
            val inSleepTime = inSleepTime()
            val getUp = getUpTime

            // 如果正在夜间慢速充电时间
            if (inSleepTime) {
                if (currentCapacityRatio >= targetRatio) {
                    // 如果已经超出了电池保护的电量，限制为50mA
                    if (lastLimitValue != lowSpeedExtreme) { // 避免重复执行操作
                        lastLimitValue = lowSpeedExtreme
                        batteryUnits.setChargeInputLimit(lastLimitValue, service)
                    }
                } else {
                    // 计算预期还需要充入多少电量（mAh）
                    val target = (targetRatio - currentCapacityRatio) / 100F * batteryCapacity
                    // 距离起床的剩余时间（小时）
                    val timeRemaining = GetUpTime(getUp).minutes / 60F

                    // 合理的充电速度 = 还需充入的电量(mAh) / timeRemaining
                    var limitValue = (target / timeRemaining).toInt()
                    if (limitValue < lowSpeedExtreme) {
                        limitValue = lowSpeedExtreme
                    } else if (limitValue > qcLimit) {
                        limitValue = qcLimit
                    }

                    if (lastLimitValue != limitValue) { // 避免重复执行操作
                        lastLimitValue = limitValue
                        batteryUnits.setChargeInputLimit(limitValue, service)
                    }
                }
                return true
            }
        }
        return false
    }

    init {
        if (keepShellAsync == null) {
            keepShellAsync = KeepShellAsync(service)
        }
        chargeConfig = service.getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
    }

    internal fun onDestroy() {
        this.resumeCharge()
        keepShellAsync?.tryExit()
        keepShellAsync = null
    }

    internal fun disableCharge() {
        Toast.makeText(Scene.context, "充电保护策略已为您暂停充电！", Toast.LENGTH_SHORT).show()
        keepShellAsync?.doCmd(DisableCharge)
        chargeDisabled = true
    }

    internal fun resumeCharge() {
        Toast.makeText(Scene.context, "充电保护策略已为您恢复充电！", Toast.LENGTH_SHORT).show()
        keepShellAsync!!.doCmd(ResumeCharge)
        chargeDisabled = false
    }

    private var lastSetChargeLimit = 0L

    // 根据电量和设置自动调节速度限制
    private fun autoChangeLimitValue(eventType: EventType) {
        // 是否开启动态调速
        val allowDynamicSpeed = chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_NIGHT_MODE, false)

        // 如果开启了动态调速并且快充满了
        if (allowDynamicSpeed && GlobalStatus.batteryCapacity > 80) {
            setChargerLimitToValue(when {
                GlobalStatus.batteryCapacity > 90 -> lowSpeedExtreme
                GlobalStatus.batteryCapacity > 85 -> lowSpeedHigh
                else -> lowSpeedMedium
            }, eventType, true) // 快充满了就限制充电速度为50mA保护电池吧！
        }

        // 又或者只开了充电加速
        else if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false)) {
            setChargerLimitToValue(qcLimit, eventType, false) // 快充满了就限制充电速度为50mA保护电池吧！
        }
    }

    private var governorTimer: Timer? = null
    private fun startGovernorTimer() {
        if (governorTimer == null) {
            Log.d("@Scene", "Start ForceQuickChargeTimer")
            governorTimer = Timer().apply {
                schedule(object : TimerTask() {
                    override fun run() {
                        governorRun()
                    }

                }, 0, 1000)
            }
        }
    }

    private fun stopGovernorTimer() {
        if (governorTimer != null) {
            Log.d("@Scene", "Stop ForceQuickChargeTimer")
            governorTimer?.cancel()
            governorTimer?.purge()
            governorTimer = null
        }
    }

    private fun governorRun() {
        if (chargeConfig.getInt(SpfConfig.CHARGE_SPF_EXEC_MODE, SpfConfig.CHARGE_SPF_EXEC_MODE_DEFAULT) == SpfConfig.CHARGE_SPF_EXEC_MODE_SPEED_FORCE) {
            if (inSleepTime() || chargeDisabled) {
                stopGovernorTimer()
            } else {
                autoChangeLimitValue(EventType.TIMER)
            }
        } else {
            stopGovernorTimer()
        }
    }

    // 限制到指定值
    private fun setChargerLimitToValue(speedMa: Int, eventType: EventType, protectedMode: Boolean) {
        val execMode = chargeConfig.getInt(SpfConfig.CHARGE_SPF_EXEC_MODE, SpfConfig.CHARGE_SPF_EXEC_MODE_DEFAULT)
        try {
            val required = when (execMode) {
                // 如果目标是降低充电速度，则不比频繁尝试调节速度，只需要在电量变化或者插拔充电器时执行即可
                SpfConfig.CHARGE_SPF_EXEC_MODE_SPEED_DOWN -> {
                    if (eventType != EventType.BATTERY_CHANGED) {
                        Log.d("@Scene", "CHARGE_SPF_EXEC_MODE_SPEED_DOWN > " + eventType.name)
                    }
                    eventType == EventType.BATTERY_CAPACITY_CHANGED || eventType == EventType.POWER_CONNECTED || eventType == EventType.POWER_DISCONNECTED
                }
                // 如果是暴力充电加速，则要尽可能高频率的执行速度调节，并且开启定时任务不断执行（除非已经进入动态调速保护阶段）
                SpfConfig.CHARGE_SPF_EXEC_MODE_SPEED_FORCE -> {
                    if (!protectedMode) {
                        if (eventType != EventType.TIMER) {
                            startGovernorTimer()
                        } else {
                            Log.d("@Scene", "Exec ForceQuickChargeTimer")
                        }
                        true
                    } else {
                        stopGovernorTimer()
                        // 如果已经进入充电保护阶段，还是要限制一下执行频率
                        !(protectedMode && (System.currentTimeMillis() - lastSetChargeLimit < 5000))
                    }
                }
                // 如果是常规加速，则在每次电池状态发生辩护时都执行（除非已经进入动态调速保护阶段）
                SpfConfig.CHARGE_SPF_EXEC_MODE_SPEED_UP -> {
                    // 如果已经进入充电保护阶段，还是要限制一下执行频率
                    !(protectedMode && (System.currentTimeMillis() - lastSetChargeLimit < 5000))
                }
                else -> false
            }

            if (required) {
                lastSetChargeLimit = System.currentTimeMillis()
                lastLimitValue = speedMa
                batteryUnits.setChargeInputLimit(lastLimitValue, service)
            }
        } catch (ex: Exception) {
        }
    }
}
