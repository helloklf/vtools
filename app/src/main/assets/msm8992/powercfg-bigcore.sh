#!/system/bin/sh

profile=$1
action=$2

# Disable thermal and bcl hotplug to switch governor
echo 0 > /sys/module/msm_thermal/core_control/enabled
for mode in /sys/devices/soc.0/qcom,bcl.*/mode
do
    echo -n disable > $mode
done
for hotplug_mask in /sys/devices/soc.0/qcom,bcl.*/hotplug_mask
do
    bcl_hotplug_mask=`cat $hotplug_mask`
    echo 0 > $hotplug_mask
done
for hotplug_soc_mask in /sys/devices/soc.0/qcom,bcl.*/hotplug_soc_mask
do
    bcl_soc_hotplug_mask=`cat $hotplug_soc_mask`
    echo 0 > $hotplug_soc_mask
done
for mode in /sys/devices/soc.0/qcom,bcl.*/mode
do
    echo -n enable > $mode
done

case "$profile" in
    "powersave")
		#echo "PowerSaveMode"
		echo "0:960000 1:960000 2:960000 3:960000 4:384000 5:384000" > /sys/module/msm_performance/parameters/cpu_max_freq
		echo "interactive" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
		echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_sched_load
		echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_migration_notif
		echo 30000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay
		echo 99 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
		echo 20000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
		echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
		echo 384000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
		echo "87 384000:97 460800:93 600000:95 672000:97 787200:98 960000:99" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
		
		echo 30000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
		echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
		echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/boostpulse_duration
		echo 384000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
		
		echo "960000" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq

		echo "0" > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
		echo "0" > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
		echo 0 > /sys/devices/system/cpu/cpu4/online
		echo 0 > /sys/devices/system/cpu/cpu5/online
		
		# 0ms input boost
		echo 0 > /sys/module/cpu_boost/parameters/input_boost_freq
		echo 0 > /sys/module/cpu_boost/parameters/input_boost_ms		

		echo "powersave" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
		echo 180000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
		echo 5 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
		echo 5 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
		echo 5 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
		echo 48 > /sys/module/msm_thermal/core_control/cpus_offlined
        ;;
	"balance")
		#echo "DefaultMode"
        echo "0:1636000 1:1636000 2:1636000 3:1636000 4:384000 5:384000" > /sys/module/msm_performance/parameters/cpu_max_freq

		echo "interactive" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
		echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_sched_load
		echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_migration_notif
		echo 30000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay
		echo 87 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
		echo 20000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
		echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
		echo 762000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
		echo "85 384000:96 460800:95 600000:96 672000:82 787200:70 864000:75 960000:80 1248000:85 1440000:90 1636000:95" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
		
		echo 30000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
		echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
		echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/boostpulse_duration
		echo 384000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
		
		echo "1636000" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
		
		echo "0" > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
		echo "0" > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
		echo 0 > /sys/devices/system/cpu/cpu4/online
		echo 0 > /sys/devices/system/cpu/cpu5/online
		
		# 0ms input boost
		echo 0 > /sys/module/cpu_boost/parameters/input_boost_freq
		echo 0 > /sys/module/cpu_boost/parameters/input_boost_ms
		
		echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
		echo 367000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
		echo 5 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
		echo 3 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
		echo 5 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

		echo 48 > /sys/module/msm_thermal/core_control/cpus_offlined
        ;;
	"performance")
		#echo "GameMode"
		echo "2" > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
		echo "2" > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
		
		echo 1 > /sys/devices/system/cpu/cpu4/online
		echo 1 > /sys/devices/system/cpu/cpu5/online
		
        echo "0:1636000 1:1636000 2:1636000 3:1636000 4:2016000 5:2016000" > /sys/module/msm_performance/parameters/cpu_max_freq
		echo 0 > /sys/module/msm_thermal/core_control/cpus_offlined
		
		echo "interactive" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
		echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_sched_load
		echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_migration_notif
		echo 19000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay
		echo 87 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
		echo 20000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
		echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
		echo 762000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
        echo "78 1248000:80 1440000:85 1636000:89" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads        
        echo 20000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
        echo 10000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
		echo "40" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/boostpulse_duration
		
		echo "1636000" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
		
        echo "interactive" > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor
        echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/use_sched_load
        echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/use_migration_notif
        echo "50000 1440000:20000" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/above_hispeed_delay
        echo 90 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/go_hispeed_load
        echo 50000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate
        echo 633600 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
        echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
        echo "82 1248000:78 1440000:87 1636000:90" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
		echo "0" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/boostpulse_duration
        echo 30000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/min_sample_time
        echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/max_freq_hysteresis

		echo "2" > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
		echo "0" > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
        echo "78 82" > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
        echo "50 55" > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres
        echo 2000 > /sys/devices/system/cpu/cpu4/core_ctl/offline_delay_ms

		# 0ms input boost
		echo "0:600000 1:600000 2:600000 3:600000" > /sys/module/cpu_boost/parameters/input_boost_freq
		echo 40 > /sys/module/cpu_boost/parameters/input_boost_ms

		echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
		echo 700000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
		echo 180000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
		echo 5 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
		echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
		echo 5 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
        ;;
	"fast")
		#echo "FastMode"
		echo "2" > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
		echo "2" > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
		
		echo 1 > /sys/devices/system/cpu/cpu4/online
		echo 1 > /sys/devices/system/cpu/cpu5/online
		
        echo "0:1636000 1:1636000 2:1636000 3:1636000 4:2016000 5:2016000" > /sys/module/msm_performance/parameters/cpu_max_freq
		echo 0 > /sys/module/msm_thermal/core_control/cpus_offlined
		
		echo "interactive" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
		echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_sched_load
		echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_migration_notif
		echo 19000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay
		echo 87 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
		echo 20000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
		echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
		echo 762000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
        echo "78 1248000:80 1440000:85 1636000:89" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads        
        echo 20000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
        echo 10000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
		echo "40" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/boostpulse_duration
		
		echo "1636000" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
		
        echo "interactive" > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor
        echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/use_sched_load
        echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/use_migration_notif
        echo "50000 1440000:20000" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/above_hispeed_delay
        echo 90 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/go_hispeed_load
        echo 50000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate
        echo 633600 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
        echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
        echo "82 1248000:78 1440000:87 1636000:90" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
		echo "0" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/boostpulse_duration
        echo 30000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/min_sample_time
        echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/max_freq_hysteresis

		echo "2" > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
		echo "0" > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
        echo "78 82" > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
        echo "50 55" > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres
        echo 2000 > /sys/devices/system/cpu/cpu4/core_ctl/offline_delay_ms

		# 0ms input boost
		echo "0:600000 1:600000 2:600000 3:600000" > /sys/module/cpu_boost/parameters/input_boost_freq
		echo 40 > /sys/module/cpu_boost/parameters/input_boost_ms

		echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
		echo 700000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
		echo 180000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
		echo 5 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
		echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
		echo 5 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
        ;;
esac

# Re-enable thermal and BCL hotplug
echo 1 > /sys/module/msm_thermal/core_control/enabled
for mode in /sys/devices/soc.0/qcom,bcl.*/mode
do
    echo -n disable > $mode
done
for hotplug_mask in /sys/devices/soc.0/qcom,bcl.*/hotplug_mask
do
    echo $bcl_hotplug_mask > $hotplug_mask
done
for hotplug_soc_mask in /sys/devices/soc.0/qcom,bcl.*/hotplug_soc_mask
do
    echo $bcl_soc_hotplug_mask > $hotplug_soc_mask
done
for mode in /sys/devices/soc.0/qcom,bcl.*/mode
do
    echo -n enable > $mode
done