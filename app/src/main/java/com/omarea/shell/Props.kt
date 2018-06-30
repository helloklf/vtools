package com.omarea.shell

import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Created by Hello on 2017/8/8.
 */

object Props {
    private val isSeLinuxEnforcing: Boolean
        get() {
            val r = SysUtils.executeCommandWithOutput(false, "getenforce")
            return r.isEmpty() || r == "Enforcing" || r == "1"
        }

    /**
     * 获取属性
     *
     * @param propName 属性名称
     * @return 内容
     */
    fun getProp(propName: String): String {
        var p: Process? = null
        try {
            p = Runtime.getRuntime().exec("sh")

            val lines = StringBuilder()
            Thread(Runnable {
                try {
                    val out = DataOutputStream(p.outputStream)
                    out.writeBytes("getprop $propName")
                    out.writeBytes("\n")
                    out.writeBytes("\n")

                    out.writeBytes("echo '--end--'\n")

                    out.writeBytes("exit\n")
                    out.writeBytes("exit\n")
                    out.flush()

                    val inputstream = p.inputStream
                    val inputstreamreader = InputStreamReader(inputstream)
                    val bufferedreader = BufferedReader(inputstreamreader)
                    while (true) {
                        val line = bufferedreader.readLine()
                        if (line != null && line != "--end--") {
                            lines.append(line)
                            lines.append("\n")
                        } else {
                            break
                        }
                    }
                    out.close()
                    bufferedreader.close()
                    inputstream.close()
                    inputstreamreader.close()
                } catch (e: Exception) {
                    Log.d("Props", propName + "\n" + e.message)
                } finally {
                    p.destroy()
                }
            }).start()
            Thread(Runnable {
                val bufferedreader = BufferedReader(InputStreamReader(p.errorStream))
                try {
                    while (true) {
                        val line = bufferedreader.readLine()
                        if (line != null && line != "--end--") {
                        } else {
                            break
                        }
                    }
                } catch (ex: Exception) {
                } finally {
                    bufferedreader.close()
                }
            }).start()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                p.waitFor(1000, TimeUnit.MILLISECONDS)
            } else {
                p.waitFor()
            }
            return lines.toString().trim()
        } catch (ignored: Exception) {

        } finally {
            if (p != null) {
                // p.destroy()
            }
        }

        return ""
    }

    fun setPorp(propName: String, value: String): Boolean {
        try {
            if (isSeLinuxEnforcing) {
                return false
            } else {
                val p = Runtime.getRuntime().exec("sh")
                val out = DataOutputStream(p.outputStream)
                out.writeBytes("setprop $propName \"$value\"")
                out.writeBytes("\n")
                out.writeBytes("exit\n")
                out.writeBytes("exit\n")
                out.writeBytes("\n")
                out.flush()
                p.waitFor()
                return true
            }
        } catch (ex: Exception) {
            return false
        }

    }
}
