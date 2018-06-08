package com.omarea.shell.cpucontrol;

public interface Constants {
    String cpu_dir = "/sys/devices/system/cpu/cpu0/";
    String cpufreq_sys_dir = "/sys/devices/system/cpu/cpu0/cpufreq/";
    String scaling_min_freq = cpufreq_sys_dir + "scaling_min_freq";
    String scaling_max_freq = cpufreq_sys_dir + "scaling_max_freq";
    String scaling_governor = cpufreq_sys_dir + "scaling_governor";
    String scaling_available_freq = cpufreq_sys_dir + "scaling_available_frequencies";
    String scaling_available_governors = cpufreq_sys_dir + "scaling_available_governors";

    String sched_boost = "/proc/sys/kernel/sched_boost";

    //Thermal
    String thermal_core_control = "/sys/module/msm_thermal/core_control/enabled";//1 0
    String thermal_vdd_restriction = "/sys/module/msm_thermal/vdd_restriction/enabled"; //1 0
    String thermal_parameters = "/sys/module/msm_thermal/parameters/enabled"; //Y N

}
