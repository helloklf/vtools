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
            // "if [[ \$(id -u 2>&1) == '0' ]] || [[ \$(\$UID) == '0' ]] || [[ \$(whoami 2>&1) == 'root' ]] || [[ \$(\$USER_ID) == '0' ]]; then\n" +
            "if [[ \$(id -u 2>&1) == '0' ]] || [[ \$(\$UID) == '0' ]] || [[ \$(whoami 2>&1) == 'root' ]] || [[ \$(set | grep 'USER_ID=0') == 'USER_ID=0' ]]; then\n" +
                    "  echo '>>> root'\n" +
                    "else\n" +
                    "if [[ -d /cache ]]; then\n" +
                    "  echo 1 > /cache/vtools_root\n" +
                    "  if [[ -f /cache/vtools_root ]] && [[ \$(cat /cache/vtools_root) == '1' ]]; then\n" +
                    "    echo '>>> root'\n" +
                    "    rm -rf /cache/vtools_root\n" +
                    "    return\n" +
                    "  fi\n" +
                    "fi\n" +
                    "exit 1\n" +
                    "exit 1\n" +
                    "fi\n"

    fun checkRoot(): Boolean {
        val r = doCmdSync(checkRootState)
        return if (r == "error" || r.contains("permission denied") || r.contains("not allowed") || r.equals("not found")) {
            if (rootMode) {
                tryExit()
            }
            false
        } else if (r.contains(">>> root")) {
            true
        } else {
            if (rootMode) {
                tryExit()
            }
            false
        }
    }

    private fun getRuntimeShell() {
        if (p != null) return
        val getSu = Thread(Runnable {
            try {
                mLock.lockInterruptibly()
                enterLockTime = System.currentTimeMillis()
                p = if (rootMode) ShellExecutor.getSuperUserRuntime() else ShellExecutor.getRuntime()
                out = p!!.outputStream
                reader = p!!.inputStream.bufferedReader()
                if (rootMode) {
                    out?.run {
                        write(checkRootState.toByteArray(Charset.defaultCharset()))
                        flush()
                    }
                }
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

    private val shellOutputCache = StringBuilder()

    //执行脚本
    public fun doCmdSync(cmd: String): String {
        if (mLock.isLocked && enterLockTime > 0 && System.currentTimeMillis() - enterLockTime > LOCK_TIMEOUT) {
            tryExit()
            Log.e("doCmdSync-Lock", "线程等待超时${System.currentTimeMillis()} - $enterLockTime > $LOCK_TIMEOUT")
        }
        val uuid = UUID.randomUUID().toString().subSequence(0, 4)
        getRuntimeShell()

        val startTag = "|$uuid>"
        val endTag = "<$uuid|"

        try {
            mLock.lockInterruptibly()

            out!!.run {
                write(br)
                write("echo '$startTag'".toByteArray(Charset.defaultCharset()))
                write(br)
                write(cmd.toByteArray(Charset.defaultCharset()))
                write(br)
                write("echo \"\"".toByteArray(Charset.defaultCharset()))
                write(br)
                write("echo '$endTag'".toByteArray(Charset.defaultCharset()))
                write(br)
                flush()
            }

            var unstart = true
            while (true && reader != null) {
                val line = reader!!.readLine()
                if (line == null || line.contains(endTag)) {
                    if (line != null) {
                        shellOutputCache.append(line.substring(0, line.indexOf(endTag)))
                    }
                    break
                } else if (line.contains(startTag)) {
                    shellOutputCache.clear()
                    shellOutputCache.append(line.substring(line.indexOf(startTag) + startTag.length))
                    unstart = false
                } else if (!unstart) {
                    shellOutputCache.append(line)
                    shellOutputCache.append("\n")
                }
            }
            // Log.e("shell-unlock", cmd)
            // Log.d("Shell", cmd.toString() + "\n" + "Result:"+results.toString().trim())
            return shellOutputCache.toString().trim()
        }
        /* catch (e: IOException) {
            tryExit()
            Log.e("KeepShellAsync", "" + e.message)
            return "error"
        }
        */
        catch (e: Exception) {
            tryExit()
            Log.e("KeepShellAsync", "" + e.message)
            return "error"
        } finally {
            enterLockTime = 0L
            mLock.unlock()
        }
    }
}
