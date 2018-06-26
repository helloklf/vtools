package com.omarea.shell

import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * 操作内核参数节点
 * Created by Hello on 2017/11/01.
 */

object KernelProrp {
    /**
     * 获取属性
     *
     * @param propName 属性名称
     * @return
     */
    @JvmOverloads
    fun getProp(propName: String, root: Boolean = false): String {
        if (!File(propName).exists()) {
            return ""
        }
        var p: Process? = null
        try {
            p = Runtime.getRuntime().exec(if (root) "su" else "sh")
            val stringBuilder = StringBuilder()
            Thread(Runnable {
                try {
                    val out = DataOutputStream(p.outputStream)
                    out.write("cat $propName".toByteArray(charset("UTF-8")))
                    out.writeBytes("\n")
                    out.writeBytes("\n")

                    out.writeBytes("echo '--end--'\n")
                    out.writeBytes("exit\n")
                    out.writeBytes("exit\n")
                    out.flush()
                    out.close()

                    val inputstream = p.inputStream
                    val inputstreamreader = InputStreamReader(inputstream)
                    val bufferedreader = BufferedReader(inputstreamreader)
                    while (true) {
                        val line = bufferedreader.readLine()
                        if (line != null && line != "--end--") {
                            stringBuilder.append(line)
                            stringBuilder.append("\n")
                        } else {
                            break
                        }
                    }
                    out.close()
                    bufferedreader.close()
                    inputstream.close()
                    inputstreamreader.close()
                } catch (e: Exception) {
                    Log.d("KernelProp", propName + "\n" + e.message)
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
                } catch (ex: Exception) {}
                finally {
                    bufferedreader.close()
                }
            }).start()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                p.waitFor(1000, TimeUnit.MILLISECONDS)
            } else {
                p.waitFor()
            }
            return stringBuilder.toString().trim()
        } catch (ignored: Exception) {
        } finally {
            if (p != null) {
                // p.destroy()
            }
        }

        return ""
    }

    /**
     * 保存属性
     *
     * @param propName 属性名称（要永久保存，请以persist.开头）
     * @param value    属性值,值尽量是简单的数字或字母，避免出现错误
     */
    fun setProp(propName: String, value: String): Boolean {
        try {
            val p = Runtime.getRuntime().exec("sh")
            val out = DataOutputStream(p.outputStream)
            out.writeBytes("echo $value > $propName")
            out.writeBytes("\n")
            out.writeBytes("\n\n\n")
            out.writeBytes("exit\n")
            out.writeBytes("exit\n")
            out.writeBytes("\n")
            out.flush()
            p.waitFor()
            return true
        } catch (ex: Exception) {
            return false
        }

    }
}
/**
 * 获取属性
 *
 * @param propName 属性名称
 * @return
 */
