#!/system/bin/sh

action=$1

init () {
  local dir=$(cd $(dirname $0); pwd)
  if [[ -f "$dir/powercfg-base.sh" ]]; then
    sh "$dir/powercfg-base.sh"
  elif [[ -f '/data/powercfg-base.sh' ]]; then
    sh /data/powercfg-base.sh
  fi
}
if [[ "$action" == "init" ]]; then
  init
  exit 0
fi

if [ ! `cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor` = "interactive" ]; then
  sh /system/etc/init.qcom.post_boot.sh
fi
if [ ! `cat /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor` = "interactive" ]; then
  sh /system/etc/init.qcom.post_boot.sh
fi

target=`getprop ro.board.platform`
if [ -f /sys/devices/soc0/soc_id ]; then
    soc_id=`cat /sys/devices/soc0/soc_id`
else
    soc_id=`cat /sys/devices/system/soc/soc0/id`
fi

# disable thermal & BCL core_control to update interactive gov settings
function disable_thermal_core_control() {
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
}

# re-enable thermal & BCL core_control now
function enable_thermal_core_control() {
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
}

# Bring up all cores online
function online_cpus() {
    # Bring up all cores online
    echo 1 > /sys/devices/system/cpu/cpu1/online
    echo 1 > /sys/devices/system/cpu/cpu2/online
    echo 1 > /sys/devices/system/cpu/cpu3/online
    echo 1 > /sys/devices/system/cpu/cpu4/online
    echo 1 > /sys/devices/system/cpu/cpu5/online
    echo 1 > /sys/devices/system/cpu/cpu6/online
    echo 1 > /sys/devices/system/cpu/cpu7/online
}

function enbale_sched_guided_freq_control() {
    # Enable sched guided freq control
    echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_sched_load
    echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/use_migration_notif
    echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/use_sched_load
    echo 1 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/use_migration_notif
    echo 50000 > /proc/sys/kernel/sched_freq_inc_notify
    echo 50000 > /proc/sys/kernel/sched_freq_dec_notify
}

# HMP Task packing settings for 8976
function set_sched_mostly_idle_load(){
    echo $0 > /proc/sys/kernel/sched_small_task
    echo $2 > /sys/devices/system/cpu/cpu0/sched_mostly_idle_load
    echo $2 > /sys/devices/system/cpu/cpu1/sched_mostly_idle_load
    echo $2 > /sys/devices/system/cpu/cpu2/sched_mostly_idle_load
    echo $2 > /sys/devices/system/cpu/cpu3/sched_mostly_idle_load
    echo $2 > /sys/devices/system/cpu/cpu4/sched_mostly_idle_load
    echo $2 > /sys/devices/system/cpu/cpu5/sched_mostly_idle_load
    echo $2 > /sys/devices/system/cpu/cpu6/sched_mostly_idle_load
    echo $2 > /sys/devices/system/cpu/cpu7/sched_mostly_idle_load
}

#/sys/module/msm_performance/parameters/cpu_max_freq
cpu_max_freq="0:3072000 1:3072000 2:3072000 3:3072000 4:3072000 5:3072000 6:3072000 7:3072000"

#/sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
hispeed_freq=1280000
#/sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
target_loads=87
#/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
scaling_min_freq=5000
#/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
scaling_max_freq=3072000


#/sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
hispeed_freq_4=1280000
#/sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
target_loads_4=87
#/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
scaling_min_freq_4=5000
#/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
scaling_max_freq_4=3072000

#/sys/devices/system/cpu/cpu0/core_ctl/min_cpus
min_cpus=2
#/sys/devices/system/cpu/cpu0/core_ctl/max_cpus
max_cpus=4
#/sys/devices/system/cpu/cpu0/core_ctl/busy_up_thres
busy_up_thres=68
#/sys/devices/system/cpu/cpu0/core_ctl/busy_down_thres
busy_down_thres=40
#/proc/sys/kernel/sched_upmigrate
sched_upmigrate=93
#/proc/sys/kernel/sched_downmigrate
sched_downmigrate=83

if [ "$action" = "powersave" ]; then
    hispeed_freq=1113600
    target_loads="75 960000:85 1113600:90 1344000:80"
    scaling_min_freq=5000
    scaling_max_freq=1280000
    hispeed_freq_4=800000
    target_loads_4="78 800000:90"
    scaling_min_freq_4=5000
    scaling_max_freq_4=1280000
    min_cpus=0
    max_cpus=4
    busy_up_thres=90
    busy_down_thres=65
    sched_upmigrate=89
    sched_downmigrate=70

elif [ "$action" = "balance" ]; then
    hispeed_freq=960000
    target_loads="75 960000:85 1113600:90 1344000:80"
    scaling_min_freq=5000
    scaling_max_freq=1500000
    hispeed_freq_4=800000
    target_loads_4="78 800000:90"
    scaling_min_freq_4=5000
    scaling_max_freq_4=1500000
    min_cpus=0
    max_cpus=4
    busy_up_thres=80
    busy_down_thres=50
    sched_upmigrate=89
    sched_downmigrate=70

elif [ "$action" = "performance" ]; then
    hispeed_freq=960000
    target_loads="70 960000:85 1113600:90 1344000:80"
    scaling_min_freq=5000
    scaling_max_freq=1800000
    hispeed_freq_4=800000
    target_loads_4="78 800000:85"
    scaling_min_freq_4=5000
    scaling_max_freq_4=1800000
    min_cpus=2
    max_cpus=4
    busy_up_thres=75
    busy_down_thres=40
    sched_upmigrate=89
    sched_downmigrate=70

elif [ "$action" = "fast" ]; then
    hispeed_freq=1113600
    target_loads="70 960000:78 1113600:87 1344000:80"
    scaling_min_freq=960000
    scaling_max_freq=3072000
    hispeed_freq_4=800000
    target_loads_4="70 800000:82"
    scaling_min_freq_4=5000
    scaling_max_freq_4=3072000
    min_cpus=4
    max_cpus=4
    busy_up_thres=70
    busy_down_thres=30
    sched_upmigrate=72
    sched_downmigrate=43
else
    exit 0
fi

function enable_core_control(){
    # Enable core control
    echo $min_cpus > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
    echo $max_cpus > /sys/devices/system/cpu/cpu4/core_ctl/max_cpus
    echo $busy_up_thres > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
    echo $busy_down_thres > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres
    echo 100 > /sys/devices/system/cpu/cpu4/core_ctl/offline_delay_ms
}

function config_8952(){
    # Apply Scheduler and Governor settings for 8952
    disable_thermal_core_control
    online_cpus
    echo $cpu_max_freq > /sys/module/msm_performance/parameters/cpu_max_freq

    # enable governor for perf cluster
    echo 1 > /sys/devices/system/cpu/cpu0/online
    echo "19000 1113600:39000" > /sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay
    echo 85 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
    echo 20000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
    echo $hispeed_freq > /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
    echo $target_loads > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
    echo 40000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
    echo 40000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/sampling_down_factor
    echo $scaling_max_freq > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
    echo $scaling_max_freq > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq

    # enable governor for power cluster
    echo 1 > /sys/devices/system/cpu/cpu4/online
    echo 39000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/above_hispeed_delay
    echo 90 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/go_hispeed_load
    echo 20000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate
    echo $hispeed_freq_4 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
    echo $target_loads_4 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/target_loads
    echo 40000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/min_sample_time
    echo 40000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/sampling_down_factor
    echo $scaling_min_freq_4 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
    echo $scaling_max_freq_4 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq

    enable_thermal_core_control
    online_cpus

    # Keeping Low power modes disabled
    echo 1 > /sys/module/lpm_levels/parameters/sleep_disabled

    # HMP scheduler (big.Little cluster related) settings
    echo $sched_upmigrate > /proc/sys/kernel/sched_upmigrate
    echo $sched_downmigrate > /proc/sys/kernel/sched_downmigrate

    enbale_sched_guided_freq_control

    # Enable core control
    echo $min_cpus > /sys/devices/system/cpu/cpu0/core_ctl/min_cpus
    echo $max_cpus > /sys/devices/system/cpu/cpu0/core_ctl/max_cpus
    echo $busy_up_thres > /sys/devices/system/cpu/cpu0/core_ctl/busy_up_thres
    echo $busy_down_thres > /sys/devices/system/cpu/cpu0/core_ctl/busy_down_thres
    echo 100 > /sys/devices/system/cpu/cpu0/core_ctl/offline_delay_ms

    # Enable dynamic clock gating
    echo 1 > /sys/module/lpm_levels/lpm_workarounds/dynamic_clock_gating
    # Enable timer migration to little cluster
    echo 1 > /proc/sys/kernel/power_aware_timer_migration
}

function config_8976() {
    panel=`cat /sys/class/graphics/fb0/modes`
    if [ "${panel:5:1}" == "x" ]; then
        panel=${panel:2:3}
    else
        panel=${panel:2:4}
    fi

    # Apply Scheduler and Governor settings for 8976
    # SoC IDs are 266, 274, 277, 278

    # HMP scheduler (big.Little cluster related) settings
    echo $sched_upmigrate > /proc/sys/kernel/sched_upmigrate
    echo $sched_downmigrate > /proc/sys/kernel/sched_downmigrate

    disable_thermal_core_control
    online_cpus
    echo $cpu_max_freq > /sys/module/msm_performance/parameters/cpu_max_freq

    # enable governor for power cluster
    echo 1 > /sys/devices/system/cpu/cpu0/online
    echo $target_loads > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
    echo 90 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load
    echo 20000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
    echo $hispeed_freq > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy
    echo 40000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
    echo $scaling_min_freq > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    echo $scaling_max_freq > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq

    # enable governor for perf cluster
    echo 1 > /sys/devices/system/cpu/cpu4/online
    echo 85 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/go_hispeed_load
    echo 20000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/timer_rate
    echo $hispeed_freq_4 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/io_is_busy
    echo 40000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/min_sample_time
    echo 40000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/sampling_down_factor
    echo $scaling_min_freq_4 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq
    echo $scaling_max_freq_4 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq

    if [ $panel -gt 1080 ]; then
        echo 19000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay
        echo 800000 > /sys/devices/system/cpu/cpu4/cpufreq/interactive/max_freq_hysteresis
        echo "19000 1382400:39000" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/above_hispeed_delay

        set_sched_mostly_idle_load 30 20
    else
        echo 39000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/above_hispeed_delay
        echo "19000 1190400:39000" > /sys/devices/system/cpu/cpu4/cpufreq/interactive/above_hispeed_delay

        set_sched_mostly_idle_load 20 30
    fi

    enable_thermal_core_control
    online_cpus

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

    enbale_sched_guided_freq_control
    enable_core_control

    # Enable timer migration to little cluster
    echo 1 > /proc/sys/kernel/power_aware_timer_migration
}

function config(){
    case "$soc_id" in
        "264" | "289")
            config_8952
        ;;
        *)
            config_8976
        ;;
    esac
}

config
