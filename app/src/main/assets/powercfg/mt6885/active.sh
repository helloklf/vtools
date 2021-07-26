function ppm() {
  echo $2 > "/proc/ppm/$1"
}

function policy() {
  echo $2 > "/proc/ppm/policy/$1"
}

function lock_freq() {
  policy ut_fix_freq_idx "$1 $2"
}

function max_freq() {
  policy hard_userlimit_max_cpu_freq "0 $1"
  policy hard_userlimit_max_cpu_freq "1 $2"
}

function min_freq() {
  policy hard_userlimit_min_cpu_freq "0 $1"
  policy hard_userlimit_min_cpu_freq "1 $2"
}

function ged() {
  echo $2 > /sys/module/ged/parameters/$1
}

function cpuset() {
  echo $1 > /dev/cpuset/background/cpus
  echo $2 > /dev/cpuset/system-background/cpus
  echo $3 > /dev/cpuset/foreground/cpus
  echo $4 > /dev/cpuset/top-app/cpus
  echo $5 > /dev/cpuset/restricted/cpus
}

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

# policy_status
# [0] PPM_POLICY_PTPOD: enabled
# [1] PPM_POLICY_UT: enabled
# [2] PPM_POLICY_FORCE_LIMIT: enabled
# [6] PPM_POLICY_HARD_USER_LIMIT: enabled
# [3] PPM_POLICY_PWR_THRO: enabled
# [4] PPM_POLICY_THERMAL: enabled
# [5] PPM_POLICY_DLPT: enabled
# [9] PPM_POLICY_SYS_BOOST: enabled
# [7] PPM_POLICY_USER_LIMIT: enabled
# [8] PPM_POLICY_LCM_OFF: disabled

# Usage: echo <idx> <1/0> > /proc/ppm/policy_status


# dump_cluster_0_dvfs_table
# 2000000 1895000 1791000 1708000 1625000 1500000 1393000 1287000 1181000 1048000 968000 862000 756000 703000 650000 500000
# dump_cluster_1_dvfs_table
# 2600000 2529000 2387000 2245000 2068000 1927000 1750000 1622000 1526000 1367000 1271000 1176000 1048000 921000 825000 730000

ppm enabled 1

ppm policy_status "1 0"
ppm policy_status "2 0"
# ppm policy_status "4 0"
ppm policy_status "5 0"
ppm policy_status "7 0"
ppm policy_status "9 0"

if [[ "$action" = "powersave" ]]; then
  #powersave

  min_freq 500000 730000
  max_freq 2000000 1927000

  ged gpu_dvfs 1
  ged gx_game_mode 0
  ged gx_3d_benchmark_on 0

  cpuset 0-1 0-3 0-7 0-7 0-3

  exit 0
elif [[ "$action" = "balance" ]]; then
  #balance

  min_freq 500000 730000
  max_freq 2000000 2433000

  ged gpu_dvfs 1
  ged gx_game_mode 0
  ged gx_3d_benchmark_on 0

  cpuset 0-1 0-3 0-7 0-7 0-3

  exit 0
elif [[ "$action" = "performance" ]]; then
  #performance

  min_freq 1181000 1367000
  max_freq 2000000 2600000

  ged gpu_dvfs 1
  ged gx_3d_benchmark_on 0
  ged gx_game_mode 1

  cpuset 0-1 0-3 0-7 0-7 0-3

  exit 0
elif [[ "$action" = "fast" ]]; then
  #fast

  min_freq 1791000 1927000
  max_freq 2000000 2600000

  ged gpu_dvfs 1
  ged gx_game_mode 0
  ged gx_3d_benchmark_on 1

  cpuset 0 0-3 0-7 0-7 0-3

  exit 0
fi
