# CPU 0
# 300000 403200 518400 614400 691200 787200 883200 979200 1075200 1171200 1248000 1344000 1420800 1516800 1612800 1708800 1804800

# CPU 4
# 710400 825600 940800 1056000 1171200 1286400 1382400 1478400 1574400 1670400 1766400 1862400 1958400 2054400 2150400 2246400 2342400 2419200

# CPU 7
#  844800 960000 1075200 1190400 1305600 1401600 1516800 1632000 1747200 1862400 1977600 2073600 2169600 2265600 2361600 2457600 2553600 2649600 2745600 2841600

# GPU
# 587000000 525000000 490000000 441600000 400000000 305000000

action=$1

init () {
  local dir=$(cd $(dirname $0); pwd)
  if [[ -f "$dir/powercfg-base.sh" ]]; then
    sh "$dir/powercfg-base.sh"
  elif [[ -f '/data/powercfg-base.sh' ]]; then
    sh /data/powercfg-base.sh
  fi
}
if [[ "$action" = "init" ]]; then
  init
  exit 0
fi

governor0=`cat /sys/devices/system/cpu/cpufreq/policy0/scaling_governor`
governor4=`cat /sys/devices/system/cpu/cpufreq/policy4/scaling_governor`
governor7=`cat /sys/devices/system/cpu/cpufreq/policy7/scaling_governor`

if [[ ! "$governor0" = "schedutil" ]]; then
  echo 'schedutil' > /sys/devices/system/cpu/cpufreq/policy0/scaling_governor
fi
if [[ ! "$governor4" = "schedutil" ]]; then
  echo 'schedutil' > /sys/devices/system/cpu/cpufreq/policy4/scaling_governor
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
    for file in `ls $dir`; do
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
    for file in `ls $dir`; do
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
gpu_max_freq='587000000'
# GPU最小频率
gpu_min_freq='305000000'
# GPU最小 power level
gpu_min_pl=5
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

# echo $gpu_max_freq > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
echo $gpu_min_freq > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
echo $gpu_max_pl > /sys/class/kgsl/kgsl-3d0/max_pwrlevel

set_gpu_min_freq() {
  index=$1

  # GPU频率表
  gpu_freqs=`cat /sys/class/kgsl/kgsl-3d0/devfreq/available_frequencies`

  target_freq=$(echo $gpu_freqs | awk "{print \$${index}}")
  if [[ "$target_freq" != "" ]]; then
    echo $target_freq > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
  fi

  # gpu_max_freq=`cat /sys/class/kgsl/kgsl-3d0/devfreq/max_freq`
  # gpu_min_freq=`cat /sys/class/kgsl/kgsl-3d0/devfreq/min_freq`
  # echo "Frequency: ${gpu_min_freq} ~ ${gpu_max_freq}"
}


set_input_boost_freq() {
    local c0="$1"
    local c1="$2"
    local c2="$3"
    local ms="$4"
    echo "0:$c0 1:$c0 2:$c0 3:$c0 4:$c1 5:$c1 6:$c1 7:$c2" > /sys/module/cpu_boost/parameters/input_boost_freq
  echo $ms > /sys/module/cpu_boost/parameters/input_boost_ms
}

set_cpu_freq()
{
  echo $1 $2 $3 $4
  echo "0:$2 1:$2 2:$2 3:$2 4:$4 5:$4 6:$4 7:$6" > /sys/module/msm_performance/parameters/cpu_max_freq
  echo $1 > /sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq
  echo $2 > /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq
  echo $3 > /sys/devices/system/cpu/cpufreq/policy4/scaling_min_freq
  echo $4 > /sys/devices/system/cpu/cpufreq/policy4/scaling_max_freq
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
  echo $3 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/down_rate_limit_us
  echo $4 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/up_rate_limit_us
  echo $5 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/down_rate_limit_us
  echo $6 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/up_rate_limit_us
}

surfaceflinger_top_app()
{
  pgrep -f surfaceflinger | while read pid; do
    echo $pid > /dev/cpuset/top-app/tasks
    # echo $pid > /dev/stune/top-app/tasks
  done
  pgrep -f system_server | while read pid; do
    echo $pid > /dev/cpuset/top-app/tasks
    echo $pid > /dev/stune/top-app/tasks
  done
}
surfaceflinger_bg_app()
{
  pgrep -f surfaceflinger | while read pid; do
    echo $pid > /dev/cpuset/system-background/tasks
    # echo $pid > /dev/stune/foreground/tasks
  done
  pgrep -f system_server | while read pid; do
    echo $pid > /dev/cpuset/system-background/tasks
    echo $pid > /dev/stune/background/tasks
  done
}

if [[ "$action" = "powersave" ]]; then
  echo 1 > /sys/devices/system/cpu/cpu4/core_ctl/enable
  echo 1 > /sys/devices/system/cpu/cpu7/core_ctl/enable
  echo 0 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus

  set_cpu_freq 300000 1804800 710400 1574400 844800 1747200
  set_input_boost_freq 1248000 0 0 40

  echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
  echo 0 > /proc/sys/kernel/sched_boost

  echo 1612800 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
  echo 1056000 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
  echo 1305600 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq

  echo 0-2 > /dev/cpuset/background/cpus
  echo 0-3 > /dev/cpuset/system-background/cpus

  sched_config "85 85" "96 96" "150" "400"

  sched_limit 0 0 0 2000 0 1000

  surfaceflinger_bg_app
  echo 0-3 > /dev/cpuset/foreground/cpus

  exit 0
fi

if [[ "$action" = "balance" ]]; then
  echo 1 > /sys/devices/system/cpu/cpu4/core_ctl/enable
  echo 1 > /sys/devices/system/cpu/cpu7/core_ctl/enable
  echo 0 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus

  set_cpu_freq 300000 1804800 710400 1862400 844800 2073600
  set_input_boost_freq 1478400 0 0 40

  echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
  echo 0 > /proc/sys/kernel/sched_boost

  echo 1612800 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
  echo 1056000 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
  echo 1305600 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq

  echo 0-2 > /dev/cpuset/background/cpus
  echo 0-3 > /dev/cpuset/system-background/cpus

  sched_config "78 85" "89 96" "150" "400"

  sched_limit 0 0 0 500 0 500

  surfaceflinger_top_app
  echo 0-6 > /dev/cpuset/foreground/cpus

  exit 0
fi

if [[ "$action" = "performance" ]]; then
  echo 3 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
  echo 0 > /sys/devices/system/cpu/cpu4/core_ctl/enable
  echo 0 > /sys/devices/system/cpu/cpu7/core_ctl/enable

  set_cpu_freq 300000 1804800 710400 2419200 825600 2841600
  set_input_boost_freq 1420800 1286400 1305600 40

  echo `expr $gpu_min_pl - 1` > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
  echo 0 > /proc/sys/kernel/sched_boost

  echo 1612800 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
  echo 1766400 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
  echo 2073600 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq

  echo 0-1 > /dev/cpuset/background/cpus
  echo 0-3 > /dev/cpuset/system-background/cpus

  sched_config "65 78" "75 88" "200" "400"

  sched_limit 0 0 0 0 0 0

  surfaceflinger_top_app
  echo 0-2,4-7 > /dev/cpuset/foreground/cpus

  exit 0
fi

if [[ "$action" = "fast" ]]; then
  echo 3 > /sys/devices/system/cpu/cpu4/core_ctl/min_cpus
  echo 0 > /sys/devices/system/cpu/cpu4/core_ctl/enable
  echo 0 > /sys/devices/system/cpu/cpu7/core_ctl/enable

  set_cpu_freq 1248000 1804800 1478400 2600000 1516800 3200000
  set_input_boost_freq 1804800 1670400 1862400 80

  echo 1612800 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
  echo 1670400 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
  echo 1862400 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq

  echo `expr $gpu_min_pl - 2` > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
  echo 0 > /proc/sys/kernel/sched_boost

  echo 0 > /dev/cpuset/background/cpus
  echo 0-3 > /dev/cpuset/system-background/cpus

  sched_config "62 75" "70 80" "300" "400"

  sched_limit 5000 0 2000 0 2000 0

  surfaceflinger_top_app
  echo 0-2,4-7 > /dev/cpuset/foreground/cpus

  exit 0
fi
