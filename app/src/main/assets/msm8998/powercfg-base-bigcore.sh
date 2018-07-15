#!/system/bin/sh

target=`getprop ro.board.platform`

case "$target" in
    "msm8998")
    chmod 0644 /sys/devices/system/cpu/cpu0/online
    chmod 0644 /sys/devices/system/cpu/cpu1/online
    chmod 0644 /sys/devices/system/cpu/cpu2/online
    chmod 0644 /sys/devices/system/cpu/cpu3/online
    chmod 0644 /sys/devices/system/cpu/cpu4/online
    chmod 0644 /sys/devices/system/cpu/cpu5/online
    chmod 0644 /sys/devices/system/cpu/cpu6/online
    chmod 0644 /sys/devices/system/cpu/cpu7/online

	#echo 2 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
	#echo 60 > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
	#echo 30 > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres
	#echo 100 > /sys/devices/system/cpu/cpu4/core_ctl/offline_delay_ms
	#echo 1 > /sys/devices/system/cpu/cpu4/core_ctl/is_big_cluster
	#echo 4 > /sys/devices/system/cpu/cpu4/core_ctl/task_thres

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

	#echo N > /sys/module/lpm_levels/system/pwr/cpu0/ret/idle_enabled
	#echo N > /sys/module/lpm_levels/system/pwr/cpu1/ret/idle_enabled
	#echo N > /sys/module/lpm_levels/system/pwr/cpu2/ret/idle_enabled
	#echo N > /sys/module/lpm_levels/system/pwr/cpu3/ret/idle_enabled
	#echo N > /sys/module/lpm_levels/system/perf/cpu4/ret/idle_enabled
	#echo N > /sys/module/lpm_levels/system/perf/cpu5/ret/idle_enabled
	#echo N > /sys/module/lpm_levels/system/perf/cpu6/ret/idle_enabled
	#echo N > /sys/module/lpm_levels/system/perf/cpu7/ret/idle_enabled
	#echo N > /sys/module/lpm_levels/system/pwr/pwr-l2-dynret/idle_enabled
	#echo N > /sys/module/lpm_levels/system/pwr/pwr-l2-ret/idle_enabled
	#echo N > /sys/module/lpm_levels/system/perf/perf-l2-dynret/idle_enabled
	#echo N > /sys/module/lpm_levels/system/perf/perf-l2-ret/idle_enabled
	#echo N > /sys/module/lpm_levels/parameters/sleep_disabled

    echo 0 > /dev/cpuset/background/cpus
    echo 0-3 > /dev/cpuset/system-background/cpus
    echo 4-7 > /dev/cpuset/foreground/boost/cpus
    echo 0-2,4-7 > /dev/cpuset/foreground/cpus
    echo 0 > /proc/sys/kernel/sched_boost

    echo 1 > /sys/devices/system/cpu/cpu4/online
    echo 1 > /sys/devices/system/cpu/cpu5/online
    echo 1 > /sys/devices/system/cpu/cpu6/online
    echo 1 > /sys/devices/system/cpu/cpu7/online

	echo 0 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
	echo 4 > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
	echo 95 > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
	echo 60 > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres

	echo 5 > /proc/sys/vm/dirty_background_ratio
	echo 50 > /proc/sys/vm/overcommit_ratio
	echo 100 > /proc/sys/vm/swap_ratio
	echo 100 > /proc/sys/vm/vfs_cache_pressure
	echo 20 > /proc/sys/vm/dirty_ratio
    echo 3 > /proc/sys/vm/page-cluster
    echo 2000 > /proc/sys/vm/dirty_expire_centisecs
    echo 5000 > /proc/sys/vm/dirty_writeback_centisecs

    echo 512 > /sys/block/sda/queue/read_ahead_kb
    echo 0 > /sys/block/sda/queue/iostats

    echo "0:1324800" > /sys/module/cpu_boost/parameters/input_boost_freq
    echo 40 > /sys/module/cpu_boost/parameters/input_boost_ms
    echo "0:0 1:0 2:0 3:0 4:2208000 5:0 6:0 7:0" > /sys/module/cpu_boost/parameters/powerkey_input_boost_freq
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
