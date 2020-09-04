package com.omarea.common.shell

import android.os.Handler
import java.nio.charset.Charset

/**
 * Created by helloklf on 2017/12/01.
 */

class AsynSuShellUnit(var handler: Handler) {
    var process: Process? = null

    private fun start(): AsynSuShellUnit {
        try {
            if (process == null)
                process = ShellExecutor.getSuperUserRuntime()

            Thread(Runnable {
                try {
                    var line: String
                    val reader = process!!.inputStream.bufferedReader()
                    while (true) {
                        line = reader.readLine()
                        if (line != null) {
                            line = line.trim()
                            if (line.isNotEmpty())
                                handler.sendMessage(handler.obtainMessage(1, line))
                        } else {
                            destroy()
                            break
                        }
                    }
                } catch (ex: Exception) {
                    print(ex.message)
                }
            }).start()
            Thread(Runnable {
                try {
                    var line: String
                    val reader = process!!.errorStream.bufferedReader()
                    while (true) {
                        line = reader.readLine().trim()
                        if (line.isNotEmpty())
                            handler.sendMessage(handler.obtainMessage(5, line))
                    }
                } catch (ex: Exception) {
                    print(ex.message)
                }
            }).start()
            handler.sendMessage(handler.obtainMessage(0, true))
        } catch (ex: Exception) {
            handler.sendMessage(handler.obtainMessage(0, false))
        }
        return this
    }

    fun destroy() {
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
        if (process == null) {
            handler.handleMessage(handler.obtainMessage(-1))
            return this
        }
        val outputStream = process!!.outputStream
        outputStream.write("\n\n".toByteArray(Charset.forName("UTF-8")))
        outputStream.write(cmd.toByteArray(Charset.forName("UTF-8")))
        outputStream.write("\n\n".toByteArray(Charset.forName("UTF-8")))
        outputStream.flush()
        return this
    }

    fun waitFor() {
        if (process == null)
            return

        val outputStream = process!!.outputStream
        val writer = outputStream.bufferedWriter()
        writer.write("exit\nexit\nexit\n")
        writer.write("\n\n")
        writer.flush()
        Thread(Runnable {
            if (process!!.waitFor() == 0) {
                handler.sendMessage(handler.obtainMessage(10, true))
            } else {
                handler.sendMessage(handler.obtainMessage(10, false))
            }
            destroy()
        }).start()
    }

    fun waitFor(next: Runnable) {
        if (process == null)
            return

        val outputStream = process!!.outputStream
        val writer = outputStream.bufferedWriter()
        writer.write("exit\nexit\nexit\n")
        writer.write("\n\n")
        writer.flush()
        Thread(Runnable {
            process!!.waitFor()
            destroy()
            handler.sendMessage(handler.obtainMessage(10, true))
            next.run()
        }).start()
    }
}
