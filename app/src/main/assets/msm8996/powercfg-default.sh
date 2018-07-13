#!/system/bin/sh

action=$1
stop perfd

echo 1 > /proc/sys/kernel/sched_prefer_sync_wakee_to_waker

echo "0" > /sys/module/cpu_boost/parameters/input_boost_freq
echo 0 > /sys/module/cpu_boost/parameters/input_boost_ms
echo 0 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/max_freq_hysteresis
echo 45 > /proc/sys/kernel/sched_downmigrate
echo 45 > /proc/sys/kernel/sched_upmigrate

function lock_value()
{
    value=$1
    path=$2
    if [[ -f $path ]]; then
        current_value="$(cat $path)"
        if [[ ! "$current_value" = "$value" ]]; then
            chmod 0664 "$path"
            echo "$value" > "$path"
            chmod 0444 "$path"
        fi;
    fi;
}
lock_value 0 /sys/devices/system/cpu/cpu0/cpufreq/interactive/boost
lock_value 0 /sys/devices/system/cpu/cpu4/cpufreq/interactive/boost

function gpu_config()
{
    gpu_freqs=`cat /sys/class/kgsl/kgsl-3d0/devfreq/available_frequencies`
    max_freq='710000000'
    for freq in $gpu_freqs; do
        if [[ $freq -gt $max_freq ]]; then
            max_freq=$freq
        fi;
    done
    gpu_min_pl=6
    if [[ -f /sys/class/kgsl/kgsl-3d0//num_pwrlevels ]];then
        gpu_min_pl=`cat /sys/class/kgsl/kgsl-3d0//num_pwrlevels`
        gpu_min_pl=`expr $gpu_min_pl - 1`
    fi;

    if [[ "$gpu_min_pl" = "-1" ]];then
        $gpu_min_pl=1
    fi;

    echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
    #echo 710000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
    echo $max_freq > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
    #echo 257000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
    echo 100000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
    echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
    echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
}

gpu_config

if [ "$action" = "powersave" ]; then
	echo "0:850000 1:850000 2:960000 3:960000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 300000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 850000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 300000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_min_freq
	echo 960000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_max_freq
	
	echo 480000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 480000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/hispeed_freq

	echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

    echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
	echo 10000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
	echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
    echo 10000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate

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

	echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

    echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
	echo 10000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
	echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
    echo 10000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate

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

	echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo `expr $gpu_min_pl - 1` > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

	echo 10000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
    echo 30000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
    echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
    echo 79000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis

	echo 0 > /proc/sys/kernel/sched_boost
    stop thermanager
    stop thermal-engine
	
	exit 0
fi

if [ "$action" = "fast" ]; then
	echo "0:2500000 1:2500000 2:2500000 3:2500000" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo 1150000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 2500000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 1248000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_min_freq
	echo 2500000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_max_freq
	
	echo 1150000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 1248000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/hispeed_freq

	echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
	echo `expr $gpu_min_pl - 1` > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	
	echo 1 > /proc/sys/kernel/sched_boost

	echo 8000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
    echo 24000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
    echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
    echo 79000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
	
	exit 0
fi
