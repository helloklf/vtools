package com.omarea.shell_utils;

import android.util.Log;

import com.omarea.common.shell.KeepShellPublic;
import com.omarea.model.ProcessDetail;
import com.omarea.model.ProcessInfo;

import java.util.ArrayList;

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

    private final String PS_COMMAND = "ps -e -o %CPU,RSS,NAME,PID,USER";
    private final String PS_DETAIL_COMMAND = "ps -e -o %CPU,RSS,NAME,PID,USER,COMMAND";

    // 兼容性检查
    public boolean supported() {
        String result = KeepShellPublic.INSTANCE.doCmdSync(PS_COMMAND + " 2>&1");
        return !(result.contains("bad -o") || result.contains("Unknown option"));
    }

    // 解析单行数据
    private ProcessInfo readRow(String row) {
        String[] columns = row.split(" +");
        if (columns.length == 5) {
            try {
                ProcessInfo processInfo = new ProcessInfo();
                processInfo.cpu = Float.parseFloat(columns[0]);
                processInfo.rss = Integer.parseInt(columns[1]);
                processInfo.name = columns[2];
                processInfo.pid = Integer.parseInt(columns[3]);
                processInfo.user = columns[4];
                return processInfo;
            } catch (Exception ex) {
                Log.e("Scene-Process", "" + ex.getMessage());
            }
        } else {
            Log.e("Scene-Process", "" + row);
        }
        return null;
    }

    // 获取所有进程
    public ArrayList<ProcessInfo> getAllProcess() {
        ArrayList<ProcessInfo> processInfoList = new ArrayList<>();
        boolean isFristRow = true;
        for (String row : KeepShellPublic.INSTANCE.doCmdSync(PS_COMMAND).split("\n")) {
            if (isFristRow) {
                isFristRow = false;
                continue;
            }

            ProcessInfo processInfo = readRow(row.trim());
            if (processInfo != null) {
                processInfoList.add(processInfo);
            }
        }

        // Log.d("Scene-Process", "" + processInfoList.size());
        return processInfoList;
    }

    // 获取进程详情
    public ProcessDetail getProcessDetail(int pid) {
        Log.d("Scene-Process", PS_DETAIL_COMMAND + " --pid " + pid);

        String[] rows = KeepShellPublic.INSTANCE.doCmdSync(PS_DETAIL_COMMAND + " --pid " + pid).split("\n");
        if (rows.length > 1) {
            String row = rows[1].trim();
            String[] columns = row.split(" +");
            if (columns.length == 6) {
                try {
                    ProcessDetail processInfo = new ProcessDetail();
                    processInfo.cpu = Float.parseFloat(columns[0]);
                    processInfo.rss = Integer.parseInt(columns[1]);
                    processInfo.name = columns[2];
                    processInfo.pid = Integer.parseInt(columns[3]);
                    processInfo.user = columns[4];
                    processInfo.command = columns[5];
                    return processInfo;
                } catch (Exception ex) {
                    Log.e("Scene-Process", "" + ex.getMessage());
                }
            } else {
                Log.e("Scene-Process", "" + row);
            }
        }
        return null;
    }

    // 强制结束进程
    public void killProcess(int pid) {
        KeepShellPublic.INSTANCE.doCmdSync("kill -9 " + pid);
    }
}
