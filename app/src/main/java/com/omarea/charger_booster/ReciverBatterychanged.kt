package com.omarea.charger_booster

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.omarea.common.shell.KeepShellAsync
import com.omarea.shell_utils.BatteryUtils
import com.omarea.store.SpfConfig
import com.omarea.vtools.R
import java.util.*

class ReciverBatterychanged(private var service: Service) : BroadcastReceiver() {
    private var chargeDisabled: Boolean = false
    private var keepShellAsync: KeepShellAsync? = null

    private var chargeConfig: SharedPreferences
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener
    private val myHandler = Handler(Looper.getMainLooper())
    private var qcLimit = 50000
    // 电池总容量（mAh）
    private val batteryCapacity = BatteryInfo().getBatteryCapacity(service)

    //显示文本消息
    private fun showMsg(msg: String, longMsg: Boolean) {
        try {
            myHandler.post {
                Toast.makeText(service, msg, if (longMsg) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
            }
        } catch (ex: Exception) {
            Log.e("BatteryService", "" + ex.message)
        }
    }

    private var batteryUnits = BatteryUtils()
    var ResumeCharge = "sh " + com.omarea.common.shared.FileWrite.writePrivateShellFile("addin/resume_charge.sh", "addin/resume_charge.sh", service)
    var DisableCharge = "sh " + com.omarea.common.shared.FileWrite.writePrivateShellFile("addin/disable_charge.sh", "addin/disable_charge.sh", service)

    //快速充电
    private fun fastCharger() {
        try {
            if (!chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false))
                return
            batteryUnits.setChargeInputLimit(qcLimit, service)
        } catch (ex: Exception) {
            Log.e("ChargeService", "" + ex.stackTrace.toString())
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        onReceiveAsync(intent, pendingResult)
    }

    private fun onReceiveAsync(intent: Intent, pendingResult: PendingResult) {
        try {
            val action = intent.action
            val onCharge = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
            val currentLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            var bpLeve = chargeConfig.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, SpfConfig.CHARGE_SPF_BP_LEVEL_DEFAULT)
            if (bpLeve <= 30) {
                showMsg("Scene：当前电池保护电量值设为小于30%，会引起一些异常，已自动替换为默认值"+  SpfConfig.CHARGE_SPF_BP_LEVEL_DEFAULT +"%！", true)
                bpLeve = SpfConfig.CHARGE_SPF_BP_LEVEL_DEFAULT
            }
            val bp = chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_BP, false)

            //BatteryProtection 如果开启充电保护
            if (bp) {
                if (onCharge) {
                    if (currentLevel >= bpLeve) {
                        disableCharge()
                    } else if (currentLevel < bpLeve - 20) {
                        resumeCharge()
                    }
                }
                //电量不足，恢复充电功能
                else if (chargeDisabled && currentLevel != -1 && currentLevel < bpLeve - 20) {
                    //电量低于保护级别20
                    resumeCharge()
                }
            }
            // 如果没有开启充电保护，并且已经禁止充电
            else if (chargeDisabled) {
                resumeCharge()
            }

            // 如果电池电量低
            if (action == Intent.ACTION_BATTERY_LOW) {
                showMsg(service.getString(R.string.battery_low), false)
                resumeCharge()
            }

            if (!sleepChargeMode(currentLevel, bpLeve, qcLimit)) {
                // 未进入电池保护状态 并且电量低于85
                if (currentLevel < 85 && (!bp || currentLevel < (bpLeve - 20))) {
                    entryFastCharge()
                }
            }
        } catch (ex: Exception) {
            showMsg("充电加速服务：\n" + ex.message, true);
        } finally {
            pendingResult.finish()
        }
    }

    /**
     * 计算并使用合理的夜间充电速度
     * @param currentCapacityRatio 当前电量百分比（0~100）
     * @param totalCapacity 总容量（mAh）
     * @param targetRatio 目标充电百分比
     */
    private fun sleepChargeMode(currentCapacityRatio: Int, targetRatio: Int, qcLimit: Int): Boolean {
        // 如果开启了夜间充电降速
        if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_NIGHT_MODE, false)) {
            val now = Calendar.getInstance()
            val nowTimeValue = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val getUp = chargeConfig.getInt(SpfConfig.CHARGE_SPF_TIME_GET_UP, SpfConfig.CHARGE_SPF_TIME_GET_UP_DEFAULT)
            val sleep = chargeConfig.getInt(SpfConfig.CHARGE_SPF_TIME_SLEEP, SpfConfig.CHARGE_SPF_TIME_SLEEP_DEFAULT)

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
                    var timeRemaining = 0F
                    // 和计算闹钟距离下一次还有多久响的逻辑有点像
                    // 如果已经过了今天的起床时间，计算到明天的起床时间还有多久
                    if (nowTimeValue > getUp) {
                        // (24 * 60) => 1440
                        // (今天剩余时间 + 明天的起床时间) / 60分钟 计算小时数
                        timeRemaining = ((1440 - nowTimeValue) + getUp) / 60F
                    }
                    // 如果还没过今天的起床时间
                    else {
                        timeRemaining = (getUp - nowTimeValue) / 60F
                    }

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
        } else {
            // TODO: 恢复正常充电速度（神仙才知道要应该恢复到多少充电速度...）
            entryFastCharge()
        }
        return false
    }

    init {
        if (keepShellAsync == null) {
            keepShellAsync = KeepShellAsync(service)
        }
        chargeConfig = service.getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
        listener = SharedPreferences.OnSharedPreferenceChangeListener { spf, key ->
            if (key == SpfConfig.CHARGE_SPF_QC_LIMIT) {
                qcLimit = spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, qcLimit)
            }
        }
        chargeConfig.registerOnSharedPreferenceChangeListener(listener)
        qcLimit = chargeConfig.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, qcLimit)
    }

    internal fun onDestroy() {
        chargeConfig.unregisterOnSharedPreferenceChangeListener(this.listener)
        this.resumeCharge()
        keepShellAsync!!.tryExit()
        keepShellAsync = null
    }

    internal fun disableCharge() {
        keepShellAsync!!.doCmd(DisableCharge)
        chargeDisabled = true
    }

    internal fun resumeCharge() {
        keepShellAsync!!.doCmd(ResumeCharge)
        chargeDisabled = false
    }

    internal fun entryFastCharge() {
        if (chargeConfig.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false)) {
            fastCharger()
        }
    }
}
