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
