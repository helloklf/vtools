package com.omarea.shell_utils;

import android.util.Log;

import com.omarea.common.shell.KeepShellPublic;
import com.omarea.model.ProcessInfo;

import java.util.ArrayList;

public class ProcessUtils {
    private final String PS_COMMAND = "ps -e -o %CPU,RSS,NAME,PID,USER";

    // 兼容性检查
    public boolean supported() {
        String result = KeepShellPublic.INSTANCE.doCmdSync(PS_COMMAND);
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

        Log.d("Scene-Process", "" + processInfoList.size());
        return processInfoList;
    }
}
