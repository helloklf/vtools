#!/system/bin/sh

target=`getprop ro.board.platform`

case "$target" in
    "msm8952")

        #Enable adaptive LMK and set vmpressure_file_min
        ProductName=`getprop ro.product.name`
        if [ "$ProductName" == "msm8952_32" ] || [ "$ProductName" == "msm8952_32_LMT" ]; then
            echo 1 > /sys/module/lowmemorykiller/parameters/enable_adaptive_lmk
            echo 53059 > /sys/module/lowmemorykiller/parameters/vmpressure_file_min
        elif [ "$ProductName" == "msm8952_64" ] || [ "$ProductName" == "msm8952_64_LMT" ]; then
            echo 1 > /sys/module/lowmemorykiller/parameters/enable_adaptive_lmk
            echo 81250 > /sys/module/lowmemorykiller/parameters/vmpressure_file_min
        fi

        if [ -f /sys/devices/soc0/soc_id ]; then
            soc_id=`cat /sys/devices/soc0/soc_id`
        else
            soc_id=`cat /sys/devices/system/soc/soc0/id`
        fi
        case "$soc_id" in
            "264" | "289")
                # Apply Scheduler and Governor settings for 8952

                # HMP scheduler settings
                echo 3 > /proc/sys/kernel/sched_window_stats_policy
                echo 3 > /proc/sys/kernel/sched_ravg_hist_size
                echo 20000000 > /proc/sys/kernel/sched_ravg_window

                # HMP Task packing settings
                echo 20 > /proc/sys/kernel/sched_small_task
                echo 30 > /sys/devices/system/cpu/cpu0/sched_mostly_idle_load
                echo 30 > /sys/devices/system/cpu/cpu1/sched_mostly_idle_load
                echo 30 > /sys/devices/system/cpu/cpu2/sched_mostly_idle_load
                echo 30 > /sys/devices/system/cpu/cpu3/sched_mostly_idle_load
                echo 30 > /sys/devices/system/cpu/cpu4/sched_mostly_idle_load
                echo 30 > /sys/devices/system/cpu/cpu5/sched_mostly_idle_load
                echo 30 > /sys/devices/system/cpu/cpu6/sched_mostly_idle_load
                echo 30 > /sys/devices/system/cpu/cpu7/sched_mostly_idle_load

                echo 3 > /sys/devices/system/cpu/cpu0/sched_mostly_idle_nr_run
                echo 3 > /sys/devices/system/cpu/cpu1/sched_mostly_idle_nr_run
                echo 3 > /sys/devices/system/cpu/cpu2/sched_mostly_idle_nr_run
                echo 3 > /sys/devices/system/cpu/cpu3/sched_mostly_idle_nr_run
                echo 3 > /sys/devices/system/cpu/cpu4/sched_mostly_idle_nr_run
                echo 3 > /sys/devices/system/cpu/cpu5/sched_mostly_idle_nr_run
                echo 3 > /sys/devices/system/cpu/cpu6/sched_mostly_idle_nr_run
                echo 3 > /sys/devices/system/cpu/cpu7/sched_mostly_idle_nr_run

                echo 0 > /sys/devices/system/cpu/cpu0/sched_prefer_idle
                echo 0 > /sys/devices/system/cpu/cpu1/sched_prefer_idle
                echo 0 > /sys/devices/system/cpu/cpu2/sched_prefer_idle
                echo 0 > /sys/devices/system/cpu/cpu3/sched_prefer_idle
                echo 0 > /sys/devices/system/cpu/cpu4/sched_prefer_idle
                echo 0 > /sys/devices/system/cpu/cpu5/sched_prefer_idle
                echo 0 > /sys/devices/system/cpu/cpu6/sched_prefer_idle
                echo 0 > /sys/devices/system/cpu/cpu7/sched_prefer_idle

                for devfreq_gov in /sys/class/devfreq/qcom,mincpubw*/governor
                do
                    echo "cpufreq" > $devfreq_gov
                done

                for devfreq_gov in /sys/class/devfreq/qcom,cpubw*/governor
                do
                    echo "bw_hwmon" > $devfreq_gov
                    for cpu_io_percent in /sys/class/devfreq/qcom,cpubw*/bw_hwmon/io_percent
                    do
                        echo 20 > $cpu_io_percent
                    done
                    for cpu_guard_band in /sys/class/devfreq/qcom,cpubw*/bw_hwmon/guard_band_mbps
                    do
                        echo 30 > $cpu_guard_band
                    done
                done

                for gpu_bimc_io_percent in /sys/class/devfreq/qcom,gpubw*/bw_hwmon/io_percent
                do
                    echo 40 > $gpu_bimc_io_percent
                done
                # disable thermal & BCL core_control to update interactive gov settings
                echo 0 > /sys/module/msm_thermal/core_control/enabled
                for mode in /sys/devices/soc.0/qcom,bcl.*/mode
                do
                    echo -n disable > $mode
                done
                for hotplug_mask in /sys/devices/soc.0/qcom,bcl.*/hotplug_mask
                do
                    bcl_hotplug_mask=`cat $hotplug_mask`
                    echo 0 > $hotplug_mask
                done
                for hotplug_soc_mask in /sys/devices/soc.0/qcom,bcl.*/hotplug_soc_mask
                do
                    bcl_soc_hotplug_mask=`cat $hotplug_soc_mask`
                    echo 0 > $hotplug_soc_mask
                done
                for mode in /sys/devices/soc.0/qcom,bcl.*/mode
                do
                    echo -n enable > $mode
                done

                # enable governor for perf cluster
                echo 1 > /sys/devices/system/cpu/cpu0/online
                echo "interactive" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
                echo "19000 1113600:39000" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay
                echo 85 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
                echo 20000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
                echo 1113600 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
                echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
                echo "1 960000:85 1113600:90 1344000:80" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
                echo 40000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
                echo 40000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/sampling_down_factor
                echo 960000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq

                # enable governor for power cluster
                echo 1 > /sys/devices/system/cpu/cpu4/online
                echo "interactive" > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor
                echo 39000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/above_hispeed_delay
                echo 90 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/go_hispeed_load
                echo 20000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate
                echo 800000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
                echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
                echo "1 800000:90" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
                echo 40000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/min_sample_time
                echo 40000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/sampling_down_factor
                echo 800000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq

                # re-enable thermal & BCL core_control now
                echo 1 > /sys/module/msm_thermal/core_control/enabled
                for mode in /sys/devices/soc.0/qcom,bcl.*/mode
                do
                    echo -n disable > $mode
                done
                for hotplug_mask in /sys/devices/soc.0/qcom,bcl.*/hotplug_mask
                do
                    echo $bcl_hotplug_mask > $hotplug_mask
                done
                for hotplug_soc_mask in /sys/devices/soc.0/qcom,bcl.*/hotplug_soc_mask
                do
                    echo $bcl_soc_hotplug_mask > $hotplug_soc_mask
                done
                for mode in /sys/devices/soc.0/qcom,bcl.*/mode
                do
                    echo -n enable > $mode
                done

                # Bring up all cores online
                echo 1 > /sys/devices/system/cpu/cpu1/online
                echo 1 > /sys/devices/system/cpu/cpu2/online
                echo 1 > /sys/devices/system/cpu/cpu3/online
                echo 1 > /sys/devices/system/cpu/cpu4/online
                echo 1 > /sys/devices/system/cpu/cpu5/online
                echo 1 > /sys/devices/system/cpu/cpu6/online
                echo 1 > /sys/devices/system/cpu/cpu7/online

                # Keeping Low power modes disabled
                echo 1 > /sys/module/lpm_levels/parameters/sleep_disabled

                # HMP scheduler (big.Little cluster related) settings
                echo 93 > /proc/sys/kernel/sched_upmigrate
                echo 83 > /proc/sys/kernel/sched_downmigrate

                # Enable sched guided freq control
                echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_sched_load
                echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_migration_notif
                echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/use_sched_load
                echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/use_migration_notif
                echo 50000 > /proc/sys/kernel/sched_freq_inc_notify
                echo 50000 > /proc/sys/kernel/sched_freq_dec_notify

                # Enable core control
                echo 2 > /sys/devices/system/cpu/cpu0/core_ctl/min_cpus
                echo 4 > /sys/devices/system/cpu/cpu0/core_ctl/max_cpus
                echo 68 > /sys/devices/system/cpu/cpu0/core_ctl/busy_up_thres
                echo 40 > /sys/devices/system/cpu/cpu0/core_ctl/busy_down_thres
                echo 100 > /sys/devices/system/cpu/cpu0/core_ctl/offline_delay_ms

                # Enable dynamic clock gating
                echo 1 > /sys/module/lpm_levels/lpm_workarounds/dynamic_clock_gating
                # Enable timer migration to little cluster
                echo 1 > /proc/sys/kernel/power_aware_timer_migration
            ;;
            *)
                panel=`cat /sys/class/graphics/fb0/modes`
                if [ "${panel:5:1}" == "x" ]; then
                    panel=${panel:2:3}
                else
                    panel=${panel:2:4}
                fi

                # Apply Scheduler and Governor settings for 8976
                # SoC IDs are 266, 274, 277, 278

                # HMP scheduler (big.Little cluster related) settings
                echo 95 > /proc/sys/kernel/sched_upmigrate
                echo 85 > /proc/sys/kernel/sched_downmigrate

                if [ $panel -gt 1080 ]; then
                    echo 2 > /proc/sys/kernel/sched_window_stats_policy
                    echo 5 > /proc/sys/kernel/sched_ravg_hist_size
                else
                    echo 3 > /proc/sys/kernel/sched_window_stats_policy
                    echo 3 > /proc/sys/kernel/sched_ravg_hist_size

                    echo 0 > /sys/devices/system/cpu/cpu0/sched_prefer_idle
                    echo 0 > /sys/devices/system/cpu/cpu1/sched_prefer_idle
                    echo 0 > /sys/devices/system/cpu/cpu2/sched_prefer_idle
                    echo 0 > /sys/devices/system/cpu/cpu3/sched_prefer_idle
                    echo 0 > /sys/devices/system/cpu/cpu4/sched_prefer_idle
                    echo 0 > /sys/devices/system/cpu/cpu5/sched_prefer_idle
                    echo 0 > /sys/devices/system/cpu/cpu6/sched_prefer_idle
                    echo 0 > /sys/devices/system/cpu/cpu7/sched_prefer_idle
                fi

                echo 3 > /sys/devices/system/cpu/cpu0/sched_mostly_idle_nr_run
                echo 3 > /sys/devices/system/cpu/cpu1/sched_mostly_idle_nr_run
                echo 3 > /sys/devices/system/cpu/cpu2/sched_mostly_idle_nr_run
                echo 3 > /sys/devices/system/cpu/cpu3/sched_mostly_idle_nr_run
                echo 3 > /sys/devices/system/cpu/cpu4/sched_mostly_idle_nr_run
                echo 3 > /sys/devices/system/cpu/cpu5/sched_mostly_idle_nr_run
                echo 3 > /sys/devices/system/cpu/cpu6/sched_mostly_idle_nr_run
                echo 3 > /sys/devices/system/cpu/cpu7/sched_mostly_idle_nr_run

                for devfreq_gov in /sys/class/devfreq/qcom,mincpubw*/governor
                do
                    echo "cpufreq" > $devfreq_gov
                done

                for devfreq_gov in /sys/class/devfreq/qcom,cpubw*/governor
                do
                    echo "bw_hwmon" > $devfreq_gov
                    for cpu_io_percent in /sys/class/devfreq/qcom,cpubw*/bw_hwmon/io_percent
                    do
                        echo 20 > $cpu_io_percent
                    done
                    for cpu_guard_band in /sys/class/devfreq/qcom,cpubw*/bw_hwmon/guard_band_mbps
                    do
                        echo 30 > $cpu_guard_band
                    done
                done

                for gpu_bimc_io_percent in /sys/class/devfreq/qcom,gpubw*/bw_hwmon/io_percent
                do
                    echo 40 > $gpu_bimc_io_percent
                done
                # disable thermal & BCL core_control to update interactive gov settings
                echo 0 > /sys/module/msm_thermal/core_control/enabled
                for mode in /sys/devices/soc.0/qcom,bcl.*/mode
                do
                    echo -n disable > $mode
                done
                for hotplug_mask in /sys/devices/soc.0/qcom,bcl.*/hotplug_mask
                do
                    bcl_hotplug_mask=`cat $hotplug_mask`
                    echo 0 > $hotplug_mask
                done
                for hotplug_soc_mask in /sys/devices/soc.0/qcom,bcl.*/hotplug_soc_mask
                do
                    bcl_soc_hotplug_mask=`cat $hotplug_soc_mask`
                    echo 0 > $hotplug_soc_mask
                done
                for mode in /sys/devices/soc.0/qcom,bcl.*/mode
                do
                    echo -n enable > $mode
                done

                # enable governor for power cluster
                echo 1 > /sys/devices/system/cpu/cpu0/online
                echo "interactive" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
                echo 90 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
                echo 20000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
                echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
                echo 40000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
                echo 691200 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq

                # enable governor for perf cluster
                echo 1 > /sys/devices/system/cpu/cpu4/online
                echo "interactive" > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor
                echo 85 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/go_hispeed_load
                echo 20000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate
                echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
                echo 40000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/min_sample_time
                echo 40000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/sampling_down_factor
                echo 883200 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq

                if [ $panel -gt 1080 ]; then
                    echo 19000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay
                    echo 1017600 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
                    echo "80" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
                    echo 1382400 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
                    echo 800000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/max_freq_hysteresis
                    echo "19000 1382400:39000" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/above_hispeed_delay
                    echo "85 1382400:90 1747200:80" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
                    # HMP Task packing settings for 8976
                    echo 30 > /proc/sys/kernel/sched_small_task
                    echo 20 > /sys/devices/system/cpu/cpu0/sched_mostly_idle_load
                    echo 20 > /sys/devices/system/cpu/cpu1/sched_mostly_idle_load
                    echo 20 > /sys/devices/system/cpu/cpu2/sched_mostly_idle_load
                    echo 20 > /sys/devices/system/cpu/cpu3/sched_mostly_idle_load
                    echo 20 > /sys/devices/system/cpu/cpu4/sched_mostly_idle_load
                    echo 20 > /sys/devices/system/cpu/cpu5/sched_mostly_idle_load
                    echo 20 > /sys/devices/system/cpu/cpu6/sched_mostly_idle_load
                    echo 20 > /sys/devices/system/cpu/cpu7/sched_mostly_idle_load
                else
                    echo 39000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay
                    echo 806400 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
                    echo "1 691200:90" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
                    echo 1190400 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
                    echo "19000 1190400:39000" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/above_hispeed_delay
                    echo "85 1190400:90 1747200:80" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
                    # HMP Task packing settings for 8976
                    echo 20 > /proc/sys/kernel/sched_small_task
                    echo 30 > /sys/devices/system/cpu/cpu0/sched_mostly_idle_load
                    echo 30 > /sys/devices/system/cpu/cpu1/sched_mostly_idle_load
                    echo 30 > /sys/devices/system/cpu/cpu2/sched_mostly_idle_load
                    echo 30 > /sys/devices/system/cpu/cpu3/sched_mostly_idle_load
                    echo 30 > /sys/devices/system/cpu/cpu4/sched_mostly_idle_load
                    echo 30 > /sys/devices/system/cpu/cpu5/sched_mostly_idle_load
                    echo 30 > /sys/devices/system/cpu/cpu6/sched_mostly_idle_load
                    echo 30 > /sys/devices/system/cpu/cpu7/sched_mostly_idle_load
                fi

                # re-enable thermal & BCL core_control now
                echo 1 > /sys/module/msm_thermal/core_control/enabled
                for mode in /sys/devices/soc.0/qcom,bcl.*/mode
                do
                    echo -n disable > $mode
                done
                for hotplug_mask in /sys/devices/soc.0/qcom,bcl.*/hotplug_mask
                do
                    echo $bcl_hotplug_mask > $hotplug_mask
                done
                for hotplug_soc_mask in /sys/devices/soc.0/qcom,bcl.*/hotplug_soc_mask
                do
                    echo $bcl_soc_hotplug_mask > $hotplug_soc_mask
                done
                for mode in /sys/devices/soc.0/qcom,bcl.*/mode
                do
                    echo -n enable > $mode
                done

                # Bring up all cores online
                echo 1 > /sys/devices/system/cpu/cpu1/online
                echo 1 > /sys/devices/system/cpu/cpu2/online
                echo 1 > /sys/devices/system/cpu/cpu3/online
                echo 1 > /sys/devices/system/cpu/cpu4/online
                echo 1 > /sys/devices/system/cpu/cpu5/online
                echo 1 > /sys/devices/system/cpu/cpu6/online
                echo 1 > /sys/devices/system/cpu/cpu7/online

                #Disable CPU retention modes for 32bit builds
                ProductName=`getprop ro.product.name`
                if [ "$ProductName" == "msm8952_32" ] || [ "$ProductName" == "msm8952_32_LMT" ]; then
                    echo N > /sys/module/lpm_levels/system/a72/cpu4/retention/idle_enabled
                    echo N > /sys/module/lpm_levels/system/a72/cpu5/retention/idle_enabled
                    echo N > /sys/module/lpm_levels/system/a72/cpu6/retention/idle_enabled
                    echo N > /sys/module/lpm_levels/system/a72/cpu7/retention/idle_enabled
                fi

                # Enable Low power modes
                echo 0 > /sys/module/lpm_levels/parameters/sleep_disabled

                # Enable sched guided freq control
                echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_sched_load
                echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_migration_notif
                echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/use_sched_load
                echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/use_migration_notif
                echo 50000 > /proc/sys/kernel/sched_freq_inc_notify
                echo 50000 > /proc/sys/kernel/sched_freq_dec_notify

                # Enable core control
                echo 0 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
                echo 4 > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
                echo 73 > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
                echo 40 > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres
                echo 200 > /sys/devices/system/cpu/cpu4/core_ctl/offline_delay_ms

                # Enable timer migration to little cluster
                echo 1 > /proc/sys/kernel/power_aware_timer_migration
            ;;
        esac
    ;;
esac

chown -h system /sys/devices/system/cpu/cpufreq/ondemand/sampling_rate
chown -h system /sys/devices/system/cpu/cpufreq/ondemand/sampling_down_factor
chown -h system /sys/devices/system/cpu/cpufreq/ondemand/io_is_busy

killall -9 vendor.qti.hardware.perf@1.0-service
echo 0 > /dev/cpuset/background/cpus
echo 0-2 > /dev/cpuset/system-background/cpus
echo 4-7 > /dev/cpuset/foreground/boost/cpus
echo 0-7 > /dev/cpuset/foreground/cpus
echo 0 > /proc/sys/kernel/sched_boost
echo 0-7 > /dev/cpuset/top-app/cpus

echo 1 > /proc/sys/kernel/sched_prefer_sync_wakee_to_waker

