package com.omarea.shell.cpucontrol;

import com.omarea.shell.KernelProrp;

public class GpuUtils {
    public static String getGpuFreq() {
        String freq = KernelProrp.INSTANCE.getProp("/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq");
        if (freq.length() > 6) {
            return freq.substring(0, freq.length() - 6);
        }
        return freq;
    }
    public static int getGpuLoad() {
        String load = KernelProrp.INSTANCE.getProp("/sys/class/kgsl/kgsl-3d0/devfreq/gpu_load");
        try {
            return Integer.parseInt(load);
        } catch (Exception ex) {
            return -1;
        }
    }
}
