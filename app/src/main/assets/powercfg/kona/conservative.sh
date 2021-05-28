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
  set_ctl cpu4 65 35 20
  set_ctl cpu7 60 30 20
  set_cpu_freq 300000 1708800 710400 1574400 844800 1747200
  # set_input_boost_freq 1248000 0 0 40
  gpu_pl_up 0
  sched_boost 0 0
  set_cpu_pl 0
  set_hispeed_freq 1612800 710400 844800
  sched_config "85 85" "96 96" "150" "400"
  sched_limit 0 0 0 2000 0 1000
  cpuset '0-2' '0-3' '0-3' '0-7'
  stune_top_app 0 0


elif [[ "$action" = "balance" ]]; then
  ctl_on cpu4
  ctl_on cpu7
  set_ctl cpu4 60 30 100
  set_ctl cpu7 60 30 100
  set_cpu_freq 300000 1804800 710400 1862400 844800 2073600
  # set_input_boost_freq 1478400 0 0 40
  gpu_pl_up 0
  sched_boost 1 0
  set_cpu_pl 1
  set_hispeed_freq 1612800 1056000 1305600
  sched_config "78 85" "89 96" "150" "400"
  sched_limit 0 0 0 500 0 500
  cpuset '0-2' '0-3' '0-6' '0-7'
  stune_top_app 0 0



elif [[ "$action" = "performance" ]]; then
  ctl_off cpu4
  ctl_off cpu7
  set_cpu_freq 300000 1804800 710400 2419200 825600 2841600
  # set_input_boost_freq 1420800 1286400 1305600 40
  gpu_pl_up 0
  sched_boost 1 0
  set_cpu_pl 1
  set_hispeed_freq 1612800 1766400 2073600
  sched_config "65 78" "75 88" "200" "400"
  sched_limit 0 0 0 0 0 0
  cpuset '0-1' '0-3' '0-6' '0-7'
  stune_top_app 0 0



elif [[ "$action" = "fast" ]]; then
  ctl_off cpu4
  ctl_off cpu7
  set_cpu_freq 1248000 1804800 1478400 2600000 1516800 3200000
  # set_input_boost_freq 1804800 1670400 1862400 80
  set_hispeed_freq 1612800 1670400 1862400
  gpu_pl_up 2
  sched_boost 1 0
  set_cpu_pl 1
  sched_config "62 75" "70 80" "300" "400"
  sched_limit 5000 0 2000 0 2000 0
  cpuset '0' '0-3' '0-6' '0-7'
  stune_top_app 1 0
fi


adjustment_by_top_app
