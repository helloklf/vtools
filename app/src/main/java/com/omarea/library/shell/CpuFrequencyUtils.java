package com.omarea.library.shell;

import com.omarea.common.shell.KeepShellPublic;
import com.omarea.common.shell.KernelProrp;
import com.omarea.model.CpuClusterStatus;
import com.omarea.model.CpuStatus;
import com.omarea.vtools.SceneJNI;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class CpuFrequencyUtils {
    private static String platform;
    private final String cpu_dir = "/sys/devices/system/cpu/cpu0/";
    private final String cpufreq_sys_dir = "/sys/devices/system/cpu/cpu0/cpufreq/";
    private final String scaling_min_freq = cpufreq_sys_dir + "scaling_min_freq";
    private final String scaling_cur_freq = cpufreq_sys_dir + "scaling_cur_freq";
    // private  final  String scaling_cur_freq = cpufreq_sys_dir + "cpuinfo_cur_freq";
    private final String scaling_max_freq = cpufreq_sys_dir + "scaling_max_freq";
    private final String scaling_governor = cpufreq_sys_dir + "scaling_governor";
    private final Object cpuClusterInfoLoading = true;
    private ArrayList<String[]> cpuClusterInfo;
    private SceneJNI JNI = new SceneJNI();
    private int coreCount = -1;

    private boolean isMTK() {
        if (platform == null) {
            platform = new PlatformUtils().getCPUName();
        }
        return platform.startsWith("mt");
    }

    private String getCpuFreqValue(String path) {
        long freqValue = JNI.getKernelPropLong(path);
        if (freqValue > -1) {
            return "" + freqValue;
        }
        return "";
    }

    public String[] getAvailableFrequencies(Integer cluster) {
        if (cluster >= getClusterInfo().size()) {
            return new String[]{};
        }
        String cpu = "cpu" + getClusterInfo().get(cluster)[0];
        String[] frequencies;
        String scaling_available_freq = cpufreq_sys_dir + "scaling_available_frequencies";
        if (new File(scaling_available_freq.replace("cpu0", cpu)).exists()) {
            frequencies = KernelProrp.INSTANCE.getProp(scaling_available_freq.replace("cpu0", cpu)).split("[ ]+");
            return frequencies;
        } else if (new File("/sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster" + cluster + "_freq_table").exists()) {
            frequencies = KernelProrp.INSTANCE.getProp("/sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster" + cluster + "_freq_table")
                    .split("[ ]+");
            return frequencies;
        } else {
            return new String[]{};
        }
    }

    public String getCurrentMaxFrequency(Integer cluster) {
        if (cluster >= getClusterInfo().size()) {
            return "";
        }
        String cpu = "cpu" + getClusterInfo().get(cluster)[0];
        return KernelProrp.INSTANCE.getProp(scaling_max_freq.replace("cpu0", cpu));
    }

    public String getCurrentMaxFrequency(String core) {
        return KernelProrp.INSTANCE.getProp(scaling_max_freq.replace("cpu0", core));
    }

    public String getCurrentFrequency(Integer cluster) {
        if (cluster >= getClusterInfo().size()) {
            return "";
        }

        String cpu = "cpu" + getClusterInfo().get(cluster)[0];
        return getCpuFreqValue(scaling_cur_freq.replace("cpu0", cpu));
    }

    public String getCurrentFrequency(String cpu) {
        return getCpuFreqValue(scaling_cur_freq.replace("cpu0", cpu));
    }

    public String getCurrentMinFrequency(Integer cluster) {
        if (cluster >= getClusterInfo().size()) {
            return "";
        }
        String cpu = "cpu" + getClusterInfo().get(cluster)[0];
        return KernelProrp.INSTANCE.getProp(scaling_min_freq.replace("cpu0", cpu));
    }

    public String getCurrentMinFrequency(String core) {
        return KernelProrp.INSTANCE.getProp(scaling_min_freq.replace("cpu0", core));
    }

    public String[] getAvailableGovernors(Integer cluster) {
        if (cluster >= getClusterInfo().size()) {
            return new String[]{};
        }
        String cpu = "cpu" + getClusterInfo().get(cluster)[0];
        String scaling_available_governors = cpufreq_sys_dir + "scaling_available_governors";
        return KernelProrp.INSTANCE.getProp(scaling_available_governors.replace("cpu0", cpu)).split("[ ]+");
    }

    public String getCurrentScalingGovernor(Integer cluster) {
        if (cluster >= getClusterInfo().size()) {
            return "";
        }
        String cpu = "cpu" + getClusterInfo().get(cluster)[0];
        return KernelProrp.INSTANCE.getProp(scaling_governor.replace("cpu0", cpu));
    }

    private String getCurrentScalingGovernor(String core) {
        return KernelProrp.INSTANCE.getProp(scaling_governor.replace("cpu0", core));
    }

    public HashMap<String, String> getCurrentScalingGovernorParams(Integer cluster) {
        if (cluster >= getClusterInfo().size()) {
            return null;
        }
        String cpu = "cpu" + getClusterInfo().get(cluster)[0];
        String governor = getCurrentScalingGovernor(cpu);
        return new FileValueMap().mapFileValue(cpu_dir.replace("cpu0", cpu) + "cpufreq/" + governor);
    }

    public HashMap<String, String> getCoregGovernorParams(Integer cluster) {
        String cpu = "cpu" + cluster;
        String governor = getCurrentScalingGovernor(cpu);
        return new FileValueMap().mapFileValue(cpu_dir.replace("cpu0", cpu) + "cpufreq/" + governor);
    }

    public void setMinFrequency(String minFrequency, Integer cluster) {
        if (cluster >= getClusterInfo().size()) {
            return;
        }

        if (isMTK()) {
            String stringBuilder = "echo " + cluster +
                    " " +
                    minFrequency +
                    " > /proc/ppm/policy/hard_userlimit_min_cpu_freq";
            KeepShellPublic.INSTANCE.doCmdSync(stringBuilder);
        } else {
            String[] cores = getClusterInfo().get(cluster);
            ArrayList<String> commands = new ArrayList<>();
            if (minFrequency != null) {
                for (String core : cores) {
                    commands.add("chmod 0664 " + scaling_min_freq.replace("cpu0", "cpu" + core));
                    commands.add("echo " + minFrequency + " > " + scaling_min_freq.replace("cpu0", "cpu" + core));
                }
                KeepShellPublic.INSTANCE.doCmdSync(commands);
            }
        }
    }

    /*
    public String getInputBoosterFreq() {
        return KernelProrp.INSTANCE.getProp("/sys/module/cpu_boost/parameters/input_boost_freq");
    }

    public void setInputBoosterFreq(String freqs) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0755 /sys/module/cpu_boost/parameters/input_boost_freq");
        commands.add("echo " + freqs + " > /sys/module/cpu_boost/parameters/input_boost_freq");

        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public String getInputBoosterTime() {
        return KernelProrp.INSTANCE.getProp("/sys/module/cpu_boost/parameters/input_boost_ms");
    }

    public void setInputBoosterTime(String time) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0755 /sys/module/cpu_boost/parameters/input_boost_ms");
        commands.add("echo " + time + " > /sys/module/cpu_boost/parameters/input_boost_ms");

        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }
    */

    public void setMaxFrequency(String maxFrequency, Integer cluster) {
        if (cluster >= getClusterInfo().size()) {
            return;
        }

        if (isMTK()) {
            String stringBuilder = "echo " + cluster +
                    " " +
                    maxFrequency +
                    " > /proc/ppm/policy/hard_userlimit_max_cpu_freq";
            KeepShellPublic.INSTANCE.doCmdSync(stringBuilder);
        } else {
            String[] cores = getClusterInfo().get(cluster);
            ArrayList<String> commands = new ArrayList<>();
            if (maxFrequency != null) {
                commands.add("chmod 0664 /sys/module/msm_performance/parameters/cpu_max_freq");
                StringBuilder stringBuilder = new StringBuilder();
                for (String core : cores) {
                    commands.add("chmod 0664 " + scaling_max_freq.replace("cpu0", "cpu" + core));
                    commands.add("echo " + maxFrequency + " > " + scaling_max_freq.replace("cpu0", "cpu" + core));
                    stringBuilder.append(core);
                    stringBuilder.append(":");
                    stringBuilder.append(maxFrequency);
                    stringBuilder.append(" ");
                }
                commands.add("echo " + stringBuilder.toString() + "> /sys/module/msm_performance/parameters/cpu_max_freq");
                KeepShellPublic.INSTANCE.doCmdSync(commands);
            }
        }
    }

    public void setGovernor(String governor, Integer cluster) {
        if (cluster >= getClusterInfo().size()) {
            return;
        }

        String[] cores = getClusterInfo().get(cluster);
        ArrayList<String> commands = new ArrayList<>();
        if (governor != null) {
            for (String core : cores) {
                commands.add("chmod 0755 " + scaling_governor.replace("cpu0", "cpu" + core));
                commands.add("echo " + governor + " > " + scaling_governor.replace("cpu0", "cpu" + core));
            }
            KeepShellPublic.INSTANCE.doCmdSync(commands);
        }
    }

    /*
    public void setCoresOnlineState(boolean[] coreStates) {
        ArrayList<String> commands = new ArrayList<>();
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }
    */

    public boolean getCoreOnlineState(int coreIndex) {
        return KernelProrp.INSTANCE.getProp("/sys/devices/system/cpu/cpu0/online".replace("cpu0", "cpu" + coreIndex)).equals("1");
    }

    public void setCoreOnlineState(int coreIndex, boolean online) {
        ArrayList<String> commands = new ArrayList<>();
        if (exynosCpuhotplugSupport() && getExynosHotplug()) {
            commands.add("echo 0 > /sys/devices/system/cpu/cpuhotplug/enabled;");
        }
        commands.add("chmod 0755 /sys/devices/system/cpu/cpu0/online".replace("cpu0", "cpu" + coreIndex));
        commands.add("echo " + (online ? "1" : "0") + " > /sys/devices/system/cpu/cpu0/online".replace("cpu0", "cpu" + coreIndex));
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public int getExynosHmpUP() {
        String up = KernelProrp.INSTANCE.getProp("/sys/kernel/hmp/up_threshold").trim();
        if (Objects.equals(up, "")) {
            return 0;
        }
        try {
            return Integer.parseInt(up);
        } catch (Exception ex) {
            return 0;
        }
    }

    public void setExynosHmpUP(int up) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 /sys/kernel/hmp/up_threshold;");
        commands.add("echo " + up + " > /sys/kernel/hmp/up_threshold;");
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public int getExynosHmpDown() {
        String value = KernelProrp.INSTANCE.getProp("/sys/kernel/hmp/down_threshold").trim();
        if (Objects.equals(value, "")) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return 0;
        }
    }

    public void setExynosHmpDown(int down) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 /sys/kernel/hmp/down_threshold;");
        commands.add("echo " + down + " > /sys/kernel/hmp/down_threshold;");
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public boolean getExynosBooster() {
        String value = KernelProrp.INSTANCE.getProp("/sys/kernel/hmp/boost").trim().toLowerCase();
        return Objects.equals(value, "1") || Objects.equals(value, "true") || Objects.equals(value, "enabled");
    }

    public void setExynosBooster(boolean hotplug) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 /sys/kernel/hmp/boost");
        commands.add("echo " + (hotplug ? 1 : 0) + " > /sys/kernel/hmp/boost");
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public boolean getExynosHotplug() {
        String value = KernelProrp.INSTANCE.getProp("/sys/devices/system/cpu/cpuhotplug/enabled").trim().toLowerCase();
        return Objects.equals(value, "1") || Objects.equals(value, "true") || Objects.equals(value, "enabled");
    }

    public void setExynosHotplug(boolean hotplug) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 /sys/devices/system/cpu/cpuhotplug/enabled;");
        commands.add("echo " + (hotplug ? 1 : 0) + " > /sys/devices/system/cpu/cpuhotplug/enabled;");
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public int getCoreCount() {
        if (coreCount > -1) {
            return coreCount;
        }
        int cores = 0;
        while (true) {
            File file = new File(cpu_dir.replace("cpu0", "cpu" + cores));
            if (file.exists()) {
                cores++;
            } else {
                break;
            }
        }
        coreCount = cores;
        return coreCount;
    }

    public ArrayList<String[]> getClusterInfo() {
        if (cpuClusterInfo != null) {
            return cpuClusterInfo;
        }
        synchronized (this) {
            int cores = 0;
            cpuClusterInfo = new ArrayList<>();
            ArrayList<String> clusters = new ArrayList<>();
            while (true) {
                File file = new File("/sys/devices/system/cpu/cpu0/cpufreq/related_cpus".replace("cpu0", "cpu" + cores));
                if (file.exists()) {
                    String relatedCpus = KernelProrp.INSTANCE.getProp("/sys/devices/system/cpu/cpu0/cpufreq/related_cpus".replace("cpu0", "cpu" + cores)).trim();
                    if (!clusters.contains(relatedCpus) && !relatedCpus.isEmpty()) {
                        clusters.add(relatedCpus);
                    }
                } else {
                    break;
                }
                cores++;
            }
            for (int i = 0; i < clusters.size(); i++) {
                cpuClusterInfo.add(clusters.get(i).split("[ ]+"));
            }
        }
        return cpuClusterInfo;
    }

    /*
    private final String sched_boost = "/proc/sys/kernel/sched_boost";
    public String getSechedBoostState() {
        return KernelProrp.INSTANCE.getProp(sched_boost);
    }

    public void setSechedBoostState(boolean enabled) {
        String val = enabled ? "1" : "0";
        ArrayList<String> commands = new ArrayList<>();
        commands.add("chmod 0664 " + sched_boost);
        commands.add("echo " + val + " > " + sched_boost);
        KeepShellPublic.INSTANCE.doCmdSync(commands);
    }

    public String[] toMhz(String... values) {
        String[] frequency = new String[values.length];

        for (int i = 0; i < values.length; i++) {
            try {
                frequency[i] = (Integer.parseInt(values[i].trim()) / 1000) + " Mhz";
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }
        return frequency;
    }
    */

    // /sys/devices/system/cpu/cpuhotplug
    public boolean exynosCpuhotplugSupport() {
        return new File("/sys/devices/system/cpu/cpuhotplug").exists();
    }

    public boolean exynosHMP() {
        return new File("/sys/kernel/hmp/down_threshold").exists() && new File("/sys/kernel/hmp/up_threshold").exists() && new File("/sys/kernel/hmp/boost").exists();
    }

    public ArrayList<String> buildShell(CpuStatus cpuStatus) {
        ArrayList<String> commands = new ArrayList<>();
        if (cpuStatus != null) {
            // thermal
            commands.addAll(new ThermalControlUtils().buildSetThermalParams(cpuStatus, commands));

            // core online
            if (cpuStatus.coreOnline != null && cpuStatus.coreOnline.size() > 0) {
                if (exynosCpuhotplugSupport() && getExynosHotplug()) {
                    commands.add("echo 0 > /sys/devices/system/cpu/cpuhotplug/enabled;");
                }
                for (int i = 0; i < cpuStatus.coreOnline.size(); i++) {
                    commands.add("chmod 0755 /sys/devices/system/cpu/cpu0/online".replace("cpu0", "cpu" + i));
                    commands.add("echo " + (cpuStatus.coreOnline.get(i) ? "1" : "0") + " > /sys/devices/system/cpu/cpu0/online".replace("cpu0", "cpu" + i));
                }
            }

            // CPU
            if (cpuStatus.cpuClusterStatuses != null && cpuStatus.cpuClusterStatuses.size() > 0) {
                ArrayList<CpuClusterStatus> params = cpuStatus.cpuClusterStatuses;
                if (params.size() <= getClusterInfo().size()) {
                    if (isMTK()) {
                        for (int cluster = 0; cluster < params.size(); cluster++) {
                            CpuClusterStatus config = params.get(cluster);
                            commands.add(String.format(Locale.getDefault(), "echo %d %s > /proc/ppm/policy/hard_userlimit_min_cpu_freq", cluster, config.min_freq));
                            commands.add(String.format(Locale.getDefault(), "echo %d %s > /proc/ppm/policy/hard_userlimit_max_cpu_freq", cluster, config.max_freq));
                        }
                    } else {
                        for (int cluster = 0; cluster < params.size(); cluster++) {
                            CpuClusterStatus config = params.get(cluster);

                            String[] cores = getClusterInfo().get(cluster);
                            if (cores.length < 1) {
                                continue;
                            }
                            String core = cores[0];
                            // for (String core : cores) {
                            if (config.governor != null && !config.governor.isEmpty()) {
                                commands.add("chmod 0755 " + scaling_governor.replace("cpu0", "cpu" + core));
                                commands.add("echo " + config.governor + " > " + scaling_governor.replace("cpu0", "cpu" + core));
                            }
                            commands.add("chmod 0664 /sys/module/msm_performance/parameters/cpu_max_freq");
                            StringBuilder stringBuilder = new StringBuilder();
                            if (config.max_freq != null && !config.max_freq.isEmpty()) {
                                commands.add("chmod 0664 " + scaling_max_freq.replace("cpu0", "cpu" + core));
                                commands.add("echo " + config.max_freq + " > " + scaling_max_freq.replace("cpu0", "cpu" + core));
                                stringBuilder.append(core);
                                stringBuilder.append(":");
                                stringBuilder.append(config.max_freq);
                                stringBuilder.append(" ");
                            }
                            commands.add("echo " + stringBuilder.toString() + "> /sys/module/msm_performance/parameters/cpu_max_freq");
                            if (config.min_freq != null && !config.min_freq.isEmpty()) {
                                commands.add("chmod 0664 " + scaling_min_freq.replace("cpu0", "cpu" + core));
                                commands.add("echo " + config.min_freq + " > " + scaling_min_freq.replace("cpu0", "cpu" + core));
                            }
                            // }
                        }
                    }
                }
            }
            // Boost
            /*
            if (!(cpuStatus.boost == null || cpuStatus.boost.isEmpty())) {
                commands.add("chmod 0664 " + sched_boost);
                commands.add("echo " + cpuStatus.boost + " > " + sched_boost);
            }
            if (!(cpuStatus.boostFreq == null || cpuStatus.boostFreq.isEmpty())) {
                commands.add("chmod 0755 /sys/module/cpu_boost/parameters/input_boost_freq");
                commands.add("echo " + cpuStatus.boostFreq + " > /sys/module/cpu_boost/parameters/input_boost_freq");
            }
            if (!(cpuStatus.boostTime == null || cpuStatus.boostTime.isEmpty())) {
                commands.add("chmod 0755 /sys/module/cpu_boost/parameters/input_boost_ms");
                commands.add("echo " + cpuStatus.boostTime + " > /sys/module/cpu_boost/parameters/input_boost_ms");
            }
            */

            // GPU
            commands.addAll(GpuUtils.buildSetAdrenoGPUParams(cpuStatus));

            // exynos
            if (exynosHMP()) {
                commands.add("chmod 0664 /sys/devices/system/cpu/cpuhotplug/enabled;");
                commands.add("echo " + (cpuStatus.exynosHotplug ? 1 : 0) + " > /sys/devices/system/cpu/cpuhotplug/enabled;");

                commands.add("chmod 0664 /sys/kernel/hmp/down_threshold;");
                commands.add("echo " + cpuStatus.exynosHmpDown + " > /sys/kernel/hmp/down_threshold;");

                commands.add("chmod 0664 /sys/kernel/hmp/up_threshold;");
                commands.add("echo " + cpuStatus.exynosHmpUP + " > /sys/kernel/hmp/up_threshold;");

                commands.add("chmod 0664 /sys/kernel/hmp/boost");
                commands.add("echo " + (cpuStatus.exynosHmpBooster ? 1 : 0) + " > /sys/kernel/hmp/boost");
            }

            // cpuset
            if (!(cpuStatus.cpusetBackground == null || cpuStatus.cpusetBackground.isEmpty())) {
                commands.add("echo " + cpuStatus.cpusetBackground + " > /dev/cpuset/background/cpus");
            }
            if (!(cpuStatus.cpusetSysBackground == null || cpuStatus.cpusetSysBackground.isEmpty())) {
                commands.add("echo " + cpuStatus.cpusetSysBackground + " > /dev/cpuset/system-background/cpus");
            }
            if (!(cpuStatus.cpusetForeground == null || cpuStatus.cpusetForeground.isEmpty())) {
                commands.add("echo " + cpuStatus.cpusetForeground + " > /dev/cpuset/foreground/cpus");
            }
            if (!(cpuStatus.cpusetRestricted == null || cpuStatus.cpusetRestricted.isEmpty())) {
                commands.add("echo " + cpuStatus.cpusetRestricted + " > /dev/cpuset/restricted/cpus");
            }
            if (!(cpuStatus.cpusetTopApp == null || cpuStatus.cpusetTopApp.isEmpty())) {
                commands.add("echo " + cpuStatus.cpusetTopApp + " > /dev/cpuset/top-app/cpus");
            }
        }

        return commands;
    }
}
