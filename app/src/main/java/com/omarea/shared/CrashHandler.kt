package com.omarea.shared

import android.app.AlertDialog
import android.content.Context
import android.os.Looper

/**
 * Created by Hello on 2017/5/24.
 */
class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {
    private var mContext: Context? = null
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(ctx: Context) {
        mContext = ctx
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        // if (!handleException(ex) && mDefaultHandler != null) {
        // mDefaultHandler.uncaughtException(thread, ex);
        // } else {
        // android.os.Process.killProcess(android.os.Process.myPid());
        // System.exit(10);
        // }
        println("uncaughtException")

        object : Thread() {
            override fun run() {
                Looper.prepare()
                AlertDialog.Builder(mContext).setTitle("Oops").setCancelable(false)
                        .setMessage("The application has stopped running...\n\n" + ex.localizedMessage).setNeutralButton("OK") { dialog, which -> System.exit(0) }
                        .create().show()
                Looper.loop()
            }
        }.start()
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成. 开发者可以根据自己的情况来自定义异常处理逻辑

     * @param ex
     * *
     * @return true:如果处理了该异常信息;否则返回false
     */
    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) {
            return true
        }
        // new Handler(Looper.getMainLooper()).post(new Runnable() {
        // @Override
        // public void run() {
        // new AlertDialog.Builder(mContext).setTitle("Oops")
        // .setMessage("The application has stopped running...").setNeutralButton("OK", null)
        // .create().show();
        // }
        // });

        return true
    }

    companion object {
        val TAG = "CrashHandler"
        val instance = CrashHandler()
    }
}
