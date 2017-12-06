package com.omarea.shell.units;

import com.omarea.shell.SysUtils;

import java.util.ArrayList;

/**
 * Created by Hello on 2017/11/01.
 */

public class FullScreenSUnit {
    public boolean FullScreen() {
        ArrayList<String> commands = new ArrayList<String>() {{
            //stringBuilder.append("settings put global policy_control immersive.full=*")
            add("settings put global policy_control immersive.full=apps,-android,-com.android.systemui,-com.tencent.mobileqq,-com.tencent.tim,-com.tencent.mm");
        }};
        return SysUtils.executeRootCommand(commands);
    }

    public boolean ExitFullScreen() {
        ArrayList<String> commands = new ArrayList<String>() {{
            add("settings put global policy_control null");
        }};
        return SysUtils.executeRootCommand(commands);
    }
}
