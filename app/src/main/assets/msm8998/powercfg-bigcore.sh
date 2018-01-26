#!/system/bin/sh

setenforce 0

action=$1

if [ ! `cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor` = "interactive" ]; then 
	sh /system/etc/init.qcom.post_boot.sh
fi

#powersave 1.6Ghz
if [ "$action" = "powersave" ]; then
	echo "0" > /sys/module/cpu_boost/parameters/input_boost_freq
	echo 0 > /sys/module/cpu_boost/parameters/input_boost_ms

	echo 0 > /dev/cpuset/background/cpus
	echo 0 > /dev/cpuset/system-background/cpus
	echo 4-7 > /dev/cpuset/foreground/boost/cpus
	echo 0-7 > /dev/cpuset/foreground/cpus
	echo 0 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
	echo 4 > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
	echo 95 > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
	echo 60 > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres
	echo 1 > /dev/cpuset/background/cpus
	echo 1 > /dev/cpuset/system-background/cpus

	echo "75 960000:95" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
	echo "87 700000:95" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
	echo "0:1036800 1:1036800 2:1036800 3:1036800 4:1267200 5:1267200 6:1267200 7:1267200" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 50000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 1036800 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 50000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 1267200 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq

	echo 748800 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 902400 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 710000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 100000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 8 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 6 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 8 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

	echo 0 > /sys/module/msm_thermal/core_control/enabled
	echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
	echo N > /sys/module/msm_thermal/parameters/enabled

	exit 0
fi


echo 1 > /sys/devices/system/cpu/cpu4/online
echo 1 > /sys/devices/system/cpu/cpu5/online
echo 1 > /sys/devices/system/cpu/cpu6/online
echo 1 > /sys/devices/system/cpu/cpu7/online

echo 2 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
echo 4 > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
echo "78 1804800:95" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
echo "83 1939200:90 2016000:95" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
echo 0 > /dev/cpuset/background/cpus
echo 0-2 > /dev/cpuset/system-background/cpus
echo 4-7 > /dev/cpuset/foreground/boost/cpus
echo 0-7 > /dev/cpuset/foreground/cpus
echo 1 > /dev/cpuset/background/cpus
echo 1-2 > /dev/cpuset/system-background/cpus

if [ "$action" = "balance" ]; then
    echo "0:1248000" > /sys/module/cpu_boost/parameters/input_boost_freq
    echo 40 > /sys/module/cpu_boost/parameters/input_boost_ms
	
	echo "0:1824000 1:1824000 2:1824000 3:1824000 4:2208000 5:2208000 6:2208000 7:2208000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 300000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 1824000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 300000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 2208000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
	
	echo 1248000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 1267200 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
	
	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 710000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 257000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 8 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 6 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

    echo 1 > /sys/module/msm_thermal/core_control/enabled
    echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
    echo Y > /sys/module/msm_thermal/parameters/enabled

	exit 0
fi

if [ "$action" = "performance" ]; then
    echo "0:1248000" > /sys/module/cpu_boost/parameters/input_boost_freq
    echo 40 > /sys/module/cpu_boost/parameters/input_boost_ms

	echo "0:1900800 1:1900800 2:1900800 3:1900800 4:2457600 5:2457600 6:2457600 7:2457600" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 300000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 1900800 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 2457600 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
	
	echo 1248000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
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
	
	exit 0
fi

if [ "$action" = "fast" ]; then
    echo "4:1804800" > /sys/module/cpu_boost/parameters/input_boost_freq
    echo 80 > /sys/module/cpu_boost/parameters/input_boost_ms

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
	echo 1 > /proc/sys/kernel/sched_boost

    echo 0 > /sys/module/msm_thermal/core_control/enabled
    echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
    echo Y > /sys/module/msm_thermal/parameters/enabled
	
	exit 0
fi
