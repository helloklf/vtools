#!/system/bin/sh

target=`getprop ro.board.platform`

case "$target" in
    "msm8996")
        # disable thermal bcl hotplug to switch governor
        echo 0 > /sys/module/msm_thermal/core_control/enabled
        echo -n disable > /sys/devices/soc/soc:qcom,bcl/mode
        bcl_hotplug_mask=`cat /sys/devices/soc/soc:qcom,bcl/hotplug_mask`
        echo 0 > /sys/devices/soc/soc:qcom,bcl/hotplug_mask
        bcl_soc_hotplug_mask=`cat /sys/devices/soc/soc:qcom,bcl/hotplug_soc_mask`
        echo 0 > /sys/devices/soc/soc:qcom,bcl/hotplug_soc_mask
        echo -n enable > /sys/devices/soc/soc:qcom,bcl/mode

        # Enable Adaptive LMK
        echo 1 > /sys/module/lowmemorykiller/parameters/enable_adaptive_lmk
        echo 81250 > /sys/module/lowmemorykiller/parameters/vmpressure_file_min
        # configure governor settings for little cluster
        echo "interactive" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
        echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_sched_load
        echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_migration_notif
        echo 19000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay
        echo 90 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
        echo 20000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
        echo 960000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
        echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
        echo 90 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
        echo 19000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
        echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
        echo 300000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
        echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/ignore_hispeed_on_notif
        # online CPU2
        echo 1 > /sys/devices/system/cpu/cpu2/online
        # configure governor settings for big cluster
        echo "interactive" > /sys/devices/system/cpu/cpu2/cpufreq/scaling_governor
        echo 1 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/use_sched_load
        echo 1 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/use_migration_notif
        echo "19000 1036800:39000 1324800:19000" > /sys/devices/system/cpu/cpu2/cpufreq/interactive/above_hispeed_delay
        echo 97 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/go_hispeed_load
        echo 20000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/timer_rate
        echo 940000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/hispeed_freq
        echo 1 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/io_is_busy
        echo "85 1500000:90 1800000:70" > /sys/devices/system/cpu/cpu2/cpufreq/interactive/target_loads
        echo 19000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/min_sample_time
        echo 0 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/max_freq_hysteresis
        echo 300000 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_min_freq
        echo 1 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/ignore_hispeed_on_notif
        # re-enable thermal and BCL hotplug
        echo 1 > /sys/module/msm_thermal/core_control/enabled
        echo -n disable > /sys/devices/soc/soc:qcom,bcl/mode
        echo $bcl_hotplug_mask > /sys/devices/soc/soc:qcom,bcl/hotplug_mask
        echo $bcl_soc_hotplug_mask > /sys/devices/soc/soc:qcom,bcl/hotplug_soc_mask
        echo -n enable > /sys/devices/soc/soc:qcom,bcl/mode
        # input boost configuration
        echo "0" > /sys/module/cpu_boost/parameters/input_boost_freq
        echo 0 > /sys/module/cpu_boost/parameters/input_boost_ms
        # Setting b.L scheduler parameters
        echo 0 > /proc/sys/kernel/sched_boost
        echo 1 > /proc/sys/kernel/sched_migration_fixup
        echo 45 > /proc/sys/kernel/sched_downmigrate
        echo 65 > /proc/sys/kernel/sched_upmigrate
        echo 400000 > /proc/sys/kernel/sched_freq_inc_notify
        echo 400000 > /proc/sys/kernel/sched_freq_dec_notify
        echo 3 > /proc/sys/kernel/sched_spill_nr_run
        echo 100 > /proc/sys/kernel/sched_init_task_load
        # Enable bus-dcvs
        for cpubw in /sys/class/devfreq/*qcom,cpubw*
        do
            echo "bw_hwmon" > $cpubw/governor
            echo 50 > $cpubw/polling_interval
            echo 1525 > $cpubw/min_freq
            echo "1525 5195 11863 13763" > $cpubw/bw_hwmon/mbps_zones
            echo 4 > $cpubw/bw_hwmon/sample_ms
            echo 34 > $cpubw/bw_hwmon/io_percent
            echo 20 > $cpubw/bw_hwmon/hist_memory
            echo 10 > $cpubw/bw_hwmon/hyst_length
            echo 0 > $cpubw/bw_hwmon/low_power_ceil_mbps
            echo 34 > $cpubw/bw_hwmon/low_power_io_percent
            echo 20 > $cpubw/bw_hwmon/low_power_delay
            echo 0 > $cpubw/bw_hwmon/guard_band_mbps
            echo 250 > $cpubw/bw_hwmon/up_scale
            echo 1600 > $cpubw/bw_hwmon/idle_mbps
        done

        for memlat in /sys/class/devfreq/*qcom,memlat-cpu*
        do
            echo "mem_latency" > $memlat/governor
            echo 10 > $memlat/polling_interval
        done
        echo "cpufreq" > /sys/class/devfreq/soc:qcom,mincpubw/governor

        soc_revision=`cat /sys/devices/soc0/revision`
        if [ "$soc_revision" == "2.0" ]; then
          #Disable suspend for v1.0 and v2.0
          echo pwr_dbg > /sys/power/wake_lock
        elif [ "$soc_revision" == "2.1" ]; then
          # Enable C4.D4.E4.M3 LPM modes
          # Disable D3 state
          echo 0 > /sys/module/lpm_levels/system/pwr/pwr-l2-gdhs/idle_enabled
          echo 0 > /sys/module/lpm_levels/system/perf/perf-l2-gdhs/idle_enabled
          # Disable DEF-FPC mode
          echo N > /sys/module/lpm_levels/system/pwr/cpu0/fpc-def/idle_enabled
          echo N > /sys/module/lpm_levels/system/pwr/cpu1/fpc-def/idle_enabled
          echo N > /sys/module/lpm_levels/system/perf/cpu2/fpc-def/idle_enabled
          echo N > /sys/module/lpm_levels/system/perf/cpu3/fpc-def/idle_enabled
        else
          # Enable all LPMs by default
          # This will enable C4, D4, D3, E4 and M3 LPMs
          echo N > /sys/module/lpm_levels/parameters/sleep_disabled
        fi
        echo N > /sys/module/lpm_levels/parameters/sleep_disabled
        # Starting io prefetcher service
    ;;
esac

rm /data/system/perfd/default_values
setprop ro.min_freq_0 384000
setprop ro.min_freq_4 384000
start perfd

# core_ctl module
insmod /system/lib/modules/core_ctl.ko
echo 2 > /sys/devices/system/cpu/cpu2/core_ctl/max_cpus
echo 0 > /sys/devices/system/cpu/cpu2/core_ctl/min_cpus
echo 25 30 > /sys/devices/system/cpu/cpu2/core_ctl/busy_up_thres
echo 12 15 > /sys/devices/system/cpu/cpu2/core_ctl/busy_down_thres
echo 2000 > /sys/devices/system/cpu/cpu2/core_ctl/offline_delay_ms
echo 1 > /sys/devices/system/cpu/cpu2/core_ctl/is_big_cluster
echo 2 > /sys/devices/system/cpu/cpu2/core_ctl/task_thres


# Configure foreground and background cpuset
echo "0-3" > /dev/cpuset/foreground/cpus
echo "2-3" > /dev/cpuset/foreground/boost/cpus
echo "0-1" > /dev/cpuset/background/cpus
echo "0-2" > /dev/cpuset/system-background/cpus
echo 0-3 > /dev/cpuset/top-app/cpus

# GPU
echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
echo 750000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
echo 133000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
echo 6 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
echo 6 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

# IO
echo 256 > /sys/block/mmcblk0/queue/read_ahead_kb
echo noop > /sys/block/mmcblk0/queue/scheduler
echo cfq > /sys/block/mmcblk0/queue/scheduler
echo sio > /sys/block/mmcblk0/queue/scheduler
echo deadline > /sys/block/mmcblk0/queue/scheduler
echo row > /sys/block/mmcblk0/queue/scheduler

#echo 1 > /sys/module/msm_thermal/core_control/enabled
#echo 0 > /sys/module/msm_thermal/core_control/cpus_offlined
#echo 0 > /sys/module/msm_thermal/core_control/enabled
#echo N > /sys/module/msm_thermal/parameters/enabled
echo 0 > /sys/module/msm_thermal/core_control/cpus_offlined
echo 1 > /sys/devices/system/cpu/cpu0/online
echo 1 > /sys/devices/system/cpu/cpu1/online
echo 1 > /sys/devices/system/cpu/cpu2/online
echo 1 > /sys/devices/system/cpu/cpu3/online

echo "0" > /sys/module/cpu_boost/parameters/input_boost_freq
echo 0 > /sys/module/cpu_boost/parameters/input_boost_ms
echo 1 > /proc/sys/kernel/sched_prefer_sync_wakee_to_waker

echo 0 > /sys/module/msm_thermal/core_control/enabled
echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
#echo N > /sys/module/msm_thermal/parameters/enabled
echo 0 > /proc/sys/kernel/sched_boost
stop thermanager
stop thermal-engine
killall -9 vendor.qti.hardware.perf@1.0-service
