platform=`getprop ro.board.platform`
ANDROID_SDK=`getprop ro.build.version.sdk`
resource_dir="./kr-script/miui/thermal_conf2/$platform"

if [[ "$ANDROID_SDK" -gt 28 ]] && [[ -n `getprop ro.miui.ui.version.name` ]] && [[ -d $resource_dir ]]
then
  if [[ -f "$resource_dir/support.sh" ]]; then
    sh "$resource_dir/support.sh"
  else
    echo 1
  fi
else
    echo 0
fi