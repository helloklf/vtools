package com.omarea.shared

import android.content.Context
import android.os.Environment
import android.util.Log
import com.omarea.shared.helper.NotifyHelper
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.Charset

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

    override fun uncaughtException(thread: Thread, ex: Throwable){
        if (ex.message != null) {
            try {
                val trace = StringWriter()
                ex.printStackTrace(PrintWriter(trace))
                val fileOutputStream = FileOutputStream(File(Environment.getExternalStorageDirectory().absolutePath + "/vtools-error.log"))
                fileOutputStream.write((ex.message + "\n\n" + trace.toString()).toByteArray(Charset.defaultCharset()))
                fileOutputStream.flush()
                fileOutputStream.close()
            } catch (ex: Exception) {
            }
        }
        Log.e("vtools-Exception", ex.message)
        try {
            if (mContext != null) {
                NotifyHelper(mContext!!, true).hideNotify()
            }
        } catch (ex: Exception) {

        }
        System.exit(0)
    }
}
