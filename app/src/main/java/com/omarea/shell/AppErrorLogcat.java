package com.omarea.shell;

import com.omarea.common.shell.KeepShellPublic;

public class AppErrorLogcat {
    public String catLogInfo() {
        return KeepShellPublic.INSTANCE.doCmdSync("logcat -d *:E | grep com.omarea.vtools");
    }
}
