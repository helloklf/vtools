package com.omarea.shell

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * 使用ROOT权限执行命令
 * Created by Hello on 2017/12/03.
 */

class SuDo(private val context: Context?) : ShellEvents() {
    private var handler: Handler? = null

    private fun noRoot() {
        if (context != null) {
            if (this.handler == null) {
                this.handler = Handler(Looper.getMainLooper())
            }
            handler!!.post { Toast.makeText(context, "没有ROOT权限无法运行", Toast.LENGTH_SHORT).show() }
        }
    }

    fun execCmd(cmd: String, sync: Boolean = false): Boolean {
        try {
            val p = Runtime.getRuntime().exec("su")
            val out = DataOutputStream(p.outputStream)
            out.write(cmd.toByteArray(charset("UTF-8")))
            out.writeBytes("\n")
            out.writeBytes("exit\n")
            out.writeBytes("exit\n")
            out.writeBytes("\n")
            out.flush()

            if (p != null) {
                Thread(Runnable {
                    val bufferedreader = BufferedReader(InputStreamReader(p.inputStream))
                    try {
                        while (true) {
                            val line = bufferedreader.readLine()
                            if (line != null) {
                                if (processHandler != null) {
                                    processHandler!!.sendMessage(processHandler!!.obtainMessage(PROCESS_EVENT_CONTENT, line))
                                }
                            } else {
                                break
                            }
                        }
                    } catch (ex: Exception) {
                    } finally {
                        bufferedreader.close()
                    }
                }).start()
                Thread(Runnable {
                    val bufferedreader = BufferedReader(InputStreamReader(p.errorStream))
                    try {
                        while (true) {
                            val line = bufferedreader.readLine()
                            if (line != null) {
                                if (processHandler != null) {
                                    processHandler!!.sendMessage(processHandler!!.obtainMessage(PROCESS_EVENT_ERROR_CONTENT, line))
                                }
                            } else {
                                break
                            }
                        }
                    } catch (ex: Exception) {
                    } finally {
                        bufferedreader.close()
                    }
                }).start()
            }
            if (sync) {
                /*
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return p.waitFor(30, TimeUnit.SECONDS)
                } else {
                    return p.waitFor() == 0
                }
                */
                return p.waitFor() == 0
            }
        } catch (e: IOException) {
            noRoot()
        } catch (ignored: Exception) {

        }
        return false
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var suDo: SuDo? = null

        fun execCmdSync(commands: List<String>): Boolean {
            val stringBuilder = StringBuilder()

            for (cmd in commands) {
                stringBuilder.append(cmd)
                stringBuilder.append("\n\n")
            }
            return KeepShellSync.doCmdSync(stringBuilder.toString()) != "error"
        }
    }
}
