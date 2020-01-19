#!/system/bin/sh

#scheduler settings
echo 3 > /proc/sys/kernel/sched_window_stats_policy
echo 3 > /proc/sys/kernel/sched_ravg_hist_size

#init task load, restrict wakeups to preferred cluster
echo 15 > /proc/sys/kernel/sched_init_task_load
# spill load is set to 100% by default in the kernel
echo 3 > /proc/sys/kernel/sched_spill_nr_run
# Apply inter-cluster load balancer restrictions
echo 1 > /proc/sys/kernel/sched_restrict_cluster_spill

#governor settings
echo 1 > /sys/devices/system/cpu/cpu0/online
echo "interactive" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
echo "19000 1401600:39000" > /sys/devices/system/cpu/cpufreq/interactive/above_hispeed_delay
echo 85 > /sys/devices/system/cpu/cpufreq/interactive/go_hispeed_load
echo 20000 > /sys/devices/system/cpu/cpufreq/interactive/timer_rate
echo 1401600 > /sys/devices/system/cpu/cpufreq/interactive/hispeed_freq
echo 0 > /sys/devices/system/cpu/cpufreq/interactive/io_is_busy
echo "85 1401600:80" > /sys/devices/system/cpu/cpufreq/interactive/target_loads
echo 39000 > /sys/devices/system/cpu/cpufreq/interactive/min_sample_time
echo 40000 > /sys/devices/system/cpu/cpufreq/interactive/sampling_down_factor
echo 652800 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq

# Bring up all cores online
echo 1 > /sys/devices/system/cpu/cpu1/online
echo 1 > /sys/devices/system/cpu/cpu2/online
echo 1 > /sys/devices/system/cpu/cpu3/online
echo 1 > /sys/devices/system/cpu/cpu4/online
echo 1 > /sys/devices/system/cpu/cpu5/online
echo 1 > /sys/devices/system/cpu/cpu6/online
echo 1 > /sys/devices/system/cpu/cpu7/online

# Enable low power modes
echo 0 > /sys/module/lpm_levels/parameters/sleep_disabled

# SMP scheduler
echo 85 > /proc/sys/kernel/sched_upmigrate
echo 85 > /proc/sys/kernel/sched_downmigrate
echo 19 > /proc/sys/kernel/sched_upmigrate_min_nice

# Enable sched guided freq control
echo 1 > /sys/devices/system/cpu/cpufreq/interactive/use_sched_load
echo 1 > /sys/devices/system/cpu/cpufreq/interactive/use_migration_notif
echo 200000 > /proc/sys/kernel/sched_freq_inc_notify
echo 200000 > /proc/sys/kernel/sched_freq_dec_notify
echo 0 > /sys/devices/system/cpu/cpufreq/interactive/use_sched_load

echo 128 > /sys/block/mmcblk0/bdi/read_ahead_kb
echo 128 > /sys/block/mmcblk0/queue/read_ahead_kb
echo 128 > /sys/block/dm-0/queue/read_ahead_kb
echo 128 > /sys/block/dm-1/queue/read_ahead_kb

echo 1 > /proc/sys/kernel/sched_prefer_sync_wakee_to_waker
