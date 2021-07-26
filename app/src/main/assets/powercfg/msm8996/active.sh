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

stop perfd

governor0=`cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor`
governor2=`cat /sys/devices/system/cpu/cpu2/cpufreq/scaling_governor`

if [ ! "$governor0" = "interactive" ] && [ ! "$governor0" = "schedutil" ]; then
  echo 'interactive' > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
  echo 'schedutil' > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
fi
if [ ! "$governor2" = "interactive" ] && [ ! "$governor2" = "schedutil" ]; then
  echo 'interactive' > /sys/devices/system/cpu/cpu2/cpufreq/scaling_governor
  echo 'schedutil' > /sys/devices/system/cpu/cpu2/cpufreq/scaling_governor
fi

function set_value()
{
  value=$1
  path=$2
  if [[ -f $path ]]; then
    current_value="$(cat $path)"
    if [[ ! "$current_value" = "$value" ]]; then
      chmod 0664 "$path"
      echo "$value" > "$path"
    fi;
  fi;
}

function lock_value()
{
  value=$1
  path=$2
  if [[ -f $path ]]; then
    current_value="$(cat $path)"
    if [[ ! "$current_value" = "$value" ]]; then
      chmod 0664 "$path"
      echo "$value" > "$path"
      chmod 0444 "$path"
    fi;
  fi;
}

lock_value 0 /sys/devices/system/cpu/cpu0/cpufreq/interactive/boost
lock_value 0 /sys/devices/system/cpu/cpu2/cpufreq/interactive/boost

function gpu_config()
{
  gpu_freqs=`cat /sys/class/kgsl/kgsl-3d0/devfreq/available_frequencies`
  max_freq='710000000'
  for freq in $gpu_freqs; do
    if [[ $freq -gt $max_freq ]]; then
      max_freq=$freq
    fi;
  done
  gpu_min_pl=6
  if [[ -f /sys/class/kgsl/kgsl-3d0/num_pwrlevels ]];then
    gpu_min_pl=`cat /sys/class/kgsl/kgsl-3d0/num_pwrlevels`
    gpu_min_pl=`expr $gpu_min_pl - 1`
  fi;

  if [[ "$gpu_min_pl" = "-1" ]];then
    $gpu_min_pl=1
  fi;

  echo "msm-adreno-tz" > /sys/class/kgsl/kgsl-3d0/devfreq/governor
  #echo 710000000 > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
  echo $max_freq > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
  #echo 257000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
  echo 100000000 > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
  echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
  echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
}

gpu_config

function set_cpu_freq()
{
    echo $1 $2 $3 $4
  #echo "0:$2 1:$2 2:$4 3:$4" > /sys/module/msm_performance/parameters/cpu_max_freq
  echo $1 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq
  echo $2 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq
  echo $3 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_min_freq
  echo $4 > /sys/devices/system/cpu/cpu2/cpufreq/scaling_max_freq
}

if [ "$action" = "powersave" ]; then
  set_cpu_freq 300000 850000 300000 960000

  echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
  echo 0 > /proc/sys/kernel/sched_boost

  echo 90 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
  set_value 480000 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
  echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
  echo 9000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
  echo 10000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
  echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy

  echo 99 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/target_loads
  set_value 480000 /sys/devices/system/cpu/cpu2/cpufreq/interactive/hispeed_freq
  echo 0 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/max_freq_hysteresis
  echo 19000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/min_sample_time
  echo 20000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/timer_rate
  echo 0 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/io_is_busy

  exit 0
fi

if [ "$action" = "balance" ]; then
  set_cpu_freq 300000 1824000 300000 1824000

  echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
  echo 0 > /proc/sys/kernel/sched_boost

  echo 88 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
  set_value 94000 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
  echo 0 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
  echo 9000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
  echo 10000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
  echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy

  echo "87 1500000:90 1800000:87" > /sys/devices/system/cpu/cpu2/cpufreq/interactive/target_loads
  set_value 96000 /sys/devices/system/cpu/cpu2/cpufreq/interactive/hispeed_freq
  echo 0 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/max_freq_hysteresis
  echo 19000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/min_sample_time
  echo 20000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/timer_rate
  echo 0 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/io_is_busy

  exit 0
fi

if [ "$action" = "performance" ]; then
  set_cpu_freq 300000 2500000 300000 2500000

  echo `expr $gpu_min_pl - 1` > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
  echo 1 > /proc/sys/kernel/sched_boost

  echo 86 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
  set_value 1150000 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
  echo 79000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
  echo 19000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
  echo 10000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
  echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy

  echo "80 1500000:87 1800000:95" > /sys/devices/system/cpu/cpu2/cpufreq/interactive/target_loads
  set_value 1248000 /sys/devices/system/cpu/cpu2/cpufreq/interactive/hispeed_freq
  echo 79000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/max_freq_hysteresis
  echo 23000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/min_sample_time
  echo 12000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/timer_rate
  echo 1 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/io_is_busy

  echo 1 > /sys/devices/system/cpu/cpu0/online
  echo 1 > /sys/devices/system/cpu/cpu1/online
  echo 1 > /sys/devices/system/cpu/cpu2/online
  echo 1 > /sys/devices/system/cpu/cpu3/online

  exit 0
fi

if [ "$action" = "fast" ]; then
  set_cpu_freq 300000 2500000 300000 2500000

  echo `expr $gpu_min_pl - 1` > /sys/class/kgsl/kgsl-3d0/default_pwrlevel
  echo 0 > /proc/sys/kernel/sched_boost

  echo 86 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/target_loads
  set_value 1150000 /sys/devices/system/cpu/cpu0/cpufreq/interactive/hispeed_freq
  echo 79000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/max_freq_hysteresis
  echo 23000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/min_sample_time
  echo 8000 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/timer_rate
  echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/io_is_busy

  echo "80 1500000:87 1800000:95" > /sys/devices/system/cpu/cpu2/cpufreq/interactive/target_loads
  set_value 1248000 /sys/devices/system/cpu/cpu2/cpufreq/interactive/hispeed_freq
  echo 79000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/max_freq_hysteresis
  echo 23000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/min_sample_time
  echo 8000 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/timer_rate
  echo 1 > /sys/devices/system/cpu/cpu2/cpufreq/interactive/io_is_busy

  exit 0
fi
