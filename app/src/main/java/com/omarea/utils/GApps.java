package com.omarea.utils;

import com.omarea.common.shell.KeepShell;

public class GApps {
    private String apps = "com.google.android.gsf\n" +
            "com.google.android.gsf.login\n" +
            "com.google.android.gms\n" +
            "com.android.vending\n" +
            "com.google.android.play.games\n" +
            "com.google.android.syncadapters.contacts\n";

    public void enable(KeepShell keepShell) {
        keepShell.doCmdSync("apps=\"" + apps + "\"\n" +
                "for app in $apps; do\n" +
                "    pm enable $app 2> /dev/null\n" +
                "    pm unsuspend $app 2> /dev/null\n" +
                "done");
    }

    public void disable(KeepShell keepShell) {
        keepShell.doCmdSync("apps=\"" + apps + "\"\n" +
                "for app in $apps; do\n" +
                "    pm suspend $app 2> /dev/null || pm disable $app 2> /dev/null\n" +
                "done");
    }
}
