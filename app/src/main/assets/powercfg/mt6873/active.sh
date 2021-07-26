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

# GPU FREQ
# 902000 890000 878000 866000 855000 843000 831000 819000 807000 795000 783000 772000 760000 748000 736000 724000 712000
# 700000 688000 667000 647000 626000 605000 585000 564000 543000 523000 502000 482000 461000 440000 420000 399000 378000 358000
function gpu_dvfs() {
    # echo $1 > /sys/module/ged/parameters/gpu_dvfs_enable
    echo $1 > /proc/mali/dvfs_enable
    if [[ "$1" == "1" ]]; then
      echo 0 > /proc/gpufreq/gpufreq_opp_freq
    elif [[ "$2" != "" ]]; then
      echo "$2" > /proc/gpufreq/gpufreq_opp_freq
    else
      echo 902000 > /proc/gpufreq/gpufreq_opp_freq
    fi
}

function cpuset() {
    echo $1 > /dev/cpuset/background/cpus
    echo $2 > /dev/cpuset/system-background/cpus
    echo $3 > /dev/cpuset/foreground/cpus
    echo $4 > /dev/cpuset/top-app/cpus
    echo $5 > /dev/cpuset/restricted/cpus
}

function eas() {
    echo ${1}000 > /sys/devices/system/cpu/cpufreq/schedutil/down_rate_limit_us
    echo ${2}000 > /sys/devices/system/cpu/cpufreq/schedutil/up_rate_limit_us
    echo 1 > /sys/devices/system/cpu/eas/enable
}

function cpufreq() {
    # 0 Normal, 1 Low Power, 2 Just Make, 3 Perf
    echo $1 > /proc/cpufreq/cpufreq_power_mode
    # 0 Normal, 1 Perf
    echo $1 > /proc/cpufreq/cpufreq_cci_mode
}

action=$1

for i in 0 1 2 3 4 5 6 7
do
  echo 1 > /sys/devices/system/cpu/cpu$i/online
done

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
# [3] PPM_POLICY_PWR_THRO: enabled
# [4] PPM_POLICY_THERMAL: enabled
# [9] PPM_POLICY_SYS_BOOST: disabled
# [6] PPM_POLICY_HARD_USER_LIMIT: enabled
# [7] PPM_POLICY_USER_LIMIT: enabled
# [8] PPM_POLICY_LCM_OFF: disabled
#
# Usage: echo <idx> <1/0> > /proc/ppm/policy_status

# dump_cluster_0_dvfs_table
# 2000000 1875000 1812000 1750000 1687000 1625000 1541000 1500000 1358000 1181000 1075000 968000 862000 756000 650000 500000
# dump_cluster_1_dvfs_table
# 2600000 2433000 2266000 2133000 1933000 1800000 1633000 1548000 1383000 1300000 1175000 1133000 1050000 925000 841000 774000

if [[ "$action" = "powersave" ]]; then
  #powersave
  ppm enabled 1

  ppm policy_status "1 0"
  ppm policy_status "2 0"
  ppm policy_status "3 0"
  ppm policy_status "4 1"
  ppm policy_status "7 0"
  ppm policy_status "9 0"

  min_freq 500000 774000
  max_freq 1812000 1933000

  gpu_dvfs 1

  cpuset 0-1 0-3 0-7 0-7 0-3

  echo 1 > /proc/cpuidle/enable
  eas 0 2
  cpufreq 1 0
  echo 0 > /proc/ufs_perf

  exit 0
elif [[ "$action" = "balance" ]]; then
  #balance
  ppm enabled 1

  ppm policy_status "1 0"
  ppm policy_status "2 0"
  ppm policy_status "3 0"
  ppm policy_status "4 1"
  ppm policy_status "7 0"
  ppm policy_status "9 0"

  min_freq 500000 774000
  max_freq 2000000 2433000

  gpu_dvfs 1

  cpuset 0-1 0-3 0-7 0-7 0-3

  echo 1 > /proc/cpuidle/enable
  eas 0 0
  cpufreq 0 0
  echo 0 > /proc/ufs_perf

  exit 0
elif [[ "$action" = "performance" ]]; then
  #performance
  ppm enabled 1

  ppm policy_status "1 0"
  ppm policy_status "2 1"
  ppm policy_status "3 0"
  ppm policy_status "4 1"
  ppm policy_status "7 0"
  ppm policy_status "9 1"

  min_freq 1181000 1300000
  max_freq 2000000 2600000

  gpu_dvfs 0 807000

  cpuset 0-1 0-3 0-7 0-7 0-3

  echo 1 > /proc/cpuidle/enable
  eas 20 0
  cpufreq 3 1
  echo 2 > /proc/ufs_perf

  exit 0
elif [[ "$action" = "fast" ]]; then
  #fast
  ppm enabled 1

  ppm policy_status "1 0"
  ppm policy_status "2 1"
  ppm policy_status "3 0"
  ppm policy_status "4 0"
  ppm policy_status "7 0"
  ppm policy_status "9 1"

  min_freq 1812000 1933000
  max_freq 2000000 2600000

  gpu_dvfs 0 902000

  cpuset 0 0-3 0-7 0-7 0-3

  echo 0 > /proc/cpuidle/enable
  eas 50 0
  cpufreq 3 1
  echo 2 > /proc/ufs_perf

  exit 0
fi
