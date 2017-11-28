#!/system/bin/sh
setenforce 0
action=$1
if [ ! `cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor` = "interactive" ]; then 
	sh /system/etc/init.qcom.post_boot.sh
fi

#echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/max_freq_hysteresis

#powersave 1.2Ghz
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
	echo "0:960000 1:960000 2:960000 3:960000 4:960000 5:960000 6:960000 7:960000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 960000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 960000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
	
	echo 720000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 720000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
	
	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 710000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
	echo 100000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
	echo 8 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
	echo 6 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo 8 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

	echo 1 > /sys/module/msm_thermal/core_control/enabled
	echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
	echo Y > /sys/module/msm_thermal/parameters/enabled
	exit 0
fi

# Enable input boost configuration
echo "0:960000" > /sys/module/cpu_boost/parameters/input_boost_freq
echo 40 > /sys/module/cpu_boost/parameters/input_boost_ms
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
	echo "0:1280000 1:1280000 2:1280000 3:1280000 4:1635000 5:1635000 6:1635000 7:1635000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 1280000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 1635000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
	
	echo 960000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 960000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

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
	echo "0:1820000 1:1820000 2:1820000 3:1820000 4:1820000 5:2017000 6:2017000 7:2017000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 1820000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 2017000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq

	echo 1280000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 1280000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

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
	echo "0:2750000 1:2750000 2:2750000 3:2750000 4:2750000 5:2750000 6:2750000 7:2750000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 2750000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 1280000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 2750000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
	
	echo 1536000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 1536000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
	
	echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
	echo 710000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
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
