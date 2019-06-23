package com.omarea.common.shell

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
public class KeepShell(private var rootMode: Boolean = true) {
    private var p: Process? = null
    private var out: OutputStream? = null
    private var reader: BufferedReader? = null

    //尝试退出命令行程序
    public fun tryExit() {
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
    private val GET_ROOT_TIMEOUT = 20000L
    private val mLock = ReentrantLock()
    private val LOCK_TIMEOUT = 10000L
    private var enterLockTime = 0L

    private var checkRootState =
            "if [[ \$(id -u 2>&1) == '0' ]] || [[ \$(\$UID) == '0' ]] || [[ \$(whoami 2>&1) == 'root' ]] || [[ \$(set | grep 'USER_ID=0') == 'USER_ID=0' ]]; then\n" +
            "  echo '###root###'\n" +
            "else\n" +
            "  exit 1\n" +
            "  exit 1\n" +
            "fi\n"

    fun checkRoot(): Boolean {
        val r = doCmdSync(checkRootState)
        if (r.contains("##root###")) {
            return true
        }
        else if (r == "error" || r.contains("permission denied") || r.contains("not allowed") || r.equals("not found")) {
            return false
        } else {
            return false
        }
    }

    private fun getRuntimeShell() {
        if (p != null) return
        val getSu = Thread(Runnable {
            try {
                mLock.lockInterruptibly()
                enterLockTime = System.currentTimeMillis()
                p = Runtime.getRuntime().exec(if (rootMode) "su" else "sh")
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
                        Log.e("c", "" + ex.message)
                    }
                }).start()
            } catch (ex: Exception) {
                Log.e("getRuntime", "" + ex.message)
            } finally {
                enterLockTime = 0L
                mLock.unlock()
            }
        })
        getSu.start()
        getSu.join(10000)
        if (p == null && getSu.state != Thread.State.TERMINATED) {
            enterLockTime = 0L
            getSu.interrupt()
        }
    }

    private var br = "\n\n".toByteArray(Charset.defaultCharset())

    interface ExecuteAsyncResult {
        fun onResult(result: String)
    }

    class ExecuteAsyncThread(private var p:Process, private var cmd: String, private var executeAsyncResult: ExecuteAsyncResult) : Thread() {
        companion object {
            private val br = "\n\n".toByteArray(Charset.defaultCharset())
        }

        override fun run() {
            try {
                val uuid = UUID.randomUUID().toString().subSequence(0, 8)
                val startTag = "--start--$uuid--"
                val endTag = "--end--$uuid--"

                val out = p.outputStream
                val reader = p.inputStream.bufferedReader()
                try {
                    out.write(br)
                    out.write("echo '$startTag'".toByteArray(Charset.defaultCharset()))
                    out.write(br)
                    out.write(cmd.toByteArray(Charset.defaultCharset()))
                    out.write(br)
                    out.write("echo \"\"".toByteArray(Charset.defaultCharset()))
                    out.write(br)
                    out.write("echo '$endTag'".toByteArray(Charset.defaultCharset()))
                    out.write(br)
                    out.flush()
                } catch (ex: java.lang.Exception) {
                    Log.e("out!!.write", "" + ex.message)
                }

                val results = StringBuilder()
                var unstart = true
                while (true) {
                    val line = reader.readLine()
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

                executeAsyncResult.onResult(results.toString().trim())
            } catch (e: IOException) {
                Log.e("KeepShellAsync", "" + e.message)
                executeAsyncResult.onResult("error")
            }
        }
    }

    //执行脚本
    public fun doCmdSync(cmd: String): String {
        if (mLock.isLocked && enterLockTime > 0 && System.currentTimeMillis() - enterLockTime > LOCK_TIMEOUT) {
            tryExit()
            Log.e("doCmdSync-Lock", "线程等待超时${System.currentTimeMillis()} - $enterLockTime > $LOCK_TIMEOUT")
        }
        getRuntimeShell()
        var result = "error"
        if (out != null && p != null) {
            try {
                mLock.lockInterruptibly()
                val t = ExecuteAsyncThread(p!!, cmd, object: ExecuteAsyncResult{
                    override fun onResult(resultOut: String) {
                        result = resultOut
                    }
                })
                t.start()
                t.join(15000)
                if (result == "error") {
                    tryExit()
                }
            } catch (ex: java.lang.Exception) {
                tryExit()
            } finally {
                enterLockTime = 0L
                mLock.unlock()
            }
        } else {
            tryExit()
        }
        return result
    }
}
