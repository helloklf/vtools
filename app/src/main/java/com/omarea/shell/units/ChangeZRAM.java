package com.omarea.shell.units;

/**
 * Created by Hello on 2017/11/01.
 */

public class ChangeZRAM {
    /*
    1 -> {
        stringBuilder.append(
                "swapoff /dev/block/zram0\n" +
                        "echo 1 > /sys/block/zram0/reset\n" +
                        "echo 597000000 > /sys/block/zram0/disksize\n" +
                        "mkswap /dev/block/zram0 &> /dev/null\n" +
                        "swapon /dev/block/zram0 &> /dev/null\n" +
                        "echo 100 > /proc/sys/vm/swappiness\n")
    }*/
}
