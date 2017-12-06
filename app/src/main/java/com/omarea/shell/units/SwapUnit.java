package com.omarea.shell.units;

import com.omarea.shell.SysUtils;

import java.util.ArrayList;

/**
 * Created by Hello on 2017/11/04.
 */

public class SwapUnit {
    public Boolean zramOn(final int sizeMB) {
        ArrayList<String> commands = new ArrayList<String>() {{
            add("if [ `cat /sys/block/zram0/disksize` != '" + sizeMB + "000000' ] ; then ");
            add("swapoff /dev/block/zram0 >/dev/null 2>&1;");
            add("echo 1 > /sys/block/zram0/reset;");
            add("echo " + sizeMB + "000000 > /sys/block/zram0/disksize;");
            add("mkswap /dev/block/zram0 >/dev/null 2>&1;");
            add("swapon /dev/block/zram0 >/dev/null 2>&1;");
            add("fi;\n");
        }};
        return SysUtils.executeRootCommand(commands);
    }

    public boolean swwapOn(String path, Boolean p) {
        ArrayList<String> commands = new ArrayList<String>();
        if (p) {
            commands.add("swapon " + path + " -p 32767\n");
        } else {
            commands.add("swapon " + path);
        }
        return SysUtils.executeRootCommand(commands);
    }
}
