# /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies
# 300000 403200 499200 576000 672000 768000 844800 940800 1036800 1113600 1209600 1305600 1382400 1478400 1555200 1632000 1708800 1785600

# /sys/devices/system/cpu/cpu4/cpufreq/scaling_available_frequencies
# 710400 825600 940800 1056000 1171200 1286400 1401600 1497600 1612800 1708800 1804800 1920000 2016000 2131200 2227200 2323200 2419200

# /sys/devices/system/cpu/cpu7/cpufreq/scaling_available_frequencies
# 825600 940800 1056000 1171200 1286400 1401600 1497600 1612800 1708800 1804800 1920000 2016000 2131200 2227200 2323200 2419200 2534400 2649600 2745600 2841600

# GPU
# 257000000 345000000 427000000 499200000 585000000 675000000 810000000


throttle() {
hint_group=""
if [[ "$top_app" != "" ]]; then
distinct_apps="
game=com.miHoYo.Yuanshen,com.miHoYo.ys.bilibili,com.miHoYo.ys.mi

mgame=com.bilibili.gcg2.bili

heavy=com.taobao.idlefish,com.taobao.taobao,com.miui.home,com.android.browser,com.baidu.tieba_mini,com.baidu.tieba,com.jingdong.app.mall

music=com.netease.cloudmusic,com.kugou.android,com.kugou.android.lite

video=com.ss.android.ugc.aweme,tv.danmaku.bili
"

  hint_group=$(echo -e "$distinct_apps" | grep "$top_app" | cut -f1 -d "=")
  current_hint=$(getprop vtools.powercfg_hint)
  hint_mode="${action}:${hint_group}"

  if [[ "$hint_mode" == "$current_hint" ]]; then
    echo "$top_app [$hint_mode > $current_hint] skip!" >> /cache/powercfg_skip.log
    exit 0
  fi

else
  hint_mode="${action}"
fi

setprop vtools.powercfg_hint "$hint_mode"
}

# throttle


# GPU频率表
gpu_freqs=`cat /sys/class/kgsl/kgsl-3d0/devfreq/available_frequencies`
# GPU最大频率
gpu_max_freq='585000000'
# GPU最小频率
gpu_min_freq='257000000'
# GPU最小 power level
gpu_min_pl=5
# GPU最大 power level
gpu_max_pl=0

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



conservative_mode() {
  local policy=/sys/devices/system/cpu/cpufreq/policy
  # local down="$1"
  # local up="$2"
  #
  # if [[ "$down" == "" ]]; then
  #   local down="20"
  # fi
  # if [[ "$up" == "" ]]; then
  #   local up="60"
  # fi

  for cluster in 0 4 7; do
    echo $cluster
    echo 'conservative' > ${policy}${cluster}/scaling_governor
    # echo $down > ${policy}${cluster}/conservative/down_threshold
    # echo $up > ${policy}${cluster}/conservative/up_threshold
    echo 0 > ${policy}${cluster}/conservative/ignore_nice_load
    echo 1000 > ${policy}${cluster}/conservative/sampling_rate # 1000us = 1ms
    echo 2 > ${policy}${cluster}/conservative/freq_step
  done

  echo $1 > ${policy}0/conservative/down_threshold
  echo $2 > ${policy}0/conservative/up_threshold
  echo $1 > ${policy}0/conservative/down_threshold
  echo $2 > ${policy}0/conservative/up_threshold

  echo $3 > ${policy}4/conservative/down_threshold
  echo $4 > ${policy}4/conservative/up_threshold
  echo $3 > ${policy}4/conservative/down_threshold
  echo $4 > ${policy}4/conservative/up_threshold

  echo $5 > ${policy}7/conservative/down_threshold
  echo $6 > ${policy}7/conservative/up_threshold
  echo $5 > ${policy}7/conservative/down_threshold
  echo $6 > ${policy}7/conservative/up_threshold
}

core_online=(1 1 1 1 1 1 1 1)
set_core_online() {
  for index in 0 1 2 3 4 5 6 7; do
    core_online[$index]=`cat /sys/devices/system/cpu/cpu$index/online`
    echo 1 > /sys/devices/system/cpu/cpu$index/online
  done
}
restore_core_online() {
  for i in "${!core_online[@]}"; do
     echo ${core_online[i]} > /sys/devices/system/cpu/cpu$i/online
  done
}


reset_basic_governor() {
  set_core_online

  # CPU
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

  # GPU
  gpu_governor=`cat /sys/class/kgsl/kgsl-3d0/devfreq/governor`
  if [[ ! "$gpu_governor" = "msm-adreno-tz" ]]; then
    echo 'msm-adreno-tz' > /sys/class/kgsl/kgsl-3d0/devfreq/governor
  fi
  echo $gpu_max_freq > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
  echo $gpu_min_freq > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq
  echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
  echo $gpu_max_pl > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
}

devfreq_backup () {
  local devfreq_backup=/cache/devfreq_backup.prop
  local backup_state=`getprop vtools.dev_freq_backup`
  if [[ ! -f $devfreq_backup ]] || [[ "$backup_state" != "true" ]]; then
    echo '' > $devfreq_backup
    local dir=/sys/class/devfreq
    for file in `ls $dir | grep -v 'kgsl-3d0'`; do
      if [ -f $dir/$file/governor ]; then
        governor=`cat $dir/$file/governor`
        echo "$file#$governor" >> $devfreq_backup
      fi
    done
    setprop vtools.dev_freq_backup true
  fi
}

devfreq_performance () {
  devfreq_backup

  local dir=/sys/class/devfreq
  local devfreq_backup=/cache/devfreq_backup.prop
  local backup_state=`getprop vtools.dev_freq_backup`

  if [[ -f "$devfreq_backup" ]] && [[ "$backup_state" == "true" ]]; then
    for file in `ls $dir | grep -v 'kgsl-3d0'`; do
      if [ -f $dir/$file/governor ]; then
        # echo $dir/$file/governor
        echo performance > $dir/$file/governor
      fi
    done
  fi
}

devfreq_restore () {
  local devfreq_backup=/cache/devfreq_backup.prop
  local backup_state=`getprop vtools.dev_freq_backup`

  if [[ -f "$devfreq_backup" ]] && [[ "$backup_state" == "true" ]]; then
    local dir=/sys/class/devfreq
    while read line; do
      if [[ "$line" != "" ]]; then
        echo ${line#*#} > $dir/${line%#*}/governor
      fi
    done < $devfreq_backup
  fi
}

set_value() {
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

set_input_boost_freq() {
  local c0="$1"
  local c1="$2"
  local c2="$3"
  local ms="$4"
  echo "0:$c0 1:$c0 2:$c0 3:$c0 4:$c1 5:$c1 6:$c1 7:$c2" > /sys/module/cpu_boost/parameters/input_boost_freq
  echo $ms > /sys/module/cpu_boost/parameters/input_boost_ms
}

set_cpu_freq() {
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

set_cpu_pl() {
  echo $1 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/pl
  echo $1 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/pl
  echo $1 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/pl
}

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

ctl_on() {
  echo 1 > /sys/devices/system/cpu/$1/core_ctl/enable
  if [[ "$2" != "" ]]; then
    echo $2 > /sys/devices/system/cpu/$1/core_ctl/min_cpus
  else
    echo 0 > /sys/devices/system/cpu/$1/core_ctl/min_cpus
  fi
}

ctl_off() {
  echo 0 > /sys/devices/system/cpu/$1/core_ctl/enable
}

set_ctl() {
  echo $2 > /sys/devices/system/cpu/$1/core_ctl/busy_up_thres
  echo $3 > /sys/devices/system/cpu/$1/core_ctl/busy_down_thres
  echo $4 > /sys/devices/system/cpu/$1/core_ctl/offline_delay_ms
}

set_hispeed_freq() {
  echo $1 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_freq
  echo $2 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_freq
  echo $3 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_freq
}

set_hispeed_load() {
  echo $1 > /sys/devices/system/cpu/cpufreq/policy0/schedutil/hispeed_load
  echo $2 > /sys/devices/system/cpu/cpufreq/policy4/schedutil/hispeed_load
  echo $3 > /sys/devices/system/cpu/cpufreq/policy7/schedutil/hispeed_load
}

sched_boost() {
  echo $1 > /proc/sys/kernel/sched_boost_top_app
  echo $2 > /proc/sys/kernel/sched_boost
}

stune_top_app() {
  echo $1 > /dev/stune/top-app/schedtune.prefer_idle
  echo $2 > /dev/stune/top-app/schedtune.boost
}

cpuset() {
  echo $1 > /dev/cpuset/background/cpus
  echo $2 > /dev/cpuset/system-background/cpus
  echo $3 > /dev/cpuset/foreground/cpus
  echo $4 > /dev/cpuset/top-app/cpus
}

# [min/max/def] pl(number)
set_gpu_pl(){
  echo $2 > /sys/class/kgsl/kgsl-3d0/${1}_pwrlevel
}

# GPU MinPowerLevel To Up
gpu_pl_up() {
  local offset="$1"
  if [[ "$offset" != "" ]] && [[ ! "$offset" -gt "$gpu_min_pl" ]]; then
    echo `expr $gpu_min_pl - $offset` > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
  elif [[ "$offset" -gt "$gpu_min_pl" ]]; then
    echo 0 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
  else
    echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/min_pwrlevel
  fi
}

# GPU MinPowerLevel To Down
gpu_pl_down() {
  local offset="$1"
  if [[ "$offset" != "" ]] && [[ ! "$offset" -gt "$gpu_min_pl" ]]; then
    echo $offset > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
  elif [[ "$offset" -gt "$gpu_min_pl" ]]; then
    echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
  else
    echo $gpu_min_pl > /sys/class/kgsl/kgsl-3d0/max_pwrlevel
  fi
}

# set_task_affinity $pid $use_cores[cpu7~cpu0]
set_task_affinity() {
  pid=$1
  mask=`echo "obase=16;$((num=2#$2))" | bc`
  for tid in $(ls "/proc/$pid/task/"); do
    taskset -p "$mask" "$tid" 1>/dev/null
  done
  taskset -p "$mask" "$pid" 1>/dev/null
}

# YuanShen
yuan_shen_opt_run() {
  if [[ $(getprop vtools.powercfg_app | grep miHoYo) == "" ]]; then
    return
  fi

  # top -H -p $(pgrep -ef Yuanshen)
  # pid=$(pgrep -ef Yuanshen)
  pid=$(pgrep -ef miHoYo)
  # mask=`echo "obase=16;$((num=2#11110000))" | bc` # F0 (cpu 7-4)
  # mask=`echo "obase=16;$((num=2#10000000))" | bc` # 80 (cpu 7)
  # mask=`echo "obase=16;$((num=2#01110000))" | bc` # 70 (cpu 6-4)
  # mask=`echo "obase=16;$((num=2#01111111))" | bc` # 7F (cpu 6-0)

  if [[ "$pid" != "" ]]; then
    for tid in $(ls "/proc/$pid/task/"); do
      if [[ -f "/proc/$pid/task/$tid/comm" ]]; then
        comm=$(cat /proc/$pid/task/$tid/comm)

        case "$comm" in
         "UnityMain")
           taskset -p "F0" "$tid" 2>&1 > /dev/null
         ;;
         # "UnityGfxDevice"*|"UnityMultiRende"*|"NativeThread"*|"UnityChoreograp"*)
         "UnityGfxDevice"*|"UnityMultiRende"*)
           taskset -p "70" "$tid" 2>&1 > /dev/null
         ;;
         *)
           taskset -p "7F" "$tid" 2>&1 > /dev/null
         ;;
        esac
      fi
    done
  fi
}

# watch_app [time] [on_tick] [on_change]
watch_app() {
  local interval="$1"
  local on_tick="$2"
  local on_change="$3"
  local app=$(getprop vtools.powercfg_app)

  if [[ "$on_tick" == "" ]]; then
    return
  fi

  local prop='vtools.perf.watch'

  local current_watch=$(getprop $prop)
  if [[ "$current_watch" != "" ]]; then
    kill -9 $current_watch 2>/dev/null
    setprop $prop ""
  fi

  if [[ "$app" == "" ]]; then
    return
  fi

  setprop $prop "$$"
  while true
  do
    sleep $interval
    current=$(getprop vtools.powercfg_app)
    if [[ "$current" == "$app" ]]; then
      $on_tick $current
    else
      setprop $prop ""
      if [[ "$on_change" ]]; then
        $on_change $current
      fi
      return
    fi
  done
}

yuan_shen_opt() {
  sleep 10
  yuan_shen_opt_run
  sleep 50
  yuan_shen_opt_run
  sleep 30
  yuan_shen_opt_run
  sleep 30
  yuan_shen_opt_run

  watch_app 120 yuan_shen_opt_run
}

adjustment_by_top_app() {
  case "$top_app" in
    # YuanShen
    "com.miHoYo.Yuanshen" | "com.miHoYo.ys.mi" | "com.miHoYo.ys.bilibili")
        ctl_off cpu4
        ctl_off cpu7
        if [[ "$action" = "powersave" ]]; then
          sched_boost 0 0
          stune_top_app 0 0
          sched_config "50 80" "67 95" "300" "400"
          gpu_pl_down 3
          set_cpu_freq 1036800 1785600 1497600 1804800 1056000 2227200
          set_hispeed_freq 1708800 1708800 2016000
          sched_limit 5000 0 5000 0 5000 0
          yuan_shen_opt &
        elif [[ "$action" = "balance" ]]; then
          sched_boost 1 0
          stune_top_app 1 10
          sched_config "50 68" "67 80" "300" "400"
          gpu_pl_down 1
          set_cpu_freq 1036800 1785600 1056000 2016000 1056000 2419200
          set_hispeed_freq 1708800 1056000 1056000
          sched_limit 5000 0 5000 0 5000 0
          yuan_shen_opt &
        elif [[ "$action" = "performance" ]]; then
          sched_boost 1 0
          stune_top_app 1 10
          gpu_pl_down 1
          set_cpu_freq 1036800 1478400 1056000 2419200 1056000 2841600
          set_hispeed_freq 1708800 1708800 1708800
          sched_limit 5000 0 5000 0 5000 0
          yuan_shen_opt &
        elif [[ "$action" = "fast" ]]; then
          sched_boost 1 0
          stune_top_app 1 100
          sched_limit 5000 0 10000 0 5000 0
          # sched_config "40 60" "50 75" "120" "150"
          yuan_shen_opt &
        fi
        cpuset '0-1' '0-3' '0-3' '0-7'
    ;;

    # Wang Zhe Rong Yao
    "com.tencent.tmgp.sgame")
        ctl_off cpu4
        ctl_on cpu7
        if [[ "$action" = "powersave" ]]; then
          # sched_config "55 68" "69 78" "300" "400"
          sched_config "52 55" "69 67" "300" "400"
          sched_boost 1 0
          stune_top_app 0 0
          cpuset '0-1' '0-1' '0-3' '0-7'
        elif [[ "$action" = "balance" ]]; then
          # sched_config "48 65" "63 75" "300" "400"
          sched_config "50 55" "65 65" "300" "400"
          sched_boost 1 0
          stune_top_app 0 1
          cpuset '0-1' '0-1' '0-6' '0-7'
        elif [[ "$action" = "performance" ]]; then
          sched_config "45 55" "55 65" "300" "400"
          sched_boost 1 0
          stune_top_app 1 20
          cpuset '0-1' '0-1' '0-6' '0-7'
        elif [[ "$action" = "fast" ]]; then
          sched_config "40 55" "50 63" "300" "400"
          cpuset '0-1' '0-1' '0-6' '0-7'
          sched_boost 1 1
          stune_top_app 1 20
        fi
    ;;

    # ShuangShengShiJie
    "com.bilibili.gcg2.bili")
        if [[ "$action" = "powersave" ]]; then
          gpu_pl_down 4
        elif [[ "$action" = "balance" ]]; then
          gpu_pl_down 3
        elif [[ "$action" = "performance" ]]; then
          gpu_pl_down 1
        elif [[ "$action" = "fast" ]]; then
          gpu_pl_down 0
        fi
        sched_config "60 68" "68 72" "140" "200"
        stune_top_app 0 0
        sched_boost 1 0
        cpuset '0-1' '0-3' '0-3' '0-7'
    ;;

    # XianYu, TaoBao, MIUI Home, Browser, TieBa Fast, TieBa、JingDong、TianMao、Mei Tuan、RE、ES、PuPuChaoShi
    "com.taobao.idlefish" | "com.taobao.taobao" | "com.miui.home" | "com.android.browser" | "com.baidu.tieba_mini" | "com.baidu.tieba" | "com.jingdong.app.mall" | "com.tmall.wireless" | "com.sankuai.meituan" | "com.speedsoftware.rootexplorer" | "com.estrongs.android.pop" | "com.pupumall.customer")
      if [[ "$action" == "balance" ]] && [[ "$top_app" == "com.miui.home" ]]; then
        sched_boost 1 0
        stune_top_app 1 1
        sched_config "45 62" "55 75" "85" "100"
      elif [[ "$action" != "powersave" ]]; then
        sched_boost 1 1
        stune_top_app 1 1
        sched_config "45 62" "55 75" "85" "100"
        # echo 4-6 > /dev/cpuset/top-app/cpus
      fi
    ;;

    # NeteaseCloudMusic, KuGou, KuGou Lite
    "com.netease.cloudmusic" | "com.kugou.android" | "com.kugou.android.lite")
      echo 0-6 > /dev/cpuset/foreground/cpus
    ;;

    # DouYin, BiliBili
    "com.ss.android.ugc.aweme" | "tv.danmaku.bili")
      ctl_on cpu4
      ctl_on cpu7

      set_ctl cpu4 85 45 0
      set_ctl cpu7 80 40 0

      sched_boost 0 0
      stune_top_app 0 0
      set_cpu_pl 0
      echo 0-3 > /dev/cpuset/foreground/cpus

      if [[ "$action" = "powersave" ]]; then
        echo 0-5 > /dev/cpuset/top-app/cpus
      elif [[ "$action" = "balance" ]]; then
        echo 0-6 > /dev/cpuset/top-app/cpus
      elif [[ "$action" = "performance" ]]; then
        echo 0-6 > /dev/cpuset/top-app/cpus
      elif [[ "$action" = "fast" ]]; then
        echo 0-7 > /dev/cpuset/top-app/cpus
      fi
      pgrep -f $top_app | while read pid; do
        # echo $pid > /dev/cpuset/foreground/cgroup.procs
        echo $pid > /dev/stune/background/cgroup.procs
      done

      sched_config "85 85" "100 100" "240" "400"
    ;;

    "default")
      echo '未适配的应用'
    ;;
  esac
}
