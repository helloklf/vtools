#!/system/bin/sh

platform=`getprop ro.board.platform`

ANDROID_SDK=`getprop ro.build.version.sdk`

if [[ "$ANDROID_SDK" -gt 24 ]];then
    if [[ "$platform" = "sdm845" ]] || [[ "$platform" = "msm8998" ]] || [[ "$platform" = "msm8996" ]] || [[ "$platform" = "msmnile" ]] || [[ "$platform" = "sdm710" ]] || [[ "$platform" = "sm6150" ]] || [[ "$platform" = "kona" ]] || [[ "$platform" = "mt6873" ]]
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
else
    echo 0
fi