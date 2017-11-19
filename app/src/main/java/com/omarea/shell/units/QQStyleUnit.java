package com.omarea.shell.units;

import com.omarea.shell.SysUtils;

import java.util.ArrayList;

/**
 * Created by Hello on 2017/11/01.
 */

public class QQStyleUnit {
    public boolean DisableQQStyle() {
        ArrayList<String> commands = new ArrayList<String>(){{
            add("rm -rf /storage/emulated/0/tencent/MobileQQ/.font_info\n" +
                    "echo \"\" > /storage/emulated/0/tencent/MobileQQ/.font_info\n" +
                    "rm -rf /storage/emulated/0/tencent/MobileQQ/font_info\n" +
                    "echo \"\" > /storage/emulated/0/tencent/MobileQQ/font_info\n" +
                    "rm -rf /sdcard/tencent/MobileQQ/.font_info\n" +
                    "echo \"\" > /sdcard/tencent/MobileQQ/.font_info\n" +
                    "rm -rf /sdcard/tencent/MobileQQ/font_info\n" +
                    "echo \"\" > /sdcard/tencent/MobileQQ/font_info\n" +
                    "rm -rf /data/data/com.tencent.mobileqq/files/bubble_info\n" +
                    "echo \"\" > /data/data/com.tencent.mobileqq/files/bubble_info\n" +
                    "rm -rf /data/data/com.tencent.mobileqq/files/pendant_info\n" +
                    "echo \"\" > /data/data/com.tencent.mobileqq/files/pendant_info\n" +
                    "rm -rf /storage/emulated/0/tencent/MobileQQ/.pendant\n" +
                    "echo \"\" > /storage/emulated/0/tencent/MobileQQ/.pendant\n" +
                    "rm -rf /sdcard/tencent/MobileQQ/.pendant\n" +
                    "echo \"\" > /sdcard/tencent/MobileQQ/.pendant\n" +
                    "pgrep com.tencent.mobileqq |xargs kill -9\n");
        }};

        return SysUtils.executeRootCommand(commands);
    }

    public boolean RestoreQQStyle() {
        ArrayList<String> commands = new ArrayList<String>(){{
            add("rm -rf /storage/emulated/0/tencent/MobileQQ/font_info\n" +
                    "rm -rf /sdcard/tencent/MobileQQ/font_info\n" +
                    "rm -rf /storage/emulated/0/tencent/MobileQQ/.font_info\n" +
                    "rm -rf /sdcard/tencent/MobileQQ/.font_info\n" +
                    "rm -rf /storage/emulated/0/tencent/MobileQQ/.pendant\n" +
                    "rm -rf /sdcard/tencent/MobileQQ/.pendant\n" +
                    "rm -rf /data/data/com.tencent.mobileqq/files/bubble_info\n" +
                    "rm -rf /data/data/com.tencent.mobileqq/files/pendant_info\n" +
                    "pgrep com.tencent.mobileqq |xargs kill -9\n");
        }};

        return SysUtils.executeRootCommand(commands);
    }
}
