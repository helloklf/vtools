package com.omarea.shared

import android.content.Context
import android.os.Looper
import android.widget.Toast

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
        Looper.getMainLooper().run {
            Toast.makeText(mContext, "发生了异常：\n\n" + ex.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }
}
