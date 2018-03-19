package com.omarea.shell.cpucontrol;

/**
 * Created by Hello on 2018/02/01.
 */

public class CpuConfigInfo {
    public CpuClusterConfiginfo[] littleCoreCluster;
    public CpuClusterConfiginfo[] BigCoreCluster;

    // /sys/module/cpu_boost/parameters/input_boost_freq
    public String inputBoosterFreq;
    // /sys/module/cpu_boost/parameters/input_boost_ms
    public String inputBoosterMs;

    // /sys/module/msm_performance/parameters/cpu_max_freq
    public String paramtersMaxFreq;
    // /proc/sys/kernel/sched_boost
    public String sechedBoost;

    // /sys/module/msm_thermal/core_control/enabled 1/0
    public String coreControlEnabled;
    // /sys/module/msm_thermal/vdd_restriction/enabled 1/0
    public String vddEnabled;
    // /sys/module/msm_thermal/parameters/enabled y/n
    public String thermalEnabled;


    /**
     * 读取当前状态为配置信息
     */
    public static CpuConfigInfo readCurrentState() {
        return new CpuConfigInfo();
    }
}
