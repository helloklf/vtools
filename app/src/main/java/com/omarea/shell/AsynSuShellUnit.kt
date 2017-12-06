package com.omarea.shell

import android.os.Handler
import android.widget.Toast
import java.nio.charset.Charset

/**
 * Created by helloklf on 2017/12/01.
 */

class AsynSuShellUnit(var handler: Handler) {
    var process: Process? = null

    private fun start(): AsynSuShellUnit {
        try {
            if (process == null)
                process = Runtime.getRuntime().exec("su")

            Thread(Runnable {
                var line = ""
                val reader = process!!.inputStream.bufferedReader()
                while (true) {
                    line = reader.readLine()
                    if (line != null) {
                        line = line.trim()
                        if (line.length > 0)
                            handler.sendMessage(handler.obtainMessage(1, line))
                    } else {
                        destroy()
                        break
                    }
                }
            }).start()
            handler.sendMessage(handler.obtainMessage(0, true))
        } catch (ex: Exception) {
            handler.sendMessage(handler.obtainMessage(0, false))
        }
        return this
    }

    private fun destroy() {
        try {
            if (process != null) {
                process!!.outputStream.close()
                process!!.destroy()
            }
        } catch (ex: Exception) {

        }
    }

    fun exec(cmd: String): AsynSuShellUnit {
        if (process == null) {
            start()
        }
        val outputStream = process!!.outputStream
        val writer = outputStream.bufferedWriter()
        writer.write(cmd)
        writer.write("\n\n")
        writer.flush()
        return this
    }

    fun waitFor() {
        if (process == null)
            return

        val outputStream = process!!.outputStream
        val writer = outputStream.bufferedWriter()
        writer.write("exit;exit;exit;")
        writer.write("\n\n")
        writer.flush()
        Thread(Runnable {
            process!!.waitFor()
            destroy()
        }).start()
    }
}
