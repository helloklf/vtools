#!/system/bin/sh

target=`getprop ro.board.platform`
function trim()
{
    var=$1
    var=${var%% }
    var=${var## }
    echo $var
}

function setvolt()
{
    cluster=$1
    freq=$2
    volt=$3
    if [ -f $cluster ]; then
        valid=`cat $cluster | grep $freq`
        if [  $(trim $valid)  -ne '' ]; then
            echo "$freq $volt" > $cluster
        fi;
    fi;
}


function set_value()
{
    value=$1
    path=$2
    if [ -f $path ]; then
        if [ ! $(trim `cat $path`) = $(trim $value) ]; then
            chmod 0664 $path
            echo $value > $path
        fi;
    fi;
}

set_value "interactive" /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
set_value "interactive" /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor

case "$target" in
    "exynos5")
        #GPU /sys/devices/14ac0000.mali/

        #highspeed_delay
        set_value 0 /sys/devices/14ac0000.mali/highspeed_delay

        #highspeed_load
        set_value 80 /sys/devices/14ac0000.mali/highspeed_load

        #highspeed_clock
        set_value 419 /sys/devices/14ac0000.mali/highspeed_clock

        #CPU
        set_value 1 /sys/devices/system/cpu/cpu0/online
        set_value 1 /sys/devices/system/cpu/cpu1/online
        set_value 1 /sys/devices/system/cpu/cpu2/online
        set_value 1 /sys/devices/system/cpu/cpu3/online
        set_value 1 /sys/devices/system/cpu/cpu4/online
        set_value 1 /sys/devices/system/cpu/cpu5/online
        set_value 1 /sys/devices/system/cpu/cpu6/online
        set_value 1 /sys/devices/system/cpu/cpu7/online

        set_value 75 /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
        set_value 1170000 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
        set_value 1 /sys/devices/system/cpu/cpu0/cpufreq/interactive/boost
        set_value 1 /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy

        set_value 80 /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
        set_value 1170000 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
        set_value 1 /sys/devices/system/cpu/cpu0/cpufreq/interactive/boost
        set_value 1 /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy

        set_value 1 /sys/devices/system/cpu/cpuhotplug/enabled
        set_value 1 /sys/devices/system/cpu/cpuhotplug/enabled
        set_value 8 /sys/devices/system/cpu/cpuhotplug/max_online_cpu
        set_value 1 /sys/devices/system/cpu/cpuhotplug/min_online_cpu
        #chmod 664 /sys/devices/system/cpu/cpuhotplug/control_online_cpus
        #echo 8 /sys/devices/system/cpu/cpuhotplug/control_online_cpus
        # /sys/devices/system/cpu/cpuhotplug/governor/....

        #
        set_value 0 /sys/power/cpuhotplug/enabled
        set_value 0 /sys/devices/system/cpu/cpuhotplug/enabled

        #HMP
        set_value 200 /sys/kernel/hmp/down_threshold
        set_value 524 /sys/kernel/hmp/up_threshold
        set_value 0 /sys/kernel/hmp/boost

        #Charge
        set_value 3150 /sys/devices/battery/hv_charge
        set_value 3150 /sys/devices/battery/ac_charge
        set_value 3150 /sys/devices/battery/ac_input
        set_value 3150 /sys/devices/battery/so_limit_charge
        set_value 3150 /sys/devices/battery/so_limit_input
        set_value 3000 /sys/devices/battery/hv_input
        set_value 1200 /sys/devices/battery/sdp_charge
        set_value 1200 /sys/devices/battery/sdp_input
        set_value 2300 /sys/devices/battery/car_charge
        set_value 2300 /sys/devices/battery/car_input
        set_value 2300 /sys/devices/battery/wc_charge
        set_value 2300 /sys/devices/battery/wc_input
        set_value 900 /sys/devices/battery/wpc_limit_charge
        set_value 900 /sys/devices/battery/otg_input
        set_value 900 /sys/devices/battery/otg_charge

        #/sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        cluster1="/sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table"
        chmod 0664 $cluster1
        setvolt $cluster1 "2704000 " "1121250"

        setvolt $cluster1 "2600000 " "1090000"
        setvolt $cluster1 "2496000 " "1062500"
        setvolt $cluster1 "2392000 " "1037500"
        setvolt $cluster1 "2288000 " "1000000"
        setvolt $cluster1 "2184000 " "962500"
        setvolt $cluster1 "2080000 " "937500"
        setvolt $cluster1 "1976000 " "912500"
        setvolt $cluster1 "1872000 " "881250"
        setvolt $cluster1 "1768000 " "856250"
        setvolt $cluster1 "1664000 " "831250"
        setvolt $cluster1 "1560000 " "793750"
        setvolt $cluster1 "1456000 " "762500"
        setvolt $cluster1 "1352000 " "737500"
        setvolt $cluster1 "1248000 " "712500"
        setvolt $cluster1 "1144000 " "693750"
        setvolt $cluster1 "1040000 " "668750"
        setvolt $cluster1 "936000 " "643750"
        setvolt $cluster1 "832000 " "618750"
        setvolt $cluster1 "728000 " "593750"
        setvolt $cluster1 "624000 " "575000"
        setvolt $cluster1 "520000 " "556250"
        setvolt $cluster1 "416000 " "556250"
        setvolt $cluster1 "312000 " "556250"
        setvolt $cluster1 "208000 " "556250"

        set_value 130000 /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_min_freq

        #/sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        cluster0="/sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table"
        chmod 0644 $cluster0
        setvolt $cluster0 "1794000 " "962500"
        setvolt $cluster0 "1690000 " "937500"
        setvolt $cluster0 "1586000 " "925000"
        setvolt $cluster0 "1482000 " "875000"
        setvolt $cluster0 "1378000 " "831250"
        setvolt $cluster0 "1274000 " "793750"
        setvolt $cluster0 "1170000 " "743750"
        setvolt $cluster0 "1066000 " "706250"
        setvolt $cluster0 "962000 " "668750"
        setvolt $cluster0 "858000 " "637500"
        setvolt $cluster0 "754000 " "606250"
        setvolt $cluster0 "650000 " "575000"
        setvolt $cluster0 "546000 " "543750"
        setvolt $cluster0 "442000 " "506250"
        setvolt $cluster0 "338000 " "500000"
        setvolt $cluster0 "234000 " "500000"
        setvolt $cluster0 "130000 " "500000"

        set_value 130000 /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_min_freq
        set_value 0 /sys/block/sda/queue/iostats
    ;;
esac

chown -h system /sys/devices/system/cpu/cpufreq/ondemand/sampling_rate
chown -h system /sys/devices/system/cpu/cpufreq/ondemand/sampling_down_factor
chown -h system /sys/devices/system/cpu/cpufreq/ondemand/io_is_busy

set_value 0 /dev/cpuset/background/cpus
set_value 1 /dev/cpuset/background/cpus
set_value '0-2' /dev/cpuset/system-background/cpus
set_value '2-3' /dev/cpuset/system-background/cpus
set_value '4-7' /dev/cpuset/foreground/boost/cpus
set_value '0-7' /dev/cpuset/foreground/cpus
set_value '0-7' /dev/cpuset/top-app/cpus
set_value "4-7" /dev/cpuset/dex2oat/cpus
set_value 0 /proc/sys/kernel/sched_boost
