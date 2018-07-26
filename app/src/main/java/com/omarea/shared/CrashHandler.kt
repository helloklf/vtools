package com.omarea.shared

import android.app.AlertDialog
import android.content.Context
import android.util.Log

/**
 * Created by Hello on 2017/5/24.
 */
class CrashHandler constructor() : Thread.UncaughtExceptionHandler {
    private var mContext: Context? = null
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(ctx: Context) {
        mContext = ctx
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        Log.e("vtools-Exception", ex.message)
        AlertDialog.Builder(mContext).setTitle("哎呀，崩溃了...")
                .setMessage(ex.toString())
                .setPositiveButton("确定", {
                    _,_ ->
                    System.exit(-1)
                })
                .create()
                .show()
    }
}
