package com.omarea.shell;

public class AppErrorLogcat {
    public String catLogInfo() {
        return KeepShellPublic.INSTANCE.doCmdSync("logcat -d *:E | grep com.omarea.vtools");
    }
}
