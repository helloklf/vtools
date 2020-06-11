platform=`getprop ro.board.platform`
ANDROID_SDK=`getprop ro.build.version.sdk`
resource_dir="./kr-script/miui/thermal_conf/$platform"

if [[ "$ANDROID_SDK" -gt 24 ]] && [[ -n `getprop ro.miui.ui.version.name` ]] && [[ -d $resource_dir ]]
then
    echo 1
else
    echo 0
fi