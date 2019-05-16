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
        modename="系统默认"
     ;;
    "high")
        modename="提高阈值"
    ;;
    "nolimits")
        modename="不限制性能"
    ;;
    "danger")
        modename="卍解"
    ;;
    *)
        modename="未替换"
    ;;
esac

echo "本功能适用于骁龙823、845、855处理器，MIUI官方系统\n当前：$modename"