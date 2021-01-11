#!/system/bin/sh

# GPU频率表
gpu_freqs=`cat /sys/class/kgsl/kgsl-3d0/devfreq/available_frequencies`
# GPU最大频率
gpu_max_freq=$(echo $gpu_freqs | awk -F ' ' '{print $NF}')
# GPU最小频率
gpu_min_freq=$(echo $gpu_freqs | awk '{print $1}')

# 输出GPU信息
echo "Frequency: ${gpu_min_freq} ~ ${gpu_max_freq}"

if [[ ! "$gpu_governor" = "msm-adreno-tz" ]]; then
  echo 'msm-adreno-tz' > /sys/class/kgsl/kgsl-3d0/devfreq/governor
fi
echo $gpu_max_freq > /sys/class/kgsl/kgsl-3d0/devfreq/max_freq
echo $gpu_min_freq > /sys/class/kgsl/kgsl-3d0/devfreq/min_freq

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


