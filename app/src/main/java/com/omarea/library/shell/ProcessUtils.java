package com.omarea.library.shell;

import android.content.Context;
import android.util.Log;

import com.omarea.common.shell.KeepShellPublic;
import com.omarea.common.shell.KernelProrp;
import com.omarea.model.ProcessInfo;
import com.omarea.shell_utils.ToyboxIntaller;

import java.util.ArrayList;

/*
 * 进程管理相关
 */
public class ProcessUtils {

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

    private static String LIST_COMMAND = null;
    private static String DETAIL_COMMAND = null;

    // 兼容性检查
    public boolean supported(Context context) {
        if (LIST_COMMAND == null || DETAIL_COMMAND == null) {
            LIST_COMMAND = "";
            DETAIL_COMMAND = "";

            String outsideToybox = new ToyboxIntaller(context).install();

            String perfectCmd = "top -o %CPU,RES,SWAP,NAME,PID,USER,COMMAND,CMDLINE -q -b -n 1 -m 65535";
            String outsidePerfectCmd = outsideToybox + " " + perfectCmd;
            // String insideCmd = "ps -e -o %CPU,RSS,SHR,NAME,PID,USER,COMMAND,CMDLINE";
            // String insideCmd = "ps -e -o %CPU,RES,SHR,RSS,NAME,PID,S,USER,COMMAND,CMDLINE";
            String insideCmd = "ps -e -o %CPU,RES,SWAP,NAME,PID,USER,COMMAND,CMDLINE";
            String outsideCmd = outsideToybox + " " + insideCmd;

            for (String cmd : new String[]{ outsidePerfectCmd, perfectCmd, outsideCmd,insideCmd }) {
                String[] rows = KeepShellPublic.INSTANCE.doCmdSync(cmd + " 2>&1").split("\n");
                String result = rows[0];
                if (rows.length > 10 && !(result.contains("bad -o") || result.contains("Unknown option") || result.contains("bad"))) {
                    LIST_COMMAND = cmd;
                    break;
                }
            }

            for (String cmd : new String[]{outsideCmd, insideCmd}) {
                String[] rows = KeepShellPublic.INSTANCE.doCmdSync(cmd + " 2>&1").split("\n");
                String result = rows[0];
                if (rows.length > 10 && !(result.contains("bad -o") || result.contains("Unknown option") || result.contains("bad"))) {
                    DETAIL_COMMAND = cmd + " --pid ";
                    break;
                }
            }
        }

        return !(LIST_COMMAND.isEmpty() || DETAIL_COMMAND.isEmpty());
    }

    private long str2Long(String str) {
        if (str.contains("K")) {
            return (long)Double.parseDouble(str.substring(0, str.indexOf("K")));
        } else if (str.contains("M")) {
            return (long)(Double.parseDouble(str.substring(0, str.indexOf("M"))) * 1024);
        } else if (str.contains("G")) {
            return (long)(Double.parseDouble(str.substring(0, str.indexOf("G"))) * 1048576);
        } else {
            return Long.parseLong(str) / 1024;
        }
    }

    // 从进程列表排除的应用
    private final ArrayList<String> excludeProcess = new ArrayList<String>() {
        {
            add("toybox-outside");
            add("toybox-outside64");
            add("ps");
            add("top");
            add("com.omarea.vtools");
        }
    };

    // 解析单行数据
    private ProcessInfo readRow(String row) {
        String[] columns = row.split(" +");
        if (columns.length >= 6) {
            try {
                ProcessInfo processInfo = new ProcessInfo();
                processInfo.cpu = Float.parseFloat(columns[0]);
                processInfo.res = str2Long(columns[1]);
                processInfo.swap = str2Long(columns[2]);
                processInfo.name = columns[3];

                if (excludeProcess.contains(processInfo.name)) {
                    return null;
                }

                processInfo.pid = Integer.parseInt(columns[4]);
                processInfo.user = columns[5];
                processInfo.command = columns[6];
                processInfo.cmdline = row.substring(row.indexOf(processInfo.command) + processInfo.command.length()).trim();
                return processInfo;
            } catch (Exception ex) {
                // Log.e("Scene-ProcessUtils", "" + ex.getMessage() + " -> " + row);
            }
        } else {
            // Log.e("Scene-ProcessUtils", "" + row);
        }
        return null;
    }

    // 获取所有进程
    public ArrayList<ProcessInfo> getAllProcess() {
        ArrayList<ProcessInfo> processInfoList = new ArrayList<>();
        boolean isFristRow = true;
        if (LIST_COMMAND != null) {
            String[] rows = KeepShellPublic.INSTANCE.doCmdSync(LIST_COMMAND).split("\n");
            for (String row : rows) {
                if (isFristRow) {
                    isFristRow = false;
                    continue;
                }

                ProcessInfo processInfo = readRow(row.trim());
                if (processInfo != null) {
                    processInfoList.add(processInfo);
                }
            }
        }
        return processInfoList;
    }

    // 获取进程详情
    public ProcessInfo getProcessDetail(int pid) {
        if (DETAIL_COMMAND != null) {
            String r = KeepShellPublic.INSTANCE.doCmdSync(DETAIL_COMMAND + pid);
            Log.d("Scene-SWAP", DETAIL_COMMAND + pid);
            Log.d("Scene-SWAP", "" + r);
            String[] rows = r.split("\n");
            if (rows.length > 1) {
                ProcessInfo row = readRow(rows[1].trim());
                if (row != null) {
                    row.cpuSet = KernelProrp.INSTANCE.getProp("/proc/" + pid + "/cpuset");
                    row.cGroup = KernelProrp.INSTANCE.getProp("/proc/" + pid + "/cgroup");
                    row.oomAdj = KernelProrp.INSTANCE.getProp("/proc/" + pid + "/oom_adj");
                    row.oomScore = KernelProrp.INSTANCE.getProp("/proc/" + pid + "/oom_score");
                    row.oomScoreAdj = KernelProrp.INSTANCE.getProp("/proc/" + pid + "/oom_score_adj");
                }
                return row;
            }
        }
        return null;
    }

    // 强制结束进程
    public void killProcess(int pid) {
        KeepShellPublic.INSTANCE.doCmdSync("kill -9 " + pid);
    }

    private boolean isAndroidProcess(ProcessInfo processInfo) {
        return (processInfo.command.contains("app_process") && processInfo.name.matches(".*\\..*"));
    }


    // 强制结束进程
    public void killProcess(ProcessInfo processInfo) {
        if (isAndroidProcess(processInfo)) {
            String packageName = processInfo.name.contains(":") ? processInfo.name.substring(0, processInfo.name.indexOf(":")) : processInfo.name;
            KeepShellPublic.INSTANCE.doCmdSync(String.format("killall -9 %s;am force-stop %s;am kill %s", packageName, packageName, packageName));
        } else {
            killProcess(processInfo.pid);
        }
    }
}
