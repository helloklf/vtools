#!/system/bin/sh

target=`getprop ro.board.platform`

case "$target" in
    "msm8953")

        if [ -f /sys/devices/soc0/soc_id ]; then
            soc_id=`cat /sys/devices/soc0/soc_id`
        else
            soc_id=`cat /sys/devices/system/soc/soc0/id`
        fi

        if [ -f /sys/devices/soc0/hw_platform ]; then
            hw_platform=`cat /sys/devices/soc0/hw_platform`
        else
            hw_platform=`cat /sys/devices/system/soc/soc0/hw_platform`
        fi

        case "$soc_id" in
            "293" | "304" )

                # Start Host based Touch processing
                case "$hw_platform" in
                     "MTP" | "Surf" | "RCM" )
                        #if this directory is present, it means that a
                        #1200p panel is connected to the device.
                        dir="/sys/bus/i2c/devices/3-0038"
                        if [ ! -d "$dir" ]; then
                              start hbtp
                        fi
                        ;;
                esac

                #scheduler settings
                echo 3 > /proc/sys/kernel/sched_window_stats_policy
                echo 3 > /proc/sys/kernel/sched_ravg_hist_size
                #task packing settings
                echo 0 > /sys/devices/system/cpu/cpu0/sched_static_cpu_pwr_cost
                echo 0 > /sys/devices/system/cpu/cpu1/sched_static_cpu_pwr_cost
                echo 0 > /sys/devices/system/cpu/cpu2/sched_static_cpu_pwr_cost
                echo 0 > /sys/devices/system/cpu/cpu3/sched_static_cpu_pwr_cost
                echo 0 > /sys/devices/system/cpu/cpu4/sched_static_cpu_pwr_cost
                echo 0 > /sys/devices/system/cpu/cpu5/sched_static_cpu_pwr_cost
                echo 0 > /sys/devices/system/cpu/cpu6/sched_static_cpu_pwr_cost
                echo 0 > /sys/devices/system/cpu/cpu7/sched_static_cpu_pwr_cost

                #init task load, restrict wakeups to preferred cluster
                echo 15 > /proc/sys/kernel/sched_init_task_load
                # spill load is set to 100% by default in the kernel
                echo 3 > /proc/sys/kernel/sched_spill_nr_run
                # Apply inter-cluster load balancer restrictions
                echo 1 > /proc/sys/kernel/sched_restrict_cluster_spill


                for devfreq_gov in /sys/class/devfreq/qcom,mincpubw*/governor
                do
                    echo "cpufreq" > $devfreq_gov
                done

                for devfreq_gov in /sys/class/devfreq/soc:qcom,cpubw/governor
                do
                    echo "bw_hwmon" > $devfreq_gov
                    for cpu_io_percent in /sys/class/devfreq/soc:qcom,cpubw/bw_hwmon/io_percent
                    do
                        echo 34 > $cpu_io_percent
                    done
                    for cpu_guard_band in /sys/class/devfreq/soc:qcom,cpubw/bw_hwmon/guard_band_mbps
                    do
                        echo 0 > $cpu_guard_band
                    done
                    for cpu_hist_memory in /sys/class/devfreq/soc:qcom,cpubw/bw_hwmon/hist_memory
                    do
                        echo 20 > $cpu_hist_memory
                    done
                    for cpu_hyst_length in /sys/class/devfreq/soc:qcom,cpubw/bw_hwmon/hyst_length
                    do
                        echo 10 > $cpu_hyst_length
                    done
                    for cpu_idle_mbps in /sys/class/devfreq/soc:qcom,cpubw/bw_hwmon/idle_mbps
                    do
                        echo 1600 > $cpu_idle_mbps
                    done
                    for cpu_low_power_delay in /sys/class/devfreq/soc:qcom,cpubw/bw_hwmon/low_power_delay
                    do
                        echo 20 > $cpu_low_power_delay
                    done
                    for cpu_low_power_io_percent in /sys/class/devfreq/soc:qcom,cpubw/bw_hwmon/low_power_io_percent
                    do
                        echo 34 > $cpu_low_power_io_percent
                    done
                    for cpu_mbps_zones in /sys/class/devfreq/soc:qcom,cpubw/bw_hwmon/mbps_zones
                    do
                        echo "1611 3221 5859 6445 7104" > $cpu_mbps_zones
                    done
                    for cpu_sample_ms in /sys/class/devfreq/soc:qcom,cpubw/bw_hwmon/sample_ms
                    do
                        echo 4 > $cpu_sample_ms
                    done
                    for cpu_up_scale in /sys/class/devfreq/soc:qcom,cpubw/bw_hwmon/up_scale
                    do
                        echo 250 > $cpu_up_scale
                    done
                    for cpu_min_freq in /sys/class/devfreq/soc:qcom,cpubw/min_freq
                    do
                        echo 1611 > $cpu_min_freq
                    done
                done

                for gpu_bimc_io_percent in /sys/class/devfreq/soc:qcom,gpubw/bw_hwmon/io_percent
                do
                    echo 40 > $gpu_bimc_io_percent
                done

		# Configure DCC module to capture critical register contents when device crashes
		for DCC_PATH in /sys/bus/platform/devices/*.dcc*
		do
			echo  0 > $DCC_PATH/enable
			echo cap >  $DCC_PATH/func_type
			echo sram > $DCC_PATH/data_sink
			echo  1 > $DCC_PATH/config_reset

			# Register specifies APC CPR closed-loop settled voltage for current voltage corner
			echo 0xb1d2c18 1 > $DCC_PATH/config

			# Register specifies SW programmed open-loop voltage for current voltage corner
			echo 0xb1d2900 1 > $DCC_PATH/config

			# Register specifies APM switch settings and APM FSM state
			echo 0xb1112b0 1 > $DCC_PATH/config

			# Register specifies CPR mode change state and also #online cores input to CPR HW
			echo 0xb018798 1 > $DCC_PATH/config

			echo  1 > $DCC_PATH/enable
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

                echo 0 > /dev/cpuset/background/cpus
                echo 0 > /dev/cpuset/system-background/cpus
                echo 1-7 > /dev/cpuset/foreground/cpus
                echo 1-3 > /dev/cpuset/foreground/boost/cpus
	;;
	esac
	;;
esac

chown -h system /sys/devices/system/cpu/cpufreq/ondemand/sampling_rate
chown -h system /sys/devices/system/cpu/cpufreq/ondemand/sampling_down_factor
chown -h system /sys/devices/system/cpu/cpufreq/ondemand/io_is_busy

emmc_boot=`getprop ro.boot.emmc`
case "$emmc_boot"
    in "true")
        chown -h system /sys/devices/platform/rs300000a7.65536/force_sync
        chown -h system /sys/devices/platform/rs300000a7.65536/sync_sts
        chown -h system /sys/devices/platform/rs300100a7.65536/force_sync
        chown -h system /sys/devices/platform/rs300100a7.65536/sync_sts
    ;;
esac

# Post-setup services
case "$target" in
    "msm8937" | "msm8953")
        echo 128 > /sys/block/mmcblk0/bdi/read_ahead_kb
        echo 128 > /sys/block/mmcblk0/queue/read_ahead_kb
        echo 128 > /sys/block/dm-0/queue/read_ahead_kb
        echo 128 > /sys/block/dm-1/queue/read_ahead_kb
        setprop sys.post_boot.parsed 1
        start gamed
    ;;
esac

# Install AdrenoTest.apk if not already installed
if [ -f /data/prebuilt/AdrenoTest.apk ]; then
    if [ ! -d /data/data/com.qualcomm.adrenotest ]; then
        pm install /data/prebuilt/AdrenoTest.apk
    fi
fi

# Install SWE_Browser.apk if not already installed
if [ -f /data/prebuilt/SWE_AndroidBrowser.apk ]; then
    if [ ! -d /data/data/com.android.swe.browser ]; then
        pm install /data/prebuilt/SWE_AndroidBrowser.apk
    fi
fi

# Let kernel know our image version/variant/crm_version
if [ -f /sys/devices/soc0/select_image ]; then
    image_version="10:"
    image_version+=`getprop ro.build.id`
    image_version+=":"
    image_version+=`getprop ro.build.version.incremental`
    image_variant=`getprop ro.product.name`
    image_variant+="-"
    image_variant+=`getprop ro.build.type`
    oem_version=`getprop ro.build.version.codename`
    echo 10 > /sys/devices/soc0/select_image
    echo $image_version > /sys/devices/soc0/image_version
    echo $image_variant > /sys/devices/soc0/image_variant
    echo $oem_version > /sys/devices/soc0/image_crm_version
fi

# Change console log level as per console config property
console_config=`getprop persist.console.silent.config`
case "$console_config" in
    "1")
        echo "Enable console config to $console_config"
        echo 0 > /proc/sys/kernel/printk
        ;;
    *)
        echo "Enable console config to $console_config"
        ;;
esac


#chmod 0644 /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
#echo "ondemand" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
#chmod 0644 /sys/module/workqueue/parameters/power_efficient
#echo "Y" > /sys/module/workqueue/parameters/power_efficient
#chmod 0644 -R /sys/devices/system/cpu/cpufreq/ondemand
#echo 1 > /sys/devices/system/cpu/cpufreq/ondemand/io_is_busy
#echo 90 > /sys/devices/system/cpu/cpufreq/ondemand/up_threshold

