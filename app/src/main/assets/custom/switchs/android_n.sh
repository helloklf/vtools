#!/system/bin/sh

ANDROID_SDK=`getprop ro.build.version.sdk`

if [[ "$ANDROID_SDK" -gt 23 ]]
then
    echo 1
else
    echo 0
fi

