action=$1
task=$2

cfg_dir=$(cd $(dirname $0); pwd)

if [[ ! -f "$cfg_dir/powercfg-utils.sh" ]]; then
  echo "The dependent '$cfg_dir/powercfg-utils.sh' was not found !" > /cache/powercfg.sh.log
  exit 1
fi

source "$cfg_dir/powercfg-utils.sh"

init () {
  if [[ -f "$cfg_dir/powercfg-base.sh" ]]; then
    source "$cfg_dir/powercfg-base.sh"
  elif [[ -f '/data/powercfg-base.sh' ]]; then
    source /data/powercfg-base.sh
  fi
}

if [[ "$action" == "init" ]]; then
  init
  exit 0
fi

reset_basic_governor

if [[ "$action" = "powersave" ]]; then
  # ctl_on cpu4
  # ctl_on cpu7
  # set_ctl cpu4 60 35 20
  # set_ctl cpu7 60 30 20
  set_cpu_freq 300000 1708800 710400 1555200 844800 1785600
  gpu_pl_up 0
  sched_boost 0 0
  set_hispeed_freq 902400 710400 844800
  sched_config "85 75" "96 86" "150" "400"
  sched_limit 0 0 0 2000 0 1000
  cpuset '0-2' '0-3' '0-3' '0-7'
  stune_top_app 0 0
  cpuctl foreground 0 0 0 1
  cpuctl background 0 0 0 0
  cpuctl top-app 0 0 0 max
  bw_min
  bw_down 3 3
  thermal_disguise 0
  set_cpu_pl 0
  if [[ "$manufacturer" == "Xiaomi" ]]; then
    stop miuibooster
  fi


elif [[ "$action" = "balance" ]]; then
  # ctl_on cpu4
  # ctl_on cpu7
  # set_ctl cpu4 60 30 100
  # set_ctl cpu7 60 30 100
  set_cpu_freq 300000 1804800 710400 1996800 844800 2380800
  gpu_pl_up 0
  sched_boost 1 0
  set_hispeed_freq 1612800 1075200 1305600
  sched_config "78 70" "89 82" "150" "400"
  sched_limit 0 0 0 500 0 500
  cpuset '0-2' '0-3' '0-6' '0-7'
  stune_top_app 0 0
  cpuctl foreground 0 1 0 max
  cpuctl background 0 1 0 1
  cpuctl top-app 0 1 0.25 max
  bw_min
  bw_down 2 2
  thermal_disguise 0
  set_cpu_pl 0
  if [[ "$manufacturer" == "Xiaomi" ]]; then
    start miuibooster
  fi


elif [[ "$action" = "performance" ]]; then
  # ctl_off cpu4
  # ctl_off cpu7
  set_cpu_freq 300000 1804800 710400 2419200 825600 2841600
  gpu_pl_up 1
  sched_boost 1 0
  set_hispeed_freq 0 0 0
  sched_config "62 55" "72 65" "200" "400"
  sched_limit 0 0 0 0 0 0
  cpuset '0-1' '0-3' '0-6' '0-7'
  stune_top_app 1 0
  cpuctl foreground 0 1 0 max
  cpuctl background 0 1 0 max
  cpuctl top-app 0 1 0.5 max
  bw_min
  bw_max
  thermal_disguise 0
  set_cpu_pl 1
  if [[ "$manufacturer" == "Xiaomi" ]]; then
    start miuibooster
  fi


elif [[ "$action" = "fast" ]]; then
  # ctl_off cpu4
  # ctl_off cpu7
  set_cpu_freq 1401600 1804800 1766400 2600000 1670400 3200000
  set_hispeed_freq 0 0 0
  gpu_pl_up 2
  sched_boost 1 1
  sched_config "62 35" "70 50" "300" "400"
  sched_limit 5000 0 2000 0 2000 0
  cpuset '0' '0-3' '0-6' '0-7'
  stune_top_app 1 50
  cpuctl foreground 0 1 0 max
  cpuctl background 0 1 0 max
  cpuctl top-app 0 1 max max
  bw_max_always
  thermal_disguise 1
  set_cpu_pl 1
  if [[ "$manufacturer" == "Xiaomi" ]]; then
    start miuibooster
  fi


fi

adjustment_by_top_app
restore_core_online
renice -n -20 `pgrep com.miui.home` 2> /dev/null
