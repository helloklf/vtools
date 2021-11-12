package com.omarea.library.shell

import android.content.Context
import android.util.Log
import com.omarea.common.shell.KeepShellPublic.doCmdSync
import com.omarea.common.shell.KernelProrp.getProp
import com.omarea.model.ProcessInfo
import com.omarea.shell_utils.ToyboxIntaller
import java.util.*

/*
* 进程管理相关
*/
class ProcessUtils(private val context: Context) {
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
    // pageSize 获取 : getconf PAGESIZE

    private val listCmd: TripleCacheValue = object : TripleCacheValue(context, "ProcessUtilsList") {
        override fun initValue(): String {
            val outsideToybox = ToyboxIntaller(context).install()
            val perfectCmd = "top -o %CPU,RES,SWAP,NAME,PID,USER,COMMAND,CMDLINE -q -b -n 1 -m 65535"
            val outsidePerfectCmd = "$outsideToybox $perfectCmd"
            // String insideCmd = "ps -e -o %CPU,RSS,SHR,NAME,PID,USER,COMMAND,CMDLINE";
            // String insideCmd = "ps -e -o %CPU,RES,SHR,RSS,NAME,PID,S,USER,COMMAND,CMDLINE";
            val insideCmd = "ps -e -o %CPU,RES,SWAP,NAME,PID,USER,COMMAND,CMDLINE"
            val outsideCmd = "$outsideToybox $insideCmd"
            for (cmd in arrayOf(outsidePerfectCmd, perfectCmd, outsideCmd, insideCmd)) {
                val rows = doCmdSync("$cmd 2>&1").split("\n".toRegex()).toTypedArray()
                val result = rows[0]
                if (rows.size > 10 && !(result.contains("bad -o") || result.contains("Unknown option") || result.contains("bad"))) {
                    return cmd
                }
            }
            return ""
        }
    }
    private val detailCmd: TripleCacheValue = object : TripleCacheValue(context, "ProcessUtilsDetail") {
        override fun initValue(): String {
            val outsideToybox = ToyboxIntaller(context).install()
            val perfectCmd = "top -o %CPU,RES,SWAP,NAME,PID,USER,COMMAND,CMDLINE -q -b -n 1 -m 65535"
            val outsidePerfectCmd = "$outsideToybox $perfectCmd"
            // String insideCmd = "ps -e -o %CPU,RSS,SHR,NAME,PID,USER,COMMAND,CMDLINE";
            // String insideCmd = "ps -e -o %CPU,RES,SHR,RSS,NAME,PID,S,USER,COMMAND,CMDLINE";
            val insideCmd = "ps -e -o %CPU,RES,SWAP,NAME,PID,USER,COMMAND,CMDLINE"
            val outsideCmd = "$outsideToybox $insideCmd"
            for (cmd in arrayOf(outsideCmd, insideCmd)) {
                val rows = doCmdSync("$cmd 2>&1").split("\n".toRegex()).toTypedArray()
                val result = rows[0]
                if (rows.size > 10 && !(result.contains("bad -o") || result.contains("Unknown option") || result.contains("bad"))) {
                    return "$cmd --pid "
                }
            }
            return ""
        }
    }

    // 兼容性检查
    fun supported(): Boolean {
        return !(listCmd.toString().isEmpty() || detailCmd.toString().isEmpty())
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
        if (columns.size >= 6) {
            try {
                val processInfo = ProcessInfo()
                processInfo.cpu = columns[0].toFloat()
                processInfo.res = str2Long(columns[1])
                processInfo.swap = str2Long(columns[2])
                processInfo.name = columns[3]
                if (excludeProcess.contains(processInfo.name)) {
                    return null
                }
                processInfo.pid = columns[4].toInt()
                processInfo.user = columns[5]
                processInfo.command = columns[6]
                processInfo.cmdline = row.substring(row.indexOf(processInfo.command) + processInfo.command.length).trim { it <= ' ' }
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
            val cmd = this.listCmd.toString()
            if (cmd.isNotEmpty()) {
                val skipRows = if (cmd.startsWith("ps")) 1 else 0
                val rows = doCmdSync(cmd).split("\n".toRegex()).toTypedArray()
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

    // 获取进程详情
    fun getProcessDetail(pid: Int): ProcessInfo? {
        val cmd = this.detailCmd.toString()
        if (cmd.isNotEmpty()) {
            val r = doCmdSync(cmd + pid)
            Log.d("Scene-SWAP", cmd + pid)
            Log.d("Scene-SWAP", "" + r)
            val rows = r.split("\n".toRegex()).toTypedArray()
            if (rows.size > 1) {
                val row = readRow(rows[1].trim { it <= ' ' })
                if (row != null) {
                    row.cpuSet = getProp("/proc/$pid/cpuset")
                    row.cGroup = getProp("/proc/$pid/cgroup")
                    row.oomAdj = getProp("/proc/$pid/oom_adj")
                    row.oomScore = getProp("/proc/$pid/oom_score")
                    row.oomScoreAdj = getProp("/proc/$pid/oom_score_adj")
                }
                return row
            }
        }
        return null
    }

    // 强制结束进程
    fun killProcess(pid: Int) {
        doCmdSync("kill -9 $pid")
    }

    private val androidProcessRegex = Regex(".*\\..*")
    private fun isAndroidProcess(processInfo: ProcessInfo): Boolean {
        return processInfo.command.contains("app_process") && processInfo.name.matches(androidProcessRegex)
    }

    // 强制结束进程
    fun killProcess(processInfo: ProcessInfo) {
        if (isAndroidProcess(processInfo)) {
            val packageName = if (processInfo.name.contains(":")) processInfo.name.substring(0, processInfo.name.indexOf(":")) else processInfo.name
            doCmdSync(String.format("killall -9 %s;am force-stop %s;am kill %s", packageName, packageName, packageName))
        } else {
            killProcess(processInfo.pid)
        }
    }
}