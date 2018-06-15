package com.omarea.vboot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Handler
import android.widget.Toast
import com.omarea.shared.Consts
import com.omarea.shared.SpfConfig
import com.omarea.shared.helper.KeepShell
import com.omarea.shared.helper.NotifyHelper
import java.io.DataOutputStream

class ReciverBatterychanged : BroadcastReceiver() {
    private var p: Process? = null
    internal var out: DataOutputStream? = null
    private var bp: Boolean = false
    private var keepShell: KeepShell? = null

    internal var context: Context? = null
    private var sharedPreferences: SharedPreferences? = null
    private var globalSharedPreferences: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private val myHandler = Handler()
    private var qcLimit = 50000

    //显示文本消息
    private fun showMsg(msg: String, longMsg: Boolean) {
        if (context != null)
            myHandler.post { Toast.makeText(context, msg, if (longMsg) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show() }
    }

    //快速充电
    private fun fastCharger() {
        if (!sharedPreferences!!.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false))
            return

        if (globalSharedPreferences!!.getBoolean(SpfConfig.GLOBAL_SPF_DEBUG, false))
            showMsg(context!!.getString(R.string.power_connected), false)
        keepShell!!.doCmd(Consts.FastChangerBase)
        keepShell!!.doCmd(computeLeves(qcLimit).toString())
    }

    private fun computeLeves(qcLimit: Int): StringBuilder {
        val arr = StringBuilder()
        if (qcLimit < 500) {
        } else {
            var level = 500
            while (level < qcLimit) {
                arr.append("echo ${level}000 > /sys/class/power_supply/battery/constant_charge_current_max;")
                arr.append("echo ${level}000 > /sys/class/power_supply/main/constant_charge_current_max;")
                arr.append("echo ${level}000 > /sys/class/qcom-battery/restricted_current;")
                level += 500
            }
        }
        arr.append("echo ${qcLimit}000 > /sys/class/power_supply/battery/constant_charge_current_max;")
        arr.append("echo ${qcLimit}000 > /sys/class/power_supply/main/constant_charge_current_max;")
        arr.append("echo ${qcLimit}000 > /sys/class/qcom-battery/restricted_current;")
        return arr;
    }

    private var lastBatteryLeavel = -1
    private var lastChangerState = false

    override fun onReceive(context: Context, intent: Intent) {
        if (keepShell == null) {
            keepShell = KeepShell(context)
        } else {
            keepShell!!.setContext(context)
        }
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(SpfConfig.CHARGE_SPF, Context.MODE_PRIVATE)
            listener = SharedPreferences.OnSharedPreferenceChangeListener { spf, key ->
                if (key == SpfConfig.CHARGE_SPF_QC_LIMIT) {
                    qcLimit = spf.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, 5000)
                }
            }
            sharedPreferences!!.registerOnSharedPreferenceChangeListener(listener)
            qcLimit = sharedPreferences!!.getInt(SpfConfig.CHARGE_SPF_QC_LIMIT, 5000)
        }
        if (globalSharedPreferences == null) {
            globalSharedPreferences = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)
        }

        this.context = context
        try {
            val action = intent.action
            val onChanger = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
            val batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)

            if (lastBatteryLeavel != batteryLevel) {
                lastBatteryLeavel = batteryLevel
            }

            //BatteryProtection
            if (sharedPreferences!!.getBoolean(SpfConfig.CHARGE_SPF_BP, false)) {
                if (onChanger) {
                    if (batteryLevel >= sharedPreferences!!.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, 85)) {
                        bp = true
                        keepShell!!.doCmd(Consts.DisableChanger)
                    } else if (batteryLevel < sharedPreferences!!.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, 85) - 20) {
                        bp = false
                        keepShell!!.doCmd(Consts.ResumeChanger)
                    }
                }
                //电量不足，恢复充电功能
                else if (action == Intent.ACTION_BATTERY_LOW) {
                    keepShell!!.doCmd(Consts.ResumeChanger)
                    Toast.makeText(this.context, context.getString(R.string.battery_low), Toast.LENGTH_SHORT).show()
                    bp = false
                } else if (bp && batteryLevel != -1 && batteryLevel < sharedPreferences!!.getInt(SpfConfig.CHARGE_SPF_BP_LEVEL, 85) - 20) {
                    //电量低于保护级别20
                    keepShell!!.doCmd(Consts.ResumeChanger)
                    bp = false
                }
            }

            if (action == Intent.ACTION_BATTERY_CHANGED) {
                //如果充电状态切换
                if (lastChangerState != onChanger) {
                    lastChangerState = onChanger
                }
                if (onChanger)
                    entryFastChanger(onChanger)
                return
            } else if (action == Intent.ACTION_POWER_CONNECTED) {
                lastChangerState = onChanger
                entryFastChanger(onChanger)
                return
            } else if (action == Intent.ACTION_BOOT_COMPLETED && onChanger) {
                lastChangerState = onChanger
                entryFastChanger(onChanger)
                return
            } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
                lastChangerState = onChanger
                entryFastChanger(onChanger)
                return
            }
        } catch (ex: Exception) {
            showMsg("充电加速服务：\n" + ex.message, true);
        }

    }

    private fun entryFastChanger(onChanger: Boolean) {
        if (onChanger) {
            if (sharedPreferences!!.getBoolean(SpfConfig.CHARGE_SPF_QC_BOOSTER, false)) {
                fastCharger()
            }
        }
    }
}
