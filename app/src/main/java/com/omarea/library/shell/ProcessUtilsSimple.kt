package com.omarea.library.shell

import android.content.Context
import com.omarea.common.shell.KeepShellPublic
import com.omarea.model.ProcessInfo
import com.omarea.model.ThreadInfo
import com.omarea.shell_utils.ToyboxIntaller
import java.util.*

/*
* 进程管理相关
*/
class ProcessUtilsSimple(private val context: Context) {
    /*
    VSS- Virtual Set Size 虚拟耗用内存（包含共享库占用的内存）
    RSS- Resident Set Size 实际使用物理内存（包含共享库占用的内存）
    PSS- Proportional Set Size 实际使用的物理内存（比例分配共享库占用的内存）
    USS- Unique Set Size 进程独自占用的物理内存（不包含共享库占用的内存）
    一般来说内存占用大小有如下规律：VSS >= RSS >= PSS >= USS
    ————————————————
    版权声明：本文为CSDN博主「火山石」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
    原文链接：https://blog.csdn.net/zhangcanyan/java/article/details/84556808
    */
    private val psCommand = object : TripleCacheValue(context, "ProcessUtils2CMD") {
        override fun initValue(): String {
            val outsideToybox = ToyboxIntaller(context).install()
            val perfectCmd = "top -o %CPU,NAME,COMMAND,PID -q -b -n 1 -m 65535"
            val outsidePerfectCmd = "$outsideToybox $perfectCmd"
            val insideCmd = "ps -e -o %CPU,NAME,COMMAND,PID"
            val outsideCmd = "$outsideToybox $insideCmd"
            for (cmd in arrayOf(outsidePerfectCmd, perfectCmd, outsideCmd, insideCmd)) {
                val rows = KeepShellPublic.doCmdSync("$cmd 2>&1").split("\n".toRegex()).toTypedArray()
                val result = rows[0]
                if (rows.size > 10 &&
                        !(
                            result.contains("bad -o") ||
                            result.contains("Unknown option") ||
                            result.contains("bad")
                        )
                ) {
                    return cmd
                }
            }
            return ""
        }
    }

    // 兼容性检查（TODO: 首次调用此函数可能比较耗时，需要调用这做loading优化体验）
    fun supported(): Boolean {
        return this.psCommand.toString().isNotEmpty()
    }

    private fun str2Long(str: String): Long {
        return when {
            str.contains("K") -> {
                str.substring(0, str.indexOf("K")).toDouble().toLong()
            }
            str.contains("M") -> {
                (str.substring(0, str.indexOf("M")).toDouble() * 1024).toLong()
            }
            str.contains("G") -> {
                (str.substring(0, str.indexOf("G")).toDouble() * 1048576).toLong()
            }
            else -> {
                str.toLong() / 1024
            }
        }
    }

    // 从进程列表排除的应用
    private val excludeProcess: ArrayList<String> = object : ArrayList<String>() {
        init {
            add("toybox-outside")
            add("toybox-outside64")
            add("ps")
            add("top")
            add("com.omarea.vtools")
        }
    }

    // 解析单行数据
    private fun readRow(row: String): ProcessInfo? {
        val columns = row.split(" +".toRegex()).toTypedArray()
        if (columns.size >= 3) {
            try {
                val processInfo = ProcessInfo()
                processInfo.cpu = columns[0].toFloat()
                processInfo.name = columns[1]
                if (excludeProcess.contains(processInfo.name)) {
                    return null
                }
                processInfo.command = columns[2]
                processInfo.pid = columns[3].toInt()
                return processInfo
            } catch (ex: Exception) {
                // Log.e("Scene-ProcessUtils", "" + ex.getMessage() + " -> " + row);
            }
        } else {
            // Log.e("Scene-ProcessUtils", "" + row);
        }
        return null
    }

    // 获取所有进程
    val allProcess: ArrayList<ProcessInfo>
        get() {
            val processInfoList = ArrayList<ProcessInfo>()
            val psCommand = this.psCommand.toString()
            if (psCommand.isNotEmpty()) {
                val skipRows = if (psCommand.startsWith("ps")) 1 else 0
                val rows = KeepShellPublic.doCmdSync(psCommand).split("\n".toRegex()).toTypedArray()
                var index = 0
                for (row in rows) {
                    if (index < skipRows) {
                        continue
                    }
                    index ++
                    val processInfo = readRow(row.trim { it <= ' ' })
                    if (processInfo != null) {
                        processInfoList.add(processInfo)
                    }
                }
            }
            return processInfoList
        }

    // 强制结束进程
    private fun killProcess(pid: Int) {
        KeepShellPublic.doCmdSync("kill -9 $pid")
    }

    private val androidProcessRegex = Regex(".*\\..*")
    private fun isAndroidProcess(processInfo: ProcessInfo): Boolean {
        return processInfo.command.contains("app_process") && processInfo.name.matches(androidProcessRegex)
    }

    // 获取安卓应用主进程PID
    fun getAppMainProcess(packageName: String?): Int {
        val pid = KeepShellPublic.doCmdSync(
            String.format("ps -ef -o PID,NAME | grep -e %s$ | egrep -o '[0-9]{1,}' | head -n 1", packageName)
        )
        return if (pid.isEmpty() || pid == "error") {
            -1
        } else pid.toInt()
    }

    // 强制结束进程
    fun killProcess(processInfo: ProcessInfo) {
        if (isAndroidProcess(processInfo)) {
            val packageName = if (processInfo.name.contains(":")) {
                processInfo.name.substring(0, processInfo.name.indexOf(":"))
            } else {
                processInfo.name
            }
            KeepShellPublic.doCmdSync(
                String.format("killall -9 %s;am force-stop %s;am kill %s", packageName, packageName, packageName)
            )
        } else {
            killProcess(processInfo.pid)
        }
    }

    // 获取某个进程的所有线程
    private fun getThreads(pid: Int): String {
        return KeepShellPublic.doCmdSync(
            String.format("top -H -b -q -n 1 -p %d -o TID,%%CPU,CMD", pid)
        )
    }

    // 获取某个进程的所有线程
    fun getThreadLoads(pid: Int): List<ThreadInfo> {
        val result = getThreads(pid).split("\n".toRegex()).toTypedArray()
        val threadData = ArrayList<ThreadInfo>()
        for (row in result) {
            val rowStr = row.trim { it <= ' ' }
            val cols = rowStr.split(" +".toRegex()).toTypedArray()
            if (cols.size > 2) {
                try {
                    val threadInfo: ThreadInfo = object : ThreadInfo() {
                        init {
                            tid = cols[0].toInt()
                            cpuLoad = cols[1].toDouble()
                            name = rowStr.substring(
                                    rowStr.indexOf(cols[1]) + cols[1].length
                            ).trim { it <= ' ' }
                        }
                    }
                    threadData.add(threadInfo)
                } catch (ex: Exception) {
                    // Log.e("Scene-ProcessUtils", "" + ex.getMessage() + " -> " + row);
                }
            } else {
                // Log.e("Scene-ProcessUtils", "" + ex.getMessage() + " -> " + row);
            }
        }
        threadData.sortWith { o1, o2 ->
            val r = o2.cpuLoad - o1.cpuLoad
            if (r > 0) 1 else if (r < 0) -1 else 0
        }
        val count = threadData.size
        /*
           String taskDir = "/proc/" + pid + "/task/";
           for (ThreadInfo threadInfo: top15) {
               threadInfo.name = KernelProrp.INSTANCE.getProp(taskDir + threadInfo.tid + "/comm");
           }
       */
        return threadData.subList(0, count.coerceAtMost(15))
    }
}