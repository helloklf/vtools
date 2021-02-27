#!/system/bin/sh

# GPU频率表
gpu_freqs_path='/sys/class/kgsl/kgsl-3d0/devfreq/available_frequencies'
# GPU频率可选项
gpu_freqs=`cat $gpu_freqs_path`
# GPU最大频率
gpu_max_freq=$(awk -F ' ' '{print $NF}' $gpu_freqs_path)
# GPU最小频率
gpu_min_freq=$(awk '{print $1}' $gpu_freqs_path)
# GPU最小 power level
gpu_min_pl=6
# GPU最大 power level
gpu_max_pl=0
# GPU默认 power level
gpu_default_pl=`cat /sys/class/kgsl/kgsl-3d0/default_pwrlevel`
# GPU型号
gpu_model=`cat /sys/class/kgsl/kgsl-3d0/gpu_model`
# GPU调度器
gpu_governor=`cat /sys/class/kgsl/kgsl-3d0/devfreq/governor`

# Power Levels
if [[ -f /sys/class/kgsl/kgsl-3d0/num_pwrlevels ]];then
    gpu_min_pl=`cat /sys/class/kgsl/kgsl-3d0/num_pwrlevels`
    gpu_min_pl=`expr $gpu_min_pl - 1`
fi;
if [[ "$gpu_min_pl" -lt 0 ]];then
    gpu_min_pl=0
fi;

# 输出GPU信息
echo "" &&
echo "GPU Mode: ${gpu_model}" &&
echo "Frequency: ${gpu_max_freq}~${gpu_min_freq}" &&
echo "Power Level: ${gpu_max_pl}~${gpu_min_pl} [${gpu_default_pl}]" &&
echo "Governor: ${gpu_governor}" &&
echo ""