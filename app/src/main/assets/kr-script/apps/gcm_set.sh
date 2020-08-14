#!/system/bin/sh

ANDROID_SDK=`getprop ro.build.version.sdk`

if [[ "$ANDROID_SDK" -gt 27 ]]
then
    setprop persist.vendor.camera.eis.enable $state;
    setprop persist.vendor.camera.HAL3.enabled $state;

    result1=`getprop persist.vendor.camera.eis.enable`
    result2=`getprop persist.vendor.camera.HAL3.enabled`

    echo "persist.vendor.camera.eis.enable=${result1}"
    echo "persist.vendor.camera.HAL3.enabled=${result2}"
else
    setprop persist.camera.HAL3.enabled $state;
    setprop persist.camera.eis.enable $state;

    result1=`getprop persist.camera.HAL3.enabled`
    result2=`getprop persist.camera.eis.enable`

    echo "persist.camera.HAL3.enabled=${result1}"
    echo "persist.camera.eis.enable=${result2}"
fi

echo '已修改，可能需要重启才能生效！' 1>&2
