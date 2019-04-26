#!/system/bin/sh

ANDROID_SDK=`getprop ro.build.version.sdk`
gcm=$1

if [[ "$ANDROID_SDK" -gt 27 ]]
then
    setprop persist.vendor.camera.eis.enable $gcm;
    setprop persist.vendor.camera.HAL3.enabled $gcm;

    echo '已修改，可能需要重启才能生效！';
    result1=`getprop persist.vendor.camera.eis.enable`
    result2=`getprop persist.vendor.camera.HAL3.enabled`

    echo "persist.vendor.camera.eis.enable=${result1}"
    echo "persist.vendor.camera.HAL3.enabled=${result2}"
else
    setprop persist.camera.HAL3.enabled $gcm;
    setprop persist.camera.eis.enable $gcm;

    echo '已修改，可能需要重启才能生效！';
    result1=`getprop persist.camera.HAL3.enabled`
    result2=`getprop persist.camera.eis.enable`

    echo "persist.camera.HAL3.enabled=${result1}"
    echo "persist.camera.eis.enable=${result2}"
fi
