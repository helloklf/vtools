#!/system/bin/sh

action=$1
stop perfd

echo 1 > /sys/devices/system/cpu/cpu0/online
echo 1 > /sys/devices/system/cpu/cpu1/online
echo 1 > /sys/devices/system/cpu/cpu2/online
echo 1 > /sys/devices/system/cpu/cpu3/online
echo 1 > /sys/devices/system/cpu/cpu4/online
echo 1 > /sys/devices/system/cpu/cpu5/online
echo 1 > /sys/devices/system/cpu/cpu6/online
echo 1 > /sys/devices/system/cpu/cpu7/online

echo 0 > /sys/module/msm_thermal/core_control/enabled
echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
echo N > /sys/module/msm_thermal/parameters/enabled

governor0=`cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor`
governor4=`cat /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor`

if [ ! "$governor0" = "schedutil" ]; then
	echo 'schedutil' > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
fi
if [ ! "$governor4" = "schedutil" ]; then
	echo 'schedutil' > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor
fi

# /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies
# 300000 403200 480000 576000 652800 748800 825600 902400 979200 1056000 1132800 1228800 1324800 1420800 1516800 1612800 1689600 1766400

# /sys/devices/system/cpu/cpu4/cpufreq/scaling_available_frequencies
# 825600 902400 979200 1056000 1209600 1286400 1363200 1459200 1536000 1612800 1689600 1766400 1843200 1920000 1996800 2092800 2169600 2246400 2323200 2400000 2476800 2553600 2649600

function set_value()
{
    value=$1
    path=$2
    if [[ -f $path ]]; then
        current_value="$(cat $path)"
        if [[ ! "$current_value" = "$value" ]]; then
            chmod 0664 "$path"
            echo "$value" > "$path"
        fi;
    fi;
}

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
    if [[ -f /sys/class/kgsl/kgsl-3d0/num_pwrlevels ]];then
        gpu_min_pl=`cat /sys/class/kgsl/kgsl-3d0/num_pwrlevels`
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

function schedutil_cfg()
{
    set_value $2 /sys/devices/system/cpu/cpu$1/cpufreq/schedutil/down_rate_limit_us
    set_value $3 /sys/devices/system/cpu/cpu$1/cpufreq/schedutil/up_rate_limit_us
    set_value $4 /sys/devices/system/cpu/cpu$1/cpufreq/schedutil/iowait_boost_enable
}

if [ "$action" = "powersave" ]; then
	set_cpu_freq 5000 1516800 5000 1612800

	echo "0" > /sys/module/cpu_boost/parameters/input_boost_freq
	echo 0 > /sys/module/cpu_boost/parameters/input_boost_ms

	echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

	echo "85 300000:85 518400:67 748800:75 1228800:78" > /sys/devices/system/cpu/cpu0/cpufreq/schedutil/target_loads
	echo 518400 > /sys/devices/system/cpu/cpu0/cpufreq/schedutil/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/schedutil/io_is_busy
    schedutil_cfg 0 1000 10000 0

	echo "99" > /sys/devices/system/cpu/cpu4/cpufreq/schedutil/target_loads
	echo 652800 > /sys/devices/system/cpu/cpu4/cpufreq/schedutil/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/schedutil/io_is_busy
    schedutil_cfg 4 1000 10000 0

	exit 0
fi

if [ "$action" = "balance" ]; then
	set_cpu_freq 5000 1766400 5000 1996800

    echo "0:1228800 1:1228800 2:1228800 3:1228800 4:0 5:0 6:0 7:0" > /sys/module/cpu_boost/parameters/input_boost_freq
    echo 40 > /sys/module/cpu_boost/parameters/input_boost_ms

	echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

    echo "83 300000:85 595200:67 825600:75 1228800:78" > /sys/devices/system/cpu/cpu0/cpufreq/schedutil/target_loads
	echo 960000 > /sys/devices/system/cpu/cpu0/cpufreq/schedutil/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/schedutil/io_is_busy
    schedutil_cfg 0 1000 5000 0

    echo "83 300000:89 1056000:89 1344000:92" > /sys/devices/system/cpu/cpu4/cpufreq/schedutil/target_loads
	echo 1056000 > /sys/devices/system/cpu/cpu4/cpufreq/schedutil/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/schedutil/io_is_busy
    schedutil_cfg 4 1000 5000 0

	exit 0
fi

echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/schedutil/io_is_busy
if [ "$action" = "performance" ]; then
	set_cpu_freq 5000 1766400 5000 2649600

    echo "0:1228800 1:1228800 2:1228800 3:1228800 4:0 5:0 6:0 7:0" > /sys/module/cpu_boost/parameters/input_boost_freq
    echo 40 > /sys/module/cpu_boost/parameters/input_boost_ms

	echo `expr $gpu_min_pl - 1` > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

    echo "73 960000:72 1478400:78 1804800:87" > /sys/devices/system/cpu/cpu0/cpufreq/schedutil/target_loads
    echo 1478400 > /sys/devices/system/cpu/cpu0/cpufreq/schedutil/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/schedutil/io_is_busy
    schedutil_cfg 0 1000 1000 1

    echo "78 1420800:80 2016000:87" > /sys/devices/system/cpu/cpu4/cpufreq/schedutil/target_loads
    echo 1267200 > /sys/devices/system/cpu/cpu4/cpufreq/schedutil/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/schedutil/io_is_busy
    schedutil_cfg 4 1000 1000 1

	exit 0
fi

if [ "$action" = "fast" ]; then
	set_cpu_freq 5000 3000000 1267200 3500000

    echo "0:0 1:0 2:0 3:0 4:1804800 5:1804800 6:1804800 7:1804800" > /sys/module/cpu_boost/parameters/input_boost_freq
    echo 80 > /sys/module/cpu_boost/parameters/input_boost_ms

    echo "72 960000:72 1478400:78 1804800:87" > /sys/devices/system/cpu/cpu0/cpufreq/schedutil/target_loads
	echo 1036800 > /sys/devices/system/cpu/cpu0/cpufreq/schedutil/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/schedutil/io_is_busy
    schedutil_cfg 0 1000 1000 1

    echo "73 1420800:78 2016000:87" > /sys/devices/system/cpu/cpu4/cpufreq/schedutil/target_loads
	echo 1420800 > /sys/devices/system/cpu/cpu4/cpufreq/schedutil/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/schedutil/io_is_busy
    schedutil_cfg 4 1000 1000 1

	echo `expr $gpu_min_pl - 1` > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 1 > /proc/sys/kernel/sched_boost

	exit 0
fi
