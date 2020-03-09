#! /vendor/bin/sh

target=`getprop ro.board.platform`

case "$target" in
	"kona")

	ddr_type=`od -An -tx /proc/device-tree/memory/ddr_device_type`
	ddr_type4="07"
	ddr_type5="08"

	# Core control parameters for gold
	echo 2 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
	echo 60 > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
	echo 30 > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres
	echo 100 > /sys/devices/system/cpu/cpu4/core_ctl/offline_delay_ms
	echo 3 > /sys/devices/system/cpu/cpu4/core_ctl/task_thres

	# Core control parameters for gold+
	echo 0 > /sys/devices/system/cpu/cpu7/core_ctl/min_cpus
	echo 60 > /sys/devices/system/cpu/cpu7/core_ctl/busy_up_thres
	echo 30 > /sys/devices/system/cpu/cpu7/core_ctl/busy_down_thres
	echo 100 > /sys/devices/system/cpu/cpu7/core_ctl/offline_delay_ms
	echo 1 > /sys/devices/system/cpu/cpu7/core_ctl/task_thres
	# Controls how many more tasks should be eligible to run on gold CPUs
	# w.r.t number of gold CPUs available to trigger assist (max number of
	# tasks eligible to run on previous cluster minus number of CPUs in
	# the previous cluster).
	#
	# Setting to 1 by default which means there should be at least
	# 4 tasks eligible to run on gold cluster (tasks running on gold cores
	# plus misfit tasks on silver cores) to trigger assitance from gold+.
	echo 1 > /sys/devices/system/cpu/cpu7/core_ctl/nr_prev_assist_thresh

	# Disable Core control on silver
	echo 0 > /sys/devices/system/cpu/cpu0/core_ctl/enable

	# Setting b.L scheduler parameters
	echo 95 95 > /proc/sys/kernel/sched_upmigrate
	echo 85 85 > /proc/sys/kernel/sched_downmigrate
	echo 100 > /proc/sys/kernel/sched_group_upmigrate
	echo 85 > /proc/sys/kernel/sched_group_downmigrate
	echo 1 > /proc/sys/kernel/sched_walt_rotate_big_tasks
	echo 400000000 > /proc/sys/kernel/sched_coloc_downmigrate_ns

	# cpuset parameters
	echo 0-2 > /dev/cpuset/background/cpus
	echo 0-3 > /dev/cpuset/system-background/cpus
	echo 0-2,4-7 > /dev/cpuset/foreground/cpus
	echo 0-7 > /dev/cpuset/top-app/cpus

	# Turn off scheduler boost at the end
	echo 0 > /proc/sys/kernel/sched_boost

	# Turn on scheduler boost for top app main
	echo 1 > /proc/sys/kernel/sched_boost_top_app

	# configure governor settings for silver cluster
	echo "schedutil" > /sys/devices/system/cpu/cpufreq/policy0/scaling_governor
	echo 0 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/down_rate_limit_us
	echo 0 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/up_rate_limit_us
        if [ `cat /sys/devices/soc0/revision` == "2.0" ]; then
		echo 1248000 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
	else
		echo 1228800 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
	fi
	echo 691200 > /sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq
	echo 1 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/pl

	# configure input boost settings
	echo "0:1324800" > /sys/devices/system/cpu/cpu_boost/input_boost_freq
	echo 120 > /sys/devices/system/cpu/cpu_boost/input_boost_ms
        echo "0:0 1:0 2:0 3:0 4:2342400 5:0 6:0 7:2361600" > /sys/devices/system/cpu/cpu_boost/powerkey_input_boost_freq
        echo 400 > /sys/devices/system/cpu/cpu_boost/powerkey_input_boost_ms

	# configure governor settings for gold cluster
	echo "schedutil" > /sys/devices/system/cpu/cpufreq/policy4/scaling_governor
	echo 0 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/down_rate_limit_us
	echo 0 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/up_rate_limit_us
	echo 1574400 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
	echo 1 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/pl

	# configure governor settings for gold+ cluster
	echo "schedutil" > /sys/devices/system/cpu/cpufreq/policy7/scaling_governor
	echo 0 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/down_rate_limit_us
	echo 0 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/up_rate_limit_us
        if [ `cat /sys/devices/soc0/revision` == "2.0" ]; then
		echo 1632000 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq
	else
		echo 1612800 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq
	fi
	echo 1 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/pl

	# Enable bus-dcvs
	for device in /sys/devices/platform/soc
	do
	    for cpubw in $device/*cpu-cpu-llcc-bw/devfreq/*cpu-cpu-llcc-bw
	    do
		echo "bw_hwmon" > $cpubw/governor
		echo 40 > $cpubw/polling_interval
		echo "4577 7110 9155 12298 14236 15258" > $cpubw/bw_hwmon/mbps_zones
		echo 4 > $cpubw/bw_hwmon/sample_ms
		echo 50 > $cpubw/bw_hwmon/io_percent
		echo 20 > $cpubw/bw_hwmon/hist_memory
		echo 10 > $cpubw/bw_hwmon/hyst_length
		echo 30 > $cpubw/bw_hwmon/down_thres
		echo 0 > $cpubw/bw_hwmon/guard_band_mbps
		echo 250 > $cpubw/bw_hwmon/up_scale
		echo 1600 > $cpubw/bw_hwmon/idle_mbps
		echo 14236 > $cpubw/max_freq
	    done

	    for llccbw in $device/*cpu-llcc-ddr-bw/devfreq/*cpu-llcc-ddr-bw
	    do
		echo "bw_hwmon" > $llccbw/governor
		echo 40 > $llccbw/polling_interval
		if [ ${ddr_type:4:2} == $ddr_type4 ]; then
			echo "1720 2086 2929 3879 5161 5931 6881 7980" > $llccbw/bw_hwmon/mbps_zones
		elif [ ${ddr_type:4:2} == $ddr_type5 ]; then
			echo "1720 2086 2929 3879 5931 6881 7980 10437" > $llccbw/bw_hwmon/mbps_zones
		fi
		echo 4 > $llccbw/bw_hwmon/sample_ms
		echo 80 > $llccbw/bw_hwmon/io_percent
		echo 20 > $llccbw/bw_hwmon/hist_memory
		echo 10 > $llccbw/bw_hwmon/hyst_length
		echo 30 > $llccbw/bw_hwmon/down_thres
		echo 0 > $llccbw/bw_hwmon/guard_band_mbps
		echo 250 > $llccbw/bw_hwmon/up_scale
		echo 1600 > $llccbw/bw_hwmon/idle_mbps
		echo 6881 > $llccbw/max_freq
	    done

	    for npubw in $device/*npu*-ddr-bw/devfreq/*npu*-ddr-bw
	    do
		echo 1 > /sys/devices/virtual/npu/msm_npu/pwr
		echo "bw_hwmon" > $npubw/governor
		echo 40 > $npubw/polling_interval
		if [ ${ddr_type:4:2} == $ddr_type4 ]; then
			echo "1720 2086 2929 3879 5931 6881 7980" > $npubw/bw_hwmon/mbps_zones
		elif [ ${ddr_type:4:2} == $ddr_type5 ]; then
			echo "1720 2086 2929 3879 5931 6881 7980 10437" > $npubw/bw_hwmon/mbps_zones
		fi
		echo 4 > $npubw/bw_hwmon/sample_ms
		echo 160 > $npubw/bw_hwmon/io_percent
		echo 20 > $npubw/bw_hwmon/hist_memory
		echo 10 > $npubw/bw_hwmon/hyst_length
		echo 30 > $npubw/bw_hwmon/down_thres
		echo 0 > $npubw/bw_hwmon/guard_band_mbps
		echo 250 > $npubw/bw_hwmon/up_scale
		echo 1600 > $npubw/bw_hwmon/idle_mbps
		echo 0 > /sys/devices/virtual/npu/msm_npu/pwr
	    done

	    for npullccbw in $device/*npu*-llcc-bw/devfreq/*npu*-llcc-bw
	    do
            echo 1 > /sys/devices/virtual/npu/msm_npu/pwr
            echo "bw_hwmon" > $npullccbw/governor
            echo 40 > $npullccbw/polling_interval
            echo "4577 7110 9155 12298 14236 15258" > $npullccbw/bw_hwmon/mbps_zones
            echo 4 > $npullccbw/bw_hwmon/sample_ms
            echo 160 > $npullccbw/bw_hwmon/io_percent
            echo 20 > $npullccbw/bw_hwmon/hist_memory
            echo 10 > $npullccbw/bw_hwmon/hyst_length
            echo 30 > $npullccbw/bw_hwmon/down_thres
            echo 0 > $npullccbw/bw_hwmon/guard_band_mbps
            echo 250 > $npullccbw/bw_hwmon/up_scale
            echo 1600 > $npullccbw/bw_hwmon/idle_mbps
            echo 0 > /sys/devices/virtual/npu/msm_npu/pwr
	    done

	    #Enable mem_latency governor for L3 scaling
	    for memlat in $device/*qcom,devfreq-l3/*cpu*-lat/devfreq/*cpu*-lat
	    do
            echo "mem_latency" > $memlat/governor
            echo 10 > $memlat/polling_interval
            echo 400 > $memlat/mem_latency/ratio_ceil
	    done

	    #Enable cdspl3 governor for L3 cdsp nodes
	    for l3cdsp in $device/*qcom,devfreq-l3/*cdsp-l3-lat/devfreq/*cdsp-l3-lat
	    do
            echo "cdspl3" > $l3cdsp/governor
	    done

	    #Enable mem_latency governor for LLCC and DDR scaling
	    for memlat in $device/*cpu*-lat/devfreq/*cpu*-lat
	    do
            echo "mem_latency" > $memlat/governor
            echo 10 > $memlat/polling_interval
            echo 400 > $memlat/mem_latency/ratio_ceil
	    done

	    #Enable compute governor for gold latfloor
	    for latfloor in $device/*cpu-ddr-latfloor*/devfreq/*cpu-ddr-latfloor*
	    do
            echo "compute" > $latfloor/governor
            echo 10 > $latfloor/polling_interval
	    done

	    #Gold L3 ratio ceil
	    for l3gold in $device/*qcom,devfreq-l3/*cpu4-cpu-l3-lat/devfreq/*cpu4-cpu-l3-lat
	    do
		    echo 4000 > $l3gold/mem_latency/ratio_ceil
	    done

	    #Prime L3 ratio ceil
	    for l3prime in $device/*qcom,devfreq-l3/*cpu7-cpu-l3-lat/devfreq/*cpu7-cpu-l3-lat
	    do
		echo 20000 > $l3prime/mem_latency/ratio_ceil
	    done

	    #Enable mem_latency governor for qoslat
	    for qoslat in $device/*qoslat/devfreq/*qoslat
	    do
            echo "mem_latency" > $qoslat/governor
            echo 10 > $qoslat/polling_interval
            echo 50 > $qoslat/mem_latency/ratio_ceil
	    done
	done
    echo N > /sys/module/lpm_levels/parameters/sleep_disabled
    ;;
esac
