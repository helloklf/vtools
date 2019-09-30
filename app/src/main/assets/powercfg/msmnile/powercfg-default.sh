#!/system/bin/sh

action=$1
stop perfd

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
# 300000 403200 499200 576000 672000 768000 844800 940800 1036800 1113600 1209600 1305600 1382400 1478400 1555200 1632000 1708800 1785600

# /sys/devices/system/cpu/cpu4/cpufreq/scaling_available_frequencies
# 710400 825600 940800 1056000 1171200 1286400 1401600 1497600 1612800 1708800 1804800 1920000 2016000 2131200 2227200 2323200 2419200

# /sys/devices/system/cpu/cpu7/cpufreq/scaling_available_frequencies
# 825600  940800 1056000 1171200 1286400 1401600 1497600 1612800 1708800 1804800 1920000 2016000 2131200 2227200 2323200 2419200 2534400 2649600 2745600 2841600

# GPU
# 810000000 585000000 499200000 427000000 345000000 257000000

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

# GPU频率表
gpu_freqs=`cat /sys/class/kgsl/kgsl-3d0/devfreq/available_frequencies`
# GPU最大频率
gpu_max_freq='585000000'
# GPU最小频率
gpu_min_freq='257000000'
# GPU最小 power level
gpu_min_pl=5
# GPU最大 power level
gpu_max_pl=0
# GPU默认 power level
gpu_default_pl=`cat /sys/class/kgsl/kgsl-3d0/default_pwrlevel`
# GPU型号
gpu_model=`cat /sys/class/kgsl/kgsl-3d0/gpu_model`
# GPU调度器
gpu_governor=`cat /sys/class/kgsl/kgsl-3d0/devfreq/governor`

# MaxFrequency、MinFrequency
for freq in $gpu_freqs; do
    if [[ $freq -gt $gpu_max_freq ]]; then
        gpu_max_freq=$freq
    fi;
    if [[ $freq -lt $gpu_min_freq ]]; then
        gpu_min_freq=$freq
    fi;
done

# Power Levels
if [[ -f /sys/class/kgsl/kgsl-3d0/num_pwrlevels ]];then
    gpu_min_pl=`cat /sys/class/kgsl/kgsl-3d0/num_pwrlevels`
    gpu_min_pl=`expr $gpu_min_pl - 1`
fi;
if [[ "$gpu_min_pl" -lt 0 ]];then
    gpu_min_pl=0
fi;

if [ ! "$gpu_governor" = "msm-adreno-tz" ]; then
	echo 'msm-adreno-tz' > /sys/class/kgsl/kgsl-3d0/devfreq/governor
fi

echo $gpu_max_freq > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
echo $gpu_min_freq > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
echo $gpu_max_pl > /sys/class/kgsl/kgsl-3d0/max_pwrlevel


function set_input_boost_freq() {
    local c0="$1"
    local c1="$2"
    local c2="$3"
    local ms="$4"
    echo "0:$c0 1:$c0 2:$c0 3:$c0 4:$c1 5:$c1 6:$c1 7:$c2" > /sys/module/cpu_boost/parameters/input_boost_freq
	echo $ms > /sys/module/cpu_boost/parameters/input_boost_ms
}

function set_cpu_freq()
{
    echo $1 $2 $3 $4
	echo "0:$2 1:$2 2:$2 3:$2 4:$4 5:$4 6:$4 7:$6" > /sys/module/msm_performance/parameters/cpu_max_freq
	echo $1 > /sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq
	echo $2 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq
	echo $3 > /sys/devices/system/cpu/cpufreq/policy4/scaling_min_freq
	echo $4 > /sys/devices/system/cpu/cpufreq/policy4/scaling_max_freq
	echo $5 > /sys/devices/system/cpu/cpufreq/policy7/scaling_min_freq
	echo $6 > /sys/devices/system/cpu/cpufreq/policy7/scaling_max_freq
}

if [ "$action" = "powersave" ]; then
	set_cpu_freq 300000 1420800 710400 1420800 825600 1420800
    set_input_boost_freq 0 0 0 0

	echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

	echo 1209600 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
	echo 825600 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
	echo 940800 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq

    echo 1-2 > /dev/cpuset/background/cpus
    echo 1-4 > /dev/cpuset/system-background/cpus

	exit 0
fi

if [ "$action" = "balance" ]; then
	set_cpu_freq 300000 1420800 710400 1420800 825600 1996800
    set_input_boost_freq 1209600 0 0 40

	echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

	echo 1478400 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
	echo 1056000 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
	echo 1286400 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq

    echo 1-2,4 > /dev/cpuset/background/cpus
    echo 1-4 > /dev/cpuset/system-background/cpus

	exit 0
fi

if [ "$action" = "performance" ]; then
	set_cpu_freq 300000 1785600 710400 2419200 825600 2841600
    set_input_boost_freq 1478400 1286400 1286400 40

	echo `expr $gpu_min_pl - 1` > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 0 > /proc/sys/kernel/sched_boost

    echo 1632000 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
    echo 1708800 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
    echo 2016000 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq

    echo 1-2 > /dev/cpuset/background/cpus
    echo 1-2 > /dev/cpuset/system-background/cpus

	exit 0
fi

if [ "$action" = "fast" ]; then
	set_cpu_freq 1036800 1785600 1286400 2600000 1286400 3200000
    set_input_boost_freq 1708800 1612800 1804800 80

	echo 1632000 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
	echo 1612800 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
	echo 1708800 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq

	echo `expr $gpu_min_pl - 1` > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
	echo 1 > /proc/sys/kernel/sched_boost

    echo 1 > /dev/cpuset/background/cpus
    echo 1-2 > /dev/cpuset/system-background/cpus

	exit 0
fi
