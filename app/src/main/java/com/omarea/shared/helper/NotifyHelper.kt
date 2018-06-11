package com.omarea.shared.helper

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.widget.RemoteViews
import com.omarea.shared.ModeList
import com.omarea.shell.KernelProrp
import com.omarea.shell.SysUtils
import com.omarea.vboot.ActivityQuickSwitchMode
import com.omarea.vboot.R
import java.io.File


/**
 * 用于在通知栏显示通知
 * Created by Hello on 2018/01/23.
 */

internal class NotifyHelper(private var context: Context, notify: Boolean = false) : ModeList() {
    private var showNofity: Boolean = false
    private var notification: Notification? = null
    private var notificationManager: NotificationManager? = null
    private var handler = Handler()

    private fun getAppName(packageName: String): CharSequence? {
        try {
            return context.packageManager.getPackageInfo(packageName, 0).applicationInfo.loadLabel(context.packageManager)
        } catch (ex: Exception) {
            return packageName
        }
    }

    private var batteryUnit = Int.MIN_VALUE
    private var batterySensor: String? = "init"
    private fun getBatteryUnit(): Int {
        if (batteryUnit == Int.MIN_VALUE) {
            val full = KernelProrp.getProp("/sys/class/power_supply/battery/charge_full_design")
            if (full != null && full.length > 0) {
                return full.length - 4
            }
            return -1
        }
        return batteryUnit
    }

    private fun getCapacity(): String? {
        return KernelProrp.getProp("/sys/class/power_supply/battery/capacity", false) + "%"
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

    private fun getBatterySensor(): String? {
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
            }
        } else {
            batterySensor = null
        }
        return batterySensor
    }

    private fun getBatteryTemp(): String {
        val sensor = getBatterySensor()
        if (sensor != null) {
            val temp = KernelProrp.getProp(sensor)
            if (temp == null || (temp.length < 4)) {
                return "? °C"
            }
            return temp.substring(0, temp.length - 3) + "°C"
        } else {
            return "? °C"
        }
    }

    private fun getBatteryIO(): String? {
        var path = ""
        if (File("/sys/class/power_supply/battery/current_now").exists()) {
            path = "/sys/class/power_supply/battery/current_now"
        } else if (File("/sys/class/power_supply/battery/BatteryAverageCurrent").exists()) {
            path = "/sys/class/power_supply/battery/BatteryAverageCurrent"
        } else {
            return "? mA"
        }

        var io = KernelProrp.getProp(path, false)
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
        if (unit != -1 && io.length > unit) {
            return start + io.substring(0, io.length - unit) + "mA"
        } else if (io.length <= 4) {
            return start + io + "mA"
        }
        return start + io
    }

    //显示通知
    internal fun notify() {
        handler.postDelayed({
            updateNotic()
        }, 1500)
    }

    private fun updateNotic () {
        var currentMode = getCurrentPowerMode()
        if (currentMode == null || currentMode.length == 0) {
            currentMode = ""
        }

        var currentApp = getCurrentPowermodeApp()
        if (currentApp == null || currentApp.length == 0) {
            currentApp = context.packageName
        }

        notifyPowerModeChange(currentApp!!, currentMode)
    }

    private fun notifyPowerModeChange(packageName: String, mode: String) {
        if (!showNofity) {
            return
        }
        val remoteViews = RemoteViews(context.packageName, R.layout.notify0)
        remoteViews.setTextViewText(R.id.notify_title, getAppName(packageName))
        remoteViews.setTextViewText(R.id.notify_text, getModName(mode))
        remoteViews.setTextViewText(R.id.notify_battery_title, "电池")
        val capacity = getCapacity()
        try {
            if (capacity != null && !capacity.isNullOrEmpty()) {
                remoteViews.setImageViewBitmap(R.id.notify_battery_icon, BitmapFactory.decodeResource(context.resources, getBatteryIcon(capacity.replace("%", "").toInt())))
            }
        } catch (ex: Exception) {
        }
        remoteViews.setTextViewText(R.id.notify_battery_text, getBatteryIO() + " " + capacity + " " + getBatteryTemp())
        remoteViews.setImageViewBitmap(R.id.notify_mode, BitmapFactory.decodeResource(context.resources, getModImage(mode)))

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
            notificationManager!!.createNotificationChannel(NotificationChannel("vtool", "常驻通知", NotificationManager.IMPORTANCE_LOW))
            builder = NotificationCompat.Builder(context, "vtool")
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
