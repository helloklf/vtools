action=$1

cfg_dir=$(cd $(dirname $0); pwd)

if [[ ! -f "$cfg_dir/powercfg-utils.sh" ]]; then
  echo "The dependent '$cfg_dir/powercfg-utils.sh' was not found !" > /cache/powercfg.sh.log
  exit 1
fi

source "$cfg_dir/powercfg-utils.sh"

init () {
  if [[ -f "$cfg_dir/powercfg-base.sh" ]]; then
    sh "$cfg_dir/powercfg-base.sh"
  elif [[ -f '/data/powercfg-base.sh' ]]; then
    sh /data/powercfg-base.sh
  fi
}

if [[ "$action" = "init" ]]; then
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
  set_cpu_freq 300000 1708800 710400 1401600 825600 1497600
  set_input_boost_freq 0 0 0 0
  gpu_pl_up 0
  sched_boost 1 0
  sched_config "85 85" "96 96" "160" "260"
  sched_limit 0 0 0 5000 0 5000
  set_hispeed_freq 1209600 825600 940800
  set_hispeed_load 90 90 90
  cpuset '0-2' '0-3' '0-3' '0-7'


elif [[ "$action" = "balance" ]]; then
  ctl_on cpu4
  ctl_on cpu7
  set_cpu_freq 300000 1708800 710400 1708800 825600 1920000
  set_input_boost_freq 1209600 0 0 40
  gpu_pl_up 0
  sched_boost 1 0
  sched_config "78 85" "89 96" "120" "200"
  sched_limit 0 0 0 500 0 500
  set_hispeed_freq 1478400 1056000 1286400
  set_hispeed_load 80 90 90
  cpuset '0-2' '0-3' '0-6' '0-7'


elif [[ "$action" = "performance" ]]; then
  ctl_off cpu4
  ctl_off cpu7
  set_cpu_freq 300000 1785600 710400 2419200 825600 2841600
  set_input_boost_freq 1478400 1286400 1286400 40
  gpu_pl_up 1
  sched_boost 1 0
  sched_config "62 78" "72 85" "85" "100"
  sched_limit 0 0 0 0 0 0
  set_hispeed_freq 1632000 1708800 2016000
  set_hispeed_load 60 70 80
  cpuset '0-2' '0-3' '0-6' '0-7'


elif [[ "$action" = "fast" ]]; then
  ctl_off cpu4
  ctl_off cpu7
  set_cpu_freq 1209600 1785600 1497600 2600000 1497600 3200000
  set_input_boost_freq 1708800 1612800 1804800 80
  gpu_pl_up 2
  sched_boost 1 0
  sched_config "55 70" "68 79" "300" "400"
  sched_limit 50000 0 20000 0 20000 0
  set_hispeed_freq 1632000 1612800 1708800
  set_hispeed_load 50 60 70
  cpuset '0-1' '0-3' '0-6' '0-7'


fi

adjustment_by_top_app
