#!/system/bin/sh

ANDROID_SDK=`getprop ro.build.version.sdk`

if [[ "$ANDROID_SDK" -gt 27 ]]
then
    gcm=`getprop persist.vendor.camera.eis.enable`
else
    gcm=`getprop persist.camera.HAL3.enabled`
fi

if [ "$gcm" = '1' ]; then
    echo 1
else
    echo 0
fi