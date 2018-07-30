package com.omarea.shell

import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.locks.ReentrantLock


/**
 * Created by Hello on 2018/01/23.
 */
object KeepShellPublic {
    private var p: Process? = null
    private var out: OutputStream? = null
    private var reader: BufferedReader? = null

    //尝试退出命令行程序
    internal fun tryExit() {
        try {
            if (out != null)
                out!!.close()
            if (reader != null)
                reader!!.close()
        } catch (ex: Exception) {
        }
        try {
            p!!.destroy()
        } catch (ex: Exception) {
        }
        enterLockTime = 0L
        out = null
        reader = null
        p = null
    }

    //获取ROOT超时时间
    private const val GET_ROOT_TIMEOUT = 20000L
    private val mLock = ReentrantLock()
    private const val LOCK_TIMEOUT = 10000L
    private var enterLockTime = 0L

    private var checkRootState =
            "if [[ `id -u 2>&1` = '0' ]]; then\n" +
                    "echo 'root';\n" +
                    "elif [[ `\$UID` = '0' ]]; then\n" +
                    "echo 'root';\n" +
                    "elif [[ `whoami 2>&1` = 'root' ]]; then\n" +
                    "echo 'root';\n" +
                    "elif [[ `set | grep 'USER_ID=0'` = 'USER_ID=0' ]]; then\n" +
                    "echo 'root';\n" +
                    "else\n" +
                    "exit 1\n" +
                    "exit 1\n" +
                    "fi;\n"

    private fun getRuntimeShell() {
        if (p != null) return
        val getSu = Thread(Runnable {
            try {
                mLock.lockInterruptibly()
                enterLockTime = System.currentTimeMillis()
                p = Runtime.getRuntime().exec("su")
                out = p!!.outputStream
                reader = p!!.inputStream.bufferedReader()
                out!!.write(checkRootState.toByteArray(Charset.defaultCharset()))
                out!!.flush()
                Thread(Runnable {
                    try {
                        val errorReader =
                                p!!.errorStream.bufferedReader()
                        while (true) {
                            Log.e("KeepShellPublic", errorReader.readLine())
                        }
                    } catch (ex: Exception) {
                        Log.e("KeepShellPublic", ex.message)
                    }
                }).start()
            } catch (ex: Exception) {
                Log.e("getRuntime", ex.message)
            } finally {
                enterLockTime = 0L
                mLock.unlock()
            }
        })
        getSu.start()
        Thread(Runnable {
            Thread.sleep(10 * 1000)
            if (p == null && getSu.state != Thread.State.TERMINATED) {
                enterLockTime = 0L
                getSu.interrupt()
            }
        }).start()
        Thread.sleep(1000)
    }

    private var br = "\n\n".toByteArray(Charset.defaultCharset())

    //执行脚本
    internal fun doCmdSync(cmd: String): String {
        if (mLock.isLocked && enterLockTime > 0 && System.currentTimeMillis() - enterLockTime > LOCK_TIMEOUT) {
            tryExit()
            Log.e("doCmdSync-Lock", "线程等待超时${System.currentTimeMillis()} - $enterLockTime > $LOCK_TIMEOUT")
        }
        val uuid = UUID.randomUUID().toString()
        getRuntimeShell()
        if (out != null) {
            val startTag = "--start--$uuid--"
            val endTag = "--end--$uuid--"
            // Log.e("shell-lock", cmd)
            try {
                mLock.lockInterruptibly()

                if (out != null) {
                    out!!.write(br)
                    out!!.write("echo '$startTag'".toByteArray(Charset.defaultCharset()))
                    out!!.write(br)
                    out!!.write(cmd.toByteArray(Charset.defaultCharset()))
                    out!!.write(br)
                    out!!.write("echo '$endTag'".toByteArray(Charset.defaultCharset()))
                    out!!.write(br)
                    out!!.flush()

                    val results = StringBuilder()
                    var unstart = true
                    while (true && reader != null) {
                        val line = reader!!.readLine()
                        if (line == null || line.contains("--end--")) {
                            break
                        } else if (line.equals(startTag)) {
                            unstart = false
                        } else if (!unstart) {
                            results.append(line)
                            results.append("\n")
                        }
                    }
                    // Log.e("shell-unlock", cmd)
                    // Log.d("Shell", cmd.toString() + "\n" + "Result:"+results.toString().trim())
                    return results.toString().trim()
                } else {
                    return "error"
                }
            } catch (e: IOException) {
                tryExit()
                Log.e("KeepShellAsync", e.message)
                return "error"
            } finally {
                enterLockTime = 0L
                mLock.unlock()
            }
        } else {
            tryExit()
            return "error"
        }
    }
}
