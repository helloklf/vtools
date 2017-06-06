package com.omarea.vboot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Handler
import android.os.Message
import android.widget.Toast
import com.omarea.shared.*
import java.io.DataOutputStream
import java.io.IOException

class reciver_batterychanged2 : BroadcastReceiver() {

    private var p: Process? = null
    internal var out: DataOutputStream? = null

    internal fun tryExit() {
        try {
            if (out != null)
                out!!.close()
        } catch (ex: Exception) {
        }

        try {
            p!!.destroy()
        } catch (ex: Exception) {
        }

    }

    @JvmOverloads internal fun DoCmd(cmd: String, isRedo: Boolean = false) {
        Thread(Runnable {
            try {
                tryExit()
                if (p == null || isRedo || out == null) {
                    tryExit()
                    p = Runtime.getRuntime().exec("su")
                    out = DataOutputStream(p!!.outputStream)
                }
                out!!.writeBytes(cmd)
                out!!.writeBytes("\n")
                out!!.flush()
                //out!!.close()
            } catch (e: IOException) {
                //重试一次
                if (!isRedo)
                    DoCmd(cmd, true)
                else
                    ShowMsg("Failed execution action!\nError message : " + e.message + "\n\n\ncommand : \r\n" + cmd,true)
            }
        }).start()
    }

    internal var context: Context? = null
    private val myHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }

    //显示文本消息
    private fun ShowMsg(msg: String, longMsg: Boolean) {
        if (context != null)
            myHandler.post { Toast.makeText(context, msg, if (longMsg) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show() }
    }

    //快速充电
    private fun FastCharger() {
        if (!ConfigInfo.getConfigInfo().QcMode)
            return
        DoCmd(Consts.FastChanger)
    }

    /**
     * 电池保护
     */
    private fun BP() {
        DoCmd(Consts.BP)
    }

    private fun BPReset() {
        DoCmd(Consts.BPReset)
    }

    internal var lastBatteryLeavel = -1
    internal var lastChangerState = false

    override fun onReceive(context: Context, intent: Intent) {
        this.context = context
        try {
            val action = intent.action
            val onChanger = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
            val batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)

            if (lastBatteryLeavel != batteryLevel) {
                EventBus.publish(Events.BatteryChanged, batteryLevel)
                lastBatteryLeavel = batteryLevel
            }

            if (onChanger && (batteryLevel < 85 || !ConfigInfo.getConfigInfo().BatteryProtection))
                BPReset()

            if (action == Intent.ACTION_BATTERY_CHANGED) {
                if (AppShared.system_inited && onChanger) {
                    if (ConfigInfo.getConfigInfo().QcMode) FastCharger()
                    if (ConfigInfo.getConfigInfo().BatteryProtection) BP()
                }
            } else if (action == Intent.ACTION_POWER_CONNECTED) {
            } else if (action == Intent.ACTION_BOOT_COMPLETED && onChanger) {
            } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
                EventBus.publish(Events.PowerDisConnection)
                return
            }

            //如果充电状态切换
            if (lastChangerState != onChanger) {
                lastChangerState = onChanger
                if (onChanger) {
                    BPReset()
                    entryFastChanger(onChanger)
                    EventBus.publish(Events.PowerConnection)
                } else
                    EventBus.publish(Events.PowerDisConnection)
            }
        } catch (ex: Exception) {
            System.out.print(ex.message);
        }

    }

    private fun entryFastChanger(onChanger: Boolean) {
        if (AppShared.system_inited && onChanger) {
            if (ConfigInfo.getConfigInfo().QcMode)
                FastCharger()
            if (ConfigInfo.getConfigInfo().BatteryProtection)
                BP()
            if (ConfigInfo.getConfigInfo().DebugMode)
                ShowMsg("充电器已连接！", false)
        }
    }

    companion object {
        var serviceHelper: ServiceHelper? = null
    }
}
