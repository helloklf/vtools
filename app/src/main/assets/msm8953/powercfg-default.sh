#!/system/bin/sh
setenforce 0
action=$1

#echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/max_freq_hysteresis

#powersave 1.2Ghz
if [ "$action" = "powersave" ]; then
    echo 1 > /sys/devices/system/cpu/cpu0/online
	echo "0:1280000 1:1280000 2:1280000 3:1280000 4:1280000 5:1280000 6:1280000 7:1280000" > /sys/module/msm_performance/parameters/cpu_max_freq
    echo "interactive" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
    echo "19000 960000:39000" > /sys/devices/system/cpu/cpufreq/interactive/above_hispeed_delay
    echo 85 > /sys/devices/system/cpu/cpufreq/interactive/go_hispeed_load
    echo 20000 > /sys/devices/system/cpu/cpufreq/interactive/timer_rate
    echo 1401600 > /sys/devices/system/cpu/cpufreq/interactive/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpufreq/interactive/io_is_busy
    echo "85 960000:80" > /sys/devices/system/cpu/cpufreq/interactive/target_loads
    echo 39000 > /sys/devices/system/cpu/cpufreq/interactive/min_sample_time
    echo 40000 > /sys/devices/system/cpu/cpufreq/interactive/sampling_down_factor
    echo 152800 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    echo 1280000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq

    echo 0 > /sys/devices/system/cpu/cpu4/online
    echo 0 > /sys/devices/system/cpu/cpu5/online
    echo 0 > /sys/devices/system/cpu/cpu6/online
    echo 0 > /sys/devices/system/cpu/cpu7/online
    echo 0 > /sys/module/lpm_levels/parameters/sleep_disabled

    echo 1 > /sys/module/msm_thermal/core_control/enabled
    echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
    echo Y > /sys/module/msm_thermal/parameters/enabled

	exit 0
fi

if [ "$action" = "balance" ]; then
    echo 1 > /sys/devices/system/cpu/cpu0/online
	echo "0:1560000 1:1560000 2:1560000 3:1560000 4:1560000 5:1560000 6:1560000 7:1560000" > /sys/module/msm_performance/parameters/cpu_max_freq
    echo "interactive" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
    echo "19000 1280000:39000" > /sys/devices/system/cpu/cpufreq/interactive/above_hispeed_delay
    echo 85 > /sys/devices/system/cpu/cpufreq/interactive/go_hispeed_load
    echo 20000 > /sys/devices/system/cpu/cpufreq/interactive/timer_rate
    echo 1401600 > /sys/devices/system/cpu/cpufreq/interactive/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpufreq/interactive/io_is_busy
    echo "85 1280000:80" > /sys/devices/system/cpu/cpufreq/interactive/target_loads
    echo 39000 > /sys/devices/system/cpu/cpufreq/interactive/min_sample_time
    echo 40000 > /sys/devices/system/cpu/cpufreq/interactive/sampling_down_factor
    echo 152800 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    echo 1560000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq

    echo 1 > /sys/devices/system/cpu/cpu4/online
    echo 1 > /sys/devices/system/cpu/cpu5/online
    echo 1 > /sys/devices/system/cpu/cpu6/online
    echo 1 > /sys/devices/system/cpu/cpu7/online
    echo 0 > /sys/module/lpm_levels/parameters/sleep_disabled

	echo 1 > /sys/module/msm_thermal/core_control/enabled
	echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
	echo Y > /sys/module/msm_thermal/parameters/enabled

	exit 0
fi

if [ "$action" = "performance" ]; then
    echo 1 > /sys/devices/system/cpu/cpu0/online
	echo "0:2016000 1:2016000 2:2016000 3:2016000 4:2016000 5:2016000 6:2016000 7:2016000" > /sys/module/msm_performance/parameters/cpu_max_freq
    echo "interactive" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
    echo "19000 1280000:39000" > /sys/devices/system/cpu/cpufreq/interactive/above_hispeed_delay
    echo 85 > /sys/devices/system/cpu/cpufreq/interactive/go_hispeed_load
    echo 20000 > /sys/devices/system/cpu/cpufreq/interactive/timer_rate
    echo 1401600 > /sys/devices/system/cpu/cpufreq/interactive/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpufreq/interactive/io_is_busy
    echo "85 1280000:80" > /sys/devices/system/cpu/cpufreq/interactive/target_loads
    echo 39000 > /sys/devices/system/cpu/cpufreq/interactive/min_sample_time
    echo 40000 > /sys/devices/system/cpu/cpufreq/interactive/sampling_down_factor
    echo 652800 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    echo 2016000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq

    echo 1 > /sys/devices/system/cpu/cpu4/online
    echo 1 > /sys/devices/system/cpu/cpu5/online
    echo 1 > /sys/devices/system/cpu/cpu6/online
    echo 1 > /sys/devices/system/cpu/cpu7/online
    echo 0 > /sys/module/lpm_levels/parameters/sleep_disabled

	echo 0 > /sys/module/msm_thermal/core_control/enabled
	echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
	echo N > /sys/module/msm_thermal/parameters/enabled

	exit 0
fi

if [ "$action" = "fast" ]; then
    echo 1 > /sys/devices/system/cpu/cpu0/online
	echo "0:2560000 1:2560000 2:2560000 3:2560000 4:2560000 5:2560000 6:2560000 7:2560000" > /sys/module/msm_performance/parameters/cpu_max_freq
    echo "interactive" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
    echo "19000 1280000:39000" > /sys/devices/system/cpu/cpufreq/interactive/above_hispeed_delay
    echo 85 > /sys/devices/system/cpu/cpufreq/interactive/go_hispeed_load
    echo 20000 > /sys/devices/system/cpu/cpufreq/interactive/timer_rate
    echo 1401600 > /sys/devices/system/cpu/cpufreq/interactive/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpufreq/interactive/io_is_busy
    echo "85 1280000:80" > /sys/devices/system/cpu/cpufreq/interactive/target_loads
    echo 39000 > /sys/devices/system/cpu/cpufreq/interactive/min_sample_time
    echo 40000 > /sys/devices/system/cpu/cpufreq/interactive/sampling_down_factor
    echo 960000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    echo 2560000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq

    echo 1 > /sys/devices/system/cpu/cpu4/online
    echo 1 > /sys/devices/system/cpu/cpu5/online
    echo 1 > /sys/devices/system/cpu/cpu6/online
    echo 1 > /sys/devices/system/cpu/cpu7/online
    echo 0 > /sys/module/lpm_levels/parameters/sleep_disabled

	echo 0 > /sys/module/msm_thermal/core_control/enabled
	echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
	echo Y > /sys/module/msm_thermal/parameters/enabled
	
	exit 0
fi
