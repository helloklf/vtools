#!/system/bin/sh

target=`getprop ro.board.platform`

function setvolt()
{
    cluster=$1
    freq=$2
    volt=$3
    if [[ -f "$cluster" ]]; then
        valid="$(cat $cluster | grep "$freq")"
        if [[ -n "$valid" && ! -z "$valid" ]]; then
            echo "$freq $volt"
            #echo "$freq $volt" > "$cluster"
        fi;
    fi;
}
#cluster="/sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table"
#setvolt $cluster "2600000 " "1090000"
#setvolt $cluster "208000 " "1090000"

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

        #
        set_value 1 /sys/power/cpuhotplug/enabled
        set_value 1 /sys/devices/system/cpu/cpuhotplug/enabled
        set_value 8 /sys/devices/system/cpu/cpuhotplug/max_online_cpu
        set_value 4 /sys/devices/system/cpu/cpuhotplug/min_online_cpu

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
        setvolt $cluster1 "2704000 " "1161250"

        setvolt $cluster1 "2600000 " "1118750"
        setvolt $cluster1 "2496000 " "1081250"
        setvolt $cluster1 "2392000 " "1056250"
        setvolt $cluster1 "2288000 " "1018750"
        setvolt $cluster1 "2184000 " "981250"
        setvolt $cluster1 "2080000 " "956250"
        setvolt $cluster1 "1976000 " "931250"
        setvolt $cluster1 "1872000 " "900000"
        setvolt $cluster1 "1768000 " "875000"
        setvolt $cluster1 "1664000 " "850000"
        setvolt $cluster1 "1560000 " "812500"
        setvolt $cluster1 "1456000 " "781250"
        setvolt $cluster1 "1352000 " "756250"
        setvolt $cluster1 "1248000 " "731250"
        setvolt $cluster1 "1144000 " "712500"
        setvolt $cluster1 "1040000 " "687500"
        setvolt $cluster1 "936000 " "662500"
        setvolt $cluster1 "832000 " "637500"
        setvolt $cluster1 "728000 " "612500"
        setvolt $cluster1 "624000 " "593750"
        setvolt $cluster1 "520000 " "575000"
        setvolt $cluster1 "416000 " "575000"
        setvolt $cluster1 "312000 " "575000"
        setvolt $cluster1 "208000 " "575000"

        set_value 130000 /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_min_freq

        #/sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        cluster0="/sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table"
        chmod 0644 $cluster0
        setvolt $cluster0 "1794000 " "962500"
        setvolt $cluster0 "1690000 " "987500"
        setvolt $cluster0 "1586000 " "950000"
        setvolt $cluster0 "1482000 " "900000"
        setvolt $cluster0 "1378000 " "856250"
        setvolt $cluster0 "1274000 " "818750"
        setvolt $cluster0 "1170000 " "768750"
        setvolt $cluster0 "1066000 " "731250"
        setvolt $cluster0 "962000 " "693750"
        setvolt $cluster0 "858000 " "662500"
        setvolt $cluster0 "754000 " "631250"
        setvolt $cluster0 "650000 " "600000"
        setvolt $cluster0 "546000 " "568750"
        setvolt $cluster0 "442000 " "531250"
        setvolt $cluster0 "338000 " "512500"
        setvolt $cluster0 "234000 " "512500"
        setvolt $cluster0 "130000 " "512500"

        set_value 130000 /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_min_freq
        set_value 0 /sys/block/sda/queue/iostats
    ;;
esac

set_value '0' /dev/cpuset/system-background/cpus
set_value '1' /dev/cpuset/background/cpus
set_value '4-7' /dev/cpuset/foreground/boost/cpus
set_value '2-7' /dev/cpuset/foreground/cpus
set_value '2-7' /dev/cpuset/top-app/cpus
set_value "4-7" /dev/cpuset/dex2oat/cpus
set_value 0 /proc/sys/kernel/sched_boost
