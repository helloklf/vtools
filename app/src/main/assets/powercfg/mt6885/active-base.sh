function ged() {
    echo $2 > /sys/module/ged/parameters/$1
}

# ged boost_gpu_enable 0
# ged enable_cpu_boost 0
# ged enable_gpu_boost 0
# ged ged_boost_enable 0
# ged ged_force_mdp_enable 0
# ged ged_smart_boost 0
# ged gx_force_cpu_boost 0
# ged gpu_dvfs_enable 1
# ged gx_3D_benchmark_on 0
# ged gx_dfps 1
# ged gx_game_mode 0

# echo 0 > /sys/kernel/debug/eara_thermal/enable

# settings put system min_refresh_rate 0
# settings put system peak_refresh_rate 120
