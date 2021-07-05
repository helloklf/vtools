target=`getprop ro.board.platform`

case "$target" in
  "lahaina")

    # Core control parameters for gold
    echo 2 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
    echo 60 > /sys/devices/system/cpu/cpu4/core_ctl/busy_up_thres
    echo 30 > /sys/devices/system/cpu/cpu4/core_ctl/busy_down_thres
    echo 100 > /sys/devices/system/cpu/cpu4/core_ctl/offline_delay_ms
    echo 3 > /sys/devices/system/cpu/cpu4/core_ctl/task_thres
    echo 1 1 1 > /sys/devices/system/cpu/cpu4/core_ctl/not_preferred

    # Core control parameters for gold+
    echo 0 > /sys/devices/system/cpu/cpu7/core_ctl/min_cpus
    echo 60 > /sys/devices/system/cpu/cpu7/core_ctl/busy_up_thres
    echo 30 > /sys/devices/system/cpu/cpu7/core_ctl/busy_down_thres
    echo 100 > /sys/devices/system/cpu/cpu7/core_ctl/offline_delay_ms
    echo 1 > /sys/devices/system/cpu/cpu7/core_ctl/task_thres
    echo 1 > /sys/devices/system/cpu/cpu4/core_ctl/not_preferred

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
    echo 0-7 > /dev/cpuset/foreground/cpus
    echo 0-7 > /dev/cpuset/top-app/cpus

    # Turn off scheduler boost at the end
    echo 0 > /proc/sys/kernel/sched_boost

    # Turn on scheduler boost for top app main
    echo 1 > /proc/sys/kernel/sched_boost_top_app

    # configure governor settings for silver cluster
    echo "schedutil" > /sys/devices/system/cpu/cpufreq/policy0/scaling_governor
    echo 0 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/down_rate_limit_us
    echo 0 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/up_rate_limit_us
    echo 300000 > /sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq
    echo 1 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/pl

    # configure input boost settings
    echo "0:1708800" > /sys/devices/system/cpu/cpu_boost/input_boost_freq
    echo 120 > /sys/devices/system/cpu/cpu_boost/input_boost_ms
    echo "0:0 1:0 2:0 3:0 4:2342400 5:0 6:0 7:2496000" > /sys/devices/system/cpu/cpu_boost/powerkey_input_boost_freq
    echo 400 > /sys/devices/system/cpu/cpu_boost/powerkey_input_boost_ms

    # configure governor settings for gold cluster
    echo "schedutil" > /sys/devices/system/cpu/cpufreq/policy4/scaling_governor
    echo 0 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/down_rate_limit_us
    echo 0 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/up_rate_limit_us
    echo 1555200 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
    echo 1 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/pl

    # configure governor settings for gold+ cluster
    echo "schedutil" > /sys/devices/system/cpu/cpufreq/policy7/scaling_governor
    echo 0 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/down_rate_limit_us
    echo 0 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/up_rate_limit_us
    echo 1670400 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq
    echo 1 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/pl

    echo N > /sys/module/lpm_levels/parameters/sleep_disabled
  ;;
esac

pgrep -f surfaceflinger | while read pid; do
  echo $pid > /dev/cpuset/top-app/tasks
  echo $pid > /dev/stune/top-app/tasks
done
pgrep -f system_server | while read pid; do
  echo $pid > /dev/cpuset/top-app/tasks
  echo $pid > /dev/stune/top-app/tasks
done
pgrep -f vendor.qti.hardware.display.composer-service | while read pid; do
  echo $pid > /dev/cpuset/top-app/tasks
  echo $pid > /dev/stune/top-app/tasks
done

cpuctl () {
 echo $2 > /dev/cpuctl/$1/cpu.uclamp.sched_boost_no_override
 echo $3 > /dev/cpuctl/$1/cpu.uclamp.latency_sensitive
 echo $4 > /dev/cpuctl/top-app/cpu.uclamp.min
 echo $5 > /dev/cpuctl/top-app/cpu.uclamp.max
}

cpuctl top-app 0 0 0 max
cpuctl foreground 0 0 0 '0.8'
cpuctl background 0 0 0 0

echo 0 > /dev/stune/nnapi-hal/schedtune.boost
echo 0 > /dev/stune/nnapi-hal/schedtune.prefer_idle

set_task_affinity `pgrep com.miui.home` 11111111
