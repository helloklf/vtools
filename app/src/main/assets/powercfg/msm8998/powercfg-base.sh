#!/system/bin/sh
# Copyright (c) 2012-2013, 2016, The Linux Foundation. All rights reserved.

echo 1 > /sys/devices/system/cpu/cpu0/online
echo 1 > /sys/devices/system/cpu/cpu1/online
echo 1 > /sys/devices/system/cpu/cpu2/online
echo 1 > /sys/devices/system/cpu/cpu3/online
echo 1 > /sys/devices/system/cpu/cpu4/online
echo 1 > /sys/devices/system/cpu/cpu5/online
echo 1 > /sys/devices/system/cpu/cpu6/online
echo 1 > /sys/devices/system/cpu/cpu7/online

target=`getprop ro.board.platform`

case "$target" in
  "msm8998")
    chmod 0755 /sys/devices/system/cpu/cpu0/online
    chmod 0755 /sys/devices/system/cpu/cpu1/online
    chmod 0755 /sys/devices/system/cpu/cpu2/online
    chmod 0755 /sys/devices/system/cpu/cpu3/online
    chmod 0755 /sys/devices/system/cpu/cpu4/online
    chmod 0755 /sys/devices/system/cpu/cpu5/online
    chmod 0755 /sys/devices/system/cpu/cpu6/online
    chmod 0755 /sys/devices/system/cpu/cpu7/online

    # Setting b.L scheduler parameters
    echo 1 > /proc/sys/kernel/sched_migration_fixup
    echo 95 > /proc/sys/kernel/sched_upmigrate
    echo 90 > /proc/sys/kernel/sched_downmigrate
    echo 100 > /proc/sys/kernel/sched_group_upmigrate
    echo 95 > /proc/sys/kernel/sched_group_downmigrate
    echo 0 > /proc/sys/kernel/sched_select_prev_cpu_us
    echo 400000 > /proc/sys/kernel/sched_freq_inc_notify
    echo 400000 > /proc/sys/kernel/sched_freq_dec_notify
    echo 5 > /proc/sys/kernel/sched_spill_nr_run
    echo 1 > /proc/sys/kernel/sched_restrict_cluster_spill
    #start iop

    # disable thermal bcl hotplug to switch governor
    echo 0 > /sys/module/msm_thermal/core_control/enabled

    # online CPU0
    echo 1 > /sys/devices/system/cpu/cpu0/online
    # configure governor settings for little cluster
    echo "interactive" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
    echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_sched_load
    echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_migration_notif
    echo 19000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay
    echo 86 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
    echo 20000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
    echo 1248000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
    echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
    echo "83 1804800:95" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
    echo 19000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
    echo 79000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
    echo 300000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/ignore_hispeed_on_notif
    # online CPU4
    echo 1 > /sys/devices/system/cpu/cpu4/online
    # configure governor settings for big cluster
    echo "interactive" > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor
    echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/use_sched_load
    echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/use_migration_notif
    echo 19000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/above_hispeed_delay
    echo 90 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/go_hispeed_load
    echo 20000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate
    echo 1574400 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
    echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
    echo "83 1939200:90 2016000:95" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
    echo 19000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/min_sample_time
    echo 79000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/max_freq_hysteresis
    echo 300000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
    echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/ignore_hispeed_on_notif

    # re-enable thermal and BCL hotplug
    echo 1 > /sys/module/msm_thermal/core_control/enabled

    # Enable input boost configuration
    echo "0:1324800" > /sys/module/cpu_boost/parameters/input_boost_freq
    echo 40 > /sys/module/cpu_boost/parameters/input_boost_ms
    echo "0:0 1:0 2:0 3:0 4:2208000 5:0 6:0 7:0" > /sys/module/cpu_boost/parameters/powerkey_input_boost_freq
    echo 400 > /sys/module/cpu_boost/parameters/powerkey_input_boost_ms

    echo 5 > /proc/sys/vm/dirty_background_ratio
    echo 30 > /proc/sys/vm/overcommit_ratio
    echo 100 > /proc/sys/vm/swap_ratio
    echo 100 > /proc/sys/vm/vfs_cache_pressure
    echo 25 > /proc/sys/vm/dirty_ratio
    echo 3 > /proc/sys/vm/page-cluster
    echo 4000 > /proc/sys/vm/dirty_expire_centisecs
    echo 6000 > /proc/sys/vm/dirty_writeback_centisecs

    echo 512 > /sys/block/sda/queue/read_ahead_kb
    echo 0 > /sys/block/sda/queue/iostats

    echo "0:1324800" > /sys/module/cpu_boost/parameters/input_boost_freq
    echo 40 > /sys/module/cpu_boost/parameters/input_boost_ms
    echo "0:1324800 1:0 2:0 3:0 4:2208000 5:2208000 6:0 7:0" > /sys/module/cpu_boost/parameters/powerkey_input_boost_freq
    echo "500" > /sys/module/cpu_boost/parameters/powerkey_input_boost_ms
    echo 'Y' > /sys/module/cpu_boost/parameters/sched_boost_on_powerkey_input
    #echo 'Y' > /sys/module/cpu_boost/parameters/sched_boost_on_input

    echo 0 > /sys/module/lowmemorykiller/parameters/enable_adaptive_lmk

    #stop woodpeckerd
    #stop debuggerd
    #stop debuggerd64
    #stop atfwd
    #stop perfd
    #stop logd
    #echo 0 > /sys/zte_power_debug/switch
    #echo N > /sys/kernel/debug/debug_enabled
  ;;
esac

stop perfd

# killall -9 vendor.qti.hardware.perf@1.0-service
echo 0 > /sys/module/msm_thermal/core_control/enabled
echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
echo N > /sys/module/msm_thermal/parameters/enabled
echo 1 > /proc/sys/kernel/sched_prefer_sync_wakee_to_waker
#stop thermanager
#stop thermal-engine

echo 0-1 > /dev/cpuset/background/cpus
echo 0-3 > /dev/cpuset/system-background/cpus
echo 4-7 > /dev/cpuset/foreground/boost/cpus
echo 0-7 > /dev/cpuset/foreground/cpus
echo 0-7 > /dev/cpuset/top-app/cpus
echo 0 > /proc/sys/kernel/sched_boost

# set_task_affinity $pid $use_cores[cpu7~cpu0]
set_task_affinity() {
  pid=$1
  mask=`echo "obase=16;$((num=2#$2))" | bc`
  for tid in $(ls "/proc/$pid/task/"); do
    taskset -p "$mask" "$tid" 1>/dev/null
  done
  taskset -p "$mask" "$pid" 1>/dev/null
}

set_task_affinity `pgrep com.miui.home` 11111111
