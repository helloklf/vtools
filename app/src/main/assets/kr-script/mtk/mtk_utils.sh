function sched_boost_get() {
  cat /sys/devices/system/cpu/sched/sched_boost | cut -f2 -d '='
}

function sched_boost_set() {
  if [[ "$state" == "no boost" ]]; then
    echo 0 > /sys/devices/system/cpu/sched/sched_boost
  elif [[ "$state" == "all boost" ]]; then
    echo 1 > /sys/devices/system/cpu/sched/sched_boost
  elif [[ "$state" == "foreground boost" ]]; then
    echo 2 > /sys/devices/system/cpu/sched/sched_boost
  fi
}

function eas_get() {
  cat /sys/devices/system/cpu/eas/enable | cut -f2 -d '='
}

function eas_set() {
  if [[ "$state" == "HMP" ]]; then
    echo 0 > /sys/devices/system/cpu/eas/enable
  elif [[ "$state" == "EAS" ]]; then
    echo 1 > /sys/devices/system/cpu/eas/enable
  elif [[ "$state" == "hybrid" ]]; then
    echo 2 > /sys/devices/system/cpu/eas/enable
  fi
}

function gpu_freq() {
  local dvfs=/proc/mali/dvfs_enable
  if [[ -f $dvfs ]]; then
    if [[ "$state" == "0" ]]; then
      echo 1 > $dvfs
    else
      echo 0 > $dvfs
    fi
  fi
  echo $state > /proc/gpufreq/gpufreq_opp_freq
}