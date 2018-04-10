package com.omarea.shared

import android.content.Context

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

        /*
        object : Thread() {
            override fun run() {
                Looper.prepare()
                try {
                    AlertDialog.Builder(mContext).setTitle("啊哦...")
                            .setCancelable(false)
                            .setMessage("这破程序又出错了...\n" + ex.localizedMessage)
                            .setNeutralButton("垃圾") { _, _ ->
                            }
                            .setPositiveButton("退出", { _, _ ->
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(0)
                            })
                            .create().show()
                } catch (ex:Exception) {
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
                Looper.loop()
            }

        }.start()
        */
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
