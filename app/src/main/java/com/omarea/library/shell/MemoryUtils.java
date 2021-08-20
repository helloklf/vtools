package com.omarea.library.shell;

import com.omarea.common.shell.KernelProrp;

public class MemoryUtils {
    public static class MemoryInfo {
        public int memTotal;
        public int memAvailable;
        public int swapCached;
        public int swapTotal;
        public int swapFree;
        public int buffers;
        public int dirty;
    }

    // 提取 /proc/meminfo 里某一行的数值，
    // 例： [MemFree:          138828 kB] => [138828]
    private static int getMemInfoRowKB(String row) {
        return Integer.parseInt(
                row.substring(
                        row.indexOf(":") + 1,
                        row.lastIndexOf(" ")
                ).trim()
        );
    }

    public MemoryInfo getMemoryInfo() {
        String[] memInfos = KernelProrp.INSTANCE.getProp("/proc/meminfo").split("\n");
        final MemoryInfo memInfo = new MemoryInfo();
        for (String row : memInfos) {
            if (row.startsWith("MemTotal")) {
                memInfo.memTotal = getMemInfoRowKB(row);
            } else if (row.startsWith("MemAvailable")) {
                memInfo.memAvailable = getMemInfoRowKB(row);
            } else if (row.startsWith("SwapCached")) {
                memInfo.swapCached = getMemInfoRowKB(row);
            } else if (row.startsWith("SwapTotal")) {
                memInfo.swapTotal = getMemInfoRowKB(row);
            } else if (row.startsWith("SwapFree")) {
                memInfo.swapFree = getMemInfoRowKB(row);
            } else if (row.startsWith("Buffers")) {
                memInfo.buffers = getMemInfoRowKB(row);
            } else if (row.startsWith("Dirty")) {
                memInfo.dirty = getMemInfoRowKB(row);
            }
        }
        return memInfo;
    }
}
