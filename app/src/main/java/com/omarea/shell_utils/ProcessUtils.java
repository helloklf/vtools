package com.omarea.shell_utils;

import android.util.Log;

import com.omarea.common.shell.KeepShellPublic;
import com.omarea.model.ProcessDetail;
import com.omarea.model.ProcessInfo;

import java.util.ArrayList;

public class ProcessUtils {
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
