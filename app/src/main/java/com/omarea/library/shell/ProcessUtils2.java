package com.omarea.library.shell;

import android.content.Context;

import com.omarea.common.shell.KeepShellPublic;
import com.omarea.model.ProcessInfo;
import com.omarea.model.ThreadInfo;
import com.omarea.shell_utils.ToyboxIntaller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
 * 进程管理相关
 */
public class ProcessUtils2 {

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

    private static String PS_COMMAND = null;

    // 兼容性检查（TODO: 首次调用此函数可能比较耗时，需要调用这做loading优化体验）
    public boolean supported(Context context) {
        if (PS_COMMAND == null) {
            PS_COMMAND = "";
            String outsideToybox = new ToyboxIntaller(context).install();

            String perfectCmd = "top -o %CPU,NAME,COMMAND,PID -q -b -n 1 -m 65535";
            String outsidePerfectCmd = outsideToybox + " " + perfectCmd;

            String insideCmd = "ps -e -o %CPU,NAME,COMMAND,PID";
            String outsideCmd = outsideToybox + " " + insideCmd;

            for (String cmd : new String[]{ outsidePerfectCmd, perfectCmd, outsideCmd, insideCmd }) {
                String[] rows = KeepShellPublic.INSTANCE.doCmdSync(cmd + " 2>&1").split("\n");
                String result = rows[0];
                if (rows.length > 10 && !(result.contains("bad -o") || result.contains("Unknown option") || result.contains("bad"))) {
                    PS_COMMAND = cmd;
                    break;
                }
            }
        }

        return !PS_COMMAND.isEmpty();
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
        if (columns.length >= 3) {
            try {
                ProcessInfo processInfo = new ProcessInfo();
                processInfo.cpu = Float.parseFloat(columns[0]);
                processInfo.name = columns[1];

                if (excludeProcess.contains(processInfo.name)) {
                    return null;
                }

                processInfo.command = columns[2];
                processInfo.pid = Integer.parseInt(columns[3]);
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
        if (PS_COMMAND != null) {
            String[] rows = KeepShellPublic.INSTANCE.doCmdSync(PS_COMMAND).split("\n");
            for (String row : rows) {
                if (isFristRow) {
                    isFristRow = false;
                    if (row.trim().contains("CPU") && row.trim().contains("NAME")) {
                        continue;
                    }
                }

                ProcessInfo processInfo = readRow(row.trim());
                if (processInfo != null) {
                    processInfoList.add(processInfo);
                }
            }
        }
        return processInfoList;
    }

    // 强制结束进程
    public void killProcess(int pid) {
        KeepShellPublic.INSTANCE.doCmdSync("kill -9 " + pid);
    }

    private boolean isAndroidProcess(ProcessInfo processInfo) {
        return (processInfo.command.contains("app_process") && processInfo.name.matches(".*\\..*"));
    }

    // 获取安卓应用主进程PID
    public int getAppMainProcess(String packageName) {
        String pid = KeepShellPublic.INSTANCE.doCmdSync(
            String.format("ps -ef -o PID,NAME | grep -e %s$ | egrep -o '[0-9]{1,}' | head -n 1", packageName)
        );
        if (pid.isEmpty() || pid.equals("error")) {
          return -1;
        }
        return Integer.parseInt(pid);
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

    // 获取某个进程的所有线程
    private String getThreads(final int pid) {
        return KeepShellPublic.INSTANCE.doCmdSync(
            String.format("top -H -b -q -n 1 -p %d -o TID,%%CPU,CMD", pid)
        );
    }

    // 获取某个进程的所有线程
    public List<ThreadInfo> getThreadLoads (final int pid) {
        String[] result = getThreads(pid).split("\n");
        ArrayList<ThreadInfo> threadData = new ArrayList<>();
        for (String row : result) {
            final String rowStr = row.trim();
            final String[] cols = rowStr.split(" +");
            if (cols.length > 2) {
                try {
                    ThreadInfo threadInfo = new ThreadInfo(){{
                        tid = Integer.parseInt(cols[0]);
                        cpuLoad = Double.parseDouble(cols[1]);
                        name = rowStr.substring(
                                rowStr.indexOf(cols[1]) + cols[1].length()
                        ).trim();
                    }};
                    threadData.add(threadInfo);
                } catch (Exception ex) {
                    // Log.e("Scene-ProcessUtils", "" + ex.getMessage() + " -> " + row);
                }
            } else {
                // Log.e("Scene-ProcessUtils", "" + ex.getMessage() + " -> " + row);
            }
        }
        Collections.sort(threadData, new Comparator<ThreadInfo>() {
            @Override
            public int compare(ThreadInfo o1, ThreadInfo o2) {
                double r = o2.cpuLoad - o1.cpuLoad;
                return r > 0 ? 1 : (r < 0 ? -1 : 0);
            }
        });
        int count = threadData.size();
        List<ThreadInfo> top15 = threadData.subList(0, Math.min(count, 15));
        /*
        String taskDir = "/proc/" + pid + "/task/";
        for (ThreadInfo threadInfo: top15) {
            threadInfo.name = KernelProrp.INSTANCE.getProp(taskDir + threadInfo.tid + "/comm");
        }
        */

        return top15;
    }
}
