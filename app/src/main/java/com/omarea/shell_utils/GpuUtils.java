package com.omarea.shell_utils;

import com.omarea.common.shell.KernelProrp;
import com.omarea.common.shell.RootFile;

public class GpuUtils {
    private static String GPU_LOAD_PATH = null;
    private static String GPU_FREQ_PATH = null;

    public static String getGpuFreq() {
        if (GPU_FREQ_PATH == null) {
            String path1 = "/sys/kernel/gpu/gpu_clock";
            String path2 = "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq";
            if (RootFile.INSTANCE.fileExists(path1)) {
                GPU_FREQ_PATH = path1;
            } else if (RootFile.INSTANCE.fileExists(path2)) {
                GPU_FREQ_PATH = path2;
            } else {
                GPU_FREQ_PATH = "";
            }
        }

        if (GPU_FREQ_PATH.isEmpty()) {
            return "";
        } else {
            String freq = KernelProrp.INSTANCE.getProp(GPU_FREQ_PATH);
            if (freq.length() > 6) {
                return freq.substring(0, freq.length() - 6);
            }
            return freq;
        }
    }

    public static int getGpuLoad() {
        if (GPU_LOAD_PATH == null) {
            String path1 = "/sys/kernel/gpu/gpu_busy";
            String path2 = "/sys/class/kgsl/kgsl-3d0/devfreq/gpu_load";
            String path3 = "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage";
            if (RootFile.INSTANCE.fileExists(path1)) {
                GPU_LOAD_PATH = path1;
            } else if (RootFile.INSTANCE.fileExists(path2)) {
                GPU_LOAD_PATH = path2;
            } else if (RootFile.INSTANCE.fileExists(path3)) {
                GPU_LOAD_PATH = path3;
            } else {
                GPU_LOAD_PATH = "";
            }
        }

        if (GPU_LOAD_PATH.equals("")) {
            return -1;
        } else {
            String load = KernelProrp.INSTANCE.getProp(GPU_LOAD_PATH);
            try {
                return Integer.parseInt(load.replace("%", "").trim());
            } catch (Exception ex) {
                return -1;
            }
        }
    }
}
