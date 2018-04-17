#!/system/bin/sh
action=$1


if [ "$action" = "powersave" ]; then
	echo "67 1036800:73" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
	echo "78" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
	echo 50000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 1900000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 50000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 1056000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq

	echo 1036800 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 729600 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

    echo 6 /sys/devices/system/cpu/cpuhotplug/max_online_cpu

    echo 0 > /sys/devices/system/cpu/cpu6/online
    echo 0 > /sys/devices/system/cpu/cpu7/online

    chmod 664 /sys/kernel/hmp/down_threshold
    echo 280 > /sys/kernel/hmp/down_threshold

    chmod 664 /sys/kernel/hmp/up_threshold
    echo 725 > /sys/kernel/hmp/up_threshold

    chmod 664 /sys/kernel/hmp/boost
    echo 0 > /sys/kernel/hmp/boost

	exit 0
fi


echo 8 /sys/devices/system/cpu/cpuhotplug/max_online_cpu
echo 1 > /sys/devices/system/cpu/cpu4/online
echo 1 > /sys/devices/system/cpu/cpu5/online
echo 1 > /sys/devices/system/cpu/cpu6/online
echo 1 > /sys/devices/system/cpu/cpu7/online

# Enable input boost configuration
echo "67 1804800:95" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
echo "73 1497600:83 1747200:87 1939200:90 2016000:95" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads

if [ "$action" = "balance" ]; then
	echo 100000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 1900000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 1497600 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq

	echo 1248000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 806400 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/

    chmod 664 /sys/kernel/hmp/down_threshold
    echo 225 > /sys/kernel/hmp/down_threshold

    chmod 664 /sys/kernel/hmp/up_threshold
    echo 600 > /sys/kernel/hmp/up_threshold

    chmod 664 /sys/kernel/hmp/boost
    echo 0 > /sys/kernel/hmp/boost

	exit 0
fi

if [ "$action" = "performance" ]; then
	echo 100000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 1900000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 100000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 2035200 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq

	echo 1555200 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 1267200 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

    chmod 664 /sys/kernel/hmp/down_threshold
    echo 175 > /sys/kernel/hmp/down_threshold

    chmod 664 /sys/kernel/hmp/up_threshold
    echo 500 > /sys/kernel/hmp/up_threshold

    chmod 664 /sys/kernel/hmp/boost
    echo 0 > /sys/kernel/hmp/boost

	exit 0
fi

if [ "$action" = "fast" ]; then
	echo 50000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
	echo 2750000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
	echo 1267200 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
	echo 2750000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq

	echo 1747200 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
	echo 2035200 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

    chmod 664 /sys/kernel/hmp/down_threshold
    echo 150 > /sys/kernel/hmp/down_threshold

    chmod 664 /sys/kernel/hmp/up_threshold
    echo 400 > /sys/kernel/hmp/up_threshold

    chmod 664 /sys/kernel/hmp/boost
    echo 1 > /sys/kernel/hmp/boost
	
	exit 0
fi
