#!/system/bin/sh

target=`getprop ro.board.platform`

case "$target" in
    "exynos5")
        #GPU /sys/devices/14ac0000.mali/

        #highspeed_delay
        chmod 664 /sys/devices/14ac0000.mali/highspeed_delay
        echo 0 > /sys/devices/14ac0000.mali/highspeed_delay
        chmod 444 /sys/devices/14ac0000.mali/highspeed_delay

        #highspeed_load
        chmod 664 /sys/devices/14ac0000.mali/highspeed_load
        echo 80 > /sys/devices/14ac0000.mali/highspeed_load
        chmod 444 /sys/devices/14ac0000.mali/highspeed_load

        #highspeed_clock
        chmod 664 /sys/devices/14ac0000.mali/highspeed_clock
        echo 419 > /sys/devices/14ac0000.mali/highspeed_clock
        chmod 444 /sys/devices/14ac0000.mali/highspeed_clock

        #CPU
        chmod 0664 /sys/devices/system/cpu/cpu0/online
        echo 1 > /sys/devices/system/cpu/cpu0/online

        chmod 0664 /sys/devices/system/cpu/cpu1/online
        echo 1 > /sys/devices/system/cpu/cpu1/online

        chmod 0664 /sys/devices/system/cpu/cpu2/online
        echo 1 > /sys/devices/system/cpu/cpu2/online

        chmod 0664 /sys/devices/system/cpu/cpu3/online
        echo 1 > /sys/devices/system/cpu/cpu3/online

        chmod 0664 /sys/devices/system/cpu/cpu4/online
        echo 1 > /sys/devices/system/cpu/cpu4/online

        chmod 0664 /sys/devices/system/cpu/cpu5/online
        echo 1 > /sys/devices/system/cpu/cpu5/online

        chmod 0664 /sys/devices/system/cpu/cpu6/online
        echo 1 > /sys/devices/system/cpu/cpu6/online

        chmod 0664 /sys/devices/system/cpu/cpu7/online
        echo 1 > /sys/devices/system/cpu/cpu7/online

        chmod 0664 /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
        echo 75 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
        chmod 0664 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
        echo 1170000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
        chmod 0664 /sys/devices/system/cpu/cpu0/cpufreq/interactive/boost
        echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/boost
        chmod 0664 /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
        echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy

        chmod 0664 /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
        echo 80 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
        chmod 0664 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
        echo 1170000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
        chmod 0664 /sys/devices/system/cpu/cpu0/cpufreq/interactive/boost
        echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/boost
        chmod 0664 /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
        echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy


        chmod 664 /sys/devices/system/cpu/cpuhotplug/enabled
        echo 1 /sys/devices/system/cpu/cpuhotplug/enabled
        chmod 664 /sys/devices/system/cpu/cpuhotplug/enabled
        echo 1 /sys/devices/system/cpu/cpuhotplug/enabled
        chmod 664 /sys/devices/system/cpu/cpuhotplug/max_online_cpu
        echo 8 /sys/devices/system/cpu/cpuhotplug/max_online_cpu
        chmod 664 /sys/devices/system/cpu/cpuhotplug/min_online_cpu
        echo 0 /sys/devices/system/cpu/cpuhotplug/min_online_cpu
        #chmod 664 /sys/devices/system/cpu/cpuhotplug/control_online_cpus
        #echo 8 /sys/devices/system/cpu/cpuhotplug/control_online_cpus
        # /sys/devices/system/cpu/cpuhotplug/governor/....

        #
        echo 0 > /sys/power/cpuhotplug/enabled

        #HMP
        chmod 664 /sys/kernel/hmp/down_threshold
        echo 200 > /sys/kernel/hmp/down_threshold

        chmod 664 /sys/kernel/hmp/up_threshold
        echo 524 > /sys/kernel/hmp/up_threshold

        chmod 664 /sys/kernel/hmp/boost
        echo 0 > /sys/kernel/hmp/boost

        #Charge
        chmod 644 /sys/devices/battery/hv_charge
        echo 3150 > /sys/devices/battery/hv_charge
        chmod 644 /sys/devices/battery/ac_charge
        echo 3150 > /sys/devices/battery/ac_charge
        chmod 644 /sys/devices/battery/ac_input
        echo 3150 > /sys/devices/battery/ac_input
        chmod 644 /sys/devices/battery/so_limit_charge
        echo 3150 > /sys/devices/battery/so_limit_charge
        chmod 644 /sys/devices/battery/so_limit_input
        echo 3150 > /sys/devices/battery/so_limit_input
        chmod 644 /sys/devices/battery/hv_input
        echo 3000 > /sys/devices/battery/hv_input

        chmod 644 /sys/devices/battery/sdp_charge
        echo 1200 > /sys/devices/battery/sdp_charge
        chmod 644 /sys/devices/battery/sdp_input
        echo 1200 > /sys/devices/battery/sdp_input

        chmod 644 /sys/devices/battery/car_charge
        echo 2300 > /sys/devices/battery/car_charge
        chmod 644 /sys/devices/battery/car_input
        echo 2300 > /sys/devices/battery/car_input

        chmod 644 /sys/devices/battery/wc_charge
        echo 2300 > /sys/devices/battery/wc_charge
        chmod 644 /sys/devices/battery/wc_input
        echo 2300 > /sys/devices/battery/wc_input

        chmod 644 /sys/devices/battery/wpc_limit_charge
        echo 900 > /sys/devices/battery/wpc_limit_charge
        chmod 644 /sys/devices/battery/otg_input
        echo 900 > /sys/devices/battery/otg_input
        chmod 644 /sys/devices/battery/otg_charge
        echo 900 > /sys/devices/battery/otg_charge

        #/sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        chmod 664 /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "2704000 1121250" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "2600000 1090000" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "2496000 1062500" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "2392000 1037500" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "2288000 1000000" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "2184000 962500" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "2080000 937500" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "1976000 912500" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "1872000 881250" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "1768000 856250" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "1664000 831250" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "1560000 793750" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "1456000 762500" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "1352000 737500" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "1248000 712500" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "1144000 693750" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "1040000 668750" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "936000 643750" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "832000 618750" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "728000 593750" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "624000 575000" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "520000 556250" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "416000 556250" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "312000 556250" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        echo "208000 556250" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_volt_table
        chmod 664 /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_min_freq
        echo 130000 > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster1_min_freq

        #/sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "1794000 962500" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "1690000 937500" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "1586000 925000" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "1482000 875000" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "1378000 831250" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "1274000 793750" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "1170000 743750" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "1066000 706250" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "962000 668750" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "858000 637500" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "754000 606250" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "650000 575000" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "546000 543750" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "442000 506250" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "338000 500000" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "234000 500000" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        echo "130000 500000" > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_volt_table
        chmod 664 /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_min_freq
        echo 130000 > /sys/devices/system/cpu/cpufreq/mp-cpufreq/cluster0_min_freq

        echo 0 >  /sys/block/sda/queue/iostats
    ;;
esac

chown -h system /sys/devices/system/cpu/cpufreq/ondemand/sampling_rate
chown -h system /sys/devices/system/cpu/cpufreq/ondemand/sampling_down_factor
chown -h system /sys/devices/system/cpu/cpufreq/ondemand/io_is_busy

chmod 664 /dev/cpuset/background/cpus
echo 0 > /dev/cpuset/background/cpus
echo 1 > /dev/cpuset/background/cpus

chmod 664 /dev/cpuset/system-background/cpus
echo 0-2 > /dev/cpuset/system-background/cpus
echo 2-3 > /dev/cpuset/system-background/cpus

chmod 664 /dev/cpuset/boost/cpus
echo 4-7 > /dev/cpuset/foreground/boost/cpus

chmod 664 /dev/cpuset/foreground/cpus
echo 0-7 > /dev/cpuset/foreground/cpus

chmod 664 /dev/cpuset/top-app/cpus
echo 0-7 > /dev/cpuset/top-app/cpus

chmod 664 /dev/cpuset/dex2oat/cpus
echo '4-7' > /dev/cpuset/dex2oat/cpus

echo 0 > /proc/sys/kernel/sched_boost
