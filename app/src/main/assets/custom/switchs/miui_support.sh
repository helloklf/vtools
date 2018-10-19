#!/system/bin/sh

if [[ -n `getprop ro.miui.ui.version.name` ]]
then
    echo 1
else
    echo 0
fi