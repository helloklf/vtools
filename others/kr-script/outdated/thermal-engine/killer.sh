echo '尝试关闭一些可能存在于系统的温控服务，包括：'

echo 'thermald'
echo 'thermanager'
echo 'mpdecision'
echo 'thermal-engine'
echo 'mi_thermald'
echo 'thermalserviced'
echo 'stop vendor.qti.hardware.perf@1.0-service'
echo 'stop vendor.qti.hardware.perf@2.0-service'
echo 'android.hardware.thermal@1.0-service'

echo ''

stop thermanager 2>/dev/null
stop thermald 2>/dev/null
stop mpdecision 2>/dev/null
stop thermal-engine 2>/dev/null
stop vendor.qti.hardware.perf@1.0-service 2>/dev/null
stop vendor.qti.hardware.perf@2.0-service 2>/dev/null
stop android.hardware.thermal@1.0-service 2>/dev/null

if [[ -f /sys/module/msm_thermal/core_control/enabled ]]; then
    echo ''
    echo 'echo 0 > /sys/module/msm_thermal/core_control/enabled'
    echo 'echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled'
    echo 'echo N > /sys/module/msm_thermal/parameters/enabled'
    echo ''
    echo 0 > /sys/module/msm_thermal/core_control/enabled 2>/dev/null
    echo 0 > /sys/module/msm_thermal/vdd_restriction/enabled 2>/dev/null
    echo N > /sys/module/msm_thermal/parameters/enabled 2>/dev/null
fi

killall -9 vendor.qti.hardware.perf@1.0-service 2>/dev/null
killall -9 vendor.qti.hardware.perf@2.0-service 2>/dev/null
killall -9 android.hardware.thermal@1.0-service 2>/dev/null
killall -9 thermanager 2>/dev/null
killall -9 thermalserviced 2>/dev/null
killall -9 thermal-engine 2>/dev/null
killall -9 mi_thermald 2>/dev/null

echo '具体效果，以实际测试为准！'
echo '在845之后的机型上，可能并不会有效果！'
