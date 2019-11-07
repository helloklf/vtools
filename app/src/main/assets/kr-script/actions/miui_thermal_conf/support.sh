#!/system/bin/sh

platform=`getprop ro.board.platform`

if [[ "$platform" = "sdm845" ]] || [[ "$platform" = "msm8998" ]] || [[ "$platform" = "msmnile" ]] || [[ "$platform" = "sdm710" ]]
then
    if [[ -n `getprop ro.miui.ui.version.name` ]]
    then
        echo 1
    else
        echo 0
    fi
else
    echo 0
fi