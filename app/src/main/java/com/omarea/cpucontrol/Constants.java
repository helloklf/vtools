package com.omarea.cpucontrol;

public interface Constants {

    String App_Tag = "Performance Tweaker";
    boolean debug = true;

    //Charger
    String constant_charge_current_max = "/sys/class/power_supply/battery/constant_charge_current_max";
    String constant_charge_warm_temp = "/sys/class/power_supply/bms/temp_warm";
    String charge_allow0 = "/sys/class/power_supply/battery/battery_charging_enabled";
    String charge_allow1 = "/sys/class/power_supply/battery/charging_enabled";
    String charge_disabled = "/sys/class/power_supply/battery/input_suspend";

    //Cpuset
    String cpuset_foreground_cpus = "/dev/cpuset/foreground/cpus";
    String cpuset_foreground_boost_cpus = "/dev/cpuset/foreground/boost/cpus";
    String cpuset_system_background_cpus = "/dev/cpuset/system-background/cpus";
    String cpuset_background_cpus = "/dev/cpuset/background/cpus";
    String cpuset_cpus = "/dev/cpuset/cpus";

    // CPU
    String core_control_online = "/sys/devices/system/cpu/cpu0/online";
    String cpufreq_sys_dir = "/sys/devices/system/cpu/cpu0/cpufreq/";
    String scaling_min_freq = cpufreq_sys_dir + "scaling_min_freq";
    String cpuinfo_min_freq = cpufreq_sys_dir + "cpuinfo_min_freq";
    String scaling_max_freq = cpufreq_sys_dir + "scaling_max_freq";
    String cpuinfo_max_freq = cpufreq_sys_dir + "cpuinfo_max_freq";
    String scaling_cur_freq = cpufreq_sys_dir + "scaling_cur_freq";
    String cpuinfo_cur_freq = cpufreq_sys_dir + "cpuinfo_cur_freq";
    String scaling_governor = cpufreq_sys_dir + "scaling_governor";
    String scaling_available_freq = cpufreq_sys_dir + "scaling_available_frequencies";
    String scaling_available_governors = cpufreq_sys_dir + "scaling_available_governors";

    String governor_prop_dir = "/sys/devices/system/cpu/cpufreq/";

    // CPU BigCores
    String cpufreq_sys_dir_bigcore = "/sys/devices/system/cpu/cpu4/cpufreq/";
    String scaling_min_freq_bigcore = cpufreq_sys_dir_bigcore + "scaling_min_freq";
    String cpuinfo_min_freq_bigcore = cpufreq_sys_dir_bigcore + "cpuinfo_min_freq";
    String scaling_max_freq_bigcore = cpufreq_sys_dir_bigcore + "scaling_max_freq";
    String cpuinfo_max_freq_bigcore = cpufreq_sys_dir_bigcore + "cpuinfo_max_freq";
    String scaling_cur_freq_bigcore = cpufreq_sys_dir_bigcore + "scaling_cur_freq";
    String cpuinfo_cur_freq_bigcore = cpufreq_sys_dir_bigcore + "cpuinfo_cur_freq";
    String scaling_governor_bigcore = cpufreq_sys_dir_bigcore + "scaling_governor";
    String scaling_available_freq_bigcore = cpufreq_sys_dir_bigcore + "scaling_available_frequencies";
    String scaling_available_governors_bigcore = cpufreq_sys_dir_bigcore + "scaling_available_governors";

    String sched_boost = "/proc/sys/kernel/sched_boost";


    // I/O
    String available_blockdevices = "/sys/block/";
    String available_schedulers = "/sys/block/mmcblk0/queue/scheduler";
    String available_schedulers_path = "/sys/block/mmcblk1/queue/scheduler";
    String available_schedulers_ufs = "/sys/block/sda/queue/scheduler";
    String time_in_states = "/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state";
    String ioscheduler_mtd = "/sys/block/mtdblock0/queue/scheduler";
    String SD_CACHE = "/sys/devices/virtual/bdi/179:0/read_ahead_kb";
    String[] readAheadKb = {
            "128", "256", "512", "1024", "2048", "3072", "4096"
    };

    //Thermal
    String thermal_core_control = "/sys/module/msm_thermal/core_control/enabled";//1 0
    String thermal_vdd_restriction = "/sys/module/msm_thermal/vdd_restriction/enabled"; //1 0
    String thermal_parameters = "/sys/module/msm_thermal/parameters/enabled"; //Y N

    // GPU
    String[] GPU_PATH = new String[]{
            "/sys/class/kgsl", "/sys/devices/platform/galcore/gpu/gpu0/gpufreq"
    };
    String[] gpu_governor_path = new String[]{
            "/kgsl-3d0/pwrscale/trustzone/governor", "/kgsl-3d0/devfreq/governor", "/scaling_governor"
    };
    String[] gpu_govs_avail_path = new String[]{
            "/kgsl-3d0/devfreq/available_governors", "/scaling_available_governors"
    };
    String[] gpu_freqs_avail = new String[]{
            "/kgsl-3d0/gpu_available_frequencies", "/kgsl-3d0/devfreq/available_frequencies",
            "/scaling_available_frequencies"
    };
    String[] gpu_freqs_max = new String[]{
            "/kgsl-3d0/max_gpuclk", "/kgsl-3d0/devfreq/max_freq", "/scaling_max_freq"
    };
    String[] gpu_freqs_min = new String[]{
            "/kgsl-3d0/min_gpuclk", "/kgsl-3d0/devfreq/min_freq", "/scaling_min_freq"
    };
    String[] mFragmentsArray = new String[]{
            "Cpu Frequency", "Time In State", "I/0 Control", "Wakelocks", "Settings"
    };

    // Wakelocks
    String[] wakelockTypes = new String[]{
            "Kernel Wakelocks", "Cpu Wakelocks", "Wakeup Triggers"
    };

    // Preferences
    String PREF_CPU_MAX_FREQ = "cpu_max_freq_pref";
    String PREF_CPU_MIN_FREQ = "cpu_min_freq_pref";
    String PREF_CPU_GOV = "governor_pref";
    String PREF_GPU_MAX = "gpu_max_freq_pref";
    String PREF_GPU_MIN = "gpu_min_freq_pref";
    String PREF_GPU_GOV = "gpu_governor_pref";
    String PREF_HOTPLUG = "cpu_hotplug";
    String PREF_IO_SCHEDULER = "disk_scheduler";
    String PREF_READ_AHEAD = "read_ahead_cache";
    String PREF_TIS_RESET_STATS = "tis_reset_stats";
    String PREF_ZERO_VALS = "non_zero_vals_only";

    // Build prop
    String BUILD_PROP = "/system/build.prop";

    // Virtual Memory
    String VM_PATH = "/proc/sys/vm";
    String[] SUPPORTED_VM = {
            "dirty_ratio", "dirty_background_ratio", "dirty_expire_centisecs",
            "dirty_writeback_centisecs", "min_free_kbytes", "overcommit_ratio", "swappiness",
            "vfs_cache_pressure", "laptop_mode", "extra_free_kbytes"
    };

    // CPU Hotplug
    String HOTPLUG_MPDEC = "mpdecision";
}
