#!/system/bin/sh

platform=`getprop ro.board.platform`

if [[ "$platform" = "sdm845" ]] || [[ "$platform" = "msm8998" ]] || [[ "$platform" = "sdm710" ]] || [[ "$platform" = "msmnile" ]]
then
    echo 'default|系统默认'
    echo 'high|提高阈值'
    echo 'nolimits|不限制性能'
    echo 'danger|卍解'
fi

