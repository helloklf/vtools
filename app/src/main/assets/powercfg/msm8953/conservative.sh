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
  echo 'interactive' > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
fi

echo 1-3 > /dev/cpuset/background/cpus
echo 1-7 > /dev/cpuset/system-background/cpus
echo 1-7 > /dev/cpuset/foreground/cpus
echo 1-7 > /dev/cpuset/foreground/boost/cpus
echo 1-7 > /dev/cpuset/top-app/cpus

echo 0 > /sys/devices/system/cpu/cpufreq/interactive/use_sched_load
echo 1 > /sys/devices/system/cpu/cpu0/online
echo 1 > /sys/devices/system/cpu/cpu1/online
echo 1 > /sys/devices/system/cpu/cpu2/online
echo 1 > /sys/devices/system/cpu/cpu3/online
echo 1 > /sys/devices/system/cpu/cpu4/online
echo 1 > /sys/devices/system/cpu/cpu5/online
echo 1 > /sys/devices/system/cpu/cpu6/online
echo 1 > /sys/devices/system/cpu/cpu7/online

echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
#echo 850000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
#echo 100000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
#echo 8 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
#echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
#echo 8 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

governor=`cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor`
if [ ! "$governor" = "interactive" ]; then
  echo 'interactive' > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
fi

#powersave 1.2Ghz
if [ "$action" = "powersave" ]; then
    echo 1 > /sys/devices/system/cpu/cpu0/online
  echo "0:1401600 1:1401600 2:1401600 3:1401600 4:1401600 5:1401600 6:1401600 7:1401600" > /sys/module/msm_performance/parameters/cpu_max_freq
    echo "19000 1036800:39000" > /sys/devices/system/cpu/cpufreq/interactive/above_hispeed_delay
    echo 85 > /sys/devices/system/cpu/cpufreq/interactive/go_hispeed_load
    echo 20000 > /sys/devices/system/cpu/cpufreq/interactive/timer_rate
    echo 1036800 > /sys/devices/system/cpu/cpufreq/interactive/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpufreq/interactive/io_is_busy
    echo "85 1036800:80" > /sys/devices/system/cpu/cpufreq/interactive/target_loads
    echo 39000 > /sys/devices/system/cpu/cpufreq/interactive/min_sample_time
    echo 40000 > /sys/devices/system/cpu/cpufreq/interactive/sampling_down_factor
    echo 5000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    echo 1401600 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq

    echo 1 > /sys/module/msm_thermal/core_control/enabled
    echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
    echo Y > /sys/module/msm_thermal/parameters/enabled

    echo 216000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
    echo 100000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
    echo 8 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
    echo 6 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
    echo 8 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

  exit 0
fi

echo 0 > /sys/module/msm_thermal/core_control/enabled
echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled
echo N > /sys/module/msm_thermal/parameters/enabled

if [ "$action" = "balance" ]; then
  echo "0:1560000 1:1560000 2:1560000 3:1560000 4:1560000 5:1560000 6:1560000 7:1560000" > /sys/module/msm_performance/parameters/cpu_max_freq
    echo "19000 1401600:39000" > /sys/devices/system/cpu/cpufreq/interactive/above_hispeed_delay
    echo 82 > /sys/devices/system/cpu/cpufreq/interactive/go_hispeed_load
    echo 20000 > /sys/devices/system/cpu/cpufreq/interactive/timer_rate
    echo 1036800 > /sys/devices/system/cpu/cpufreq/interactive/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpufreq/interactive/io_is_busy
    echo "67 1401600:80" > /sys/devices/system/cpu/cpufreq/interactive/target_loads
    echo 39000 > /sys/devices/system/cpu/cpufreq/interactive/min_sample_time
    echo 40000 > /sys/devices/system/cpu/cpufreq/interactive/sampling_down_factor
    echo 5000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    echo 1560000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq

    echo 650000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
    echo 100000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
    echo 8 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
    echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
    echo 6 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

  exit 0
fi

if [ "$action" = "performance" ]; then
    echo 1 > /sys/devices/system/cpu/cpu0/online
  echo "0:2016000 1:2016000 2:2016000 3:2016000 4:2016000 5:2016000 6:2016000 7:2016000" > /sys/module/msm_performance/parameters/cpu_max_freq
    echo "19000 1401600:39000" > /sys/devices/system/cpu/cpufreq/interactive/above_hispeed_delay
    echo 82 > /sys/devices/system/cpu/cpufreq/interactive/go_hispeed_load
    echo 20000 > /sys/devices/system/cpu/cpufreq/interactive/timer_rate
    echo 1560000 > /sys/devices/system/cpu/cpufreq/interactive/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpufreq/interactive/io_is_busy
    echo "67 1560000:78 1840000:87" > /sys/devices/system/cpu/cpufreq/interactive/target_loads
    echo 39000 > /sys/devices/system/cpu/cpufreq/interactive/min_sample_time
    echo 40000 > /sys/devices/system/cpu/cpufreq/interactive/sampling_down_factor
    echo 152800 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    echo 2016000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq

    echo 750000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
    echo 100000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
    echo 6 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
    echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
    echo 5 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

  exit 0
fi

if [ "$action" = "fast" ]; then
    echo 1 > /sys/devices/system/cpu/cpu0/online
  echo "0:2560000 1:2560000 2:2560000 3:2560000 4:2560000 5:2560000 6:2560000 7:2560000" > /sys/module/msm_performance/parameters/cpu_max_freq
    echo "19000 1401600:39000" > /sys/devices/system/cpu/cpufreq/interactive/above_hispeed_delay
    echo 85 > /sys/devices/system/cpu/cpufreq/interactive/go_hispeed_load
    echo 20000 > /sys/devices/system/cpu/cpufreq/interactive/timer_rate
    echo 1840000 > /sys/devices/system/cpu/cpufreq/interactive/hispeed_freq
    echo 0 > /sys/devices/system/cpu/cpufreq/interactive/io_is_busy
    echo "60 1401600:73" > /sys/devices/system/cpu/cpufreq/interactive/target_loads
    echo 39000 > /sys/devices/system/cpu/cpufreq/interactive/min_sample_time
    echo 40000 > /sys/devices/system/cpu/cpufreq/interactive/sampling_down_factor
    echo 1401600 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
    echo 2560000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq

    echo 850000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
    echo 310000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
    echo 5 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
    echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
    echo 3 > /sys/class/kgsl/kgsl-3d0/default_pwrlevel

  exit 0
fi
