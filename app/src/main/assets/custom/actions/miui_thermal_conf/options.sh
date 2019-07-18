#!/system/bin/sh

platform=`getprop ro.board.platform`

if [[ "$platform" = "sdm845" ]] || [[ "$platform" = "msm8998" ]] || [[ "$platform" = "sdm710" ]] || [[ "$platform" = "msmnile" ]]
then
    echo 'default|系统默认 (凉)'
    echo 'high|提高阈值 (温)'
    echo 'high2|稳定性能 (热)'
    echo 'nolimits|极致性能 (烫)'
    echo 'danger|疯狂模式 (炸)'
fi

