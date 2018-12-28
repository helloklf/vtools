package com.omarea.shell.cpucontrol;

import com.omarea.shell.KernelProrp;
import com.omarea.shell.RootFile;

public class GpuUtils {
    public static String getGpuFreq() {
        String freq = KernelProrp.INSTANCE.getProp("/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq");
        if (freq.length() > 6) {
            return freq.substring(0, freq.length() - 6);
        }
        return freq;
    }

    private static final String GPU_LOAD_PATH_1 = "/sys/class/kgsl/kgsl-3d0/devfreq/gpu_load";
    private static final String GPU_LOAD_PATH_2 = "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage";
    private static String gpuLoadPath = null;

    public static int getGpuLoad() {
        if (gpuLoadPath == null) {
            if (RootFile.INSTANCE.fileExists(GPU_LOAD_PATH_1)){
                gpuLoadPath = GPU_LOAD_PATH_1;
            } else if (RootFile.INSTANCE.fileExists(GPU_LOAD_PATH_2)){
                gpuLoadPath = GPU_LOAD_PATH_2;
            } else {
                gpuLoadPath = "";
            }
        } else if (gpuLoadPath.equals("")) {
            return -1;
        }
        String load = KernelProrp.INSTANCE.getProp(gpuLoadPath);
        try {
            return Integer.parseInt(load.replace("%", "").trim());
        } catch (Exception ex) {
            return -1;
        }
    }
}
