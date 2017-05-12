#!/system/bin/sh

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

echo 0 > /sys/module/lpm_levels/system/a53/cpu0/retention/idle_enabled
echo 0 > /sys/module/lpm_levels/system/a53/cpu1/retention/idle_enabled
echo 0 > /sys/module/lpm_levels/system/a53/cpu2/retention/idle_enabled
echo 0 > /sys/module/lpm_levels/system/a53/cpu3/retention/idle_enabled

echo 0 > /sys/module/lpm_levels/system/a53/a53-l2-retention/idle_enabled

# Configure governor settings for little cluster
echo "interactive" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_sched_load
echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_migration_notif
echo 30000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay
echo 87 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
echo 20000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
echo 762000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
echo "85 384000:96 460800:95 600000:96 672000:82 787200:70 864000:75 960000:80 1248000:85 1440000:90 1636000:99" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
echo 30000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/boostpulse_duration
echo 384000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq

# input boost configuration
echo 0 > /sys/module/cpu_boost/parameters/input_boost_freq
echo 0 > /sys/module/cpu_boost/parameters/input_boost_ms

# core_ctl module
#insmod /system/lib/modules/core_ctl.ko

# Setting b.L scheduler parameters
echo 1 > /proc/sys/kernel/sched_migration_fixup
echo 15 > /proc/sys/kernel/sched_small_task
echo 20 > /proc/sys/kernel/sched_mostly_idle_load
echo 3 > /proc/sys/kernel/sched_mostly_idle_nr_run
echo 85 > /proc/sys/kernel/sched_upmigrate
echo 70 > /proc/sys/kernel/sched_downmigrate
echo 7500000 > /proc/sys/kernel/sched_cpu_high_irqload
echo 60 > /proc/sys/kernel/sched_heavy_task
echo 65 > /proc/sys/kernel/sched_init_task_load
echo 200000000 > /proc/sys/kernel/sched_min_runtime
echo 400000 > /proc/sys/kernel/sched_freq_inc_notify
echo 400000 > /proc/sys/kernel/sched_freq_dec_notify

#enable rps static configuration
echo 8 > /sys/class/net/rmnet_ipa0/queues/rx-0/rps_cpus
for devfreq_gov in /sys/class/devfreq/qcom,cpubw*/governor
do
    echo "bw_hwmon" > $devfreq_gov
done
for devfreq_gov in /sys/class/devfreq/qcom,mincpubw*/governor
do
    echo "cpufreq" > $devfreq_gov
done

echo 0 > /proc/sys/kernel/sched_boost

echo "0-5" > /dev/cpuset/foreground/cpus
echo "4-5" > /dev/cpuset/foreground/boost/cpus
echo "0-3" > /dev/cpuset/background/cpus
echo "0-3" > /dev/cpuset/system-background/cpus

echo 9 > /proc/sys/kernel/sched_upmigrate_min_nice

# perfd
ext=$(getprop "ro.vendor.extension_library")
if [ "$ext" = "libqti-perfd-client.so" ]; then
	rm /data/system/perfd/default_values
	setprop ro.min_freq_0 384000
	start perfd
fi

chown -h system -R /sys/devices/system/cpu/
chown -h system -R /sys/module/msm_thermal/
chown -h system -R /sys/module/msm_performance/
chown -h system -R /sys/module/cpu_boost/
chown -h system -R /sys/devices/soc.0/qcom,bcl.*/
chown -h system -R /sys/class/devfreq/qcom,cpubw*/
chown -h system -R /sys/class/devfreq/qcom,mincpubw*/
chown -h system -R /sys/class/kgsl/kgsl-3d0/
chown -h system -R /sys/class/kgsl/kgsl-3d0/devfreq/

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

# Let kernel know our image version/variant/crm_version
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

# 禁用Big核心
echo 48 > /sys/module/msm_thermal/core_control/cpus_offlined

# GPU
echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
echo 367000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
echo 180000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
echo 5 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
echo 3 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
echo 5 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

# IO
echo 256 > /sys/block/mmcblk0/queue/read_ahead_kb
echo noop > /sys/block/mmcblk0/queue/scheduler

#ZRAM
MemTotalStr=`cat /proc/meminfo | grep MemTotal`
MemTotal=${MemTotalStr:16:8}
ZRAM_THRESHOLD=2097153
IsLowMemory=0
((IsLowMemory=MemTotal<ZRAM_THRESHOLD?1:0))
if [ "$IsLowMemory" = "1" ]; then
    setprop ro.config.zram true
     swapoff /dev/block/zram0
     echo 1 > /sys/block/zram0/reset
     echo 1536000000 > /sys/block/zram0/disksize
     mkswap /dev/block/zram0 &> /dev/null
     swapon /dev/block/zram0 &> /dev/null
     echo 100 > /proc/sys/vm/swappiness
fi


# post init done
setprop ts.post_init_done 1

profile=`getprop persist.ts.profile`
if [ "$profile" = "" ]; then
	# balanced by default
	profile=1
fi

# Call ts_power.sh, if found
if [ -f /data/ts_power.sh ]; then
	logi "Call /data/ts_power.sh set_profile $profile"
	sh /data/ts_power.sh set_profile $profile
elif [ -f /system/etc/ts_power.sh ]; then
	logi "Call /system/etc/ts_power.sh set_profile $profile"
	sh /system/etc/ts_power.sh set_profile $profile
fi