#!/system/bin/sh

action=$1


if [ "$action" = "powersave" ]; then
	echo "0" > /sys/module/cpu_boost/parameters/input_boost_freq
	echo 0 > /sys/module/cpu_boost/parameters/input_boost_ms

	echo 0 > /dev/cpuset/background/cpus
	echo 0 > /dev/cpuset/system-background/cpus
	echo 4-7 > /dev/cpuset/foreground/boost/cpus
	echo 0-7 > /dev/cpuset/foreground/cpus
	echo 0 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
	echo 4 > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
	echo 82 > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
	echo 55 > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres
	echo 1 > /dev/cpuset/background/cpus
	echo 1 > /dev/cpuset/system-background/cpus

	echo "67 1036800:73" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
	echo "78" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
	echo "0:2750000 1:2750000 2:2750000 3:2750000 4:1056000 5:1056000 6:1056000 7:1056000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 50000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 2750000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 50000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 1056000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
	
	echo 1036800 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 729600 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

	echo 710000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 100000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 8 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 6 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 8 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

	echo 0 > /proc/sys/kernel/sched_boost

	echo 0 > /sys/module/msm_thermal/core_control/enabled
	echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
	echo N > /sys/module/msm_thermal/parameters/enabled

    echo 1 > /sys/devices/system/cpu/cpu4/online
    echo 1 > /sys/devices/system/cpu/cpu5/online
    echo 0 > /sys/devices/system/cpu/cpu6/online
    echo 0 > /sys/devices/system/cpu/cpu7/online

	exit 0
fi

echo 1 > /sys/devices/system/cpu/cpu4/online
echo 1 > /sys/devices/system/cpu/cpu5/online
echo 1 > /sys/devices/system/cpu/cpu6/online
echo 1 > /sys/devices/system/cpu/cpu7/online

# Enable input boost configuration
echo "0" > /sys/module/cpu_boost/parameters/input_boost_freq
echo 0 > /sys/module/cpu_boost/parameters/input_boost_ms
echo "67 1804800:95" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
echo "73 1497600:83 1747200:87 1939200:90 2016000:95" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
echo 0-1 > /dev/cpuset/background/cpus
echo 0-3 > /dev/cpuset/system-background/cpus
echo 0-7 > /dev/cpuset/foreground/cpus
echo 4-7 > /dev/cpuset/foreground/boost/cpus

if [ "$action" = "balance" ]; then
	echo "0:2750000 1:2750000 2:2750000 3:2750000 4:1497600 5:1497600 6:1497600 7:1497600" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 2750000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 1497600 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
	
	echo 1248000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 806400 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 710000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 257000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 8 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 6 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

	echo 0 > /proc/sys/kernel/sched_boost

	echo 0 > /sys/module/msm_thermal/core_control/enabled
	echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
	echo N > /sys/module/msm_thermal/parameters/enabled

    echo 4 > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
    echo 2 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
    echo 80 > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
    echo 52 > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres

	exit 0
fi

if [ "$action" = "performance" ]; then
	echo "0:2750000 1:2750000 2:2750000 3:2750000 4:2035200 5:2035200 6:2035200 7:2035200" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 2750000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 2035200 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq

	echo 1555200 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 1267200 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 710000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 257000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 8 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 6 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

	echo 0 > /proc/sys/kernel/sched_boost

	echo 0 > /sys/module/msm_thermal/core_control/enabled
	echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
	echo N > /sys/module/msm_thermal/parameters/enabled

    echo 4 > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
    echo 2 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
    echo 75 > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
    echo 45 > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres

	exit 0
fi

if [ "$action" = "fast" ]; then
	echo "0:2750000 1:2750000 2:2750000 3:2750000 4:2750000 5:2750000 6:2750000 7:2750000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 50000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 2750000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 1267200 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 2750000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
	
	echo 1747200 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 2035200 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 850000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 257000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 8 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 5 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

	echo 0 > /sys/module/msm_thermal/core_control/enabled
	echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
	echo Y > /sys/module/msm_thermal/parameters/enabled

    echo 4 > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
    echo 4 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
    echo 65 > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
    echo 45 > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres

	echo 1 > /proc/sys/kernel/sched_boost
	
	exit 0
fi
