package com.omarea.shell.cpucontrol;

/**
 * Created by Hello on 2018/02/01.
 */

public class CpuClusterConfiginfo {
    // cpu0
    public String coreIndex;

    // /sys/devices/system/cpu/cpu4/core_ctl/is_big_cluster
    public Boolean isBigCluster;

    // /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq
    public String currentFreq;
    // /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    public String minFreq;
    // /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
    public String maxFreq;

    // #region core_ctl
    // /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
    public String ctlMinCpus;
    // /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
    public String ctlMaxCpus1;
    // /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
    public String ctlUpThres;
    // /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres
    public String ctlDownThres;
    // /sys/devices/system/cpu/cpu4/core_ctl/offline_delay_ms
    public String ctlOfflineDelayMs;
    // /sys/devices/system/cpu/cpu4/core_ctl/task_thres
    public String ctlTaskThres;
    // #endregion

    // /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies
    public String[] frequencies;
    // /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors
    public String[] governors;

    // #region interactive
    // /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
    public String targetLoad;
    // /sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay
    public String goHispeedDelay;
    // /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
    public String goHispeedLoad;
    // /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
    public String goHispeedFreq;
    // /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
    public String ioIsBusy;
    // #endregion

    // /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
    public String governor;
    // /sys/devices/system/cpu/cpu0/online
    public Boolean online;
}
