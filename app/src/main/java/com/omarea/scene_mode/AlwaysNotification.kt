package com.omarea.scene_mode

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.BatteryManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.omarea.Scene
import com.omarea.data.EventType
import com.omarea.data.GlobalStatus
import com.omarea.data.IEventReceiver
import com.omarea.store.SpfConfig
import com.omarea.vtools.R

/**
 * 常驻通知
 */
internal class AlwaysNotification(
        private var context: Context,
        notify: Boolean = false,
        override val isAsync: Boolean = false) : ModeSwitcher(), IEventReceiver {
    override fun eventFilter(eventType: EventType): Boolean {
        return eventType == EventType.SCENE_MODE_ACTION
    }

    override fun onReceive(eventType: EventType, data: HashMap<String, Any>?) {
        notify()
    }

    override fun onSubscribe() {

    }

    override fun onUnsubscribe() {

    }

    private var showNofity: Boolean = false
    private var notification: Notification? = null
    private var notificationManager: NotificationManager? = null
    private var globalSPF = context.getSharedPreferences(SpfConfig.GLOBAL_SPF, Context.MODE_PRIVATE)

    private fun getAppName(packageName: String): CharSequence? {
        try {
            return context.packageManager.getPackageInfo(packageName, 0).applicationInfo.loadLabel(context.packageManager)
        } catch (ex: Exception) {
            return packageName
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

    //显示通知
    internal fun notify() {
        try {
            var currentMode = getCurrentPowerMode()
            if (currentMode.length == 0) {
                currentMode = ""
            }

            var currentApp = getCurrentPowermodeApp()
            if (currentApp.isEmpty()) {
                currentApp = "android"

                notifyPowerModeChange(currentApp, currentMode)
            } else {
                notifyPowerModeChange(currentApp, currentMode)
            }
        } catch (ex: Exception) {
        }
    }

    private fun notifyPowerModeChange(packageName: String, mode: String) {
        if (!showNofity) {
            return
        }

        var batteryImage: Bitmap? = null
        var batteryIO: String? = ""
        var batteryTemp = ""
        var modeImage = BitmapFactory.decodeResource(context.resources, getModImage(mode))

        try {
            batteryIO = "${GlobalStatus.batteryCurrentNow}mA"
            batteryTemp = "${GlobalStatus.temperatureCurrent}°C"

            if (GlobalStatus.batteryStatus == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                batteryImage = BitmapFactory.decodeResource(context.resources, getBatteryIcon(GlobalStatus.batteryCapacity))
            } else {
                batteryImage = BitmapFactory.decodeResource(context.resources, R.drawable.b_4)
            }
            modeImage = BitmapFactory.decodeResource(context.resources, getModImage(mode))
        } catch (ex: Exception) {
        }

        val remoteViews = this.getRemoteViews().apply {
            setTextViewText(R.id.notify_title, getAppName(packageName))
            setTextViewText(R.id.notify_text, getModName(mode))
            setTextViewText(R.id.notify_battery_text, "$batteryIO ${GlobalStatus.batteryCapacity}% $batteryTemp")
            if (modeImage != null) {
                setImageViewBitmap(R.id.notify_mode, modeImage)
            }
            if (batteryImage != null) {
                setImageViewBitmap(R.id.notify_battery_icon, batteryImage)
            }
        }

        val clickIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ReceiverSceneMode::class.java).putExtra("packageName", packageName),
                PendingIntent.FLAG_UPDATE_CURRENT)

        val icon = getModIcon(mode)
        notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager!!.getNotificationChannel("vtool-long-time") == null) {
                notificationManager!!.createNotificationChannel(NotificationChannel("vtool-long-time", "常驻通知", NotificationManager.IMPORTANCE_LOW))
            }
            NotificationCompat.Builder(context, "vtool-long-time")
        } else {
            NotificationCompat.Builder(context)
        }
        notification =
                builder.setSmallIcon(if (false) R.drawable.fanbox else icon)
                        .setContent(remoteViews)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true)
                        .setOngoing(false)
                        //.setDefaults(Notification.DEFAULT_SOUND)
                        .setContentIntent(clickIntent)
                        .build()

        notification!!.flags = Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE
        notificationManager?.notify(0x100, notification)
    }

    private fun getRemoteViews(): RemoteViews {
        val layout = (if (Scene.isNightMode && globalSPF.getBoolean(SpfConfig.GLOBAL_NIGHT_BLACK_NOTIFICATION, false)) {
            R.layout.layout_notification_dark
        } else {
            R.layout.layout_notification
        })
        return RemoteViews(context.packageName, layout)
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
