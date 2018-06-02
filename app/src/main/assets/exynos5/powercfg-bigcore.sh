#!/system/bin/sh
action=$1

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

function cpuset()
{
    set_value '0' /dev/cpuset/system-background/cpus
    set_value '1' /dev/cpuset/background/cpus
    set_value '4-7' /dev/cpuset/foreground/boost/cpus
    set_value '2-7' /dev/cpuset/foreground/cpus
    set_value '2-7' /dev/cpuset/top-app/cpus
    set_value "4-7" /dev/cpuset/dex2oat/cpus
    #set_value 0 /proc/sys/kernel/sched_boost
}

function disabled_hotplug()
{
    set_value 0 /sys/power/cpuhotplug/enabled;
    set_value 0 /sys/devices/system/cpu/cpuhotplug/enabled;
}

cpuset

if [ "$action" = "powersave" ]; then
    set_value 5 /sys/devices/system/cpu/cpuhotplug/max_online_cpu
    disabled_hotplug
    set_value 1 /sys/devices/system/cpu/cpu4/online
    set_value 0 /sys/devices/system/cpu/cpu5/online
    set_value 0 /sys/devices/system/cpu/cpu6/online
    set_value 0 /sys/devices/system/cpu/cpu7/online
    set_value "67 1036800:73" /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
    set_value "78" /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
    set_value 50000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    set_value 1900000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
    set_value 50000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
    set_value 1248000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
    set_value 1036800 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
    set_value 936000 /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
    set_value 280 /sys/kernel/hmp/down_threshold
    set_value 725 /sys/kernel/hmp/up_threshold
    set_value 0 /sys/kernel/hmp/boost

	exit 0
fi

# Enable input boost configuration
set_value "67 1804800:95" /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
set_value "73 1497600:83 1747200:87 1939200:90 2016000:95" /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads

if [ "$action" = "balance" ]; then
    set_value 6 /sys/devices/system/cpu/cpuhotplug/max_online_cpu
    disabled_hotplug
    set_value 1 /sys/devices/system/cpu/cpu4/online
    set_value 1 /sys/devices/system/cpu/cpu5/online
    set_value 0 /sys/devices/system/cpu/cpu6/online
    set_value 0 /sys/devices/system/cpu/cpu7/online
    set_value 100000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    set_value 1900000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
    set_value 100000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
    set_value 1456000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
    set_value 1378000 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
    set_value 1144000 /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
    set_value 225 /sys/kernel/hmp/down_threshold
    set_value 600 /sys/kernel/hmp/up_threshold
    set_value 0 /sys/kernel/hmp/boost

	exit 0
fi

if [ "$action" = "performance" ]; then
    set_value 8 /sys/devices/system/cpu/cpuhotplug/max_online_cpu
    disabled_hotplug
    set_value 1 /sys/devices/system/cpu/cpu4/online
    set_value 1 /sys/devices/system/cpu/cpu5/online
    set_value 1 /sys/devices/system/cpu/cpu6/online
    set_value 1 /sys/devices/system/cpu/cpu7/online
	set_value 100000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	set_value 1900000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	set_value 100000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	set_value 2080000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
	set_value 1378000 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	set_value 1248000 /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
    set_value 175 /sys/kernel/hmp/down_threshold
    set_value 500 /sys/kernel/hmp/up_threshold
    set_value 0 /sys/kernel/hmp/boost

	exit 0
fi

if [ "$action" = "fast" ]; then
    set_value 8 /sys/devices/system/cpu/cpuhotplug/max_online_cpu
    disabled_hotplug
    set_value 1 /sys/devices/system/cpu/cpu4/online
    set_value 1 /sys/devices/system/cpu/cpu5/online
    set_value 1 /sys/devices/system/cpu/cpu6/online
    set_value 1 /sys/devices/system/cpu/cpu7/online
	set_value 50000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	set_value 3000000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	set_value 1248000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	set_value 2900000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
	set_value 1872000 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	set_value 2035200 /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
    set_value 0 /sys/kernel/hmp/down_threshold
    set_value 1 /sys/kernel/hmp/up_threshold
    set_value 1 /sys/kernel/hmp/boost
	
	exit 0
fi
