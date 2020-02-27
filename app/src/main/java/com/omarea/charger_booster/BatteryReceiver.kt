package com.omarea.charger_booster

import android.content.Context
import android.content.SharedPreferences
import android.os.BatteryManager
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShellAsync
import com.omarea.data_collection.EventReceiver
import com.omarea.data_collection.EventType
import com.omarea.data_collection.GlobalStatus
import com.omarea.shell_utils.BatteryUtils
import com.omarea.store.SpfConfig
import com.omarea.utils.GetUpTime
import java.util.*

class BatteryReceiver(private var service: Context) : EventReceiver {
    override fun eventFilter(eventType: EventType): Boolean {
        return when (eventType) {
            // EventType.BATTERY_CAPACITY_CHANGED, // 电量百分比变化
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
                if (!isSleepTime) {
                    setChargerLimit()
                }
            }
        } catch (ex: Exception) {
        }
    }

    private var chargeDisabled: Boolean = false
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

    /**
     * 计算并使用合理的夜间充电速度
     * @param currentCapacityRatio 当前电量百分比（0~100）
     * @param targetRatio 目标充电百分比
     * @param qcLimit 充电速度限制
     */
    private fun sleepChargeMode(currentCapacityRatio: Int, targetRatio: Int, qcLimit: Int): Boolean {
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

            // 如果正在夜间慢速充电时间
            if (inSleepTime) {
                if (currentCapacityRatio >= targetRatio) {
                    // 如果已经超出了电池保护的电量，限制为50mA
                    batteryUnits.setChargeInputLimit(50, service)
                } else {
                    // 计算预期还需要充入多少电量（mAh）
                    val target = (targetRatio - currentCapacityRatio) / 100F * batteryCapacity
                    // 距离起床的剩余时间（小时）
                    val timeRemaining = GetUpTime(getUp).minutes / 60F

                    // 合理的充电速度 = 还需充入的电量(mAh) / timeRemaining
                    var limitValue = (target / timeRemaining).toInt()
                    if (limitValue < 50) {
                        limitValue = 50
                    } else if (limitValue > qcLimit) {
                        limitValue = qcLimit
                    }

                    batteryUnits.setChargeInputLimit(limitValue, service)
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
        keepShellAsync?.doCmd(DisableCharge)
        chargeDisabled = true
    }

    internal fun resumeCharge() {
        keepShellAsync!!.doCmd(ResumeCharge)
        chargeDisabled = false
    }

    //快速充电
    private fun setChargerLimit() {
        try {
            if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false)) {
                batteryUnits.setChargeInputLimit(qcLimit, service)
            }
        } catch (ex: Exception) {
        }
    }
}
