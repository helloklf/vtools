#!/system/bin/sh

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


# Setting b.L scheduler parameters
# default sched up and down migrate values are 90 and 85
# echo 65 > /proc/sys/kernel/sched_downmigrate
# echo 71 > /proc/sys/kernel/sched_upmigrate
# default sched up and down migrate values are 100 and 95
# echo 85 > /proc/sys/kernel/sched_group_downmigrate
# echo 100 > /proc/sys/kernel/sched_group_upmigrate


if [ "$action" = "powersave" ]; then
  set_cpu_freq 5000 1612800 5000 1555200
  set_input_boost_freq 0 0 0
  set_hispeed_freq 1248000 806400
  sched_boost 0 0
  stune_top_app 0 0
  ctl_on cpu0
  ctl_on cpu6
  sched_config 75 92 380 500
  sched_limit 0 0 500 1000
  cpuset '0-1' '0-3' '0-3' '0-7'

elif [ "$action" = "balance" ]; then
  set_cpu_freq 5000 1708800 5000 1843200
  set_input_boost_freq 0 0 0
  set_hispeed_freq 1248000 1209600
  sched_boost 1 0
  stune_top_app 0 0
  ctl_off cpu0
  ctl_on cpu6
  sched_config 68 82 300 400
  sched_limit 0 0 0 0
  cpuset '0-1' '0-3' '0-5' '0-7'

elif [ "$action" = "performance" ]; then
  set_cpu_freq 300000 2500000 300000 2750000
  set_input_boost_freq 1804800 1939200 120
  set_hispeed_freq 1708800 1209600
  gpu_pl_up 1
  sched_boost 1 0
  stune_top_app 0 0
  ctl_off
  ctl_off
  sched_config 60 78 300 400
  sched_limit 2000 1000 0 0
  cpuset '0-1' '0-3' '0-5' '0-7'

elif [ "$action" = "fast" ]; then
  set_cpu_freq 1708800 2500000 1209600 2750000
  set_input_boost_freq 1804800 1939200 500
  set_hispeed_freq 300000 300000
  gpu_pl_up 2
  sched_boost 1 1
  stune_top_app 1 20
  ctl_off
  ctl_off
  sched_config 50 75 300 400
  sched_limit 5000 2000 0 0
  cpuset '0-1' '0-3' '0-5' '0-7'

fi
