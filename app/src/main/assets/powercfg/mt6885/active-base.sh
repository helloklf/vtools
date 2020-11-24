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

set_top_app()
{
  echo -n "  + top-app $1 "
  # pgrep 精确度有点差
  pgrep -f $1 | while read pid; do
    echo -n "$pid "
    echo $pid > /dev/cpuset/top-app/tasks
    echo $pid > /dev/stune/top-app/tasks
  done
  echo ""
}

set_apps() {
  set_top_app android.hardware.audio
  set_top_app android.hardware.bluetooth
  set_top_app com.android.permissioncontroller
  set_top_app vendor.qti.hardware.display.composer-service
  set_top_app android.hardware.graphics.composer
  set_top_app surfaceflinger
  set_top_app system_server
  set_top_app audioserver
  set_top_app servicemanager
  set_top_app com.android.systemui
  set_top_app com.miui.home
}

set_apps
