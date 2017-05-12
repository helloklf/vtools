#!/system/bin/sh

action=$1

if [ "$action" = "powersave" ]; then
	echo "0:864000 1:864000 2:864000 3:864000 4:384000 5:384000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo "0" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_sched_load
	echo "48" /sys/module/msm_thermal/core_control/cpus_offlined
	echo "0" > /sys/devices/system/cpu/cpu4/online
	echo "0" > /sys/devices/system/cpu/cpu5/online
	echo "0" > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
	echo "0" > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
	
	echo 864000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 384000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	exit 0
fi

if [ "$action" = "performance" ]; then
	echo "0:1636000 1:1636000 2:1636000 3:1636000 4:384000 5:384000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo "0" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_sched_load
	echo "48" /sys/module/msm_thermal/core_control/cpus_offlined
	echo "0" > /sys/devices/system/cpu/cpu4/online
	echo "0" > /sys/devices/system/cpu/cpu5/online
	echo "0" > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
	echo "0" > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
	
	echo 1636000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 384000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	exit 0
fi

if [ "$action" = "fast" ]; then
	echo "0:1636000 1:1636000 2:1636000 3:1636000 4:384000 5:384000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo "0" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_sched_load
	echo "48" /sys/module/msm_thermal/core_control/cpus_offlined
	echo "0" > /sys/devices/system/cpu/cpu4/online
	echo "0" > /sys/devices/system/cpu/cpu5/online
	echo "0" > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
	echo "0" > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
	
	echo 1636000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 960000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	exit 0
fi

if [ "$action" = "balance" ]; then
	echo "0:1248000 1:1248000 2:1248000 3:1248000 4:384000 5:384000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo "0" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_sched_load
	echo "48" /sys/module/msm_thermal/core_control/cpus_offlined
	echo "0" > /sys/devices/system/cpu/cpu4/online
	echo "0" > /sys/devices/system/cpu/cpu5/online
	echo "0" > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
	echo "0" > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
	
	echo 1248000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 384000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	exit 0
fi