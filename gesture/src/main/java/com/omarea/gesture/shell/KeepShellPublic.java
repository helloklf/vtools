package com.omarea.gesture.shell;

import java.util.List;

/**
 * Created by Hello on 2018/01/23.
 */
public class KeepShellPublic {
    private static KeepShell keepShell = null;

    public static Boolean doCmdSync(List<String> commands) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String cmd : commands) {
            stringBuilder.append(cmd);
            stringBuilder.append("\n\n");
        }

        return doCmdSync(stringBuilder.toString()) != "error";
    }

    //执行脚本
    public static String doCmdSync(String cmd) {
        if (keepShell == null) {
            keepShell = new KeepShell();
        }
        return keepShell.doCmdSync(cmd);
    }

    //执行脚本
    public static Boolean checkRoot() {
        if (keepShell == null) {
            keepShell = new KeepShell();
        }
        return keepShell.checkRoot();
    }

    public static void tryExit() {
        if (keepShell != null) {
            keepShell.tryExit();
            keepShell = null;
        }
    }
}
