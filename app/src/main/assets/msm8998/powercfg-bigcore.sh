#!/system/bin/sh

setenforce 0

action=$1

if [ ! `cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor` = "interactive" ]; then 
	sh /system/etc/init.qcom.post_boot.sh
fi

echo "0:1324800" > /sys/module/cpu_boost/parameters/input_boost_freq
echo 40 > /sys/module/cpu_boost/parameters/input_boost_ms

#powersave 1.6Ghz
if [ "$action" = "powersave" ]; then
	echo "83 720000:90 960000:95" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
	echo "83 960000:95" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
	echo "0:1280000 1:1280000 2:1280000 3:1280000 4:1280000 5:1280000 6:1280000 7:1280000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 1280000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 1280000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
	
	echo 720000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 720000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
	
	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 710000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 100000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 8 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 6 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 7 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

	exit 0
fi

echo "83 1804800:95" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
echo "83 1939200:90 2016000:95" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads

if [ "$action" = "balance" ]; then
	
	echo "0:1820000 1:1820000 2:1820000 3:1820000 4:2017000 5:2017000 6:2017000 7:2017000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 1820000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 2017000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
	
	echo 960000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 960000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
	
	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 710000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 257000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 8 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 6 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

	exit 0
fi

if [ "$action" = "performance" ]; then
	echo "0:2016000 1:2016000 2:2016000 3:2016000 4:2560000 5:2560000 6:2560000 7:2560000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 2016000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 2560000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
	
	echo 1280000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 1280000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
	
	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 710000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 257000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 8 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 6 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost
	
	exit 0
fi

if [ "$action" = "fast" ]; then
	echo "0:2016000 1:2016000 2:2016000 3:2016000 4:2560000 5:2560000 6:2560000 7:2560000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 2016000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 1280000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 2560000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
	
	echo 1536000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 1536000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
	
	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 710000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 257000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 8 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 5 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 1 > /proc/sys/kernel/sched_boost
	
	exit 0
fi