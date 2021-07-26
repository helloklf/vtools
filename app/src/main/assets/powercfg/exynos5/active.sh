#!/system/bin/sh

action=$1

init () {
  local dir=$(cd $(dirname $0); pwd)
  if [[ -f "$dir/powercfg-base.sh" ]]; then
    sh "$dir/powercfg-base.sh"
  elif [[ -f '/data/powercfg-base.sh' ]]; then
    sh /data/powercfg-base.sh
  fi
}
if [[ "$action" == "init" ]]; then
  init
  exit 0
fi

# /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_freq_table
# 1690000 1586000 1482000 1378000 1274000 1170000 1066000 962000 858000 754000 650000 546000 442000 338000

# /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_freq_table
# 2600000 2496000 2392000 2288000 2184000 2080000 1976000 1872000 1768000 1664000 1560000 1456000 1352000 1248000 1144000 1040000 936000 832000 728000 624000 520000 416000 312000

if [ ! `cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor` = "interactive" ]; then
  echo 'interactive' > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
fi
if [ ! `cat /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor` = "interactive" ]; then
  echo 'interactive' > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor
fi

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

function cpuset()
{
    set_value '0-3' /dev/cpuset/system-background/cpus
    set_value '0-1' /dev/cpuset/background/cpus
    set_value '4-7' /dev/cpuset/foreground/boost/cpus
    set_value '0-7' /dev/cpuset/foreground/cpus
    set_value '0-7' /dev/cpuset/top-app/cpus
    set_value "0-6" /dev/cpuset/dex2oat/cpus
    set_value 0 /proc/sys/kernel/sched_boost
}

function disabled_hotplug()
{
    set_value 0 /sys/power/cpuhotplug/enabled;
    set_value 0 /sys/devices/system/cpu/cpuhotplug/enabled;
}

cpuset
lock_value 0 /sys/devices/system/cpu/cpu0/cpufreq/interactive/boost
lock_value 0 /sys/devices/system/cpu/cpu4/cpufreq/interactive/boost

if [[ "$action" = "powersave" ]]; then
    set_value 6 /sys/devices/system/cpu/cpuhotplug/max_online_cpu
    disabled_hotplug
    set_value 1 /sys/devices/system/cpu/cpu4/online
    set_value 1 /sys/devices/system/cpu/cpu5/online
    set_value 0 /sys/devices/system/cpu/cpu6/online
    set_value 0 /sys/devices/system/cpu/cpu7/online

  set_value 50000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
  set_value 1900000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
  set_value 50000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
  set_value 1040000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
  set_value 546000 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
  set_value 520000 /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

    set_value 225 /sys/kernel/hmp/down_threshold
    set_value 600 /sys/kernel/hmp/up_threshold
    set_value 0 /sys/kernel/hmp/boost

    set_value "80 338000:90 650000:75 1066000:83 1274000:85 1482000:83" /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
    set_value "90 312000:92 520000:95 832000:93 1040000:87 1456000:87 1872000:89 2080000:90 2392000：92" /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads

    echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
  echo 9000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
    echo 10000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate

    echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
    echo 19000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/min_sample_time
    echo 20000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate

  exit 0
fi

set_value 8 /sys/devices/system/cpu/cpuhotplug/max_online_cpu
disabled_hotplug
set_value 1 /sys/devices/system/cpu/cpu4/online
set_value 1 /sys/devices/system/cpu/cpu5/online
set_value 1 /sys/devices/system/cpu/cpu6/online
set_value 1 /sys/devices/system/cpu/cpu7/online

if [[ "$action" = "balance" ]]; then
    set_value 100000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    set_value 1690000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
    set_value 100000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
    set_value 1872000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
    set_value 962000 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
    set_value 936000 /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

    set_value 175 /sys/kernel/hmp/down_threshold
    set_value 500 /sys/kernel/hmp/up_threshold
    set_value 0 /sys/kernel/hmp/boost

    set_value "80 338000:90 650000:75 1066000:83 1274000:85 1482000:83" /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
    set_value "84 312000:92 520000:95 832000:93 1040000:75 1456000:87 1872000:89 2080000:90 2392000：92" /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads

    echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
  echo 9000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
    echo 10000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate

    echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
    echo 19000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/min_sample_time
    echo 20000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate

  exit 0
fi

if [[ "$action" = "performance" ]]; then
  set_value 100000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
  set_value 1900000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
  set_value 100000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
  set_value 2392000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
  set_value 1378000 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
  set_value 1456000 /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

    set_value 170 /sys/kernel/hmp/down_threshold
    set_value 310 /sys/kernel/hmp/up_threshold
    set_value 0 /sys/kernel/hmp/boost
    echo 90 > /sys/power/little_thermal_temp
    echo 4500 > /sys/power/ipa/tdp

    set_value "76 338000:90 650000:70 1066000:80 1274000:83 1482000:85" /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
    set_value "80 312000:92 520000:87 832000:88 1040000:75 1456000:87 1872000:89 2080000:90 2392000：92" /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads

    echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
  echo 39000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
    echo 5000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate

    echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
    echo 39000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/min_sample_time
    echo 5000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate

  exit 0
fi

if [[ "$action" = "fast" ]]; then
  set_value 100000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
  set_value 3000000 /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
  set_value 1248000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
  set_value 2900000 /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq
  set_value 1482000 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
  set_value 1872000 /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq

    set_value 170 /sys/kernel/hmp/down_threshold
    set_value 310 /sys/kernel/hmp/up_threshold
    set_value 1 /sys/kernel/hmp/boost
    echo 90 > /sys/power/little_thermal_temp
    echo 5000 > /sys/power/ipa/tdp

    set_value "76 338000:90 650000:70 1066000:80 1274000:83 1482000:85" /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
    set_value "79 312000:92 520000:77 832000:78 1040000:75 1456000:85 1872000:88 2080000:90 2392000：92" /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads

    echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
  echo 39000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
    echo 5000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate

    echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
    echo 39000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/min_sample_time
    echo 5000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate

  exit 0
fi
