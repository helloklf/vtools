package com.omarea.common.shell

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by Hello on 2018/01/23.
 */
public class KeepShellAsync(private var context: Context?, private var rootMode: Boolean = true) : ShellEvents() {
    companion object {
        private val keepShells = HashMap<String, KeepShellAsync>()
        fun getInstance(key: String): KeepShellAsync {
            synchronized(keepShells) {
                if (!keepShells.containsKey(key)) {
                    keepShells.put(key, KeepShellAsync(null))
                }
                return keepShells.get(key)!!
            }
        }

        fun destoryInstance(key: String) {
            synchronized(keepShells) {
                if (!keepShells.containsKey(key)) {
                    return
                } else {
                    val keepShell = keepShells.get(key)!!
                    keepShells.remove(key)
                    keepShell.tryExit()
                }
            }
        }

        fun destoryAll() {
            synchronized(keepShells) {
                while (keepShells.isNotEmpty()) {
                    val key = keepShells.keys.first()
                    val keepShell = keepShells.get(key)!!
                    keepShells.remove(key)
                    keepShell.tryExit()
                }
            }
        }
    }

    private var p: Process? = null
    private var out: BufferedWriter? = null
    private var handler: Handler = Handler(Looper.getMainLooper())
    private val mLock = ReentrantLock()

    fun setHandler(handler: Handler) {
        this.processHandler = handler
    }

    private fun showMsg(msg: String) {
        try {
            if (context != null)
                handler.post {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
        } catch (ex: Exception) {
        }
    }

    //尝试退出命令行程序
    public fun tryExit() {
        try {
            if (out != null)
                out!!.close()
            out = null
        } catch (ex: Exception) {
        }
        out = null
        try {
            p!!.destroy()
        } catch (ex: Exception) {
        }
        p = null
    }

    //获取ROOT超时时间
    private val GET_ROOT_TIMEOUT = 20000L
    private var threadStarted = false
    private var cmdsCache = StringBuilder()

    private fun getRuntimeShell(cmd: String?, error: Runnable?) {
        if (threadStarted) {
            cmdsCache.append(cmd)
            cmdsCache.append("\n\n")
            return
        }
        val thread = Thread(Runnable {
            try {
                tryExit()
                p = if (rootMode) ShellExecutor.getSuperUserRuntime() else ShellExecutor.getRuntime()

                if (processHandler != null) {
                    processHandler!!.sendMessage(processHandler!!.obtainMessage(PROCESS_EVENT_STAR))
                }
                if (p != null) {
                    Thread(Runnable {
                        val bufferedreader = BufferedReader(InputStreamReader(p!!.inputStream))
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
                        val bufferedreader = BufferedReader(InputStreamReader(p!!.errorStream))
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
                out = p!!.outputStream.bufferedWriter()
                if (out == null) {
                    error?.run()
                } else if (cmd != null) {
                    out!!.write(cmd)
                    out!!.write("\n\n")
                    if (cmdsCache.isNotEmpty()) {
                        out!!.write(cmdsCache.toString())
                        cmdsCache = StringBuilder()
                    }
                    out!!.flush()
                }
            } catch (e: Exception) {
                if (out == null) {
                    error?.run()
                } else {
                    showMsg("获取ROOT权限失败！")
                }
            } finally {
                threadStarted = false
            }
        })
        thread.start()
        threadStarted = true
        handler.postDelayed({
            if (p == null && thread.isAlive && !thread.isInterrupted) {
                thread.interrupt()
                tryExit()
                if (error != null) {
                    error.run()
                } else {
                    showMsg("获取Root权限超时！")
                }
                threadStarted = false
            }
        }, GET_ROOT_TIMEOUT)
    }

    //执行脚本
    public fun doCmd(cmd: String, isRedo: Boolean = false) {
        try {
            //tryExit()
            if (p == null || isRedo || out == null) {
                getRuntimeShell(cmd, Runnable {
                    //重试一次
                    if (!isRedo)
                        doCmd(cmd, true)
                    else
                        showMsg("Failed execution action!\nError message : Unable to obtain Root permissions\n\n\ncommand : \r\n$cmd")
                })
            } else {
                out!!.write(cmd)
                out!!.write("\n\n")
                out!!.flush()
            }
        } catch (e: IOException) {
            //重试一次
            if (!isRedo)
                doCmd(cmd, true)
            else
                showMsg("Failed execution action!\nError message : " + e.message + "\n\n\ncommand : \r\n" + cmd)
        }
    }
}
