package com.omarea.shared.helper

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.BatteryManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.widget.RemoteViews
import com.omarea.shared.BatteryHistoryStore
import com.omarea.shared.ModeList
import com.omarea.shared.model.BatteryStatus
import com.omarea.shell.KeepShellPublic
import com.omarea.shell.KernelProrp
import com.omarea.shell.RootFile
import com.omarea.shell.SysUtils
import com.omarea.vtools.ActivityQuickSwitchMode
import com.omarea.vtools.R
import kotlin.math.abs


/**
 * 用于在通知栏显示通知
 * Created by Hello on 2018/01/23.
 */

internal class NotifyHelper(private var context: Context, notify: Boolean = false) : ModeList() {
    private var showNofity: Boolean = false
    private var notification: Notification? = null
    private var notificationManager: NotificationManager? = null
    private var batteryHistoryStore: BatteryHistoryStore? = null

    private fun getAppName(packageName: String): CharSequence? {
        try {
            return context.packageManager.getPackageInfo(packageName, 0).applicationInfo.loadLabel(context.packageManager)
        } catch (ex: Exception) {
            return packageName
        }
    }

    private var batteryTemp = "?°C"
    private var batteryCapacity = ""
    private var batteryStatus = "0"

    private var batteryUnit = Int.MIN_VALUE
    private fun getBatteryUnit(): Int {
        if (batteryUnit == Int.MIN_VALUE) {
            val full = KernelProrp.getProp("/sys/class/power_supply/battery/charge_full_design")
            if (full.length >= 4) {
                return full.length - 4
            }
            batteryUnit = Int.MIN_VALUE
        }
        return batteryUnit
    }

    private fun getCapacity(): Int {
        if (batteryCapacity.isEmpty() || batteryCapacity == "%?") {
            return Int.MIN_VALUE
        } else {
            return batteryCapacity.toInt()
        }
    }

    private fun getBatteryIcon(capacity: Int): Int {
        if (capacity < 20)
            return R.drawable.b_0
        if (capacity < 30)
            return R.drawable.b_1
        if (capacity < 70)
            return R.drawable.b_2
        return R.drawable.b_3
    }

    private fun updateBatteryInfo() {
        val batteryInfo = KeepShellPublic.doCmdSync("dumpsys battery")
        val batteryInfos = batteryInfo.split("\n")

        // 由于部分手机相同名称的参数重复出现，并且值不同，为了避免这种情况，加个额外处理，同名参数只读一次
        var levelReaded = false
        var tempReaded = false
        var statusReaded = false

        for (item in batteryInfos) {
            val info = item.trim()
            val index = info.indexOf(":")
            if (index > Int.MIN_VALUE && index < info.length) {
                val value = info.substring(info.indexOf(":") + 1).trim()
                if (info.startsWith("status")) {
                    if (!statusReaded) {
                        batteryStatus = value
                        statusReaded = true
                    } else {
                        continue
                    }
                } else if (info.startsWith("level")) {
                    if (!levelReaded) {
                        batteryCapacity = value
                        levelReaded = true
                    } else continue
                } else if (info.startsWith("temperature")) {
                    if (!tempReaded) {
                        tempReaded = true
                        batteryTemp = value
                    } else continue
                }
            }
        }
    }

    private fun getBatteryTemp(): String {
        if (batteryTemp == "?°C" || batteryTemp.isEmpty()) {
            return ""
        }
        try {
            return (batteryTemp.toInt() / 10.0).toString()
        } catch (ex: Exception) {
            return ""
        }
    }

    private fun getBatterySensor(): String? {
        var batterySensor: String? = null
        if (batterySensor == "init") {
            batterySensor = SysUtils.executeCommandWithOutput(false, "for sensor in /sys/class/thermal/*; do\n" +
                    "\ttype=\"\$(cat \$sensor/type)\"\n" +
                    "\tif [[ \"\$type\" = \"battery\" && -f \"\$sensor/temp\" ]]; then\n" +
                    "\t\techo \"\$sensor/temp\";\n" +
                    "\t\texit 0;\n" +
                    "\tfi;\n" +
                    "done;")
            if (batterySensor != null) {
                batterySensor = batterySensor!!.trim()
            } else {
                batterySensor = null
            }
        }
        return batterySensor
    }

    var path: String? = null
    private var isHuawei = false
    private var isOther = false
    private fun getBatteryIO(): String {
        if (isHuawei) {
            return getBatteryIOForHuawei()
        }
        if (path == null) {
            if (RootFile.itemExists("/sys/class/power_supply/battery/current_now")) {
                path = "/sys/class/power_supply/battery/current_now"
            } else if (RootFile.itemExists("/sys/class/power_supply/battery/BatteryAverageCurrent")) {
                path = "/sys/class/power_supply/battery/BatteryAverageCurrent"
            } else {
                val io = getBatteryIOForHuawei()
                if (!io.isEmpty()) {
                    isHuawei = true
                }
                return io;
            }
        }

        var io = KernelProrp.getProp(path!!)
        if (io.isEmpty()) {
            return ""
        }
        try {
            val unit = getBatteryUnit()
            var start = ""
            if (io.startsWith("+")) {
                start = "+"
                io = io.substring(1, io.length)
            } else if (io.startsWith("-")) {
                start = "-"
                io = io.substring(1, io.length)
            } else {
                start = ""
            }
            if (unit != Int.MIN_VALUE && io.length > unit) {
                val v = io.substring(0, io.length - unit)
                if (v.length > 4) {
                    return start + v.substring(0, v.length - 3)
                }
                return start + v
            } else if (io.length <= 4) {
                return start + io
            } else if (io.length >= 8) {
                return start + io.substring(0, io.length - 6)
            } else if (io.length >= 5) {
                return start + io.substring(0, io.length - 3)
            }
            return start + io
        } catch (ex: Exception) {
            return ""
        }
    }

    private fun getBatteryIOForHuawei(): String {
        if (RootFile.itemExists("/sys/class/power_supply/Battery/uevent")) {
            path = "/sys/class/power_supply/Battery/uevent"
        } else if (RootFile.itemExists("/sys/class/power_supply/bms/uevent")) {
            path = "/sys/class/power_supply/bms/uevent"
        } else {
            return ""
        }

        try {
            val io = KernelProrp.getProp(path!!, "POWER_SUPPLY_CURRENT_NOW=")
            return io.replace("POWER_SUPPLY_CURRENT_NOW=", "").replace("error", "")
        } catch (ex: Exception) {
            return ""
        }
    }

    //显示通知
    internal fun notify() {
        updateNotic()
    }

    private fun updateNotic() {
        try {
            var currentMode = getCurrentPowerMode()
            if (currentMode == null || currentMode.length == 0) {
                currentMode = ""
            }

            var currentApp = getCurrentPowermodeApp()
            if (currentApp == null || currentApp.length == 0) {
                currentApp = context.packageName
            }

            notifyPowerModeChange(currentApp!!, currentMode)
        } catch (ex: Exception) {
            Log.e("NotifyHelper", ex.localizedMessage)
        }
    }

    private fun notifyPowerModeChange(packageName: String, mode: String) {
        if (!showNofity) {
            return
        }

        val status = BatteryStatus()
        status.packageName = packageName
        status.mode = mode
        status.time = System.currentTimeMillis()

        var batteryImage: Bitmap? = null
        var batteryIO: String? = ""
        var batteryTemp = ""
        var modeImage = BitmapFactory.decodeResource(context.resources, getModImage(mode))
        try {
            updateBatteryInfo()
            val io = getBatteryIO()
            if (io.isNotEmpty()) {
                batteryIO = io + "mA"
            } else {
                batteryIO = "?mAh"
            }
            batteryTemp = getBatteryTemp()
            if (batteryTemp.isEmpty()) {
                batteryTemp = "?°C"
            } else {
                status.temperature = batteryTemp.toFloat()
                batteryTemp += "°C"
            }
            status.level = getCapacity()
            if (batteryStatus.isNotEmpty())
                status.status = batteryStatus.toInt()
            if (status.level > Int.MIN_VALUE) {
                if (status.status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    batteryImage = BitmapFactory.decodeResource(context.resources, R.drawable.b_4)
                } else {
                    if (io.isNotEmpty()) {
                        status.io = abs(io.toInt())
                    }
                    batteryImage = BitmapFactory.decodeResource(context.resources, getBatteryIcon(status.level))
                }
            }
            modeImage = BitmapFactory.decodeResource(context.resources, getModImage(mode))
        } catch (ex: Exception) {
            Log.e("NotifyHelper", ex.message)
        }

        if (batteryHistoryStore == null) {
            batteryHistoryStore = BatteryHistoryStore(context)
        }

        if (status.status != BatteryManager.BATTERY_STATUS_CHARGING) {
            if (status.status > Int.MIN_VALUE && status.io > Int.MIN_VALUE) {
                batteryHistoryStore!!.insertHistory(status)
            }
        }

        val remoteViews = RemoteViews(context.packageName, R.layout.notify0)
        remoteViews.setTextViewText(R.id.notify_title, getAppName(packageName))
        remoteViews.setTextViewText(R.id.notify_text, getModName(mode))
        remoteViews.setTextViewText(R.id.notify_battery_text, "$batteryIO ${status.level}% $batteryTemp")
        if (modeImage != null) {
            remoteViews.setImageViewBitmap(R.id.notify_mode, modeImage)
        }
        if (batteryImage != null) {
            remoteViews.setImageViewBitmap(R.id.notify_battery_icon, batteryImage)
        }

        val intent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, ActivityQuickSwitchMode::class.java).putExtra("packageName", packageName),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val icon = getModIcon(mode)
        notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        var builder: NotificationCompat.Builder? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager!!.getNotificationChannel("vtool-long-time") == null) {
                notificationManager!!.createNotificationChannel(NotificationChannel("vtool-long-time", "常驻通知", NotificationManager.IMPORTANCE_LOW))
            }
            builder = NotificationCompat.Builder(context, "vtool-long-time")
        } else {
            builder = NotificationCompat.Builder(context)
        }
        notification =
                builder.setSmallIcon(if (false) R.drawable.fanbox else icon)
                        .setContent(remoteViews)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true)
                        //.setDefaults(Notification.DEFAULT_SOUND)
                        .setContentIntent(intent)
                        .build()

        notification!!.flags = Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        notificationManager?.notify(0x100, notification)
    }

    //隐藏通知
    internal fun hideNotify() {
        if (notification != null) {
            notificationManager?.cancel(0x100)
            notification = null
            notificationManager = null
        }
    }

    internal fun setNotify(show: Boolean) {
        this.showNofity = show
        if (!show) {
            hideNotify()
        } else {
            notify()
        }
    }

    init {
        showNofity = notify
    }
}
