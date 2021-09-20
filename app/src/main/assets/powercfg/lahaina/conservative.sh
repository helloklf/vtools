action=$1
task=$2

cfg_dir=$(cd $(dirname $0); pwd)

if [[ ! -f "$cfg_dir/powercfg-utils.sh" ]]; then
  echo "The dependent '$cfg_dir/powercfg-utils.sh' was not found !" > /cache/powercfg.sh.log
  exit 1
fi

source "$cfg_dir/powercfg-utils.sh"

init () {
  echo '[Scene PerfConfig Init] ...'
  if [[ -f "$cfg_dir/powercfg-base.sh" ]]; then
    source "$cfg_dir/powercfg-base.sh"
  elif [[ -f '/data/powercfg-base.sh' ]]; then
    source /data/powercfg-base.sh
  fi
  echo '[Scene PerfConfig Init] √'
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
  set_hispeed_freq 1612800 710400 844800
  sched_config "85 85" "96 96" "150" "400"
  sched_limit 0 500 0 2000 0 1000
  cpuset '0' '0-3' '0-3' '0-7'
  stune_top_app 0 0
  cpuctl top-app 0 0 0 max
  bw_min
  bw_down 3 3
  thermal_disguise 0


elif [[ "$action" = "balance" ]]; then
  # ctl_on cpu4
  # ctl_on cpu7
  # set_ctl cpu4 60 30 100
  # set_ctl cpu7 60 30 100
  set_cpu_freq 300000 1804800 710400 1881600 844800 2035200
  gpu_pl_up 0
  sched_boost 1 0
  set_hispeed_freq 1612800 1075200 1305600
  sched_config "78 85" "89 96" "150" "400"
  sched_limit 0 0 0 500 0 500
  cpuset '0-2' '0-3' '0-6' '0-7'
  stune_top_app 0 0
  # cpuctl top-app 0 1 0.1 max
  cpuctl top-app 0 1 0 max
  bw_min
  bw_down 2 2
  thermal_disguise 0


elif [[ "$action" = "performance" ]]; then
  # ctl_off cpu4
  # ctl_off cpu7
  set_cpu_freq 300000 1804800 710400 2419200 825600 2841600
  gpu_pl_up 0
  sched_boost 1 0
  set_hispeed_freq 1612800 1766400 2035200
  sched_config "65 78" "75 88" "200" "400"
  sched_limit 0 0 0 0 0 0
  cpuset '0-1' '0-3' '0-6' '0-7'
  stune_top_app 0 0
  cpuctl top-app 0 1 0.25 max
  bw_min
  bw_max
  thermal_disguise 0


elif [[ "$action" = "fast" ]]; then
  # ctl_off cpu4
  # ctl_off cpu7
  set_cpu_freq 1401600 1804800 1440000 2600000 1555200 3200000
  set_hispeed_freq 1708800 1881600 1670400
  gpu_pl_up 2
  sched_boost 1 0
  sched_config "62 75" "70 80" "300" "400"
  sched_limit 5000 0 2000 0 2000 0
  cpuset '0' '0-3' '0-6' '0-7'
  stune_top_app 1 0
  cpuctl top-app 0 1 max max
  bw_max_always
  thermal_disguise 1


fi

adjustment_by_top_app
restore_core_online
renice -n -20 `pgrep com.miui.home` 2> /dev/null
