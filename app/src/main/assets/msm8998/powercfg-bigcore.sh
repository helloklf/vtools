#!/system/bin/sh

action=$1
stop perfd

echo 0 > /sys/module/msm_thermal/core_control/enabled
echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
echo N > /sys/module/msm_thermal/parameters/enabled
echo 1 > /proc/sys/kernel/sched_prefer_sync_wakee_to_waker

#if [ ! `cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor` = "interactive" ]; then
#	sh /system/etc/init.qcom.post_boot.sh
#fi

# /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies
# 300000 364800 441600 518400 595200 672000 748800 825600 883200 960000 1036800 1094400 1171200 1248000 1324800 1401600 1478400 1555200 1670400 1747200 1824000 1900800

# /sys/devices/system/cpu/cpu4/cpufreq/scaling_available_frequencies
# 300000 345600 422400 499200 576000 652800 729600 806400 902400 979200 1056000 1132800 1190400 1267200 1344000 1420800 1497600 1574400 1651200 1728000 1804800 1881600 1958400 2035200 2112000 2208000 2265600 2323200 2342400 2361600 2457600

echo 4-7 > /dev/cpuset/foreground/boost/cpus
echo 0-7 > /dev/cpuset/foreground/cpus

echo 1 > /sys/devices/system/cpu/cpu0/online
echo 1 > /sys/devices/system/cpu/cpu1/online
echo 1 > /sys/devices/system/cpu/cpu2/online
echo 1 > /sys/devices/system/cpu/cpu3/online
echo 1 > /sys/devices/system/cpu/cpu4/online
echo 1 > /sys/devices/system/cpu/cpu5/online
echo 1 > /sys/devices/system/cpu/cpu6/online
echo 1 > /sys/devices/system/cpu/cpu7/online

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

function set_cpu_freq()
{
    echo $1 $2 $3 $4
	echo "0:$2 1:$2 2:$2 3:$2 4:$4 5:$4 6:$4 7:$4" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo $1 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo $2 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo $3 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo $4 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
}

if [ "$action" = "powersave" ]; then
	echo "0" > /sys/module/cpu_boost/parameters/input_boost_freq
	echo 0 > /sys/module/cpu_boost/parameters/input_boost_ms

	set_cpu_freq 5000 1401600 5000 1344000

	echo "85 300000:85 595200:67 825600:75 1248000:78" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
	echo 518400 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq

	echo "87 300000:89 1056000:89 1344000:92" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
	echo 499200 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

	echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

    echo 0-2 > /dev/cpuset/background/cpus
    echo 0-3 > /dev/cpuset/system-background/cpus

    echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
	echo 10000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
	echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
    echo 10000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate

	exit 0
fi


echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy

if [ "$action" = "balance" ]; then
	set_cpu_freq 5000 1670400 5000 1804800

    echo "0:1248000 1:1248000 2:1248000 3:1248000 4:0 5:0 6:0 7:0" > /sys/module/cpu_boost/parameters/input_boost_freq
    echo 40 > /sys/module/cpu_boost/parameters/input_boost_ms

	echo "83 300000:85 595200:67 825600:75 1248000:78 1478400:83" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
	echo 960000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq

	echo "83 300000:89 1056000:89 1344000:92 1651200:95" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
	echo 1056000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

	echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

    echo 0-1 > /dev/cpuset/background/cpus
    echo 0-3 > /dev/cpuset/system-background/cpus

    echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
	echo 10000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
	echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
    echo 10000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate

	exit 0
fi

if [ "$action" = "performance" ]; then
	set_cpu_freq 5000 1900800 5000 2457600

    echo "0:1248000 1:1248000 2:1248000 3:1248000 4:0 5:0 6:0 7:0" > /sys/module/cpu_boost/parameters/input_boost_freq
    echo 40 > /sys/module/cpu_boost/parameters/input_boost_ms

    echo "73 960000:72 1478400:78 1804800:87 2208000:93" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
    echo 1478400 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq

    echo "78 1497600:80 2016000:87" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
    echo 1267200 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

	echo `expr $gpu_min_pl - 1` > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

    echo 0-1 > /dev/cpuset/background/cpus
    echo 0-1 > /dev/cpuset/system-background/cpus

    echo 10000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
    echo 30000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
    echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
    echo 79000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis

    stop thermanager
    stop thermal-engine

	exit 0
fi

if [ "$action" = "fast" ]; then
	set_cpu_freq 5000 2750000 1056000 2750000

    echo "0:0 1:0 2:0 3:0 4:1804800 5:1804800 6:1804800 7:1804800" > /sys/module/cpu_boost/parameters/input_boost_freq
    echo 50 > /sys/module/cpu_boost/parameters/input_boost_ms

    echo "72 960000:72 1478400:78 1804800:87 2208000:91" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
	echo 1036800 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq

    echo "73 1497600:78 2016000:87" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
	echo 1497600 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

	echo `expr $gpu_min_pl - 1` > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 1 > /proc/sys/kernel/sched_boost

    #echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/boost
    #echo 8000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/boostpulse_duration

    echo 0 > /dev/cpuset/background/cpus
    echo 0-1 > /dev/cpuset/system-background/cpus

	echo 8000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
    echo 24000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
    echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
    echo 79000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
	
	exit 0
fi
