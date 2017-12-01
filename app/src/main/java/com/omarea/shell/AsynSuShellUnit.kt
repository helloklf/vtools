package com.omarea.shell

import android.os.Handler
import java.nio.charset.Charset

/**
 * Created by helloklf on 2017/12/01.
 */

class AsynSuShellUnit {
    var handler: Handler
    lateinit var process: Process

    constructor(handler: Handler) {
        this.handler = handler
    }


    fun start() {
        try {
            if (process == null)
                process = Runtime.getRuntime().exec("su")

            handler.sendMessage(handler.obtainMessage(0, true))
        } catch (ex: Exception) {
            handler.sendMessage(handler.obtainMessage(0, false))
        }
    }
    fun destroy() {
        try {
            if (process != null){
                process.outputStream.close()
                process.destroy()
            }
        }
        catch (ex: Exception) {

        }
    }

    fun exec(charset: Charset) {
        val outputStream = process.outputStream
        outputStream.writer(charset)
        outputStream.writer(kotlin.text.charset("\n\n"))
        outputStream.flush()
    }
}
