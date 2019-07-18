#!/system/bin/sh

path="${MAGISK_PATH}/system/vendor/etc/thermal-engine.current.ini"
if [[ -f "${MAGISK_PATH}/system/vendor/etc/thermal-engine.current.ini" ]]
then
    mode=`cat $path`
else
    mode=''
fi

modename=""
case "$mode" in
    "default")
        modename="系统默认 (凉)"
     ;;
    "high")
        modename="提高阈值 (温)"
    ;;
    "high2")
        modename="稳定性能 (热)"
    ;;
    "nolimits")
        modename="极致性能 (烫)"
    ;;
    "danger")
        modename="疯狂模式 (炸)"
    ;;
    *)
        modename="未替换"
    ;;
esac

echo "本功能适用于骁龙835、845、855处理器，MIUI官方系统\n当前：$modename"