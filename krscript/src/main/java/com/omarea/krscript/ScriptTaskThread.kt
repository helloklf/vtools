package com.omarea.krscript

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.view.View
import android.widget.RemoteViews
import com.omarea.common.ui.DialogHelper
import com.omarea.krscript.executor.ShellExecutor
import com.omarea.krscript.model.RunnableNode
import com.omarea.krscript.model.ShellHandlerBase

class ScriptTaskThread(private var process: Process) : Thread() {
    override fun run() {
        try {
            process.waitFor()
        } catch (ex: java.lang.Exception) {
        }
    }

    class ServiceShellHandlerBase(private val context: Context, private val runnableNode: RunnableNode, private val notificationID: Int, private val finishedIntent: Intent) : ShellHandlerBase() {
        private var notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        private val notificationTitle = runnableNode.title
        private var notificationMessageRows = ArrayList<String>()
        private var notificationMShortMsg = ""
        private var progressCurrent = 0
        private var progressTotal = 0
        private var someIgnored = false
        private var forceStop: Runnable? = null
        private var isFinished = false
        private val stopIntent = PendingIntent.getBroadcast(context, 0, finishedIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        private fun updateNotification() {
            if (notificationMessageRows.size > 8) {
                synchronized(notificationMessageRows) {
                    notificationMessageRows.remove(notificationMessageRows.first())
                    someIgnored = true
                }
            }

            val expandView = RemoteViews(context.getPackageName(), R.layout.kr_task_notification)
            expandView.setTextViewText(R.id.kr_task_title, notificationTitle)
            expandView.setTextViewText(R.id.kr_task_log, notificationMessageRows.joinToString("", if (someIgnored) "……\n" else "").trim())
            expandView.setProgressBar(R.id.kr_task_progress, progressTotal, progressCurrent, progressTotal < 0)
            expandView.setViewVisibility(R.id.kr_task_progress, if (progressTotal == progressCurrent) View.GONE else View.VISIBLE)
            expandView.setViewVisibility(R.id.kr_task_stop, if ((forceStop == null && !runnableNode.interruptable) || isFinished) View.GONE else View.VISIBLE)
            if (runnableNode.interruptable && forceStop != null && !isFinished) {
                expandView.setOnClickPendingIntent(R.id.kr_task_stop, stopIntent)
            }

            val notificationBuilder = Notification.Builder(context)
                    .setContentTitle("" + notificationTitle)
                    .setContentText("" + notificationMShortMsg + " >> " + notificationMessageRows.lastOrNull())
                    .setSmallIcon(R.drawable.kr_run)
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
            if (progressTotal != progressCurrent) {
                notificationBuilder.setProgress(progressTotal, progressCurrent, progressTotal < 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationBuilder.setCustomBigContentView(expandView)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!channelCreated) {
                    val channel = NotificationChannel(channelId, context.getString(R.string.kr_script_task_notification), NotificationManager.IMPORTANCE_DEFAULT)
                    channel.enableLights(false)
                    channel.enableVibration(false)
                    channel.setSound(null, null)
                    notificationManager.createNotificationChannel(channel)
                }
                channelCreated = true
                notificationBuilder.setChannelId(channelId)
            } else {
                notificationBuilder.setSound(null)
                notificationBuilder.setVibrate(null)
            }

            val notification = notificationBuilder.build()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                notification.bigContentView = expandView
            }

            notificationManager.notify(notificationID, notification); // 发送通知
        }

        override fun updateLog(msg: SpannableString?) {
        }

        override fun onReader(msg: Any?) {
            synchronized(notificationMessageRows) {
                notificationMessageRows.add("" + msg?.toString())
                updateNotification()
            }
        }

        override fun onError(msg: Any?) {
            notificationMShortMsg = context.getString(R.string.kr_script_task_has_error)
            synchronized(notificationMessageRows) {
                notificationMessageRows.add("" + msg?.toString())
                updateNotification()
            }
        }

        override fun onWrite(msg: Any?) {
        }

        override fun onExit(msg: Any?) {
            isFinished = true
            notificationMShortMsg = context.getString(R.string.kr_script_task_finished)
            synchronized(notificationMessageRows) {
                if (msg == 0) {
                    notificationMessageRows.add("\n" + context.getString(R.string.kr_shell_completed))
                } else {
                    notificationMessageRows.add("\n" + context.getString(R.string.kr_shell_finish_error) + " " + msg?.toString())
                }
                updateNotification()
            }
        }

        override fun onStart(forceStop: Runnable?) {
            this.forceStop = forceStop
            updateNotification()
        }

        override fun onStart(msg: Any?) {
            notificationMShortMsg = context.getString(R.string.kr_script_task_running)
        }

        override fun onProgress(current: Int, total: Int) {
            progressCurrent = current
            progressTotal = total
            updateNotification()
        }
    }

    companion object {
        private var channelCreated = false
        private val channelId = "kr_script_task_notification"
        private val ACTION_NAME = ".KrScriptTaskFinished"
        private var notificationCounter = 34050

        fun startTask(context: Context, script: String, params: HashMap<String, String>?, nodeInfo: RunnableNode, onExit: Runnable, onDismiss: Runnable): BroadcastReceiver {
            val applicationContext = context.applicationContext
            notificationCounter += 1
            val notificationID = notificationCounter

            val finishedIntent = Intent(context.packageName + ACTION_NAME).apply {
                putExtra("id", notificationID)
            }

            val handler = ServiceShellHandlerBase(applicationContext, nodeInfo, notificationCounter, finishedIntent)
            val process = ShellExecutor().execute(
                    context,
                    nodeInfo.interruptable,
                    script,
                    {
                        context.sendBroadcast(finishedIntent)
                    },
                    params,
                    handler)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent != null && intent.hasExtra("id")) {
                        if (intent.getIntExtra("id", 0) == notificationID) {
                            try {
                                process.destroy()
                            } catch (ex: java.lang.Exception) {
                            }

                            try {
                                onExit.run()
                                onDismiss.run()
                            } catch (ex: Exception) {
                            }
                            try {
                                applicationContext.unregisterReceiver(this)
                            } catch (ex: java.lang.Exception) {
                            }
                        }
                    }
                }
            }
            applicationContext.registerReceiver(receiver, IntentFilter(applicationContext.packageName + ACTION_NAME))

            val bundle = Bundle()
            params?.run {
                bundle.putSerializable("params", params)
            }
            DialogHelper.helpInfo(context, context.getString(R.string.kr_bg_task_start), context.getString(R.string.kr_bg_task_start_desc))
            // Toast.makeText(applicationContext, applicationContext.getString(R.string.kr_bg_task_start), Toast.LENGTH_SHORT).show()

            return receiver
        }
    }
}
