package com.omarea.shell_utils;

import com.omarea.common.shell.KeepShellPublic;
import com.omarea.common.shell.KernelProrp;
import com.omarea.common.shell.RootFile;
import com.omarea.model.CpuStatus;

import java.io.File;
import java.util.ArrayList;

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
            String path4 = "/sys/class/kgsl/kgsl-3d0/gpuload";
            if (RootFile.INSTANCE.fileExists(path1)) {
                GPU_LOAD_PATH = path1;
            } else if (RootFile.INSTANCE.fileExists(path2)) {
                GPU_LOAD_PATH = path2;
            } else if (RootFile.INSTANCE.fileExists(path3)) {
                GPU_LOAD_PATH = path3;
            } else if (RootFile.INSTANCE.fileExists(path4)) {
                GPU_LOAD_PATH = path4;
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


    public static String[] adrenoGPUFreqs() {
        String freqs = KernelProrp.INSTANCE.getProp("/sys/class/kgsl/kgsl-3d0/devfreq/available_frequencies");
        return freqs.split(" ");
    }

    public static boolean isAdrenoGPU() {
        return new File("/sys/class/kgsl/kgsl-3d0").exists();
    }

    public static String[] getAdrenoGPUGovernors() {
        String g = KernelProrp.INSTANCE.getProp("/sys/class/kgsl/kgsl-3d0/devfreq/available_governors");
        return g.split(" ");
    }

    public static String getAdrenoGPUMinFreq() {
        return KernelProrp.INSTANCE.getProp("/sys/class/kgsl/kgsl-3d0/devfreq/min_freq");
    }

    public static void setAdrenoGPUMinFreq(String value) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 /sys/class/kgsl/kgsl-3d0/devfreq/min_freq;");
        commands.add("echo " + value + " > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq;");
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public static String getAdrenoGPUMaxFreq() {
        return KernelProrp.INSTANCE.getProp("/sys/class/kgsl/kgsl-3d0/devfreq/max_freq");
    }

    public static void setAdrenoGPUMaxFreq(String value) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 /sys/class/kgsl/kgsl-3d0/devfreq/max_freq;");
        commands.add("echo " + value + " > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq;");
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public static String getAdrenoGPUGovernor() {
        return KernelProrp.INSTANCE.getProp("/sys/class/kgsl/kgsl-3d0/devfreq/governor");
    }

    public static void setAdrenoGPUGovernor(String value) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 /sys/class/kgsl/kgsl-3d0/devfreq/governor;");
        commands.add("echo " + value + " > /sys/class/kgsl/kgsl-3d0/devfreq/governor;");
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public static String getAdrenoGPUMinPowerLevel() {
        return KernelProrp.INSTANCE.getProp("/sys/class/kgsl/kgsl-3d0/min_pwrlevel");
    }

    public static void setAdrenoGPUMinPowerLevel(String value) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 /sys/class/kgsl/kgsl-3d0/min_pwrlevel;");
        commands.add("echo " + value + " > /sys/class/kgsl/kgsl-3d0/min_pwrlevel;");
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public static String getAdrenoGPUMaxPowerLevel() {
        return KernelProrp.INSTANCE.getProp("/sys/class/kgsl/kgsl-3d0/max_pwrlevel");
    }

    public static void setAdrenoGPUMaxPowerLevel(String value) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 /sys/class/kgsl/kgsl-3d0/max_pwrlevel;");
        commands.add("echo " + value + " > /sys/class/kgsl/kgsl-3d0/max_pwrlevel;");
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public static String getAdrenoGPUDefaultPowerLevel() {
        return KernelProrp.INSTANCE.getProp("/sys/class/kgsl/kgsl-3d0/default_pwrlevel");
    }

    public static void setAdrenoGPUDefaultPowerLevel(String value) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 /sys/class/kgsl/kgsl-3d0/default_pwrlevel;");
        commands.add("echo " + value + " > /sys/class/kgsl/kgsl-3d0/default_pwrlevel;");
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public static String[] getAdrenoGPUPowerLevels() {
        String leves = KernelProrp.INSTANCE.getProp("/sys/class/kgsl/kgsl-3d0/num_pwrlevels");
        try {
            int max = Integer.parseInt(leves);
            ArrayList<String> arr = new ArrayList<>();
            for (int i = 0; i < max; i++) {
                arr.add("" + i);
            }
            return arr.toArray(new String[arr.size()]);
        } catch (Exception ignored) {
        }
        return new String[]{};
    }

    public static ArrayList<String> buildSetAdrenoGPUParams(CpuStatus cpuState, ArrayList<String> commands) {
        // governor
        if (!cpuState.adrenoGovernor.equals("")) {
            commands.add("chmod 0664 /sys/class/kgsl/kgsl-3d0/devfreq/governor;");
            commands.add("echo " + cpuState.adrenoGovernor + " > /sys/class/kgsl/kgsl-3d0/devfreq/governor;");
        }
        // min feq
        if (!cpuState.adrenoMinFreq.equals("")) {
            commands.add("chmod 0664 /sys/class/kgsl/kgsl-3d0/devfreq/min_freq;");
            commands.add("echo " + cpuState.adrenoMinFreq + " > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq;");
        }
        // max freq
        if (!cpuState.adrenoMaxFreq.equals("")) {
            commands.add("chmod 0664 /sys/class/kgsl/kgsl-3d0/devfreq/max_freq;");
            commands.add("echo " + cpuState.adrenoMaxFreq + " > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq;");
        }
        // min power level
        if (!cpuState.adrenoMinPL.equals("")) {
            commands.add("chmod 0664 /sys/class/kgsl/kgsl-3d0/min_pwrlevel;");
            commands.add("echo " + cpuState.adrenoMinPL + " > /sys/class/kgsl/kgsl-3d0/min_pwrlevel;");
        }
        // max power level
        if (!cpuState.adrenoMaxPL.equals("")) {
            commands.add("chmod 0664 /sys/class/kgsl/kgsl-3d0/max_pwrlevel;");
            commands.add("echo " + cpuState.adrenoMaxPL + " > /sys/class/kgsl/kgsl-3d0/max_pwrlevel;");
        }
        // default power level
        if (!cpuState.adrenoDefaultPL.equals("")) {
            commands.add("chmod 0664 /sys/class/kgsl/kgsl-3d0/default_pwrlevel;");
            commands.add("echo " + cpuState.adrenoDefaultPL + " > /sys/class/kgsl/kgsl-3d0/default_pwrlevel;");
        }
        return commands;
    }
}
