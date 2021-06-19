#!/system/bin/sh

#! /vendor/bin/sh

target=`getprop ro.board.platform`

case "$target" in
  "kona")

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
    if [[ `cat /sys/devices/soc0/revision` == "2.0" ]]; then
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
