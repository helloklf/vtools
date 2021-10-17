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

if [[ "$action" == "fast" ]]; then
  devfreq_performance
else
  devfreq_restore
fi
reset_basic_governor

if [[ "$action" = "powersave" ]]; then
  ctl_on cpu4
  ctl_on cpu7

  set_cpu_freq 300000 1708800 710400 1612800 825600 1920000
  gpu_pl_up 0
  sched_boost 1 0
  stune_top_app 0 0
  sched_config "75 75" "87 90" "160" "260"
  sched_limit 0 0 0 5000 0 5000
  set_hispeed_freq 1209600 825600 940800
  set_hispeed_load 90 90 90
  cpuset '0-2' '0-3' '0-3' '0-7'
  bw_min
  bw_down 1
  set_cpu_pl 0


elif [[ "$action" = "balance" ]]; then
  ctl_on cpu4
  ctl_on cpu7
  set_cpu_freq 300000 1785600 710400 1920000 825600 2323200
  gpu_pl_up 0
  sched_boost 1 0
  stune_top_app 0 0
  sched_config "65 70" "80 87" "120" "200"
  sched_limit 0 0 0 500 0 500
  set_hispeed_freq 1478400 1056000 1286400
  set_hispeed_load 80 90 90
  cpuset '0-2' '0-3' '0-6' '0-7'
  bw_min
  bw_down 1
  set_cpu_pl 1


elif [[ "$action" = "performance" ]]; then
  ctl_off cpu4
  ctl_off cpu7
  set_cpu_freq 300000 1785600 710400 2419200 825600 2841600
  gpu_pl_up 1
  sched_boost 1 0
  stune_top_app 0 0
  sched_config "60 70" "72 83" "85" "100"
  sched_limit 0 0 0 0 0 0
  set_hispeed_freq 1632000 1708800 2016000
  set_hispeed_load 60 70 80
  cpuset '0-2' '0-3' '0-6' '0-7'
  bw_min
  bw_max
  set_cpu_pl 1


elif [[ "$action" = "fast" ]]; then
  ctl_off cpu4
  ctl_off cpu7
  set_cpu_freq 1036800 1785600 1612800 2600000 1708800 3200000
  gpu_pl_up 2
  sched_boost 1 0
  stune_top_app 1 0
  sched_config "55 69" "68 79" "300" "400"
  sched_limit 50000 0 20000 0 20000 0
  set_hispeed_freq 0 0 0
  set_hispeed_load 50 60 70
  cpuset '0-1' '0-3' '0-6' '0-7'
  bw_max_always
  set_cpu_pl 1


fi

adjustment_by_top_app
renice -n -20 `pgrep com.miui.home` 2> /dev/null

