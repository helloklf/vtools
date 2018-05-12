#!/system/bin/sh

action=$1

if [ ! `cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor` = "interactive" ]; then 
	sh /system/etc/init.qcom.post_boot.sh
fi

echo "0" > /sys/module/cpu_boost/parameters/input_boost_freq
echo 0 > /sys/module/cpu_boost/parameters/input_boost_ms
echo 0 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/max_freq_hysteresis
echo 45 > /proc/sys/kernel/sched_downmigrate
echo 45 > /proc/sys/kernel/sched_upmigrate

if [ "$action" = "powersave" ]; then
	echo "0:850000 1:850000 2:960000 3:960000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 300000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 850000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 300000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_min_freq
	echo 960000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_max_freq
	
	echo 480000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 480000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/hispeed_freq
	
	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 133000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 133000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 6 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 6 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 6 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

	exit 0
fi

if [ "$action" = "balance" ]; then
	echo "0:1248000 1:1248000 2:1555000 3:1555000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 300000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 1248000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 300000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_min_freq
	echo 1555000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_max_freq
	
	echo 600000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 700000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/hispeed_freq
	
	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 315000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 133000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 6 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 4 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 6 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

	exit 0
fi

if [ "$action" = "performance" ]; then
	echo "0:1824000 1:1824000 2:1824000 3:1824000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 300000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 1824000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 300000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_min_freq
	echo 1824000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_max_freq
	
	echo 1228800 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 1036800 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/hispeed_freq
	
	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 510000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 133000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 6 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 2 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 6 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost
	
	exit 0
fi

if [ "$action" = "fast" ]; then
	echo "0:2500000 1:2500000 2:2500000 3:2500000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 1150000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 2500000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 1248000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_min_freq
	echo 2500000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_max_freq
	
	echo 2500000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 2500000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/hispeed_freq
	
	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 750000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 133000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 6 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 2 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	
	echo 1 > /proc/sys/kernel/sched_boost
	
	exit 0
fi
