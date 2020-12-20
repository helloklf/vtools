function ged() {
    echo $2 > /sys/module/ged/parameters/$1
}

function gpu_limit() {
  # echo [id][up_enable][low_enable] > /proc/gpufreq/gpufreq_limit_table
  # ex: echo 3 0 0 > /proc/gpufreq/gpufreq_limit_table
  # means disable THERMAL upper_limit_idx & lower_limit_idx
  #
  #        [name]  [id]     [prio]   [up_idx] [up_enable]  [low_idx] [low_enable]
  #        STRESS     0          8         -1          0         -1          0
  #          PROC     1          7         34          0         34          0
  #         PTPOD     2          6         -1          0         -1          0
  #       THERMAL     3          5         -1          0         -1          0
  #       BATT_OC     4          5         -1          0         -1          0
  #      BATT_LOW     5          5         -1          0         -1          0
  #  BATT_PERCENT     6          5         -1          0         -1          0
  #           PBM     7          5         -1          0         -1          0
  #        POLICY     8          4         -1          0         -1          0

  local limited="$1"
  for i in 0 1 2 3 4 5 6 7 8
  do
      # echo $i $limited $limited
      echo $i $limited $limited > /proc/gpufreq/gpufreq_limit_table
  done
}

# gpu_limit 0

ged boost_gpu_enable 0
ged enable_cpu_boost 0
ged enable_gpu_boost 0
ged ged_boost_enable 0
ged ged_force_mdp_enable 0
ged ged_smart_boost 0
ged gx_force_cpu_boost 0
ged gpu_dvfs_enable 1
ged gx_3D_benchmark_on 0
ged gx_dfps 1
ged gx_game_mode 0

echo 0 > /sys/kernel/debug/eara_thermal/enable

# settings put system min_refresh_rate 0
# settings put system peak_refresh_rate 120
