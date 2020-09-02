package com.omarea.library.shell;

import com.omarea.common.shell.KeepShell;

public class GAppsUtilis {
    public void enable(KeepShell keepShell) {
        keepShell.doCmdSync(
                "pm enable com.google.android.gsf 2> /dev/null\n" +
                        "pm enable com.google.android.gsf.login 2> /dev/null\n" +
                        "pm enable com.google.android.gms 2> /dev/null\n" +
                        "pm enable com.android.vending 2> /dev/null\n" +
                        "pm enable com.google.android.play.games 2> /dev/null\n" +
                        "pm enable com.google.android.syncadapters.contacts 2> /dev/null");
    }

    public void disable(KeepShell keepShell) {
        keepShell.doCmdSync("pm disable com.google.android.gsf 2> /dev/null\n" +
                "pm disable com.google.android.gsf.login 2> /dev/null\n" +
                "pm disable com.google.android.gms 2> /dev/null\n" +
                "pm disable com.android.vending 2> /dev/null\n" +
                "pm disable com.google.android.play.games 2> /dev/null\n" +
                "pm disable com.google.android.syncadapters.contacts 2> /dev/null");
    }
}
