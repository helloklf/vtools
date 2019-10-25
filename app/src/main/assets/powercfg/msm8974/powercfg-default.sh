#!/system/bin/sh
action=$1

function set_freq() {
    local freq_min="$1"
    local freq_max="$2"

    echo 1 > /sys/devices/system/cpu/cpu0/online
    echo $freq_min > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    echo $freq_max > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq


    echo 1 > /sys/devices/system/cpu/cpu1/online
    echo $freq_min > /sys/devices/system/cpu/cpu1/cpufreq/scaling_min_freq
    echo $freq_max > /sys/devices/system/cpu/cpu1/cpufreq/scaling_max_freq

    echo 1 > /sys/devices/system/cpu/cpu2/online
    echo $freq_min > /sys/devices/system/cpu/cpu2/cpufreq/scaling_min_freq
    echo $freq_max > /sys/devices/system/cpu/cpu2/cpufreq/scaling_max_freq

    echo 1 > /sys/devices/system/cpu/cpu3/online
    echo $freq_min > /sys/devices/system/cpu/cpu3/cpufreq/scaling_min_freq
    echo $freq_max > /sys/devices/system/cpu/cpu3/cpufreq/scaling_max_freq
}

if [[ "$action" = "powersave" ]]; then
    start mpdecision

    if [[ -f /system/bin/changepowermode.sh ]]; then
        /system/bin/changepowermode.sh middle
    fi
    set_freq 300000 1958400

	exit 0
elif [[ "$action" = "balance" ]]; then
    stop mpdecision

    if [[ -f /system/bin/changepowermode.sh ]]; then
        /system/bin/changepowermode.sh middle
    else
        set_freq 300000 2457600
    fi

	exit 0
elif [[ "$action" = "performance" ]]; then
    stop mpdecision

    if [[ -f /system/bin/changepowermode.sh ]]; then
        /system/bin/changepowermode.sh high
    else
        set_freq 300000 2457600
    fi

	exit 0
elif [[ "$action" = "fast" ]]; then
	#fast
    stop mpdecision

    if [[ -f /system/bin/changepowermode.sh ]]; then
        /system/bin/changepowermode.sh high
    else
        set_freq 300000 2457600
    fi

	exit 0
fi
