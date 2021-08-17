#!/system/bin/sh

# CPU 0
# 300000 576000 614400 864000 1075200 1363200 1516800 1651200 1804800

# CPU 6
# 652800 940800 1152000 1478400 1728000 1900800 2092800 2208000

# CPU 7
# 806400 1094400 1401600 1766400 1996800 2188800 2304000 2400000

# GPU
# 625000000 500000000 400000000 275000000

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

governor0=`cat /sys/devices/system/cpu/cpufreq/policy0/scaling_governor`
governor6=`cat /sys/devices/system/cpu/cpufreq/policy6/scaling_governor`
governor7=`cat /sys/devices/system/cpu/cpufreq/policy7/scaling_governor`

if [[ ! "$governor0" = "schedutil" ]]; then
  echo 'schedutil' > /sys/devices/system/cpu/cpufreq/policy0/scaling_governor
fi
if [[ ! "$governor6" = "schedutil" ]]; then
  echo 'schedutil' > /sys/devices/system/cpu/cpufreq/policy6/scaling_governor
fi
if [[ ! "$governor7" = "schedutil" ]]; then
  echo 'schedutil' > /sys/devices/system/cpu/cpufreq/policy7/scaling_governor
fi

governor_backup () {
  local governor_backup=/cache/governor_backup.prop
  local backup_state=`getprop vtools.dev_freq_backup`
  if [[ ! -f $governor_backup ]] || [[ "$backup_state" != "true" ]]; then
    echo '' > $governor_backup
    local dir=/sys/class/devfreq
    for file in `ls $dir | grep -v 'kgsl-3d0'`; do
      if [ -f $dir/$file/governor ]; then
        governor=`cat $dir/$file/governor`
        echo "$file#$governor" >> $governor_backup
      fi
    done
    setprop vtools.dev_freq_backup true
  fi
}

governor_performance () {
  governor_backup

  local dir=/sys/class/devfreq
  local governor_backup=/cache/governor_backup.prop
  local backup_state=`getprop vtools.dev_freq_backup`

  if [[ -f "$governor_backup" ]] && [[ "$backup_state" == "true" ]]; then
    for file in `ls $dir | grep -v 'kgsl-3d0'`; do
      if [ -f $dir/$file/governor ]; then
        # echo $dir/$file/governor
        echo performance > $dir/$file/governor
      fi
    done
  fi
}

governor_restore () {
  local governor_backup=/cache/governor_backup.prop
  local backup_state=`getprop vtools.dev_freq_backup`

  if [[ -f "$governor_backup" ]] && [[ "$backup_state" == "true" ]]; then
    local dir=/sys/class/devfreq
    while read line; do
      if [[ "$line" != "" ]]; then
        echo ${line#*#} > $dir/${line%#*}/governor
      fi
    done < $governor_backup
  fi
}

if [[ "$action" == "fast" ]]; then
  governor_performance
else
  governor_restore
fi

set_value()
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

# GPU频率表
gpu_freqs=`cat /sys/class/kgsl/kgsl-3d0/devfreq/available_frequencies`
# GPU最大频率
gpu_max_freq='625000000'
# GPU最小频率
gpu_min_freq='275000000'
# GPU最小 power level
gpu_min_pl=3
# GPU最大 power level
gpu_max_pl=0
# GPU默认 power level
gpu_default_pl=`cat /sys/class/kgsl/kgsl-3d0/default_pwrlevel`
# GPU型号
gpu_model=`cat /sys/class/kgsl/kgsl-3d0/gpu_model`
# GPU调度器
gpu_governor=`cat /sys/class/kgsl/kgsl-3d0/devfreq/governor`

# MaxFrequency、MinFrequency
for freq in $gpu_freqs; do
    if [[ $freq -gt $gpu_max_freq ]]; then
        gpu_max_freq=$freq
    fi;
    if [[ $freq -lt $gpu_min_freq ]]; then
        gpu_min_freq=$freq
    fi;
done

# Power Levels
if [[ -f /sys/class/kgsl/kgsl-3d0/num_pwrlevels ]];then
    gpu_min_pl=`cat /sys/class/kgsl/kgsl-3d0/num_pwrlevels`
    gpu_min_pl=`expr $gpu_min_pl - 1`
fi;
if [[ "$gpu_min_pl" -lt 0 ]];then
    gpu_min_pl=0
fi;

if [[ ! "$gpu_governor" = "msm-adreno-tz" ]]; then
  echo 'msm-adreno-tz' > /sys/class/kgsl/kgsl-3d0/devfreq/governor
fi

echo $gpu_max_freq > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
echo $gpu_min_freq > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
echo $gpu_max_pl > /sys/class/kgsl/kgsl-3d0/max_pwrlevel

set_cpu_freq()
{
  echo $1 $2 $3 $4
  echo "0:$2 1:$2 2:$2 3:$2 4:$2 5:$2 6:$4 7:$6" > /sys/module/msm_performance/parameters/cpu_max_freq
  echo $1 > /sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq
  echo $2 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq
  echo $3 > /sys/devices/system/cpu/cpufreq/policy6/scaling_min_freq
  echo $4 > /sys/devices/system/cpu/cpufreq/policy6/scaling_max_freq
  echo $5 > /sys/devices/system/cpu/cpufreq/policy7/scaling_min_freq
  echo $6 > /sys/devices/system/cpu/cpufreq/policy7/scaling_max_freq
}

sched_config() {
    echo "$1" > /proc/sys/kernel/sched_downmigrate
    echo "$2" > /proc/sys/kernel/sched_upmigrate
    echo "$1" > /proc/sys/kernel/sched_downmigrate
    echo "$2" > /proc/sys/kernel/sched_upmigrate

    echo "$3" > /proc/sys/kernel/sched_group_downmigrate
    echo "$4" > /proc/sys/kernel/sched_group_upmigrate
    echo "$3" > /proc/sys/kernel/sched_group_downmigrate
    echo "$4" > /proc/sys/kernel/sched_group_upmigrate
}

sched_limit() {
    echo $1 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/down_rate_limit_us
    echo $2 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/up_rate_limit_us
    echo $3 > /sys/devices/system/cpu/cpufreq/policy6/schedutil/down_rate_limit_us
    echo $4 > /sys/devices/system/cpu/cpufreq/policy6/schedutil/up_rate_limit_us
    echo $5 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/down_rate_limit_us
    echo $6 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/up_rate_limit_us
}

if [[ "$action" = "powersave" ]]; then
  set_cpu_freq 300000 1804800 652800 1900800 806400 2188800

  echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
  echo 0 > /proc/sys/kernel/sched_boost

  echo 1651200 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
  echo 1152000 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
  echo 1401600 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq

  echo 0-2 > /dev/cpuset/background/cpus
  echo 0-3 > /dev/cpuset/system-background/cpus

  sched_config "78 78" "96 96" "300" "400"

  sched_limit 0 500 0 1000 0 1000
  echo 0-3 > /dev/cpuset/foreground/cpus

  exit 0
fi

if [[ "$action" = "balance" ]]; then
  set_cpu_freq 300000 1804800 652800 1900800 806400 2188800

  echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
  echo 0 > /proc/sys/kernel/sched_boost

  echo 1651200 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
  echo 1152000 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
  echo 1401600 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq

  echo 0-2 > /dev/cpuset/background/cpus
  echo 0-3 > /dev/cpuset/system-background/cpus

  sched_config "68 68" "78 78" "300" "400"

  sched_limit 0 0 0 500 0 500
  echo 0-5 > /dev/cpuset/foreground/cpus

  exit 0
fi

if [[ "$action" = "performance" ]]; then
  set_cpu_freq 300000 1804800 652800 2208000 806400 2400000

  echo `expr $gpu_min_pl - 1` > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
  echo 0 > /proc/sys/kernel/sched_boost

  echo 1651200 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
  echo 1478400 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
  echo 1996800 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq

  echo 0-2 > /dev/cpuset/background/cpus
  echo 0-3 > /dev/cpuset/system-background/cpus

  sched_config "65 65" "75 75" "300" "400"

  sched_limit 0 0 0 0 0 0
  echo 0-7 > /dev/cpuset/foreground/cpus

  exit 0
fi

if [[ "$action" = "fast" ]]; then
  set_cpu_freq 300000 1804800 1152000 2208000 1401600 2400000

  echo `expr $gpu_min_pl - 2` > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
  echo 0 > /proc/sys/kernel/sched_boost

  echo 1651200 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
  echo 1478400 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
  echo 2188800 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq

  echo 0-2 > /dev/cpuset/background/cpus
  echo 0-3 > /dev/cpuset/system-background/cpus

  sched_config "55 55" "68 68" "300" "400"

  sched_limit 5000 0 2000 0 2000 0
  echo 0-7 > /dev/cpuset/foreground/cpus

  exit 0
fi
