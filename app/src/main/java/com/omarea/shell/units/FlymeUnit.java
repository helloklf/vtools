package com.omarea.shell.units;

import com.omarea.shell.SysUtils;

import java.util.ArrayList;

/**
 * Created by Hello on 2017/11/01.
 */

public class FlymeUnit {
    public boolean StaticBlur() {
        ArrayList<String> commands = new ArrayList<String>() {{
            add("setprop persist.sys.static_blur_mode true");
        }};
        return SysUtils.executeRootCommand(commands);
    }

    public boolean DisableMtkLog() {
        ArrayList<String> commands = new ArrayList<String>() {{
            add("rm -rf /sdcard/mtklog");
            add("echo 0 > /sdcard/mtklog");
        }};
        return SysUtils.executeRootCommand(commands);
    }
}
